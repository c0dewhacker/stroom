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

package stroom.annotation.client;

import stroom.annotation.client.ChooserPresenter.ChooserView;
import stroom.ui.config.client.UiConfigCache;
import stroom.widget.dropdowntree.client.view.QuickFilter;
import stroom.widget.dropdowntree.client.view.QuickFilterTooltipUtil;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

class ChooserViewImpl extends ViewWithUiHandlers<ChooserUiHandlers> implements ChooserView {

    public interface Binder extends UiBinder<Widget, ChooserViewImpl> {

    }

    @UiField
    QuickFilter nameFilter;
    @UiField
    FlowPanel bottom;
    @UiField
    Label clearSelection;

    private final Widget widget;

    @Inject
    ChooserViewImpl(final Binder binder,
                    final UiConfigCache uiConfigCache) {
        widget = binder.createAndBindUi(this);

        // Only deals in lists of strings so no field defs required.
        uiConfigCache.get()
                .onSuccess(uiConfig ->
                        nameFilter.registerPopupTextProvider(() ->
                                QuickFilterTooltipUtil.createTooltip(
                                        "Choose Item Quick Filter",
                                        uiConfig.getHelpUrlQuickFilter())));
    }

    @Override
    public void setBottomWidget(final Widget widget) {
        widget.getElement().addClassName("w-100");
        bottom.add(widget);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void clearFilter() {
        nameFilter.clear();
    }

    @Override
    public void setClearSelectionText(final String text) {
        if (text != null && !text.isEmpty()) {
            clearSelection.setText(text);
            clearSelection.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
        } else {
            clearSelection.setText("");
            clearSelection.getElement().getStyle().setDisplay(Display.NONE);
        }
    }

    @UiHandler("nameFilter")
    public void onFilterChange(final ValueChangeEvent<String> e) {
        if (getUiHandlers() != null) {
            getUiHandlers().onFilterChange(e.getValue());
        }
    }

    @UiHandler("clearSelection")
    public void onClearSelectionClick(final ClickEvent e) {
        if (getUiHandlers() != null) {
            getUiHandlers().onClearSelection();
        }
    }
}
