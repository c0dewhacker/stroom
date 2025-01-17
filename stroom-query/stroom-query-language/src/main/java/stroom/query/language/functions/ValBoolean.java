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

import java.util.Objects;

public final class ValBoolean implements Val {

    public static final Type TYPE = Type.BOOLEAN;
    static final ValBoolean TRUE = new ValBoolean(true);
    static final ValBoolean FALSE = new ValBoolean(false);
    private final boolean value;

    private ValBoolean(final boolean value) {
        this.value = value;
    }

    public static ValBoolean create(final boolean value) {
        if (value) {
            return TRUE;
        }
        return FALSE;
    }

    @Override
    public Integer toInteger() {
        return value
                ? 1
                : 0;
    }

    @Override
    public Long toLong() {
        return value
                ? 1L
                : 0L;
    }

    @Override
    public Float toFloat() {
        return value
                ? 1F
                : 0F;
    }

    @Override
    public Double toDouble() {
        return value
                ? 1D
                : 0D;
    }

    @Override
    public Boolean toBoolean() {
        return value;
    }

    @Override
    public String toString() {
        return value
                ? "true"
                : "false";
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
        final ValBoolean that = (ValBoolean) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
