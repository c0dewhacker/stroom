package stroom.util.shared;

import stroom.test.common.TestUtil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TestPropertyPath {

    @Test
    void blank() {
        PropertyPath propertyPath = PropertyPath.blank();

        assertThat(propertyPath.toString())
                .isEqualTo("");

        assertThat(propertyPath.getParts())
                .isEmpty();

        assertThat(propertyPath.isBlank())
                .isTrue();
    }

    @Test
    void getPropertyPath() {
        PropertyPath propertyPath = PropertyPath.fromParts("stroom", "node", "name");

        assertThat(propertyPath.toString())
                .isEqualTo("stroom.node.name");

        assertThat(propertyPath.getParts())
                .containsExactly("stroom", "node", "name");
    }

    @Test
    void merge() {
        PropertyPath propertyPath1 = PropertyPath.fromParts("stroom", "node");
        PropertyPath propertyPath2 = PropertyPath.fromParts("name");
        assertThat(propertyPath1.merge(propertyPath2).toString())
                .isEqualTo("stroom.node.name");
    }

    @Test
    void merge2() {
        PropertyPath propertyPath1 = PropertyPath.fromParts("stroom", "node");
        String part2 = "name";
        assertThat(propertyPath1.merge(part2).toString()).isEqualTo("stroom.node.name");
    }

    @Test
    void containsPart1() {
        PropertyPath propertyPath = PropertyPath.fromParts("stroom", "node", "name");
        propertyPath.getParts()
                .forEach(part -> {
                    assertThat(propertyPath.containsPart(part))
                            .isTrue();
                });
    }

    @Test
    void containsPart2() {
        PropertyPath propertyPath = PropertyPath.fromParts("stroom", "node", "name");
        assertThat(propertyPath.containsPart("not_found"))
                .isFalse();
    }

    @Test
    void builder() {
        PropertyPath propertyPath = PropertyPath.builder()
                .add("stroom")
                .add("node")
                .add("name")
                .build();

        assertThat(propertyPath.toString())
                .isEqualTo("stroom.node.name");
    }

    @Test
    void testEqualsIgnoreCase1() {
        doEqualsIgnoreCaseTest("stroom.node.name", "stroom.node.name", true);
    }

    @Test
    void testEqualsIgnoreCase2() {
        doEqualsIgnoreCaseTest("stroom.NODE.name", "stroom.node.name", true);
    }

    @Test
    void testEqualsIgnoreCase3() {
        doEqualsIgnoreCaseTest("stroom.NODE.name", "STROOM.node.NAME", true);
    }

    @Test
    void testEqualsIgnoreCase4() {
        doEqualsIgnoreCaseTest("stroom.node.name", "stroom.xxxxxxx.name", false);
    }

    @Test
    void testEqualsIgnoreCase5() {
        doEqualsIgnoreCaseTest("stroom.node.name", "stroom.node", false);
    }

    private void doEqualsIgnoreCaseTest(final String pathString1,
                                        final String pathString2,
                                        final boolean expectedResult) {
        PropertyPath path1 = PropertyPath.fromPathString(pathString1);
        PropertyPath path2 = PropertyPath.fromPathString(pathString2);

        boolean result = path1.equalsIgnoreCase(path2);

        assertThat(result)
                .isEqualTo(expectedResult);
    }

    @TestFactory
    Stream<DynamicTest> testGetParentPropertyName() {
        return TestUtil.buildDynamicTestStream()
                .withInputType(PropertyPath.class)
                .withOutputType(String.class)
                .withTestFunction(testCase ->
                        testCase.getInput().getParentPropertyName()
                                .orElse(null))
                .withSimpleEqualityAssertion()
                .addCase(PropertyPath.blank(), null)
                .addCase(PropertyPath.fromParts("root"), null)
                .addCase(PropertyPath.fromParts("root", "child"), "root")
                .addCase(PropertyPath.fromParts("root", "child", "grandchild"), "child")
                .build();
    }


    @Test
    void testGetParent1() {
        final PropertyPath propertyPath = PropertyPath.fromPathString("stroom.node.name");
        assertThat(propertyPath.getParent())
                .hasValue(PropertyPath.fromPathString("stroom.node"));
    }

    @Test
    void testGetParent2() {
        final PropertyPath propertyPath = PropertyPath.fromPathString("stroom");
        assertThat(propertyPath.getParent())
                .isEmpty();
    }

    @Test
    void testGetParent3() {
        final PropertyPath propertyPath = PropertyPath.fromPathString("");
    }

    @Test
    void testSerde() throws IOException {
        doSerdeTest(PropertyPath.fromPathString("stroom.node.name"), PropertyPath.class);
    }

    @Test
    void testSerde_empty() throws IOException {
        doSerdeTest(PropertyPath.blank(), PropertyPath.class);
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    @Test
    void testInterning1() {
        final PropertyPath propertyPath1 = PropertyPath.fromParts(
                new String("root"),
                new String("child"));
        final PropertyPath propertyPath2 = PropertyPath.fromParts(
                new String("root"),
                new String("child"));

        // Interning means they should be the same instance
        assertThat(propertyPath1.getParts().get(0))
                .isSameAs(propertyPath2.getParts().get(0));

        assertThat(propertyPath2.getParts().get(1))
                .isSameAs(propertyPath2.getParts().get(1));
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    @Test
    void testInterning2() {
        final PropertyPath propertyPath1 = PropertyPath.fromParts(
                new String("root"),
                new String("child"));
        final PropertyPath propertyPath2 = PropertyPath.fromParts(
                new String("root"),
                new String("child"));

        // Interning means they should be the same instance
        assertThat(propertyPath1.getLeafPart())
                .isSameAs(propertyPath2.getLeafPart());

        assertThat(propertyPath2.getParentParts())
                .isSameAs(propertyPath2.getParentParts());
    }

    private <T> void doSerdeTest(final T entity,
                                 final Class<T> clazz) throws IOException {

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        assertThat(mapper.canSerialize(entity.getClass()))
                .isTrue();

        String json = mapper.writeValueAsString(entity);
        System.out.println("\n" + json);

        final T entity2 = mapper.readValue(json, clazz);

        assertThat(entity2)
                .isEqualTo(entity);
    }
}
