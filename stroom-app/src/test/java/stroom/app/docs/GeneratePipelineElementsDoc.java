package stroom.app.docs;

import stroom.pipeline.factory.ConfigurableElement;
import stroom.pipeline.factory.Element;
import stroom.pipeline.factory.PipelineProperty;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.pipeline.source.SourceElement;
import stroom.util.logging.AsciiTable;
import stroom.util.logging.AsciiTable.Column;
import stroom.util.logging.LogUtil;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generate the content for content/en/docs/user-guide/pipelines/element-reference.md
 * and layouts/shortcodes/pipe-elm.html in stroom-docs.
 * <p>
 * generatePipelineElementReferenceContent ploduces something like this, with
 * a H2 for each category and a H3 for each element in that category.
 * <p>
 * Once the doc has been amended with descriptions you will prob need to run this
 * the diff the output against the stroom-docs file to merge new/changed elements
 * over.
 *
 * <pre>
 * ## Reader
 *
 * ### BOMRemovalFilterInput
 *
 * Icon: {{< stroom-icon "pipeline/stream.svg" >}}
 *
 * Roles:
 *
 * * HasTargets
 * * Mutator
 * * Reader
 * * Stepping
 * </pre>
 *
 * <p>
 * generatePipelineElementReferenceContent produces something like this, with
 */
public class GeneratePipelineElementsDoc {

    private static final String PACKAGE_NAME = "stroom";
    private static final String MISSING_CATEGORY_DESCRIPTION = "> TODO - Add description";

    @Disabled // Manual only
    @Test
    void generatePipelineElementReferenceContent() {
        try (ScanResult scanResult =
                new ClassGraph()
                        .enableAllInfo()             // Scan classes, methods, fields, annotations
                        .acceptPackages(PACKAGE_NAME)  // Scan com.xyz and subpackages (omit to scan all packages)
                        .scan()) {                   // Start the scan

            scanResult.getClassesImplementing(Element.class.getName())
                    .parallelStream()
                    .map(GeneratePipelineElementsDoc::mapClassInfo)
                    .filter(Objects::nonNull)
                    .filter(elementInfo -> !Category.INTERNAL.equals(elementInfo.category))
                    .sequential()
                    .collect(Collectors.groupingBy(ElementInfo::getCategory))
                    .entrySet()
                    .stream()
                    .sorted(Entry.comparingByKey())
                    .map(GeneratePipelineElementsDoc::mapCategoryGroup)
                    .forEach(System.out::println);
        }
    }

    /**
     * The output needs to be manually copied into
     * layouts/shortcodes/pipe-elm.html in stroom-docs
     */
    @Disabled // Manual only
    @Test
    void generatePipeElmShortcodeNames() {
        try (ScanResult scanResult =
                new ClassGraph()
                        .enableAllInfo()             // Scan classes, methods, fields, annotations
                        .acceptPackages(PACKAGE_NAME)  // Scan com.xyz and subpackages (omit to scan all packages)
                        .scan()) {                   // Start the scan

            scanResult.getClassesImplementing(Element.class.getName())
                    .parallelStream()
                    .map(GeneratePipelineElementsDoc::mapClassInfo)
                    .filter(Objects::nonNull)
                    .filter(elementInfo ->
                            !Category.INTERNAL.equals(elementInfo.category)
                                    || SourceElement.class.equals(elementInfo.clazz))
                    .sequential()
                    .sorted(Comparator.comparing(ElementInfo::getType))
                    .map(elementInfo -> {
                        final String template = "\"{}\" \"{}\"";
                        return LogUtil.message(template, elementInfo.type, elementInfo.iconFilename);
                    })
                    .forEach(System.out::println);
        }
    }

    private static String mapCategoryGroup(final Entry<Category, List<ElementInfo>> entry) {
        final Category category = entry.getKey();
        final List<ElementInfo> elementInfoList = entry.getValue();

        final String elementsText = convertElementsToText(elementInfoList);
        final String categoryDescription = Optional.ofNullable(category.getDescription())
                .orElse(MISSING_CATEGORY_DESCRIPTION);

        return LogUtil.message("""
                        ## {}

                        {}

                        {}
                        """,
                category.getDisplayValue(),
                categoryDescription,
                elementsText);
    }

    private static String convertElementsToText(final List<ElementInfo> elementInfoList) {
        final String elementsText = elementInfoList.stream()
                .sorted(Comparator.comparing(ElementInfo::getType))
                .map(elementInfo -> {

                    final String descriptionText = elementInfo.description != null
                            ? elementInfo.description
                            : "> TODO - Add description";

                    // Add the &nbsp; at the end so the markdown processor treats the line as a <p>
                    final String iconText = elementInfo.iconFilename != null
                            ? LogUtil.message("""
                            {{< pipe-elm "{}" >}}&nbsp;""", elementInfo.type)
                            : "";

                    final String rolesText = buildRolesText(elementInfo.roles);
//                    final String rolesText = !elementInfo.roles.isEmpty()
//                            ? ("\n\n**Roles:**\n" + elementInfo.roles
//                            .stream()
//                            .sorted()
//                            .map(WordUtils::capitalize)
//                            .map(role -> "\n* " + role)
//                            .collect(Collectors.joining()))
//                            : "";

                    final String propsText;
                    if (!elementInfo.propertyInfoList.isEmpty()) {
                        propsText = "\n\n**Element properties:**\n\n" + AsciiTable.builder(elementInfo.propertyInfoList)
                                .withColumn(Column.builder("Name", PropertyInfo::getName)
                                        .build())
                                .withColumn(Column.builder("Description", PropertyInfo::getDescription)
                                        .build())
                                .withColumn(Column.builder("Default Value", (PropertyInfo propInfo) ->
                                                propInfo.getDefaultValue().isEmpty()
                                                        ? "-"
                                                        : propInfo.getDefaultValue())
                                        .build())
                                .build();
                    } else {
                        propsText = "";
                    }

                    final String template = """
                            ### {}

                            {}

                            {}{}

                            """;

                    return LogUtil.message(
                            template,
                            elementInfo.getType(),
                            iconText,
                            descriptionText,
//                            elementInfo.category.getDisplayValue(),
//                            rolesText,
                            propsText
                    );
                })
                .collect(Collectors.joining("\n"));
        return elementsText;
    }

    private static String buildRolesText(final Set<String> roles) {
        final Function<String, String> hasRole = roleName ->
                roles.contains(roleName)
                        ? "Yes"
                        : "No";

        final List<Tuple2<String, String>> data = List.of(
                Tuple.of("Can be stepped:", hasRole.apply(PipelineElementType.VISABILITY_STEPPING)),
                Tuple.of("Transforms input:", hasRole.apply(PipelineElementType.ROLE_MUTATOR)),
                Tuple.of("Validates input:", hasRole.apply(PipelineElementType.ROLE_MUTATOR))
        );
        return "\n\n**Features**:\n\n" + AsciiTable.builder(data)
                .withColumn(Column.builder("Feature", (Tuple2<String, String> tuple) -> tuple._1()).build())
                .withColumn(Column.builder("Yes/No", (Tuple2<String, String> tuple) -> tuple._2).build())
                .build();
    }


    private static ElementInfo mapClassInfo(final ClassInfo elementClassInfo) {
        final Class<? extends Element> clazz = (Class<? extends Element>) elementClassInfo.loadClass();
        if (!clazz.isInterface()
                && !Modifier.isAbstract(clazz.getModifiers())
                && !clazz.getSimpleName().startsWith("Abstract")
                && clazz.isAnnotationPresent(ConfigurableElement.class)) {

            final ConfigurableElement elementAnno = clazz.getAnnotation(ConfigurableElement.class);

            final String description = getStringValue(elementAnno.description());
            final String iconFileName = getStringValue(elementAnno.icon().getRelativePathStr());
            final String type = getStringValue(elementAnno.type());
            final Category category = elementAnno.category();
            final Set<String> roles = new HashSet<>(Arrays.asList(elementAnno.roles()));
            final List<PropertyInfo> propertyInfoList = getPropertyInfoList(clazz);

            return new ElementInfo(clazz, type, iconFileName, category, description, roles, propertyInfoList);
        } else {
            return null;
        }
    }

    private static String getStringValue(final String value) {
        if (value == null || value.isEmpty() || value.isBlank()) {
            return null;
        } else {
            return value;
        }
    }

    private static List<PropertyInfo> getPropertyInfoList(final Class<?> clazz) {

        return Arrays.stream(clazz.getMethods())
                .filter(method -> method.isAnnotationPresent(PipelineProperty.class))
                .map(method -> {
                    final PipelineProperty pipelinePropertyAnno = method.getAnnotation(PipelineProperty.class);
                    final String name = makePropertyName(method.getName());
                    return new PropertyInfo(
                            name,
                            pipelinePropertyAnno.description(),
                            pipelinePropertyAnno.defaultValue());
                })
                .sorted(Comparator.comparing(PropertyInfo::getName))
                .collect(Collectors.toList());
    }

    private static String makePropertyName(final String methodName) {
        // Convert the setter to a camel case property name.
        String name = methodName.substring(3);
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        return name;
    }


    // --------------------------------------------------------------------------------


    private static class ElementInfo {

        private final Class<? extends Element> clazz;
        private final String type;
        private final String iconFilename;
        private final Category category;
        private final String description;
        private final Set<String> roles;
        private final List<PropertyInfo> propertyInfoList;

        public ElementInfo(final Class<? extends Element> clazz,
                           final String type,
                           final String iconFilename,
                           final Category category,
                           final String description,
                           final Set<String> roles,
                           final List<PropertyInfo> propertyInfoList) {
            this.clazz = clazz;
            this.type = type;
            this.iconFilename = iconFilename;
            this.category = category;
            this.description = description;
            this.roles = roles;
            this.propertyInfoList = propertyInfoList;
        }

        public String getType() {
            return type;
        }

        public Category getCategory() {
            return category;
        }

        @Override
        public String toString() {
            return "ElementInfo{" +
                    "type='" + type + '\'' +
                    ", iconFilename='" + iconFilename + '\'' +
                    ", category=" + category +
                    ", description='" + description + '\'' +
                    ", roles=" + roles +
                    ", propertyInfoList=" + propertyInfoList +
                    '}';
        }
    }

    private static class PropertyInfo {

        private final String name;
        private final String description;
        private final String defaultValue;

        public PropertyInfo(final String name,
                            final String description,
                            final String defaultValue) {
            this.name = name;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        @Override
        public String toString() {
            return "PropertyInfo{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", defaultValue='" + defaultValue + '\'' +
                    '}';
        }
    }
}
