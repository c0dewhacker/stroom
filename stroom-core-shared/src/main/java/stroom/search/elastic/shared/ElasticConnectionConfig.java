package stroom.search.elastic.shared;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonPropertyOrder({"connectionUrls,caCertificate,useAuthentication,apiKeyId,apiKeySecret,socketTimeoutMillis"})
@XmlRootElement(name = "connection")
@XmlType(name = "ElasticConnectionConfig", propOrder = {"connectionUrls", "caCertificate", "useAuthentication", "apiKeyId", "apiKeySecret", "socketTimeoutMillis"})
public class ElasticConnectionConfig implements Serializable {
    private List<String> connectionUrls = new ArrayList<>();

    /**
     * DER or PEM-encoded CA certificate for X.509 verification
     */
    private String caCertificate;

    private boolean useAuthentication = false;

    private String apiKeyId;

    /**
     * Plain-text API key (not serialised)
     */
    private String apiKeySecret;

    /**
     * Socket timeout duration. Any Elasticsearch requests are expected to complete within this interval,
     * else the request is aborted and an `Error` is reported.
     */
    private int socketTimeoutMillis = -1;

    public List<String> getConnectionUrls() { return connectionUrls; }

    public void setConnectionUrls(final List<String> connectionUrls) { this.connectionUrls = connectionUrls; }

    public String getCaCertificate() { return caCertificate; }

    public void setCaCertificate(final String caCertificate) { this.caCertificate = caCertificate; }

    public boolean getUseAuthentication() { return useAuthentication; }

    public void setUseAuthentication(final boolean useAuthentication) { this.useAuthentication = useAuthentication; }

    public String getApiKeyId() { return apiKeyId; }

    public void setApiKeyId(final String apiKeyId) { this.apiKeyId = apiKeyId; }

    public String getApiKeySecret() { return apiKeySecret; }

    public void setApiKeySecret(final String apiKeySecret) { this.apiKeySecret = apiKeySecret; }

    public int getSocketTimeoutMillis() { return socketTimeoutMillis; }

    public void setSocketTimeoutMillis(final int socketTimeoutMillis) { this.socketTimeoutMillis = socketTimeoutMillis; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ElasticConnectionConfig)) return false;
        final ElasticConnectionConfig that = (ElasticConnectionConfig)o;

        return Objects.equals(connectionUrls, that.connectionUrls) &&
                Objects.equals(caCertificate, that.caCertificate) &&
                Objects.equals(useAuthentication, that.useAuthentication) &&
                Objects.equals(apiKeyId, that.apiKeyId) &&
                Objects.equals(apiKeySecret, that.apiKeySecret) &&
                Objects.equals(socketTimeoutMillis, that.socketTimeoutMillis);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionUrls);
    }

    @Override
    public String toString() {
        return "ElasticConnectionConfig{" +
                "connectionUrls='" + String.join(",", connectionUrls) + '\'' +
                "caCertPath='" + caCertificate + '\'' +
                "useAuthentication=" + useAuthentication +
                "apiKeyId='" + apiKeyId + '\'' +
                "apiKeySecret='<redacted>'" +
                "socketTimeoutMillis=" + socketTimeoutMillis +
                '}';
    }
}
