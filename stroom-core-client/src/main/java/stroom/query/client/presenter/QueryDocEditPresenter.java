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
import stroom.analytics.shared.AnalyticNotificationConfig;
import stroom.analytics.shared.AnalyticNotificationDestinationType;
import stroom.analytics.shared.AnalyticNotificationStreamDestination;
import stroom.analytics.shared.AnalyticProcessType;
import stroom.analytics.shared.AnalyticRuleDoc;
import stroom.analytics.shared.AnalyticRuleResource;
import stroom.analytics.shared.QueryLanguageVersion;
import stroom.analytics.shared.ScheduledQueryAnalyticProcessConfig;
import stroom.analytics.shared.StreamingAnalyticProcessConfig;
import stroom.analytics.shared.TableBuilderAnalyticProcessConfig;
import stroom.dashboard.shared.ValidateExpressionResult;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.document.client.event.OpenDocumentEvent;
import stroom.document.client.event.ShowCreateDocumentDialogEvent;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.entity.client.presenter.HasToolbar;
import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.ExplorerResource;
import stroom.query.client.presenter.QueryEditPresenter.QueryEditView;
import stroom.query.shared.QueryDoc;
import stroom.query.shared.QueryResource;
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
    private static final QueryResource QUERY_RESOURCE = GWT.create(QueryResource.class);

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
        registerHandler(createRuleButton.addClickHandler(event -> createRule()));
    }

    private void createRule() {
        uiConfigCache.get().onSuccess(uiConfig -> {
            final AnalyticUiDefaultConfig analyticUiDefaultConfig = uiConfig.getAnalyticUiDefaultConfig();
            if (analyticUiDefaultConfig.getDefaultErrorFeed() == null) {
                AlertEvent.fireError(this, "No default error feed configured", null);
            } else if (analyticUiDefaultConfig.getDefaultDestinationFeed() == null) {
                AlertEvent.fireError(this, "No default destination feed configured", null);
            } else if (analyticUiDefaultConfig.getDefaultNode() == null) {
                AlertEvent.fireError(this, "No default processing node configured", null);
            } else {
                final String query = queryEditPresenter.getQuery();
                final Rest<ValidateExpressionResult> rest = restFactory.create();
                rest
                        .onSuccess(validateExpressionResult -> {
                            if (!validateExpressionResult.isOk()) {
                                AlertEvent.fireError(this,
                                        validateExpressionResult.getString(),
                                        null);
                            } else {
                                AnalyticProcessType analyticProcessType = AnalyticProcessType.STREAMING;
                                if (validateExpressionResult.isGroupBy()) {
                                    analyticProcessType = AnalyticProcessType.SCHEDULED_QUERY;
                                }
                                createRule(analyticUiDefaultConfig, query, analyticProcessType);
                            }
                        })
                        .onFailure(throwable -> {
                            AlertEvent.fireErrorFromException(this, throwable, null);
                        })
                        .call(QUERY_RESOURCE)
                        .validateQuery(query);
            }
        });
    }

    private void createRule(final AnalyticUiDefaultConfig analyticUiDefaultConfig,
                            final String query,
                            final AnalyticProcessType analyticProcessType) {
        final Consumer<ExplorerNode> newDocumentConsumer = newNode -> {
            final DocRef ruleDoc = newNode.getDocRef();
            loadNewRule(ruleDoc, analyticUiDefaultConfig, query, analyticProcessType);
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

    private void loadNewRule(final DocRef ruleDocRef,
                             final AnalyticUiDefaultConfig analyticUiDefaultConfig,
                             final String query,
                             final AnalyticProcessType analyticProcessType) {
        final Rest<AnalyticRuleDoc> rest = restFactory.create();
        rest
                .onSuccess(doc -> {
                    // Create default config.
                    switch (analyticProcessType) {
                        case SCHEDULED_QUERY:
                            createDefaultScheduledRule(ruleDocRef, doc, analyticUiDefaultConfig, query);
                            break;
                        case TABLE_BUILDER:
                            createDefaultTableBuilderRule(ruleDocRef, doc, analyticUiDefaultConfig, query);
                            break;
                        default:
                            createDefaultStreamingRule(ruleDocRef, doc, analyticUiDefaultConfig, query);
                    }
                })
                .call(ANALYTIC_RULE_RESOURCE)
                .fetch(ruleDocRef.getUuid());
    }

    private void createDefaultStreamingRule(final DocRef ruleDocRef,
                                            final AnalyticRuleDoc doc,
                                            final AnalyticUiDefaultConfig analyticUiDefaultConfig,
                                            final String query) {
        final StreamingAnalyticProcessConfig analyticProcessConfig =
                StreamingAnalyticProcessConfig.builder()
                        .errorFeed(analyticUiDefaultConfig.getDefaultErrorFeed())
                        .build();
        AnalyticRuleDoc updated = doc
                .copy()
                .languageVersion(QueryLanguageVersion.STROOM_QL_VERSION_0_1)
                .query(query)
                .analyticProcessType(AnalyticProcessType.STREAMING)
                .analyticProcessConfig(analyticProcessConfig)
                .analyticNotificationConfig(createDefaultNotificationConfig(analyticUiDefaultConfig))
                .build();
        updateRule(ruleDocRef, updated);
    }

    private void createDefaultScheduledRule(final DocRef ruleDocRef,
                                            final AnalyticRuleDoc doc,
                                            final AnalyticUiDefaultConfig analyticUiDefaultConfig,
                                            final String query) {
        final SimpleDuration oneHour = SimpleDuration.builder().time(1).timeUnit(TimeUnit.HOURS).build();
        final ScheduledQueryAnalyticProcessConfig analyticProcessConfig =
                ScheduledQueryAnalyticProcessConfig.builder()
                        .node(analyticUiDefaultConfig.getDefaultNode())
                        .errorFeed(analyticUiDefaultConfig.getDefaultErrorFeed())
                        .minEventTimeMs(System.currentTimeMillis())
                        .maxEventTimeMs(null)
                        .queryFrequency(oneHour)
                        .timeToWaitForData(oneHour)
                        .build();
        AnalyticRuleDoc updated = doc
                .copy()
                .languageVersion(QueryLanguageVersion.STROOM_QL_VERSION_0_1)
                .query(query)
                .analyticProcessType(AnalyticProcessType.SCHEDULED_QUERY)
                .analyticProcessConfig(analyticProcessConfig)
                .analyticNotificationConfig(createDefaultNotificationConfig(analyticUiDefaultConfig))
                .build();
        updateRule(ruleDocRef, updated);
    }

    private void createDefaultTableBuilderRule(final DocRef ruleDocRef,
                                               final AnalyticRuleDoc doc,
                                               final AnalyticUiDefaultConfig analyticUiDefaultConfig,
                                               final String query) {
        final SimpleDuration oneHour = SimpleDuration.builder().time(1).timeUnit(TimeUnit.HOURS).build();
        final TableBuilderAnalyticProcessConfig analyticProcessConfig =
                TableBuilderAnalyticProcessConfig.builder()
                        .node(analyticUiDefaultConfig.getDefaultNode())
                        .errorFeed(analyticUiDefaultConfig.getDefaultErrorFeed())
                        .minMetaCreateTimeMs(System.currentTimeMillis())
                        .maxMetaCreateTimeMs(null)
                        .timeToWaitForData(oneHour)
                        .build();
        AnalyticRuleDoc updated = doc
                .copy()
                .languageVersion(QueryLanguageVersion.STROOM_QL_VERSION_0_1)
                .query(query)
                .analyticProcessType(AnalyticProcessType.TABLE_BUILDER)
                .analyticProcessConfig(analyticProcessConfig)
                .analyticNotificationConfig(createDefaultNotificationConfig(analyticUiDefaultConfig))
                .build();
        updateRule(ruleDocRef, updated);
    }

    private AnalyticNotificationConfig createDefaultNotificationConfig(
            final AnalyticUiDefaultConfig analyticUiDefaultConfig) {
        final AnalyticNotificationStreamDestination destination =
                AnalyticNotificationStreamDestination.builder()
                        .useSourceFeedIfPossible(false)
                        .destinationFeed(analyticUiDefaultConfig.getDefaultDestinationFeed())
                        .build();
        return AnalyticNotificationConfig
                .builder()
                .limitNotifications(false)
                .maxNotifications(100)
                .resumeAfter(SimpleDuration.builder().time(1).timeUnit(TimeUnit.HOURS).build())
                .destinationType(AnalyticNotificationDestinationType.STREAM)
                .destination(destination)
                .build();
    }

    private void updateRule(final DocRef ruleDocRef,
                            final AnalyticRuleDoc ruleDoc) {
        final Rest<AnalyticRuleDoc> rest = restFactory.create();
        rest
                .onSuccess(doc -> OpenDocumentEvent.fire(
                        QueryDocEditPresenter.this,
                        ruleDocRef,
                        true,
                        false))
                .call(ANALYTIC_RULE_RESOURCE)
                .update(ruleDocRef.getUuid(), ruleDoc);
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
