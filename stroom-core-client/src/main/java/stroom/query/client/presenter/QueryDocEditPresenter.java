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

package stroom.query.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.analytics.shared.AnalyticNotification;
import stroom.analytics.shared.AnalyticNotificationResource;
import stroom.analytics.shared.AnalyticNotificationStreamConfig;
import stroom.analytics.shared.AnalyticProcessorFilter;
import stroom.analytics.shared.AnalyticProcessorFilterResource;
import stroom.analytics.shared.AnalyticRuleDoc;
import stroom.analytics.shared.AnalyticRuleResource;
import stroom.analytics.shared.AnalyticRuleType;
import stroom.analytics.shared.QueryLanguageVersion;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.document.client.event.RefreshDocumentEvent;
import stroom.document.client.event.ShowCreateDocumentDialogEvent;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.entity.client.presenter.HasToolbar;
import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.ExplorerResource;
import stroom.query.client.presenter.QueryEditPresenter.QueryEditView;
import stroom.query.shared.QueryDoc;
import stroom.svg.shared.SvgImage;
import stroom.ui.config.client.UiConfigCache;
import stroom.ui.config.shared.AnalyticUiDefaultConfig;
import stroom.util.shared.time.SimpleDuration;
import stroom.util.shared.time.TimeUnit;
import stroom.widget.button.client.ButtonPanel;
import stroom.widget.button.client.InlineSvgButton;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;

public class QueryDocEditPresenter extends DocumentEditPresenter<QueryEditView, QueryDoc> implements HasToolbar {

    private static final ExplorerResource EXPLORER_RESOURCE = GWT.create(ExplorerResource.class);
    private static final AnalyticRuleResource ANALYTIC_RULE_RESOURCE = GWT.create(AnalyticRuleResource.class);
    private static final AnalyticNotificationResource ANALYTIC_NOTIFICATION_RESOURCE =
            GWT.create(AnalyticNotificationResource.class);
    private static final AnalyticProcessorFilterResource ANALYTIC_PROCESSOR_FILTER_RESOURCE =
            GWT.create(AnalyticProcessorFilterResource.class);

    private final QueryEditPresenter queryEditPresenter;
    private final RestFactory restFactory;
    private final UiConfigCache uiConfigCache;
    private final InlineSvgButton createRuleButton;
    private final ButtonPanel toolbar;
    private DocRef docRef;

    @Inject
    public QueryDocEditPresenter(final EventBus eventBus,
                                 final QueryEditPresenter queryEditPresenter,
                                 final RestFactory restFactory,
                                 final UiConfigCache uiConfigCache) {
        super(eventBus, queryEditPresenter.getView());
        this.queryEditPresenter = queryEditPresenter;
        this.restFactory = restFactory;
        this.uiConfigCache = uiConfigCache;

        createRuleButton = new InlineSvgButton();
        createRuleButton.setSvg(SvgImage.DOCUMENT_ANALYTIC_RULE);
        createRuleButton.setTitle("Create Rule");
        createRuleButton.setVisible(true);

        toolbar = new ButtonPanel();
        toolbar.addButton(createRuleButton);
    }

    @Override
    public List<Widget> getToolbars() {
        final List<Widget> list = new ArrayList<>();
        list.add(toolbar);
        list.addAll(queryEditPresenter.getToolbars());
        return list;
    }

    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(queryEditPresenter.addDirtyHandler(event -> {
            if (event.isDirty()) {
                setDirty(true);
            }
        }));
        registerHandler(createRuleButton.addClickHandler(event -> {
            createRule();
        }));
    }

    private void createRule() {
        uiConfigCache.get().onSuccess(uiConfig -> {
            final AnalyticUiDefaultConfig analyticUiDefaultConfig = uiConfig.getAnalyticUiDefaultConfig();
            if (analyticUiDefaultConfig.getDefaultFeed() == null) {
                AlertEvent.fireError(this, "No default destination feed configured", null);
            } else if (analyticUiDefaultConfig.getDefaultNode() == null) {
                AlertEvent.fireError(this, "No default processing node configured", null);
            } else {
                createRule(analyticUiDefaultConfig);
            }
        });
    }

    private void createRule(final AnalyticUiDefaultConfig analyticUiDefaultConfig) {
        final Consumer<ExplorerNode> newDocumentConsumer = newNode -> {
            final DocRef ruleDoc = newNode.getDocRef();
            loadNewRule(ruleDoc, analyticUiDefaultConfig);
        };

        // First get the explorer node for the docref.
        final Rest<ExplorerNode> rest = restFactory.create();
        rest
                .onSuccess(explorerNode -> {
                    // Ask the user to create a new document.
                    ShowCreateDocumentDialogEvent.fire(
                            this,
                            "Create New Analytic Rule",
                            explorerNode,
                            AnalyticRuleDoc.DOCUMENT_TYPE,
                            docRef.getName(),
                            true,
                            newDocumentConsumer);
                })
                .call(EXPLORER_RESOURCE)
                .getFromDocRef(docRef);
    }

    private void loadNewRule(final DocRef ruleDocRef, final AnalyticUiDefaultConfig analyticUiDefaultConfig) {
        final Rest<AnalyticRuleDoc> rest = restFactory.create();
        rest
                .onSuccess(doc -> {
                    AnalyticRuleDoc updated = doc
                            .copy()
                            .languageVersion(QueryLanguageVersion.STROOM_QL_VERSION_0_1)
                            .query(queryEditPresenter.getQuery())
                            .dataRetention(SimpleDuration.builder().time(1).timeUnit(TimeUnit.DAYS).build())
                            .analyticRuleType(AnalyticRuleType.BATCH_QUERY)
                            .build();
                    updateRule(ruleDocRef, updated, analyticUiDefaultConfig);
                })
                .call(ANALYTIC_RULE_RESOURCE)
                .fetch(ruleDocRef.getUuid());
    }

    private void updateRule(final DocRef ruleDocRef,
                            final AnalyticRuleDoc ruleDoc,
                            final AnalyticUiDefaultConfig analyticUiDefaultConfig) {
        final Rest<AnalyticRuleDoc> rest = restFactory.create();
        rest
                .onSuccess(doc -> createNewNotification(ruleDocRef, ruleDoc, analyticUiDefaultConfig))
                .call(ANALYTIC_RULE_RESOURCE)
                .update(ruleDocRef.getUuid(), ruleDoc);
    }

    private void createNewNotification(final DocRef ruleDocRef,
                                       final AnalyticRuleDoc ruleDoc,
                                       final AnalyticUiDefaultConfig analyticUiDefaultConfig) {
        final AnalyticNotificationStreamConfig config = AnalyticNotificationStreamConfig
                .builder()
                .timeToWaitForData(SimpleDuration.builder().time(1).timeUnit(TimeUnit.HOURS).build())
                .useSourceFeedIfPossible(false)
                .destinationFeed(analyticUiDefaultConfig.getDefaultFeed())
                .build();
        final AnalyticNotification newNotification = AnalyticNotification
                .builder()
                .analyticUuid(ruleDocRef.getUuid())
                .enabled(true)
                .config(config)
                .build();
        final Rest<AnalyticNotification> rest = restFactory.create();
        rest
                .onSuccess(result -> createNewProcessorFilter(ruleDocRef, ruleDoc, analyticUiDefaultConfig))
                .call(ANALYTIC_NOTIFICATION_RESOURCE)
                .create(newNotification);
    }

    private void createNewProcessorFilter(final DocRef ruleDocRef,
                                          final AnalyticRuleDoc ruleDoc,
                                          final AnalyticUiDefaultConfig analyticUiDefaultConfig) {
        final AnalyticProcessorFilter newFilter = AnalyticProcessorFilter.builder()
                .analyticUuid(ruleDocRef.getUuid())
                .enabled(true)
                .minMetaCreateTimeMs(System.currentTimeMillis())
                .maxMetaCreateTimeMs(null)
                .node(analyticUiDefaultConfig.getDefaultNode())
                .build();
        final Rest<AnalyticProcessorFilter> rest = restFactory.create();
        rest
                .onSuccess(result ->
                        AlertEvent.fireInfo(QueryDocEditPresenter.this,
                                "Created new rule '" +
                                        ruleDocRef.getName() +
                                        "'", () ->
                                        RefreshDocumentEvent.fire(QueryDocEditPresenter.this, ruleDocRef)))
                .call(ANALYTIC_PROCESSOR_FILTER_RESOURCE)
                .create(newFilter);
    }

    @Override
    public void onRead(final DocRef docRef, final QueryDoc entity, final boolean readOnly) {
        this.docRef = docRef;
        queryEditPresenter.setQuery(docRef, entity.getQuery(), readOnly);
    }

    @Override
    protected QueryDoc onWrite(final QueryDoc entity) {
        entity.setQuery(queryEditPresenter.getQuery());
        return entity;
    }

    @Override
    public void onClose() {
        queryEditPresenter.onClose();
        super.onClose();
    }
}
