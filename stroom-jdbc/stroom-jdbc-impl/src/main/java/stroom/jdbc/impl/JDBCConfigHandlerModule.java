package stroom.jdbc.impl;

import stroom.docstore.api.DocumentActionHandlerBinder;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.jdbc.shared.JDBCConfigDoc;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.RestResourcesBinder;

import com.google.inject.AbstractModule;

public class JDBCConfigHandlerModule extends AbstractModule {

    @Override
    protected void configure() {
        GuiceUtil.buildMultiBinder(binder(), ExplorerActionHandler.class)
                .addBinding(JDBCConfigStoreImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ImportExportActionHandler.class)
                .addBinding(JDBCConfigStoreImpl.class);

        DocumentActionHandlerBinder.create(binder())
                .bind(JDBCConfigDoc.DOCUMENT_TYPE, JDBCConfigStoreImpl.class);

        RestResourcesBinder.create(binder())
                .bind(JDBCConfigResourceImpl.class);
    }
}
