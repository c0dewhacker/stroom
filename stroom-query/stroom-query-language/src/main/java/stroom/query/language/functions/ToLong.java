/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.query.language.functions;


@SuppressWarnings("unused") //Used by FunctionFactory
@FunctionDef(
        name = ToLong.NAME,
        commonCategory = FunctionCategory.CAST,
        commonReturnType = ValLong.class,
        commonReturnDescription = "The value as the long type.",
        signatures = @FunctionSignature(
                description = "Converts the supplied value to an long (if it can be). For example, converting " +
                        "the text \"12\" to the number 12. If the value looks like the default date format then " +
                        "this function will parse the date string and return the number of milliseconds since " +
                        "the epoch (1 Jan 1970).",
                args = @FunctionArg(
                        name = "value",
                        description = "Field, the result of another function or a constant.",
                        argType = Val.class)))
class ToLong extends AbstractCast {

    static final String NAME = "toLong";
    private static final ValErr ERROR = ValErr.create("Unable to cast to a long");
    private static final Cast CAST = new Cast();

    public ToLong(final String name) {
        super(name);
    }

    @Override
    AbstractCaster getCaster() {
        return CAST;
    }

    @Override
    public Type getCommonReturnType() {
        return Type.LONG;
    }

    private static class Cast extends AbstractCaster {

        @Override
        Val cast(final Val val) {
            if (!val.type().isValue()) {
                return val;
            }

            final Long value = val.toLong();
            if (value != null) {
                return ValLong.create(value);
            }
            return ERROR;
        }
    }
}
