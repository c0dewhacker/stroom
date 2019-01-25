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

import com.google.inject.AbstractModule;
import stroom.cache.impl.CacheModule;
import stroom.data.meta.impl.db.MetaDbModule;

public class ToolModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new CacheModule());
        install(new stroom.activity.impl.mock.MockActivityModule());
        install(new stroom.cluster.impl.MockClusterModule());
        install(new MetaDbModule());
        install(new stroom.data.store.impl.fs.FileSystemDataStoreModule());
        install(new stroom.persist.EntityManagerModule());
        install(new stroom.event.logging.impl.EventLoggingModule());
        install(new stroom.node.impl.NodeServiceModule());
        install(new stroom.security.impl.mock.MockSecurityContextModule());
    }
}