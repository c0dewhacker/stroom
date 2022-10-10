package stroom.test.common;

import io.vavr.Tuple;

/**
 * Useful class for holding input and expected output values for a test case.
 *
 * @param <I> The type of the test case input. If there are multiple inputs then
 *            {@code I} may be a {@link Tuple} of input values.
 * @param <O> The type of the test case expected output. If there are multiple outputs then
 *            {@code O} may be a {@link Tuple} of input values.
 */
public class TestCase<I, O> {

    private final I input;
    private final O expectedOutput;
    private final Class<? extends Throwable> expectedThrowableType;
    private final String name;

    TestCase(final I input,
             final O expectedOutput,
             final Class<? extends Throwable> expectedThrowableType,
             final String name) {

        this.input = input;
        this.expectedOutput = expectedOutput;
        this.expectedThrowableType = expectedThrowableType;
        this.name = name;
    }

    public static <I, O> TestCase<I, O> of(final I input,
                                           final O expectedOutput) {
        return new TestCase<>(input, expectedOutput, null, null);
    }

    public static <I, O> TestCase<I, O> of(final String name,
                                           final I input,
                                           final O expectedOutput) {
        return new TestCase<>(input, expectedOutput, null, name);
    }

    public static <I> TestCase<I, ?> throwing(final I input,
                                              final Class<? extends Throwable> expectedThrowable) {
        return new TestCase<>(input, null, expectedThrowable, null);
    }

    public static <I> TestCase<I, ?> throwing(final String name,
                                              final I input,
                                              final Class<? extends Throwable> expectedThrowable) {
        return new TestCase<>(input, null, expectedThrowable, name);
    }

    public I getInput() {
        return input;
    }

    public O getExpectedOutput() {
        return expectedOutput;
    }

    public Class<? extends Throwable> getExpectedThrowableType() {
        return expectedThrowableType;
    }

    public boolean isExpectedToThrow() {
        return expectedThrowableType != null;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "input=" + input +
                ", expectedOutput=" + expectedOutput +
                ", name='" + name + '\'' +
                '}';
    }
}
