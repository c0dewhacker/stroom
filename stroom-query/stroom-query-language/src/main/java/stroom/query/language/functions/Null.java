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
        name = Null.NAME,
        commonCategory = FunctionCategory.VALUE,
        commonReturnType = ValNull.class,
        commonReturnDescription = "A null value.",
        signatures = @FunctionSignature(
                description = "Returns a null value.",
                args = {}))
public class Null extends AbstractStaticFunction {

    static final String NAME = "null";

    public static final StaticValueGen GEN = new StaticValueGen(ValNull.INSTANCE);

    public Null(final String name) {
        super(name, GEN);
    }

    @Override
    public Type getCommonReturnType() {
        return Type.NULL;
    }
}
