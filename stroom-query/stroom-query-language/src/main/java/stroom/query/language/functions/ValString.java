/*
 * Copyright 2018 Crown Copyright
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

package stroom.query.language.functions;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public final class ValString implements Val {

    public static final Type TYPE = Type.STRING;
    static final ValString EMPTY = new ValString("");
    private final String value;
    private transient Optional<Double> optionalDouble;
    private transient Optional<Long> optionalLong;

    private ValString(final String value) {
        this.value = value;
    }

    public static ValString create(final String value) {
        if ("".equals(value)) {
            return EMPTY;
        }
        return new ValString(value);
    }

    @Override
    public Integer toInteger() {
        Long l = toLong();
        if (l != null) {
            return l.intValue();
        }
        return null;
    }

    @Override
    public Long toLong() {
        if (optionalLong == null) {
            try {
                optionalLong = Optional.of(DateUtil.parseNormalDateTimeString(value));
            } catch (final RuntimeException e) {
                try {
                    optionalLong = Optional.of(Long.valueOf(value));
                } catch (final RuntimeException e2) {
                    optionalLong = Optional.empty();
                }
            }

        }
        return optionalLong.orElse(null);
    }

    @Override
    public Float toFloat() {
        Double d = toDouble();
        if (d != null) {
            return d.floatValue();
        }
        return null;
    }

    @Override
    public Double toDouble() {
        if (optionalDouble == null) {
            try {
                optionalDouble = Optional.of((double) DateUtil.parseNormalDateTimeString(value));
            } catch (final RuntimeException e) {
                try {
                    optionalDouble = Optional.of(new BigDecimal(value).doubleValue());
                } catch (final RuntimeException e2) {
                    optionalDouble = Optional.empty();
                }
            }
        }
        return optionalDouble.orElse(null);
    }

    @Override
    public Boolean toBoolean() {
        try {
            return Boolean.valueOf(value);
        } catch (final RuntimeException e) {
            // Ignore.
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public void appendString(final StringBuilder sb) {
        // Assume that strings are single quoted even though they may actually be double quoted in source.
        sb.append("'");
        sb.append(value.replaceAll("'", "\\\\'"));
        sb.append("'");
    }

    @Override
    public Type type() {
        return TYPE;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ValString valString = (ValString) o;
        return Objects.equals(value, valString.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
