package stroom.jdbc.impl;

import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.IsStroomConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
public class JDBCConfig extends AbstractConfig implements IsStroomConfig {
    private final String skeletonConfigContent;
    private final CacheConfig jdbcConfigDocCache;

    public JDBCConfig() {
        skeletonConfigContent = DEFAULT_SKELETON_CONFIG_CONTENT;
        jdbcConfigDocCache = CacheConfig.builder()
                .maximumSize(1000L)
                .expireAfterAccess(StroomDuration.ofSeconds(10))
                .build();
    }

    @JsonCreator
    public JDBCConfig(@JsonProperty("skeletonConfigContent") final String skeletonConfigContent,
                       @JsonProperty("JDBCConfigDocCache") final CacheConfig jdbcConfigDocCache) {
        this.skeletonConfigContent = skeletonConfigContent;
        this.jdbcConfigDocCache = jdbcConfigDocCache;
    }

    @JsonProperty("skeletonConfigContent")
    @JsonPropertyDescription("The value of this property will be used to pre-populate a new JDBC Configuration. "
            + "It must be in Java Properties File format. Its purpose is to provide a skeleton for creating a working "
            + "JDBC Connection Configuration.")
    public String getSkeletonConfigContent() {
        return skeletonConfigContent;
    }

    @JsonProperty("JDBCConfigDocCache")
    public CacheConfig getJDBCConfigDocCache() {
        return jdbcConfigDocCache;
    }

    @Override
    public String toString() {
        return "JDBCConfig{" +
                "JDBCConfigDocCache=" + jdbcConfigDocCache +
                '}';
    }

    private static final String DEFAULT_SKELETON_CONFIG_CONTENT = """
        # A short list of JDBC Connection Options
        #jdbc.url=
        #jdbc.username=
        #jdbc.password=
        #
        #Connection Pooling Options
        #jdbc.pooling=true
        #jdbc.initialPoolSize=5
        #jdbc.maxPoolSize=20
        #jdbc.minPoolSize=2
        #jdbc.maxIdleTime=300
        #
        # SSL Options
        #jdbc.useSSL=false
        #jdbc.sslKeyStore=/path/to/keystore
        #jdbc.sslKeyStorePassword=
        #jdbc.sslTruststore=/path/to/truststore
        #jdbc.sslTrustStorePassword=
        #jdbc.sslVerifyServerCertificates=true
        #
        # Client Certificates (optional)
        #jdbc.sslClientCertificate=/path/to/client-certificate
        #jdbc.sslClientKey=/path/to/clientKey
        #jdbc.sslClientKeyPassword=
        #
        #Connection Timeout
        #jdbc.connectionTimeout=5000
        # Other Options
        # We don't want to autocommit. we will manually commit in endProcessing()
        jdbc.autocommit=false
        """;
}
