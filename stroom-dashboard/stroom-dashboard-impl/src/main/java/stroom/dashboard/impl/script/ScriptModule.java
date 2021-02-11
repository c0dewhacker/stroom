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

package stroom.dashboard.impl.script;

import stroom.docstore.api.DocumentActionHandlerBinder;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.script.shared.ScriptDoc;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.RestResourcesBinder;
import stroom.util.guice.ServletBinder;

import com.google.inject.AbstractModule;

public class ScriptModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ScriptStore.class).to(ScriptStoreImpl.class);

        ServletBinder.create(binder())
                .bind(ScriptServlet.class);

        GuiceUtil.buildMultiBinder(binder(), ExplorerActionHandler.class)
                .addBinding(ScriptStoreImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ImportExportActionHandler.class)
                .addBinding(ScriptStoreImpl.class);

        DocumentActionHandlerBinder.create(binder())
                .bind(ScriptDoc.DOCUMENT_TYPE, ScriptStoreImpl.class);

        RestResourcesBinder.create(binder())
                .bind(ScriptResourceImpl.class);
    }
}
