/*
 * Copyright 2019 Crown Copyright
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

package stroom.jdbc.impl;

import stroom.docstore.api.DocumentActionHandlerBinder;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.jdbc.shared.JDBCConfigDoc;
import stroom.util.guice.GuiceUtil;

import stroom.util.shared.Clearable;

import com.google.inject.AbstractModule;


public class JDBCConfigModule extends AbstractModule {

    @Override
    protected void configure() {
//        bind(KafkaProducerFactory.class).to(KafkaProducerFactoryImpl.class);
        bind(JDBCConfigStore.class).to(JDBCConfigStoreImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ExplorerActionHandler.class)
                .addBinding(JDBCConfigStoreImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ImportExportActionHandler.class)
                .addBinding(JDBCConfigStoreImpl.class);

        DocumentActionHandlerBinder.create(binder())
                .bind(JDBCConfigDoc.DOCUMENT_TYPE, JDBCConfigStoreImpl.class);

//        HasSystemInfoBinder.create(binder())
//                .bind(KafkaProducerFactoryImpl.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class)
                .addBinding(JDBCConfigDocCache.class);

//        LifecycleBinder.create(binder())
//                .bindShutdownTaskTo(KafkaProducerFactoryShutdown.class);
    }
}
