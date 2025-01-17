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

package stroom.data.store.util;

import stroom.data.shared.StreamTypeNames;
import stroom.data.store.api.Store;
import stroom.data.store.api.Target;
import stroom.data.store.api.TargetUtil;
import stroom.data.store.impl.fs.FsVolumeConfig;
import stroom.data.store.impl.fs.FsVolumeService;
import stroom.meta.api.MetaProperties;
import stroom.test.common.util.db.DbTestModule;
import stroom.test.common.util.db.DbTestUtil;
import stroom.test.common.util.test.FileSystemTestUtil;
import stroom.util.io.HomeDirProviderImpl;
import stroom.util.io.TempDirProviderImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class TestStreamGrepTool {

    @Inject
    private Store streamStore;
    @Inject
    private FsVolumeConfig volumeConfig;
    @Inject
    private FsVolumeService fsVolumeService;
    @Inject
    private HomeDirProviderImpl homeDirProvider;
    @Inject
    private TempDirProviderImpl tempDirProvider;

    @Mock
    private ToolInjector toolInjector;

    @TempDir
    static Path tempDir;

    @BeforeEach
    void setup() {
        final Injector injector = Guice.createInjector(
                new DbTestModule(),
                new ToolModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        super.configure();
                        bind(FsVolumeConfig.class)
                                .toProvider(() -> getVolumeConfig());
                    }
                });
        injector.injectMembers(this);

        homeDirProvider.setHomeDir(tempDir);
        tempDirProvider.setTempDir(tempDir);

        Mockito.when(toolInjector.getInjector())
                .thenReturn(injector);

        // Clear the current DB.
        DbTestUtil.clear();

        // Clear any lingering volumes or data. Need to do this after the db clear
        // to ensure the local vol list gets reset
        fsVolumeService.clear();
        fsVolumeService.ensureDefaultVolumes();
    }

    private FsVolumeConfig getVolumeConfig() {
        final String path = tempDir
                .resolve("volumes/defaultStreamVolume")
                .toAbsolutePath()
                .toString();

        return new FsVolumeConfig()
                .withDefaultStreamVolumePaths(List.of(path));
    }

    @Test
    void test() {
        final String feedName = FileSystemTestUtil.getUniqueTestString();

        try {
            addData(feedName, "This is some test data to match on");
            addData(feedName, "This is some test data to not match on");

            final StreamGrepTool streamGrepTool = new StreamGrepTool(toolInjector);
            streamGrepTool.setFeed(feedName);

            streamGrepTool.run();

        } catch (final RuntimeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void addData(final String feedName, final String data) {
        final MetaProperties metaProperties = MetaProperties.builder()
                .feedName(feedName)
                .typeName(StreamTypeNames.RAW_EVENTS)
                .build();
        try (final Target streamTarget = streamStore.openTarget(metaProperties)) {
            TargetUtil.write(streamTarget, data);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
