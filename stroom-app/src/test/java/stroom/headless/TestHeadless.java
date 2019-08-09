/*
 * Copyright 2016 Crown Copyright
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

package stroom.headless;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.test.ContentImportService.ContentPack;
import stroom.test.ContentPackDownloader;
import stroom.test.common.ComparisonHelper;
import stroom.test.common.util.test.TempDir;
import stroom.test.common.util.test.TempDirExtension;
import stroom.util.io.FileUtil;
import stroom.util.shared.Version;
import stroom.util.zip.ZipUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@ExtendWith(TempDirExtension.class)
class TestHeadless {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestHeadless.class);

    private static final Version CORE_XML_SCHEMAS_VERSION = Version.of(1, 0);
    private static final Version EVENT_LOGGING_XML_SCHEMA_VERSION = Version.of(1, 0);

    @Test
    void test(@TempDir Path newTempDir) throws IOException {
//        StroomProperties.setOverrideProperty("stroom.temp", FileUtil.getCanonicalPath(newTempDir), StroomProperties.Source.TEST);

        // Make sure the new temp directory is empty.
        if (Files.isDirectory(newTempDir)) {
            FileUtils.deleteDirectory(newTempDir.toFile());
        }

        final Path base = StroomHeadlessTestFileUtil.getTestResourcesDir();
        final Path testPath = base.resolve("TestHeadless");
        final Path tmpPath = testPath.resolve("tmp");
        FileUtil.deleteDir(tmpPath);
        Files.createDirectories(tmpPath);

        final Path contentDirPath = tmpPath.resolve("content");
        final Path inputDirPath = tmpPath.resolve("input");
        final Path outputDirPath = tmpPath.resolve("output");

        Files.createDirectories(contentDirPath);
        Files.createDirectories(inputDirPath);
        Files.createDirectories(outputDirPath);

        final Path samplesPath = base.resolve("../../../../stroom-core/src/test/resources/samples").toAbsolutePath().normalize();
        final Path outputFilePath = outputDirPath.resolve("output");
        final Path expectedOutputFilePath = testPath.resolve("expectedOutput");

        // Create input zip file
        final Path rawInputPath = testPath.resolve("input");
        Files.createDirectories(rawInputPath);
        final Path inputFilePath = inputDirPath.resolve("001.zip");
        Files.deleteIfExists(inputFilePath);
        ZipUtil.zip(inputFilePath, rawInputPath);

        // Create config zip file
        final Path contentPacks = tmpPath.resolve("contentPacks");
        Files.createDirectories(contentPacks);
        importXmlSchemas(contentPacks);

        // Copy required config into the temp dir.
        final Path rawConfigPath = tmpPath.resolve("config");
        Files.createDirectories(rawConfigPath);
        final Path configUnzippedDirPath = samplesPath.resolve("config");
        FileUtils.copyDirectory(configUnzippedDirPath.toFile(), rawConfigPath.toFile());

        // Unzip the downloaded content packs into the temp dir.
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(contentPacks)) {
            stream.forEach(file -> {
                try {
                    ZipUtil.unzip(file, rawConfigPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        // Build the config zip file.
        final Path configFilePath = tmpPath.resolve("config.zip");
        Files.deleteIfExists(configFilePath);
        ZipUtil.zip(configFilePath, rawConfigPath);

        final Headless headless = new Headless();

        headless.setConfig(FileUtil.getCanonicalPath(configFilePath));
        headless.setContent(FileUtil.getCanonicalPath(contentDirPath));
        headless.setInput(FileUtil.getCanonicalPath(inputDirPath));
        headless.setOutput(FileUtil.getCanonicalPath(outputFilePath));
        headless.setTmp(FileUtil.getCanonicalPath(newTempDir));
        headless.run();

        final List<String> expectedLines = Files.readAllLines(expectedOutputFilePath, Charset.defaultCharset());
        final List<String> outputLines = Files.readAllLines(outputFilePath, Charset.defaultCharset());

        LOGGER.info("Comparing expected vs actual:\n\t{}\n\t{}",
                expectedOutputFilePath.toAbsolutePath().toString(),
                outputFilePath.toAbsolutePath().toString());

        // same number of lines output as expected
        assertThat(outputLines).hasSize(expectedLines.size());

        // make sure all lines are present in both
        assertThat(new HashSet<>(outputLines)).isEqualTo(new HashSet<>(expectedLines));

        // content should exactly match expected file
        ComparisonHelper.compareFiles(expectedOutputFilePath, outputFilePath);
    }

    private void importXmlSchemas(final Path path) {
        importContentPacks(Arrays.asList(
                ContentPack.of("core-xml-schemas", CORE_XML_SCHEMAS_VERSION),
                ContentPack.of("event-logging-xml-schema", EVENT_LOGGING_XML_SCHEMA_VERSION)
        ), path);
    }

    private void importContentPacks(final List<ContentPack> packs, final Path path) {
        packs.forEach(pack -> ContentPackDownloader.downloadContentPack(pack.getName(), pack.getVersion(), path));
    }
}
