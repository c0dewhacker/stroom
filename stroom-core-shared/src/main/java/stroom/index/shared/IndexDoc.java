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

package stroom.index.shared;

import stroom.docref.DocRef;
import stroom.docref.HasDisplayValue;
import stroom.docstore.shared.Doc;
import stroom.svg.shared.SvgImage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({
        "type",
        "uuid",
        "name",
        "version",
        "createTimeMs",
        "updateTimeMs",
        "createUser",
        "updateUser",
        "description",
        "maxDocsPerShard",
        "partitionBy",
        "partitionSize",
        "shardsPerPartition",
        "retentionDayAge",
        "fields",
        "timeField",
        "volumeGroupName",
        "defaultExtractionPipeline"})
@JsonInclude(Include.NON_NULL)
public class IndexDoc extends Doc {

    public static final int DEFAULT_MAX_DOCS_PER_SHARD = 1000000000;
    private static final int DEFAULT_SHARDS_PER_PARTITION = 1;
    private static final PartitionBy DEFAULT_PARTITION_BY = PartitionBy.MONTH;
    private static final int DEFAULT_PARTITION_SIZE = 1;
    private static final String DEFAULT_TIME_FIELD = "EventTime";

    public static final String DOCUMENT_TYPE = "Index";
    public static final SvgImage ICON = SvgImage.DOCUMENT_INDEX;

    @JsonProperty
    private String description;
    @JsonProperty
    private Integer maxDocsPerShard;
    @JsonProperty
    private PartitionBy partitionBy;
    @JsonProperty
    private Integer partitionSize;
    @JsonProperty
    private Integer shardsPerPartition;
    @JsonProperty
    private Integer retentionDayAge;
    @JsonProperty
    private List<IndexField> fields;
    @JsonProperty
    private String timeField;
    @JsonProperty
    private String volumeGroupName;
    @JsonProperty
    private DocRef defaultExtractionPipeline;

    public IndexDoc() {
        maxDocsPerShard = DEFAULT_MAX_DOCS_PER_SHARD;
        partitionBy = DEFAULT_PARTITION_BY;
        partitionSize = DEFAULT_PARTITION_SIZE;
        shardsPerPartition = DEFAULT_SHARDS_PER_PARTITION;
        timeField = DEFAULT_TIME_FIELD;
    }

    @JsonCreator
    public IndexDoc(@JsonProperty("type") final String type,
                    @JsonProperty("uuid") final String uuid,
                    @JsonProperty("name") final String name,
                    @JsonProperty("version") final String version,
                    @JsonProperty("createTimeMs") final Long createTimeMs,
                    @JsonProperty("updateTimeMs") final Long updateTimeMs,
                    @JsonProperty("createUser") final String createUser,
                    @JsonProperty("updateUser") final String updateUser,
                    @JsonProperty("description") final String description,
                    @JsonProperty("maxDocsPerShard") final Integer maxDocsPerShard,
                    @JsonProperty("partitionBy") final PartitionBy partitionBy,
                    @JsonProperty("partitionSize") final Integer partitionSize,
                    @JsonProperty("shardsPerPartition") final Integer shardsPerPartition,
                    @JsonProperty("retentionDayAge") final Integer retentionDayAge,
                    @JsonProperty("fields") final List<IndexField> fields,
                    @JsonProperty("timeField") final String timeField,
                    @JsonProperty("volumeGroupName") final String volumeGroupName,
                    @JsonProperty("defaultExtractionPipeline") final DocRef defaultExtractionPipeline) {
        super(type, uuid, name, version, createTimeMs, updateTimeMs, createUser, updateUser);
        this.description = description;
        this.maxDocsPerShard = maxDocsPerShard;
        this.partitionBy = partitionBy;
        this.partitionSize = partitionSize;
        this.shardsPerPartition = shardsPerPartition;
        this.retentionDayAge = retentionDayAge;
        this.fields = fields;
        this.timeField = timeField;
        this.volumeGroupName = volumeGroupName;
        this.defaultExtractionPipeline = defaultExtractionPipeline;

        if (this.maxDocsPerShard == null) {
            this.maxDocsPerShard = DEFAULT_MAX_DOCS_PER_SHARD;
        }
        if (this.partitionBy == null) {
            this.partitionBy = DEFAULT_PARTITION_BY;
        }
        if (this.partitionSize == null) {
            this.partitionSize = DEFAULT_PARTITION_SIZE;
        }
        if (this.shardsPerPartition == null) {
            this.shardsPerPartition = DEFAULT_SHARDS_PER_PARTITION;
        }
    }

    /**
     * @return A new {@link DocRef} for this document's type with the supplied uuid.
     */
    public static DocRef getDocRef(final String uuid) {
        return DocRef.builder(DOCUMENT_TYPE)
                .uuid(uuid)
                .build();
    }

    /**
     * @return A new builder for creating a {@link DocRef} for this document's type.
     */
    public static DocRef.TypedBuilder buildDocRef() {
        return DocRef.builder(DOCUMENT_TYPE);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Integer getMaxDocsPerShard() {
        return maxDocsPerShard;
    }

    public void setMaxDocsPerShard(final Integer maxDocsPerShard) {
        this.maxDocsPerShard = maxDocsPerShard;
    }

    public PartitionBy getPartitionBy() {
        return partitionBy;
    }

    public void setPartitionBy(final PartitionBy partitionBy) {
        this.partitionBy = partitionBy;
    }

    public Integer getPartitionSize() {
        return partitionSize;
    }

    public void setPartitionSize(final Integer partitionSize) {
        this.partitionSize = partitionSize;
    }

    public Integer getShardsPerPartition() {
        return shardsPerPartition;
    }

    public void setShardsPerPartition(final Integer shardsPerPartition) {
        this.shardsPerPartition = shardsPerPartition;
    }

    public Integer getRetentionDayAge() {
        return retentionDayAge;
    }

    public void setRetentionDayAge(final Integer retentionDayAge) {
        this.retentionDayAge = retentionDayAge;
    }

    public List<IndexField> getFields() {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        return fields;
    }

    public void setFields(final List<IndexField> fields) {
        this.fields = fields;
    }

    public String getTimeField() {
        return timeField;
    }

    public void setTimeField(final String timeField) {
        this.timeField = timeField;
    }

    public String getVolumeGroupName() {
        return volumeGroupName;
    }

    public void setVolumeGroupName(String volumeGroupName) {
        this.volumeGroupName = volumeGroupName;
    }

    public DocRef getDefaultExtractionPipeline() {
        return defaultExtractionPipeline;
    }

    public void setDefaultExtractionPipeline(final DocRef defaultExtractionPipeline) {
        this.defaultExtractionPipeline = defaultExtractionPipeline;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final IndexDoc indexDoc = (IndexDoc) o;
        return maxDocsPerShard == indexDoc.maxDocsPerShard &&
                partitionSize == indexDoc.partitionSize &&
                shardsPerPartition == indexDoc.shardsPerPartition &&
                Objects.equals(description, indexDoc.description) &&
                partitionBy == indexDoc.partitionBy &&
                Objects.equals(timeField, indexDoc.timeField) &&
                Objects.equals(retentionDayAge, indexDoc.retentionDayAge) &&
                Objects.equals(fields, indexDoc.fields) &&
                Objects.equals(volumeGroupName, indexDoc.volumeGroupName) &&
                Objects.equals(defaultExtractionPipeline, indexDoc.defaultExtractionPipeline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                description,
                maxDocsPerShard,
                partitionBy,
                partitionSize,
                shardsPerPartition,
                retentionDayAge,
                fields,
                timeField,
                volumeGroupName,
                defaultExtractionPipeline);
    }

    public enum PartitionBy implements HasDisplayValue {
        DAY("Day"),
        WEEK("Week"),
        MONTH("Month"),
        YEAR("Year");

        private final String displayValue;

        PartitionBy(final String displayValue) {
            this.displayValue = displayValue;
        }

        @Override
        public String getDisplayValue() {
            return displayValue;
        }
    }
}
