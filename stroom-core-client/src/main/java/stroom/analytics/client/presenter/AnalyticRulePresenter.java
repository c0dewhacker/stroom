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

package stroom.analytics.client.presenter;

import stroom.analytics.shared.AnalyticProcessType;
import stroom.analytics.shared.AnalyticRuleDoc;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.DocumentEditTabProvider;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.entity.client.presenter.MarkdownEditPresenter;
import stroom.entity.client.presenter.MarkdownTabProvider;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import javax.inject.Provider;

public class AnalyticRulePresenter
        extends DocumentEditTabPresenter<LinkTabPanelView, AnalyticRuleDoc> {

    private static final TabData NOTIFICATION = new TabDataImpl("Notification");
    private static final TabData PROCESSING = new TabDataImpl("Processing");
    private static final TabData SHARDS = new TabDataImpl("Shards");
    private static final TabData DOCUMENTATION = new TabDataImpl("Documentation");

    @Inject
    public AnalyticRulePresenter(final EventBus eventBus,
                                 final LinkTabPanelView view,
                                 final Provider<AnalyticNotificationPresenter> notificationPresenterProvider,
                                 final Provider<AnalyticProcessingPresenter> processPresenterProvider,
                                 final Provider<AnalyticDataShardsPresenter> analyticDataShardsPresenterProvider,
                                 final Provider<MarkdownEditPresenter> markdownEditPresenterProvider) {
        super(eventBus, view);

        final AnalyticProcessingPresenter analyticProcessingPresenter = processPresenterProvider.get();
        analyticProcessingPresenter.addChangeDataHandler(e ->
                setRuleType(analyticProcessingPresenter.getView().getProcessingType()));

        addTab(PROCESSING, new DocumentEditTabProvider<>(() -> analyticProcessingPresenter));
        addTab(NOTIFICATION, new DocumentEditTabProvider<>(notificationPresenterProvider::get));
        addTab(SHARDS, new DocumentEditTabProvider<>(analyticDataShardsPresenterProvider::get));
        addTab(DOCUMENTATION, new MarkdownTabProvider<AnalyticRuleDoc>(eventBus, markdownEditPresenterProvider) {
            @Override
            public void onRead(final MarkdownEditPresenter presenter,
                               final DocRef docRef,
                               final AnalyticRuleDoc document,
                               final boolean readOnly) {
                presenter.setText(document.getDescription());
                presenter.setReadOnly(readOnly);
            }

            @Override
            public AnalyticRuleDoc onWrite(final MarkdownEditPresenter presenter,
                                           final AnalyticRuleDoc document) {
                return document.copy().description(presenter.getText()).build();
            }
        });
        selectTab(PROCESSING);
    }

    @Override
    protected void onRead(final DocRef docRef, final AnalyticRuleDoc document, final boolean readOnly) {
        super.onRead(docRef, document, readOnly);
        setRuleType(document.getAnalyticProcessType());
    }

    private void setRuleType(final AnalyticProcessType analyticProcessType) {
        setTabHidden(SHARDS, analyticProcessType != AnalyticProcessType.TABLE_BUILDER);
    }

    @Override
    public String getType() {
        return AnalyticRuleDoc.DOCUMENT_TYPE;
    }
}
