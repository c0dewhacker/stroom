/*
 * Copyright 2016 Crown Copyright
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

package stroom.analytics.client.view;

import stroom.analytics.client.presenter.AnalyticRuleSettingsPresenter.AnalyticRuleSettingsView;
import stroom.analytics.shared.AnalyticRuleType;
import stroom.analytics.shared.QueryLanguageVersion;
import stroom.document.client.event.DirtyUiHandlers;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.item.client.ItemListBox;
import stroom.util.shared.time.SimpleDuration;
import stroom.widget.customdatebox.client.DurationPicker;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class AnalyticRuleSettingsViewImpl
        extends ViewWithUiHandlers<DirtyUiHandlers>
        implements AnalyticRuleSettingsView, ReadOnlyChangeHandler {

    private final Widget widget;

    @UiField
    ItemListBox<QueryLanguageVersion> languageVersion;
    @UiField
    ItemListBox<AnalyticRuleType> analyticRuleType;
    @UiField
    DurationPicker dataRetention;

    @Inject
    public AnalyticRuleSettingsViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);

        languageVersion.addItem(QueryLanguageVersion.STROOM_QL_VERSION_0_1);
        languageVersion.addItem(QueryLanguageVersion.SIGMA);

        languageVersion.addSelectionHandler(e -> getUiHandlers().onDirty());
        analyticRuleType.addSelectionHandler(e -> {
            getUiHandlers().onDirty();
        });

        analyticRuleType.addItem(AnalyticRuleType.EVENT);
        analyticRuleType.addItem(AnalyticRuleType.AGGREGATE);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public QueryLanguageVersion getLanguageVersion() {
        return languageVersion.getSelectedItem();
    }

    public void setLanguageVersion(final QueryLanguageVersion languageVersion) {
        if (languageVersion == null) {
            this.languageVersion.setSelectedItem(QueryLanguageVersion.STROOM_QL_VERSION_0_1);
        } else {
            this.languageVersion.setSelectedItem(languageVersion);
        }
    }

    @Override
    public AnalyticRuleType getAnalyticRuleType() {
        return this.analyticRuleType.getSelectedItem();
    }

    @Override
    public void setAnalyticRuleType(final AnalyticRuleType analyticRuleType) {
        this.analyticRuleType.setSelectedItem(analyticRuleType);
    }

    @Override
    public SimpleDuration getDataRetention() {
        return dataRetention.getValue();
    }

    @Override
    public void setDataRetention(final SimpleDuration dataRetention) {
        this.dataRetention.setValue(dataRetention);
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
    }

    @UiHandler("dataRetention")
    public void onDataRetention(final ValueChangeEvent<SimpleDuration> event) {
        getUiHandlers().onDirty();
    }

    public interface Binder extends UiBinder<Widget, AnalyticRuleSettingsViewImpl> {

    }
}