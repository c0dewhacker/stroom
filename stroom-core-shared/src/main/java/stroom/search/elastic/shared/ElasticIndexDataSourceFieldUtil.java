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

package stroom.search.elastic.shared;

import stroom.datasource.api.v2.BooleanField;
import stroom.datasource.api.v2.ConditionSet;
import stroom.datasource.api.v2.DateField;
import stroom.datasource.api.v2.DoubleField;
import stroom.datasource.api.v2.FloatField;
import stroom.datasource.api.v2.IdField;
import stroom.datasource.api.v2.IntegerField;
import stroom.datasource.api.v2.IpV4AddressField;
import stroom.datasource.api.v2.KeywordField;
import stroom.datasource.api.v2.LongField;
import stroom.datasource.api.v2.QueryField;
import stroom.datasource.api.v2.TextField;

import java.util.List;
import java.util.stream.Collectors;

public final class ElasticIndexDataSourceFieldUtil {

    public static List<QueryField> getDataSourceFields(final ElasticIndexDoc index) {
        if (index == null || index.getFields() == null) {
            return null;
        }

        return index.getFields()
                .stream()
                .map(ElasticIndexDataSourceFieldUtil::convert)
                .collect(Collectors.toList());
    }

    private static QueryField convert(final ElasticIndexField field) {
        final ElasticIndexFieldType fieldType = field.getFieldUse();
        final String fieldName = field.getFieldName();
        final ConditionSet supportedConditions = fieldType.getSupportedConditions();
        switch (fieldType) {
            case ID:
                return new IdField(fieldName, supportedConditions, null, field.isIndexed());
            case BOOLEAN:
                return new BooleanField(fieldName, supportedConditions, null, field.isIndexed());
            case INTEGER:
                return new IntegerField(fieldName, supportedConditions, null, field.isIndexed());
            case LONG:
                return new LongField(fieldName, supportedConditions, null, field.isIndexed());
            case FLOAT:
                return new FloatField(fieldName, supportedConditions, null, field.isIndexed());
            case DOUBLE:
                return new DoubleField(fieldName, supportedConditions, null, field.isIndexed());
            case DATE:
                return new DateField(fieldName, supportedConditions, null, field.isIndexed());
            case TEXT:
                return new TextField(fieldName, supportedConditions, null, field.isIndexed());
            case KEYWORD:
                return new KeywordField(fieldName, supportedConditions, null, field.isIndexed());
            case IPV4_ADDRESS:
                return new IpV4AddressField(fieldName, supportedConditions, null, field.isIndexed());
        }
        return null;
    }
}
