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

public final class ValDouble implements ValNumber {

    public static final Type TYPE = Type.DOUBLE;
    private final double value;
    private transient Optional<String> optionalString;

    private ValDouble(final double value) {
        this.value = value;
    }

    public static ValDouble create(final double value) {
        return new ValDouble(value);
    }

    @Override
    public Integer toInteger() {
        return (int) value;
    }

    @Override
    public Long toLong() {
        return (long) value;
    }

    @Override
    public Float toFloat() {
        return (float) value;
    }

    @Override
    public Double toDouble() {
        return value;
    }

    @Override
    public Boolean toBoolean() {
        return value != 0;
    }

    @Override
    public String toString() {
        if (optionalString == null) {
            try {
                final BigDecimal bigDecimal = BigDecimal.valueOf(value);
                optionalString = Optional.of(bigDecimal.stripTrailingZeros().toPlainString());
            } catch (final RuntimeException e) {
                optionalString = Optional.empty();
            }

        }
        return optionalString.orElse(null);
    }

    @Override
    public void appendString(final StringBuilder sb) {
        sb.append(this);
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
        final ValDouble valDouble = (ValDouble) o;
        return Double.compare(valDouble.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
