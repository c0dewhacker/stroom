/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.index.impl;

import stroom.docref.DocRef;
import stroom.index.shared.AllPartition;
import stroom.index.shared.IndexField;
import stroom.index.shared.IndexFieldType;
import stroom.index.shared.IndexFieldsMap;
import stroom.index.shared.IndexShardKey;
import stroom.index.shared.Partition;
import stroom.index.shared.TimePartition;
import stroom.pipeline.LocationFactoryProxy;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.errorhandler.ErrorStatistics;
import stroom.pipeline.errorhandler.LoggedException;
import stroom.pipeline.factory.ConfigurableElement;
import stroom.pipeline.factory.PipelineProperty;
import stroom.pipeline.factory.PipelinePropertyDocRef;
import stroom.pipeline.filter.AbstractXMLFilter;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.pipeline.state.MetaHolder;
import stroom.query.language.functions.ValString;
import stroom.search.extraction.FieldValue;
import stroom.svg.shared.SvgImage;
import stroom.util.CharBuffer;
import stroom.util.date.DateUtil;
import stroom.util.shared.Severity;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * The index filter... takes the index XML and builds the LUCENE documents
 */
@ConfigurableElement(
        type = "IndexingFilter",
        category = Category.FILTER,
        description = """
                A filter to send source data to an index.
                """,
        roles = {
                PipelineElementType.ROLE_TARGET,
                PipelineElementType.ROLE_HAS_TARGETS,
                PipelineElementType.VISABILITY_SIMPLE},
        icon = SvgImage.PIPELINE_INDEX)
class IndexingFilter extends AbstractXMLFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingFilter.class);

    private static final String RECORD = "record";
    private static final String DATA = "data";
    private static final String NAME = "name";
    private static final String VALUE = "value";

    private final MetaHolder metaHolder;
    private final LocationFactoryProxy locationFactory;
    private final Indexer indexer;
    private final ErrorReceiverProxy errorReceiverProxy;
    private final IndexStructureCache indexStructureCache;
    private final CharBuffer debugBuffer = new CharBuffer(10);
    private IndexFieldsMap indexFieldsMap;
    private DocRef indexRef;
    private stroom.index.shared.IndexDoc index;
    private final TimePartitionFactory timePartitionFactory = new TimePartitionFactory();
    private final TreeMap<Long, TimePartition> timePartitionTreeMap = new TreeMap<>();
    private final Map<Partition, IndexShardKey> indexShardKeyMap = new HashMap<>();
    private IndexDocument document;

    private Locator locator;

    private Long currentEventTime;
    private Partition defaultPartition;

    @Inject
    IndexingFilter(final MetaHolder metaHolder,
                   final LocationFactoryProxy locationFactory,
                   final Indexer indexer,
                   final ErrorReceiverProxy errorReceiverProxy,
                   final IndexStructureCache indexStructureCache) {
        this.metaHolder = metaHolder;
        this.locationFactory = locationFactory;
        this.indexer = indexer;
        this.errorReceiverProxy = errorReceiverProxy;
        this.indexStructureCache = indexStructureCache;
    }

    /**
     * Initialise
     */
    @Override
    public void startProcessing() {
        try {
            if (indexRef == null) {
                log(Severity.FATAL_ERROR, "Index has not been set", null);
                throw LoggedException.create("Index has not been set");
            }

            // Get the index and index fields from the cache.
            final IndexStructure indexStructure = indexStructureCache.get(indexRef);
            if (indexStructure == null) {
                log(Severity.FATAL_ERROR, "Unable to load index", null);
                throw LoggedException.create("Unable to load index");
            }

            index = indexStructure.getIndex();
            indexFieldsMap = indexStructure.getIndexFieldsMap();

            // Create a key to create shards with.
            if (metaHolder == null || metaHolder.getMeta() == null || index.getPartitionBy() == null) {
                // Many tests don't use streams so where this is the case just
                // create a basic key.
                final Partition partition = AllPartition.INSTANCE;
                defaultPartition = partition;
                final IndexShardKey indexShardKey = IndexShardKeyUtil.createKey(index, partition);
                indexShardKeyMap.put(indexShardKey.getPartition(), indexShardKey);
            } else {
                final long metaCreateMs = metaHolder.getMeta().getCreateMs();
                final TimePartition timePartition = timePartitionFactory.create(index, metaCreateMs);
                defaultPartition = timePartition;
                final IndexShardKey indexShardKey = IndexShardKeyUtil.createKey(index, timePartition);
                indexShardKeyMap.put(indexShardKey.getPartition(), indexShardKey);
                timePartitionTreeMap.put(timePartition.getPartitionFromTime(), timePartition);
            }
        } finally {
            super.startProcessing();
        }
    }

    /**
     * Sets the locator to use when reporting errors.
     *
     * @param locator The locator to use.
     */
    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator;
        super.setDocumentLocator(locator);
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
            throws SAXException {
        if (DATA.equals(localName) && document != null) {
            String name = atts.getValue(NAME);
            String value = atts.getValue(VALUE);
            if (name != null && value != null) {
                name = name.trim();
                value = value.trim();

                if (name.length() > 0 && value.length() > 0) {
                    // See if we can get this field.
                    final IndexField indexField = indexFieldsMap.get(name);
                    if (indexField != null) {
                        // Index the current content if we are to store or index
                        // this field.
                        if (indexField.isIndexed() || indexField.isStored()) {
                            processIndexContent(indexField, value);
                        }
                    } else {
                        log(Severity.WARNING, "Attempt to index unknown field: " + name, null);
                    }
                }
            }
        } else if (RECORD.equals(localName)) {
            // Create a document to store fields in.
            document = new IndexDocument();
        }

        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (RECORD.equals(localName)) {
            processDocument();
            document = null;
            currentEventTime = null;

            if (errorReceiverProxy.getErrorReceiver() != null
                    && errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics) {
                ((ErrorStatistics) errorReceiverProxy.getErrorReceiver()).checkRecord(-1);
            }
        }

        super.endElement(uri, localName, qName);
    }

    private void processDocument() {
        // Write the document if we have dropped out of the record element and
        // have indexed some fields.
        if (document.getValues().size() > 0) {
            try {
                Partition partition = defaultPartition;
                if (currentEventTime != null) {
                    final Entry<Long, TimePartition> entry = timePartitionTreeMap.floorEntry(currentEventTime);
                    if (entry != null &&
                            entry.getValue().getPartitionFromTime() <= currentEventTime &&
                            entry.getValue().getPartitionToTime() > currentEventTime) {
                        partition = entry.getValue();

                    } else {
                        final TimePartition timePartition = timePartitionFactory.create(index, currentEventTime);
                        timePartitionTreeMap.put(timePartition.getPartitionFromTime(), timePartition);
                        partition = timePartition;
                    }
                }

                final IndexShardKey indexShardKey =
                        indexShardKeyMap.computeIfAbsent(partition, k -> IndexShardKeyUtil.createKey(index, k));

                indexer.addDocument(indexShardKey, document);
            } catch (final RuntimeException e) {
                log(Severity.FATAL_ERROR, e.getMessage(), e);
                // Terminate processing as this is a fatal error.
                throw LoggedException.wrap(e);
            }
        }
    }

    private void processIndexContent(final IndexField indexField, final String value) {
        try {
            if (currentEventTime == null &&
                    IndexFieldType.DATE_FIELD.equals(indexField.getFieldType()) &&
                    indexField.getFieldName().equals(index.getTimeField())) {
                try {
                    // Set the current event time if this is a recognised event time field.
                    currentEventTime = DateUtil.parseUnknownString(value);
                } catch (final RuntimeException e) {
                    LOGGER.trace(e.getMessage(), e);
                }
            }


//            Field field = null;
//
//            if (IndexFieldType.INTEGER_FIELD.equals(indexField.getFieldType())) {
//                FieldValue fieldValue = new FieldValue(indexField, ValString.create(value));
//
//                try {
//                    final int val = Integer.parseInt(value);
//                    field = FieldFactory.createInt(indexField, val);
//                } catch (final Exception e) {
//                    LOGGER.trace(e.getMessage(), e);
//                }
//            } else if (IndexFieldType.LONG_FIELD.equals(indexField.getFieldType())) {
//                try {
//                    final long val = Long.parseLong(value);
//                    field = FieldFactory.create(indexField, val);
//                } catch (final Exception e) {
//                    LOGGER.trace(e.getMessage(), e);
//                }
//            } else if (IndexFieldType.FLOAT_FIELD.equals(indexField.getFieldType())) {
//                try {
//                    final float val = Float.parseFloat(value);
//                    field = FieldFactory.createFloat(indexField, val);
//                } catch (final Exception e) {
//                    LOGGER.trace(e.getMessage(), e);
//                }
//            } else if (IndexFieldType.DOUBLE_FIELD.equals(indexField.getFieldType())) {
//                try {
//                    final double val = Double.parseDouble(value);
//                    field = FieldFactory.createDouble(indexField, val);
//                } catch (final Exception e) {
//                    LOGGER.trace(e.getMessage(), e);
//                }
//            } else if (IndexFieldType.DATE_FIELD.equals(indexField.getFieldType())) {
//                try {
//                    final long val = DateUtil.parseUnknownString(value);
//
//                    // Set the current event time if this is a recognised event time field.
//                    if (currentEventTime == null && indexField.getFieldName().equals(index.getTimeField())) {
//                        currentEventTime = val;
//                    }
//
//                    field = FieldFactory.create(indexField, val);
//                } catch (final RuntimeException e) {
//                    LOGGER.trace(e.getMessage(), e);
//                }
//            } else if (indexField.getFieldType().isNumeric()) {
//                try {
//                    final long val = Long.parseLong(value);
//                    field = FieldFactory.create(indexField, val);
//                } catch (final Exception e) {
//                    LOGGER.trace(e.getMessage(), e);
//                }
//            } else {
//                field = FieldFactory.create(indexField, value);
//            }

            // Add the current field to the document if it is not null.
            final FieldValue fieldValue = new FieldValue(indexField, ValString.create(value));

            // Output some debug.
            if (LOGGER.isDebugEnabled()) {
                debugBuffer.append("processIndexContent() - Adding to index indexName=");
                debugBuffer.append(indexRef.getName());
                debugBuffer.append(" name=");
                debugBuffer.append(indexField.getFieldName());
                debugBuffer.append(" value=");
                debugBuffer.append(value);

                final String debug = debugBuffer.toString();
                debugBuffer.clear();

                LOGGER.debug(debug);
            }

            document.add(fieldValue);

        } catch (final RuntimeException e) {
            log(Severity.ERROR, e.getMessage(), e);
        }
    }

    @PipelineProperty(description = "The index to send records to.", displayPriority = 1)
    @PipelinePropertyDocRef(types = stroom.index.shared.IndexDoc.DOCUMENT_TYPE)
    public void setIndex(final DocRef indexRef) {
        this.indexRef = indexRef;
    }

    private void log(final Severity severity, final String message, final Exception e) {
        errorReceiverProxy.log(severity, locationFactory.create(locator), getElementId(), message, e);
    }
}
