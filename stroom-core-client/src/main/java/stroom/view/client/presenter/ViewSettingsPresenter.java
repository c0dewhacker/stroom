/*
 * Copyright 2022 Crown Copyright
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

package stroom.view.client.presenter;

import stroom.data.client.presenter.EditExpressionPresenter;
import stroom.datasource.api.v2.FieldInfo;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.explorer.client.presenter.EntityDropDownPresenter;
import stroom.explorer.shared.ExplorerTreeFilter;
import stroom.explorer.shared.NodeFlag;
import stroom.meta.shared.MetaFields;
import stroom.pipeline.shared.PipelineDoc;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.client.presenter.SimpleFieldSelectionListModel;
import stroom.security.shared.DocumentPermissionNames;
import stroom.ui.config.client.UiConfigCache;
import stroom.ui.config.shared.QueryConfig;
import stroom.util.shared.GwtNullSafe;
import stroom.view.client.presenter.ViewSettingsPresenter.ViewSettingsView;
import stroom.view.shared.ViewDoc;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;

import java.util.Objects;
import java.util.stream.Collectors;

public class ViewSettingsPresenter extends DocumentEditPresenter<ViewSettingsView, ViewDoc> {

    private final RestFactory restFactory;
    private final EntityDropDownPresenter dataSourceSelectionPresenter;
    private final EntityDropDownPresenter pipelineSelectionPresenter;
    private final EditExpressionPresenter expressionPresenter;
    private boolean isDataSourceSelectionInitialised = false;
    private boolean isPipelineSelectionInitialised = false;

    @Inject
    public ViewSettingsPresenter(final EventBus eventBus,
                                 final ViewSettingsView view,
                                 final RestFactory restFactory,
                                 final EntityDropDownPresenter dataSourceSelectionPresenter,
                                 final EntityDropDownPresenter pipelineSelectionPresenter,
                                 final EditExpressionPresenter expressionPresenter,
                                 final UiConfigCache uiConfigCache) {
        super(eventBus, view);
        this.restFactory = restFactory;
        this.dataSourceSelectionPresenter = dataSourceSelectionPresenter;
        this.pipelineSelectionPresenter = pipelineSelectionPresenter;
        this.expressionPresenter = expressionPresenter;

        view.setDataSourceSelectionView(dataSourceSelectionPresenter.getView());
        view.setPipelineSelectionView(pipelineSelectionPresenter.getView());
        view.setExpressionView(expressionPresenter.getView());

        dataSourceSelectionPresenter.setNodeFlags(NodeFlag.DATA_SOURCE);
        dataSourceSelectionPresenter.setRequiredPermissions(DocumentPermissionNames.USE);

        pipelineSelectionPresenter.setIncludedTypes(PipelineDoc.DOCUMENT_TYPE);
        pipelineSelectionPresenter.setRequiredPermissions(DocumentPermissionNames.USE);

        // Filter the pipeline picker by tags, if configured
        uiConfigCache.get().onSuccess(extendedUiConfig ->
                GwtNullSafe.consume(
                        extendedUiConfig.getQuery(),
                        QueryConfig::getViewPipelineSelectorIncludedTags,
                        ExplorerTreeFilter::createTagQuickFilterInput,
                        pipelineSelectionPresenter::setQuickFilter));
    }

    @Override
    protected void onBind() {
        registerHandlers();
    }

    private void registerHandlers() {
        registerHandler(dataSourceSelectionPresenter.addDataSelectionHandler(event -> {
            final DocRef selectedEntityReference = dataSourceSelectionPresenter.getSelectedEntityReference();
            // Don't want to fire dirty event when the entity is first set
            if (isDataSourceSelectionInitialised) {
                if (!Objects.equals(getEntity().getDataSource(), selectedEntityReference)) {
                    setDirty(true);
                }
            } else {
                isDataSourceSelectionInitialised = true;
            }
        }));
        registerHandler(pipelineSelectionPresenter.addDataSelectionHandler(event -> {
            final DocRef selectedEntityReference = pipelineSelectionPresenter.getSelectedEntityReference();
            // Don't want to fire dirty event when the entity is first set
            if (isPipelineSelectionInitialised) {
                if (!Objects.equals(getEntity().getPipeline(), selectedEntityReference)) {
                    setDirty(true);
                }
            } else {
                isPipelineSelectionInitialised = true;
            }
        }));
        registerHandler(expressionPresenter.addDirtyHandler(event -> setDirty(true)));
    }

    @Override
    protected void onRead(final DocRef docRef, final ViewDoc entity, final boolean readOnly) {
        dataSourceSelectionPresenter.setSelectedEntityReference(entity.getDataSource());
        pipelineSelectionPresenter.setSelectedEntityReference(entity.getPipeline());
        final SimpleFieldSelectionListModel fieldSelectionBoxModel = new SimpleFieldSelectionListModel();
        fieldSelectionBoxModel.addItems(MetaFields
                .getAllFields()
                .stream()
                .map(FieldInfo::create)
                .collect(Collectors.toList()));
        expressionPresenter.init(restFactory, MetaFields.STREAM_STORE_DOC_REF, fieldSelectionBoxModel);

        // Read expression.
        ExpressionOperator root = entity.getFilter();
        if (root == null) {
            root = ExpressionOperator.builder().build();
        }
        expressionPresenter.read(root);
    }

    @Override
    protected ViewDoc onWrite(final ViewDoc entity) {
        entity.setDataSource(dataSourceSelectionPresenter.getSelectedEntityReference());
        entity.setPipeline(pipelineSelectionPresenter.getSelectedEntityReference());
        entity.setFilter(expressionPresenter.write());
        return entity;
    }


    // --------------------------------------------------------------------------------


    public interface ViewSettingsView extends View {

        void setDataSourceSelectionView(View view);

        void setExpressionView(View view);

        void setPipelineSelectionView(View view);
    }
}
