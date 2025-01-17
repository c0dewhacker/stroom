package stroom.meta.api;

import stroom.util.NullSafe;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Map that does not care about key case.
 */
public class AttributeMap extends CIStringHashMap {

    private static final Pattern DE_SERIALISED_VALUE_DELIMITER_PATTERN = Pattern.compile("\\n");

    private final boolean overrideEmbeddedMeta;

    public AttributeMap(final boolean overrideEmbeddedMeta) {
        this.overrideEmbeddedMeta = overrideEmbeddedMeta;
    }

    public AttributeMap(final boolean overrideEmbeddedMeta, final Map<String, String> values) {
        this.overrideEmbeddedMeta = overrideEmbeddedMeta;
        putAll(values);
    }

    public AttributeMap() {
        this.overrideEmbeddedMeta = false;
    }

    public AttributeMap(final Map<String, String> values) {
        this.overrideEmbeddedMeta = false;
        putAll(values);
    }

    private AttributeMap(final Builder builder) {
        overrideEmbeddedMeta = builder.overrideEmbeddedMeta;
        putAll(builder.attributes);
    }

    /**
     * Put an entry where the value is itself a collection of values, e.g. a list of files
     */
    public String putCollection(final String key, Collection<String> values) {
        final String value;
        if (values == null) {
            value = null;
        } else if (values.isEmpty()) {
            value = "";
        } else {
            value = String.join(AttributeMapUtil.DE_SERIALISED_VALUE_DELIMITER, values);
        }
        return put(key, value);
    }

    /**
     * Get the value for a given key as a collection, e.g. where the value is known to be a
     * delimited collection of items. If the value only contains one item, then a singleton
     * collection is returned.
     */
    public Collection<String> getAsCollection(final String key) {
        final String val = get(key);
        if (NullSafe.isEmptyString(val)) {
            return Collections.emptyList();
        } else {
            return DE_SERIALISED_VALUE_DELIMITER_PATTERN.splitAsStream(val)
                    .toList();
        }
    }


    public static Builder copy(final AttributeMap copy) {
        final Builder builder = new Builder();
        builder.overrideEmbeddedMeta = copy.overrideEmbeddedMeta;
        builder.attributes = new AttributeMap();
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void removeAll(final Collection<String> keySet) {
        for (final String key : keySet) {
            remove(key);
        }
    }

    public boolean isOverrideEmbeddedMeta() {
        return overrideEmbeddedMeta;
    }


    // --------------------------------------------------------------------------------


    public static final class Builder {

        private boolean overrideEmbeddedMeta = false;
        private AttributeMap attributes = new AttributeMap();

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder withOverrideEmbeddedMeta(final boolean val) {
            overrideEmbeddedMeta = val;
            return this;
        }

        public Builder overrideEmbeddedMeta() {
            overrideEmbeddedMeta = true;
            return this;
        }

        public Builder put(final String key, final String value) {
            Objects.requireNonNull(key);
            attributes.put(key, value);
            return this;
        }

        public Builder putCollection(final String key, Collection<String> values) {
            Objects.requireNonNull(key);
            attributes.putCollection(key, values);
            return this;
        }

        public AttributeMap build() {
            return new AttributeMap(this);
        }
    }
}
