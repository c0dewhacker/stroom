/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.query.common.v2;

import stroom.dashboard.expression.v1.FieldIndex;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ref.ErrorConsumer;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.FlatResult;
import stroom.query.api.v2.FlatResultBuilder;
import stroom.query.api.v2.Format.Type;
import stroom.query.api.v2.OffsetRange;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.Result;
import stroom.query.api.v2.ResultBuilder;
import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchRequestSource;
import stroom.query.api.v2.TableSettings;
import stroom.query.api.v2.TimeFilter;
import stroom.query.common.v2.format.FieldFormatter;
import stroom.query.util.LambdaLogger;
import stroom.query.util.LambdaLoggerFactory;
import stroom.util.concurrent.UncheckedInterruptedException;
import stroom.util.logging.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FlatResultCreator implements ResultCreator {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(FlatResultCreator.class);

    private final FieldFormatter fieldFormatter;
    private final List<Mapper> mappers;
    private final List<Field> fields;

    private final ErrorConsumer errorConsumer = new ErrorConsumerImpl();

    public FlatResultCreator(final DataStoreFactory dataStoreFactory,
                             final SearchRequest searchRequest,
                             final String componentId,
                             final ResultRequest resultRequest,
                             final Map<String, String> paramMap,
                             final FieldFormatter fieldFormatter,
                             final Sizes defaultMaxResultsSizes) {
        this.fieldFormatter = fieldFormatter;

        // User may have added a vis pane but not defined the vis
        final List<TableSettings> tableSettings = resultRequest.getMappings()
                .stream()
                .filter(Objects::nonNull)
                .toList();

        if (tableSettings.size() > 1) {
            mappers = new ArrayList<>(tableSettings.size() - 1);
            for (int i = 0; i < tableSettings.size() - 1; i++) {
                final TableSettings parent = tableSettings.get(i);
                final TableSettings child = tableSettings.get(i + 1);

                // Create a set of sizes that are the minimum values for the combination of user provided sizes for the
                // parent table and the default maximum sizes.
                final Sizes sizes = Sizes.min(Sizes.create(parent.getMaxResults()), defaultMaxResultsSizes);
                final int maxItems = sizes.size(0);

                final List<Integer> childMaxResults = child != null
                        ? child.getMaxResults()
                        : Collections.emptyList();
                final Sizes maxResults = Sizes.create(childMaxResults, Integer.MAX_VALUE);
                final DataStoreSettings dataStoreSettings = DataStoreSettings
                        .createBasicSearchResultStoreSettings()
                        .copy()
                        .maxResults(maxResults)
                        .storeSize(Sizes.create(Integer.MAX_VALUE))
                        .build();


                mappers.add(new Mapper(
                        dataStoreFactory,
                        dataStoreSettings,
                        searchRequest.getSearchRequestSource(),
                        searchRequest.getKey(),
                        componentId,
                        parent,
                        child,
                        paramMap,
                        maxItems,
                        errorConsumer));
            }
        } else {
            LOGGER.debug(() -> LogUtil.message(
                    "Invalid non-null tableSettings count ({}) for search: {} and componentId: {}",
                    tableSettings.size(),
                    searchRequest.getKey(),
                    componentId));
            errorConsumer.add(() -> LogUtil.message(
                    "Component with ID: '{}' has not been configured correctly so will not show any data.",
                    componentId));
            mappers = Collections.emptyList();
        }

        final TableSettings child = tableSettings.get(tableSettings.size() - 1);

        fields = child != null
                ? child.getFields()
                : Collections.emptyList();
    }

    private List<Object> toNodeKey(final Map<Integer, List<Field>> groupFields, final Key key) {
        if (key == null || key.getKeyParts().size() == 0) {
            return null;
        }

        if (!key.isGrouped()) {
            return null;
        }

        int depth = 0;
        final List<Object> result = new ArrayList<>(key.getKeyParts().size());
        for (final KeyPart keyPart : key.getKeyParts()) {
            final Val[] values = keyPart.getGroupValues();

            if (values.length == 0) {
                result.add(null);
            } else if (values.length == 1) {
                final Val val = values[0];
                if (val == null) {
                    result.add(null);
                } else {
                    Field field = null;

                    final List<Field> fields = groupFields.get(depth);
                    if (fields != null) {
                        field = fields.get(0);
                    }

                    result.add(convert(field, val));
                }

            } else {
                final StringBuilder sb = new StringBuilder();
                for (Val val : values) {
                    if (val != null) {
                        sb.append(val);
                    }
                    sb.append("|");
                }
                sb.setLength(sb.length() - 1);
                result.add(sb.toString());
            }

            depth++;
        }

        return result;
    }

    @Override
    public Result create(final DataStore dataStore, final ResultRequest resultRequest) {
        FlatResultBuilder flatResultBuilder = FlatResult.builder();
        create(dataStore, resultRequest, flatResultBuilder);
        return flatResultBuilder.build();
    }

    @Override
    public void create(final DataStore dataStore,
                       final ResultRequest resultRequest,
                       final ResultBuilder<?> resultBuilder) {
        final FlatResultBuilder flatResultBuilder = (FlatResultBuilder) resultBuilder;

        if (!errorConsumer.hasErrors()) {
            try {
                // Map data.
                DataStore mappedDataStore = dataStore;
                for (final Mapper mapper : mappers) {
                    mappedDataStore = mapper.map(mappedDataStore, resultRequest.getTimeFilter());
                }

                final Map<Integer, List<Field>> groupFields = new HashMap<>();
                for (final Field field : fields) {
                    if (field.getGroup() != null) {
                        groupFields.computeIfAbsent(field.getGroup(), k ->
                                        new ArrayList<>())
                                .add(field);
                    }
                }

                // Get top level items.
                mappedDataStore.fetch(
                        resultRequest.getRequestedRange(),
                        OpenGroups.ALL,
                        resultRequest.getTimeFilter(),
                        IdentityItemMapper.INSTANCE,
                        item -> {
                            final List<Object> resultList = new ArrayList<>(fields.size() + 3);

                            final Key key = item.getKey();
                            if (key != null) {
                                resultList.add(toNodeKey(groupFields, key.getParent()));
                                resultList.add(toNodeKey(groupFields, key));
                                resultList.add(key.getDepth());
                            } else {
                                resultList.add(null);
                                resultList.add(null);
                                resultList.add(0);
                            }

                            // Convert all list into fully resolved objects evaluating
                            // functions where necessary.
                            int i = 0;
                            for (final Field field : fields) {
                                final Val val = item.getValue(i);
                                Object result = null;
                                if (val != null) {
                                    // Convert all list into fully resolved
                                    // objects evaluating functions where necessary.
                                    if (fieldFormatter != null) {
                                        result = fieldFormatter.format(field, val);
                                    } else {
                                        result = convert(field, val);
                                    }
                                }

                                resultList.add(result);
                                i++;
                            }

                            // Add the values.
                            flatResultBuilder.addValues(resultList);
                        },
                        flatResultBuilder::totalResults);

                final List<Field> structure = new ArrayList<>();
                structure.add(Field.builder().name(":ParentKey").build());
                structure.add(Field.builder().name(":Key").build());
                structure.add(Field.builder().name(":Depth").build());
                structure.addAll(this.fields);

                flatResultBuilder
                        .componentId(resultRequest.getComponentId())
                        .errors(errorConsumer.getErrors())
                        .structure(structure);

            } catch (final UncheckedInterruptedException e) {
                LOGGER.debug(e::getMessage, e);
            } catch (final Exception e) {
                LOGGER.error(() ->
                        LogUtil.message(
                                "Error creating result for resultRequest {}", resultRequest.getComponentId()), e);
                errorConsumer.add(e);
            }
        }

        flatResultBuilder
                .componentId(resultRequest.getComponentId())
                .errors(errorConsumer.getErrors());
    }

    // TODO : Replace this with conversion at the item level.
    private Object convert(final Field field, final Val val) {
        if (field != null && field.getFormat() != null && field.getFormat().getType() != null) {
            final Type type = field.getFormat().getType();
            if (Type.NUMBER.equals(type) || Type.DATE_TIME.equals(type)) {
                return val.toDouble();
            }
        }

        return val.toString();
    }

    private static class Mapper {

        private final DataStoreFactory dataStoreFactory;
        private final DataStoreSettings dataStoreSettings;
        private final SearchRequestSource searchRequestSource;
        private final QueryKey queryKey;
        private final String componentId;
        private final TableSettings child;
        private final Map<String, String> paramMap;
        private final int maxItems;
        private final ErrorConsumer errorConsumer;
        private final FieldIndex childFieldIndex;

        private final int[] parentFieldIndices;

        Mapper(final DataStoreFactory dataStoreFactory,
               final DataStoreSettings dataStoreSettings,
               final SearchRequestSource searchRequestSource,
               final QueryKey queryKey,
               final String componentId,
               final TableSettings parent,
               final TableSettings child,
               final Map<String, String> paramMap,
               final int maxItems,
               final ErrorConsumer errorConsumer) {
            this.dataStoreFactory = dataStoreFactory;
            this.dataStoreSettings = dataStoreSettings;
            this.searchRequestSource = searchRequestSource;
            this.queryKey = queryKey;
            this.componentId = componentId;
            this.child = child;
            this.paramMap = paramMap;
            this.maxItems = maxItems;
            this.errorConsumer = errorConsumer;

            final FieldIndex parentFieldIndex = new FieldIndex();

            // Parent fields are now table column names.
            for (final Field field : parent.getFields()) {
                parentFieldIndex.create(field.getName());
            }

            // Extract child fields from expressions.
            childFieldIndex = new FieldIndex();
            final List<Field> childFields = child != null
                    ? child.getFields()
                    : Collections.emptyList();
            CompiledFields.create(childFields, childFieldIndex, paramMap);

            // Create the index mapping.
            parentFieldIndices = new int[childFieldIndex.size()];
            for (int i = 0; i < childFieldIndex.size(); i++) {
                final String childField = childFieldIndex.getField(i);
                final Integer parentIndex = parentFieldIndex.getPos(childField);
                parentFieldIndices[i] = Objects.requireNonNullElse(parentIndex, -1);
            }
        }

        public DataStore map(final DataStore dataStore, final TimeFilter timeFilter) {
            // Create a set of max result sizes that are determined by the supplied max results or default to integer
            // max value.
            final DataStore childDataStore = dataStoreFactory.create(
                    searchRequestSource,
                    queryKey,
                    componentId,
                    child,
                    childFieldIndex,
                    paramMap,
                    dataStoreSettings,
                    errorConsumer);

            // Get top level items.
            // TODO : Add an option to get detail level items rather than root level items.
            dataStore.fetch(
                    new OffsetRange(0, maxItems),
                    OpenGroups.ALL,
                    timeFilter,
                    IdentityItemMapper.INSTANCE,
                    item -> {
                        final Val[] values = new Val[parentFieldIndices.length];
                        for (int i = 0; i < parentFieldIndices.length; i++) {
                            final int index = parentFieldIndices[i];
                            if (index != -1) {
                                // TODO : @66 Currently evaluating more values than will be needed.
                                final Val val = item.getValue(index);
                                values[i] = val;
                            }
                        }
                        childDataStore.add(Val.of(values));
                    },
                    null);

            return childDataStore;
        }
    }
}
