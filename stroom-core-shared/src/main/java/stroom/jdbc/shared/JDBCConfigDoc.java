package stroom.jdbc.shared;

import stroom.docref.DocRef;
import stroom.docstore.shared.Doc;
import stroom.svg.shared.SvgImage;
import stroom.util.shared.HasData;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
        "data"})
@JsonInclude(Include.NON_EMPTY)
public class JDBCConfigDoc extends Doc implements HasData {

    public static final String DOCUMENT_TYPE = "JDBCConfig";
    public static final SvgImage ICON = SvgImage.PIPELINE_STATISTICS;

    @JsonProperty
    private String description = "";

    @JsonProperty
    private String data = "";

    public JDBCConfigDoc() {
    }

    public JDBCConfigDoc(final String type, final String uuid, final String name) {
        super(type, uuid, name);
    }

    @JsonCreator
    public JDBCConfigDoc(@JsonProperty("type") final String type,
                         @JsonProperty("uuid") final String uuid,
                         @JsonProperty("name") final String name,
                         @JsonProperty("version") final String version,
                         @JsonProperty("createTimeMs") final Long createTimeMs,
                         @JsonProperty("updateTimeMs") final Long updateTimeMs,
                         @JsonProperty("createUser") final String createUser,
                         @JsonProperty("updateUser") final String updateUser,
                         @JsonProperty("description") final String description,
                         @JsonProperty("data") final String data) {
        super(type, uuid, name, version, createTimeMs, updateTimeMs, createUser, updateUser);
        this.description = description;
        this.data = data;
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

    @SuppressWarnings("unused")
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setData(final String data) {
        this.data = data;
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
        final JDBCConfigDoc other = (JDBCConfigDoc) o;
        return Objects.equals(description, other.description) &&
                Objects.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), description, data);
    }

}
