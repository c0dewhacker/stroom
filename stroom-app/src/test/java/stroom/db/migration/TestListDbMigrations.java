package stroom.db.migration;

import stroom.util.ColouredStringBuilder;
import stroom.util.ConsoleColour;
import stroom.util.shared.Version;

import com.google.common.base.Strings;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static stroom.util.ConsoleColour.BLUE;
import static stroom.util.ConsoleColour.RED;
import static stroom.util.ConsoleColour.YELLOW;

public class TestListDbMigrations {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestListDbMigrations.class);

    // Have to cope with stroom mig files, e.g. 07_00_00_017__IDX_SHRD.sql
    // and auth mig files, e.g. V2_1__Create_tables.sql
    private static final Pattern MIGRATION_FILE_REGEX_PATTERN = Pattern.compile(
            "^V([0-9]{1,2}_){2}[0-9]{1,2}(_[0-9]+)?__.*\\.(sql|java)$");
    private static final Pattern MIGRATION_FILE_PREFIX_REGEX_PATTERN = Pattern.compile(
            "^V([0-9]{1,2})_([0-9]{1,2})_([0-9]{1,2})");
    private static final Pattern MIGRATION_PATH_REGEX_PATTERN = Pattern.compile("^.*/src/main/.*$");

    Map<String, List<Script>> moduleToScriptMap = new HashMap<>();
    private int maxFileNameLength;

    @Test
    void listDbMigrationsByVersion() throws IOException {
        populateMigrationsMap();

        final Map<Version, Map<String, List<Script>>> map = moduleToScriptMap.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(Script::getVersion, Collectors.groupingBy(Script::moduleName)));

        final ColouredStringBuilder stringBuilder = new ColouredStringBuilder();
        final Comparator<String> moduleComparator = buildModuleNameComparator();
        final Comparator<String> filenameComparator = buildFileNameComparator();

        map.entrySet()
                .stream()
                .sorted(Comparator.comparing(Entry::getKey))
                .forEach(entry -> {
                    final Version version = entry.getKey();
                    final Map<String, List<Script>> prefixToScriptsMap = entry.getValue();
                    stringBuilder.appendMagenta(version.toString())
                            .append("\n");
                    prefixToScriptsMap.entrySet()
                            .stream()
                            .sorted(Comparator.comparing(Entry::getKey, moduleComparator))
                            .forEach(entry2 -> {
                                final String moduleName = entry2.getKey();
                                final List<Script> scripts = entry2.getValue();
                                if (!scripts.isEmpty()) {
                                    stringBuilder
                                            .append("  ")
                                            .appendMagenta(moduleName)
                                            .append("\n");
                                    scripts.forEach(script -> {
                                        appendScript(stringBuilder, script, "    ");
                                    });
                                }
                            });
                });
        System.out.println(stringBuilder.toString());
    }

    /**
     * Finds all the v7 DB migration scripts and dumps them out in order.
     * Useful for seeing the sql and java migrations together
     */
    @Test
    void listDbMigrationsByModule() throws IOException {

        populateMigrationsMap();

        final ColouredStringBuilder stringBuilder = new ColouredStringBuilder();

        final Comparator<String> moduleComparator = buildModuleNameComparator();

        moduleToScriptMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(moduleComparator))
                .forEach(entry -> {
                    stringBuilder
                            .appendMagenta(entry.getKey())
                            .append("\n");
                    entry.getValue()
                            .forEach(script -> {
                                appendScript(stringBuilder, script, "  ");
                            });
                    stringBuilder.append("\n");
                });
//        LOGGER.info("\n{}", stringBuilder.toString());
        System.out.println(stringBuilder.toString());
    }

    private void appendScript(final ColouredStringBuilder stringBuilder,
                              final Script script,
                              final String padding) {
        String filename = script.fileName();
        stringBuilder.append(padding);

        final ConsoleColour colour;
        if (filename.endsWith(".sql")) {
            colour = YELLOW;
        } else if (filename.endsWith(".java")) {
            colour = BLUE;
        } else {
            colour = RED;
        }
        stringBuilder
                .append(Strings.padEnd(filename, maxFileNameLength, ' '), colour)
                .append(" - ")
                .append(script.path().toString(), colour)
                .append("\n");
    }

    private static Comparator<String> buildModuleNameComparator() {
        // Core is always run first so list it first
        final Comparator<String> moduleComparator = (o1, o2) -> {
            String stroomCoreModuleName = "stroom-core";

            if (Objects.equals(o1, o2)) {
                return 0;
            } else if (stroomCoreModuleName.equals(o1)) {
                return -1;
            } else if (stroomCoreModuleName.equals(o2)) {
                return 1;
            } else {
                return Comparator.<String>naturalOrder().compare(o1, o2);
            }
        };
        return moduleComparator;
    }

    private void populateMigrationsMap() throws IOException {
        if (moduleToScriptMap.isEmpty()) {
            Path projectRoot = Paths.get("../").toAbsolutePath().normalize();

            try (Stream<Path> stream = Files.list(projectRoot)) {
                stream
                        .filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().startsWith("stroom-"))
                        .sorted()
                        .forEach(this::inspectModule);
            }

            maxFileNameLength = moduleToScriptMap.values().stream()
                    .flatMap(value -> value.stream()
                            .map(Script::fileName))
                    .mapToInt(String::length)
                    .max()
                    .orElse(60);
        }
    }

    private void inspectModule(final Path moduleDir) {

        // Core is always run first so list it first
        final Comparator<String> fileNameComparator = buildFileNameComparator();
        final String moduleName = moduleDir.getFileName().toString();

        Path projectRootDir = moduleDir;
        while (!Files.exists(projectRootDir.resolve(".git"))) {
            projectRootDir = projectRootDir.resolve("..")
                    .toAbsolutePath()
                    .normalize();
        }
        final Path projectRootDirFinal = projectRootDir;

        try (Stream<Path> stream = Files.walk(moduleDir)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            MIGRATION_PATH_REGEX_PATTERN.asMatchPredicate().test(path.toString()))
                    .map(path ->
                            new Script(
                                    moduleName,
                                    path.getFileName().toString(),
                                    projectRootDirFinal.relativize(path)))
                    .filter(script ->
                            MIGRATION_FILE_REGEX_PATTERN.asMatchPredicate().test(script.fileName()))
                    .sorted(Comparator.comparing(Script::fileName, fileNameComparator))
                    .forEach(tuple -> {
                        moduleToScriptMap.computeIfAbsent(moduleName, k -> new ArrayList<>())
                                .add(tuple);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Comparator<String> buildFileNameComparator() {
        return (name1, name2) -> {

            final Pattern authMigrationPattern = Pattern.compile("^V[0-9]_.*");

            String name1Modified = name1;
            String name2Modified = name2;

            LOGGER.trace("[{}] [{}]", name1Modified, name2Modified);

            // Special case for auth as the filenames have a different format
            // so we need to strip the non numeric parts off then sort numerically
            if (authMigrationPattern.matcher(name1Modified).matches()) {
                name1Modified = name1Modified.replaceFirst("^V", "V0");
            }
            if (authMigrationPattern.matcher(name2Modified).matches()) {
                name2Modified = name2Modified.replaceFirst("^V", "V0");
            }

            return Comparator.<String>naturalOrder().compare(name1Modified, name2Modified);
        };
    }


    // --------------------------------------------------------------------------------


    private record Script(String moduleName,
                          String fileName,
                          Path path) {

        String getVersionPrefix() {
            try {
                final Matcher matcher = MIGRATION_FILE_PREFIX_REGEX_PATTERN.matcher(fileName);
                if (matcher.find()) {
                    return matcher.group();
                } else {
                    throw new RuntimeException("Prefix not found for '" + fileName + "'");
                }
            } catch (IllegalStateException e) {
                throw new RuntimeException("Prefix not found for '" + fileName + "': " + e.getMessage());
            }
        }

        Version getVersion() {
            try {
                final Matcher matcher = MIGRATION_FILE_PREFIX_REGEX_PATTERN.matcher(fileName);
                if (matcher.find()) {
                    return new Version(
                            Integer.parseInt(matcher.group(1)),
                            Integer.parseInt(matcher.group(2)),
                            Integer.parseInt(matcher.group(3)));
                } else {
                    throw new RuntimeException("Prefix not found for '" + fileName + "'");
                }
            } catch (IllegalStateException e) {
                throw new RuntimeException("Prefix not found for '" + fileName + "': " + e.getMessage());
            }
        }
    }
}
