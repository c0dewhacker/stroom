package stroom.query.api.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
@JsonInclude(Include.NON_NULL)
public class ResultStoreInfo {

    @JsonProperty
    private final QueryKey queryKey;
    @JsonProperty
    private final String userId;
    @JsonProperty
    private final long creationTime;
    @JsonProperty
    private final String nodeName;
    @JsonProperty
    private final long storeSize;
    @JsonProperty
    private final boolean complete;
    @JsonProperty
    private final SearchTaskProgress taskProgress;
    @JsonProperty
    private final ResultStoreSettings resultStoreSettings;

    @JsonCreator
    public ResultStoreInfo(@JsonProperty("queryKey") final QueryKey queryKey,
                           @JsonProperty("userId") final String userId,
                           @JsonProperty("creationTime") final long creationTime,
                           @JsonProperty("nodeName") final String nodeName,
                           @JsonProperty("storeSize") final long storeSize,
                           @JsonProperty("complete") final boolean complete,
                           @JsonProperty("taskProgress") final SearchTaskProgress taskProgress,
                           @JsonProperty("resultStoreSettings") final ResultStoreSettings resultStoreSettings) {
        this.queryKey = queryKey;
        this.userId = userId;
        this.creationTime = creationTime;
        this.nodeName = nodeName;
        this.storeSize = storeSize;
        this.complete = complete;
        this.taskProgress = taskProgress;
        this.resultStoreSettings = resultStoreSettings;
    }

    public QueryKey getQueryKey() {
        return queryKey;
    }

    public String getUserId() {
        return userId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getNodeName() {
        return nodeName;
    }

    public long getStoreSize() {
        return storeSize;
    }

    public boolean isComplete() {
        return complete;
    }

    public SearchTaskProgress getTaskProgress() {
        return taskProgress;
    }

    public ResultStoreSettings getResultStoreSettings() {
        return resultStoreSettings;
    }
}