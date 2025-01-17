package stroom.analytics.impl;

import stroom.config.common.AbstractDbConfig;
import stroom.config.common.ConnectionConfig;
import stroom.config.common.ConnectionPoolConfig;
import stroom.config.common.HasDbConfig;
import stroom.query.common.v2.AnalyticResultStoreConfig;
import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.BootStrapConfig;
import stroom.util.shared.IsStroomConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@SuppressWarnings("unused")
@JsonPropertyOrder(alphabetic = true)
public class AnalyticsConfig extends AbstractConfig implements IsStroomConfig, HasDbConfig {

    private final AnalyticsDbConfig dbConfig;
    @JsonPropertyDescription("Name of timezone (ZoneId) that will be used during alert generation.")
    private final String timezone;
    @JsonPropertyDescription("Configuration for the data store used for analytics.")
    private final AnalyticResultStoreConfig resultStoreConfig;
    @JsonPropertyDescription("Email service configuration.")
    private final EmailConfig emailConfig;
    @JsonPropertyDescription("Configuration for caching streaming analytics.")
    private final CacheConfig streamingAnalyticCache;

    public AnalyticsConfig() {
        dbConfig = new AnalyticsDbConfig();
        timezone = "UTC";
        resultStoreConfig = new AnalyticResultStoreConfig();
        emailConfig = new EmailConfig();
        streamingAnalyticCache = CacheConfig.builder()
                .maximumSize(1000L)
                .expireAfterAccess(StroomDuration.ofMinutes(10))
                .build();
    }

    @SuppressWarnings("unused")
    @JsonCreator
    public AnalyticsConfig(@JsonProperty("db") final AnalyticsDbConfig dbConfig,
                           @JsonProperty("timezone") final String timezone,
                           @JsonProperty("resultStore") final AnalyticResultStoreConfig resultStoreConfig,
                           @JsonProperty("emailConfig") final EmailConfig emailConfig,
                           @JsonProperty("streamingAnalyticCache") final CacheConfig streamingAnalyticCache) {
        this.dbConfig = dbConfig;
        this.timezone = timezone;
        this.resultStoreConfig = resultStoreConfig;
        this.emailConfig = emailConfig;
        this.streamingAnalyticCache = streamingAnalyticCache;
    }

    @Override
    @JsonProperty("db")
    public AnalyticsDbConfig getDbConfig() {
        return dbConfig;
    }

    @JsonProperty
    public String getTimezone() {
        return timezone;
    }

    @JsonProperty("resultStore")
    public AnalyticResultStoreConfig getResultStoreConfig() {
        return resultStoreConfig;
    }

    @JsonProperty("emailConfig")
    public EmailConfig getEmailConfig() {
        return emailConfig;
    }

    @JsonProperty("streamingAnalyticCache")
    public CacheConfig getStreamingAnalyticCache() {
        return streamingAnalyticCache;
    }

    @BootStrapConfig
    public static class AnalyticsDbConfig extends AbstractDbConfig implements IsStroomConfig {

        public AnalyticsDbConfig() {
            super();
        }

        @JsonCreator
        public AnalyticsDbConfig(
                @JsonProperty(PROP_NAME_CONNECTION) final ConnectionConfig connectionConfig,
                @JsonProperty(PROP_NAME_CONNECTION_POOL) final ConnectionPoolConfig connectionPoolConfig) {
            super(connectionConfig, connectionPoolConfig);
        }
    }
}
