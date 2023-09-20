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
 *
 */

package stroom.jdbc.client;

import stroom.core.client.ContentManager;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.docstore.shared.DocRefUtil;
import stroom.document.client.DocumentPlugin;
import stroom.document.client.DocumentPluginEventManager;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.jdbc.client.presenter.JDBCConfigPresenter;
import stroom.jdbc.shared.JDBCConfigDoc;
import stroom.jdbc.shared.JDBCConfigResource;
import stroom.security.client.api.ClientSecurityContext;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

import java.util.function.Consumer;
import javax.inject.Singleton;

@Singleton
public class JDBCConfigPlugin extends DocumentPlugin<JDBCConfigDoc> {

    private static final JDBCConfigResource JDBC_CONFIG_RESOURCE = GWT.create(JDBCConfigResource.class);
    private final Provider<JDBCConfigPresenter> editorProvider;

    private final RestFactory restFactory;

    @Inject
    public JDBCConfigPlugin(final EventBus eventBus,
                             final Provider<JDBCConfigPresenter> editorProvider,
                             final RestFactory restFactory,
                             final ContentManager contentManager,
                             final DocumentPluginEventManager entityPluginEventManager,
                             final ClientSecurityContext securityContext) {
        super(eventBus, contentManager, entityPluginEventManager, securityContext);
        this.editorProvider = editorProvider;
        this.restFactory = restFactory;
    }

    @Override
    protected DocumentEditPresenter<?, ?> createEditor() {
        return editorProvider.get();
    }

    @Override
    public String getType() {
        return JDBCConfigDoc.DOCUMENT_TYPE;
    }

    @Override
    protected DocRef getDocRef(final JDBCConfigDoc document) {
        return DocRefUtil.create(document);
    }

    @Override
    public void load(final DocRef docRef,
                     final Consumer<JDBCConfigDoc> resultConsumer,
                     final Consumer<Throwable> errorConsumer) {
        final Rest<JDBCConfigDoc> rest = restFactory.create();
        rest
                .onSuccess(resultConsumer)
                .onFailure(errorConsumer)
                .call(JDBC_CONFIG_RESOURCE)
                .fetch(docRef.getUuid());
    }

    @Override
    public void save(final DocRef docRef,
                     final JDBCConfigDoc document,
                     final Consumer<JDBCConfigDoc> resultConsumer,
                     final Consumer<Throwable> errorConsumer) {
        final Rest<JDBCConfigDoc> rest = restFactory.create();
        rest
                .onSuccess(resultConsumer)
                .onFailure(errorConsumer)
                .call(JDBC_CONFIG_RESOURCE)
                .update(document.getUuid(), document);
    }
}
