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
        name = NotEquals.NAME,
        aliases = {NotEquals.ALIAS, NotEquals.SHORT_ALIAS},
        commonCategory = FunctionCategory.LOGIC,
        commonReturnType = ValBoolean.class,
        commonReturnDescription = "True if the two values are not equal.",
        signatures = @FunctionSignature(
                description = "Tests the non equality of two values. It will attempt to convert the parameters " +
                        "to the type of the first parameter, e.g. ne(123, \"123\") returns true.",
                args = {
                        @FunctionArg(
                                name = "arg1",
                                description = "Field, the result of another function or a constant.",
                                argType = Val.class),
                        @FunctionArg(
                                name = "arg2",
                                description = "Field, the result of another function or a constant.",
                                argType = Val.class)}))
class NotEquals extends AbstractEqualityFunction {

    static final String NAME = "!=";
    static final String ALIAS = "notEquals";
    static final String SHORT_ALIAS = "ne";
    private static final EqualsEvaluator EVALUATOR = new EqualsEvaluator();

    public NotEquals(final String name) {
        super(name, NAME);
    }

    @Override
    Evaluator createEvaluator() {
        return EVALUATOR;
    }

    private static class EqualsEvaluator extends Evaluator {

        @Override
        protected Val evaluate(final Val a, final Val b) {
            if (a.getClass().equals(b.getClass())) {
                if (a instanceof ValInteger) {
                    return ValBoolean.create(!a.toInteger().equals(b.toInteger()));
                }
                if (a instanceof ValLong) {
                    return ValBoolean.create(!a.toLong().equals(b.toLong()));
                }
                if (a instanceof ValBoolean) {
                    return ValBoolean.create(!a.toBoolean().equals(b.toBoolean()));
                }
            } else {
                final Double da = a.toDouble();
                final Double db = b.toDouble();
                if (da != null && db != null) {
                    return ValBoolean.create(!da.equals(db));
                }
            }

            return ValBoolean.create(!a.toString().equals(b.toString()));
        }
    }
}
