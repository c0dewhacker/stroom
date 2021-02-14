/*
 * Copyright 2016 Crown Copyright
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

import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.search.solr.shared.SolrIndexFieldType;
import stroom.util.shared.HasDisplayValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Wrapper for index field info
 * </p>
 */
@JsonInclude(Include.NON_DEFAULT)
@JsonPropertyOrder({
        "fieldUse",
        "fieldName",
        "fieldType",
        "stored",
        "indexed"
})
public class ElasticIndexField implements HasDisplayValue, Comparable<ElasticIndexField>, Serializable {
    private static final long serialVersionUID = 3100770758821157580L;

    private ElasticIndexFieldType fieldUse = ElasticIndexFieldType.TEXT;
    private String fieldName;
    private String fieldType;
    private boolean stored;
    private boolean indexed = true;

    /**
     * Defines a list of the {@link Condition} values supported by this field,
     * can be null in which case a default set will be returned. Not persisted
     * in the XML
     */
    @JsonIgnore
    @XmlTransient
    private List<Condition> supportedConditions;

    public ElasticIndexField() {
        // Default constructor necessary for GWT serialisation.
    }

    private ElasticIndexField(final ElasticIndexFieldType fieldUse,
                              final String fieldName,
                              final boolean stored,
                              final boolean indexed,
                              final boolean termPositions,
                              final List<Condition> supportedConditions) {
        setFieldUse(fieldUse);
        setFieldName(fieldName);
        setStored(stored);
        setIndexed(indexed);

        if (supportedConditions != null) {
            this.supportedConditions = new ArrayList<>(supportedConditions);
        }
    }

    public void setFieldUse(final ElasticIndexFieldType fieldUse) {
        if (fieldUse == null) {
            this.fieldUse = ElasticIndexFieldType.TEXT;
        }

        this.fieldUse = fieldUse;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(final String fieldType) {
        this.fieldType = fieldType;
    }

    public boolean isStored() {
        return stored;
    }

    public void setStored(final boolean stored) {
        this.stored = stored;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(final boolean indexed) {
        this.indexed = indexed;
    }

    public List<Condition> getSupportedConditions() {
        if (supportedConditions == null) {
            return getDefaultConditions();
        } else {
            return supportedConditions;
        }
    }

    public void setSupportedConditions(final List<Condition> supportedConditions) {
        if (supportedConditions == null) {
            this.supportedConditions = null;
        } else {
            this.supportedConditions = new ArrayList<>(supportedConditions);
        }
    }

    /*
    Factory methods
     */

    public static ElasticIndexField createIdField(final String fieldName) {
        return new ElasticIndexField(ElasticIndexFieldType.ID, fieldName, true, true, false, null);
    }

    public static ElasticIndexField createBooleanField(final String fieldName) {
        return new ElasticIndexField(ElasticIndexFieldType.BOOLEAN, fieldName, false, true, false, null);
    }

    public static ElasticIndexField createNumericField(final String fieldName) {
        return new ElasticIndexField(ElasticIndexFieldType.NUMBER, fieldName, false, true, false, null);
    }

    public static ElasticIndexField createDateField(final String fieldName) {
        return new ElasticIndexField(ElasticIndexFieldType.DATE, fieldName, false, true, false, null);
    }

    public static ElasticIndexField createArrayField(final String fieldName) {
        return new ElasticIndexField(ElasticIndexFieldType.ARRAY, fieldName, false, true, false, null);
    }

    public static ElasticIndexField createTextField(final String fieldName) {
        return new ElasticIndexField(ElasticIndexFieldType.TEXT, fieldName, false, true, false, null);
    }

    public ElasticIndexFieldType getFieldUse() {
        return fieldUse;
    }

    @JsonIgnore
    @Override
    public String getDisplayValue() {
        return fieldName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ElasticIndexField)) return false;
        final ElasticIndexField that = (ElasticIndexField) o;
        return stored == that.stored &&
                indexed == that.indexed &&
                fieldUse == that.fieldUse &&
                Objects.equals(fieldName, that.fieldName) &&
                Objects.equals(fieldType, that.fieldType) &&
                Objects.equals(supportedConditions, that.supportedConditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldUse, fieldName, fieldType, stored, indexed, supportedConditions);
    }

    @Override
    public String toString() {
        return fieldName;
    }

    @Override
    public int compareTo(final ElasticIndexField o) {
        return fieldName.compareToIgnoreCase(o.fieldName);
    }

    private List<Condition> getDefaultConditions() {
        final List<Condition> conditions = new ArrayList<>();

        if (fieldUse != null) {
            // First make sure the operator is set.
            switch (fieldUse) {
                case ID:
                    conditions.add(Condition.EQUALS);
                    conditions.add(Condition.IN);
                    conditions.add(Condition.IN_DICTIONARY);
                    break;
                case TEXT:
                    conditions.add(Condition.EQUALS);
                    conditions.add(Condition.IN);
                    conditions.add(Condition.IN_DICTIONARY);
                    break;
                case DATE:
                    conditions.add(Condition.EQUALS);
                    conditions.add(Condition.GREATER_THAN);
                    conditions.add(Condition.GREATER_THAN_OR_EQUAL_TO);
                    conditions.add(Condition.LESS_THAN);
                    conditions.add(Condition.LESS_THAN_OR_EQUAL_TO);
                    conditions.add(Condition.BETWEEN);
                    conditions.add(Condition.IN);
                    conditions.add(Condition.IN_DICTIONARY);
                    break;
                default:
                    if (fieldUse.isNumeric()) {
                        conditions.add(Condition.EQUALS);
                        conditions.add(Condition.GREATER_THAN);
                        conditions.add(Condition.GREATER_THAN_OR_EQUAL_TO);
                        conditions.add(Condition.LESS_THAN);
                        conditions.add(Condition.LESS_THAN_OR_EQUAL_TO);
                        conditions.add(Condition.BETWEEN);
                        conditions.add(Condition.IN);
                        conditions.add(Condition.IN_DICTIONARY);
                    }
                    break;
            }
        }

        return conditions;
    }
}
