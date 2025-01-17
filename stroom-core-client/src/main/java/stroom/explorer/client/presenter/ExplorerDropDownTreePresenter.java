/*
 * Copyright 2017 Crown Copyright
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

package stroom.explorer.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.data.client.event.DataSelectionEvent;
import stroom.data.client.event.DataSelectionEvent.DataSelectionHandler;
import stroom.data.client.event.HasDataSelectionHandlers;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.explorer.shared.DocumentTypes;
import stroom.explorer.shared.ExplorerConstants;
import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.ExplorerResource;
import stroom.explorer.shared.ExplorerTreeFilter;
import stroom.explorer.shared.NodeFlag;
import stroom.ui.config.client.UiConfigCache;
import stroom.widget.dropdowntree.client.view.DropDownTreeUiHandlers;
import stroom.widget.dropdowntree.client.view.DropDownTreeView;
import stroom.widget.dropdowntree.client.view.QuickFilterTooltipUtil;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupType;
import stroom.widget.util.client.SelectionType;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;

class ExplorerDropDownTreePresenter
        extends MyPresenterWidget<DropDownTreeView>
        implements DropDownTreeUiHandlers, HasDataSelectionHandlers<ExplorerNode> {

    private static final ExplorerResource EXPLORER_RESOURCE = GWT.create(ExplorerResource.class);

    private final ExtendedExplorerTree explorerTree;
    private final RestFactory restFactory;
    private boolean allowFolderSelection;
    private String caption = "Choose item";
    private ExplorerNode selectedExplorerNode;
    private String initialQuickFilter;

    @Inject
    ExplorerDropDownTreePresenter(final EventBus eventBus,
                                  final DropDownTreeView view,
                                  final RestFactory restFactory,
                                  final UiConfigCache uiConfigCache) {
        super(eventBus, view);
        this.restFactory = restFactory;

        getView().setUiHandlers(this);

        explorerTree = new ExtendedExplorerTree(this, restFactory);
        setIncludeNullSelection(true);

        // Add views.
        view.setCellTree(explorerTree);

        uiConfigCache.get()
                .onSuccess(uiConfig ->
                        view.setQuickFilterTooltipSupplier(() -> QuickFilterTooltipUtil.createTooltip(
                                "Choose Item Quick Filter",
                                ExplorerTreeFilter.FIELD_DEFINITIONS,
                                uiConfig.getHelpUrlQuickFilter())));
    }

    @Override
    protected void onHide() {
    }

    public void show() {
        refresh();
        final PopupSize popupSize = PopupSize.resizable(500, 550);
        ShowPopupEvent.builder(this)
                .popupType(PopupType.OK_CANCEL_DIALOG)
                .popupSize(popupSize)
                .caption(caption)
                .onShow(e -> getView().focus())
                .onHideRequest(e -> {
                    if (e.isOk()) {
                        final ExplorerNode selected = getSelectedEntityData();
                        if (isSelectionAllowed(selected)) {
                            DataSelectionEvent.fire(ExplorerDropDownTreePresenter.this, selected, false);
                            selectedExplorerNode = selected;
                            e.hide();
                        } else {
                            AlertEvent.fireError(ExplorerDropDownTreePresenter.this,
                                    "You must choose a valid item.", null);
                        }
                    } else {
                        e.hide();
                    }
                })
                .fire();
    }

    protected void setIncludeNullSelection(final boolean includeNullSelection) {
        explorerTree.getTreeModel().setIncludeNullSelection(includeNullSelection);
    }

    protected void setSelectedTreeItem(final ExplorerNode selectedItem,
                                       final SelectionType selectionType,
                                       final boolean initial) {
        // Is the selection type valid?
        if (isSelectionAllowed(selectedItem)) {
            // Drop down presenters need to know what the initial selection was so that they can
            // update the name of their selected item properly.
            if (initial) {
                DataSelectionEvent.fire(this, selectedItem, false);
            } else if (selectionType.isDoubleSelect()) {
                DataSelectionEvent.fire(this, selectedItem, true);
                this.selectedExplorerNode = selectedItem;
                HidePopupEvent.builder(this).fire();
            }
        }
    }

    private boolean isSelectionAllowed(final ExplorerNode selected) {
        if (selected == null) {
            return true;
        }
        if (allowFolderSelection) {
            return true;
        }

        return !DocumentTypes.isFolder(selected.getType());
    }

    @Override
    public void nameFilterChanged(final String text) {
//        GWT.log("nameFilterChanged: " + text);
        explorerTree.changeNameFilter(text);
    }

    public void refresh() {
        // Refresh gets called on show so no point doing it before then
        explorerTree.setSelectedItem(selectedExplorerNode);
        getView().setQuickFilter(initialQuickFilter);
        explorerTree.getTreeModel().setInitialNameFilter(initialQuickFilter);
        explorerTree.getTreeModel().reset(initialQuickFilter);
        explorerTree.getTreeModel().setEnsureVisible(selectedExplorerNode);
        explorerTree.getTreeModel().refresh();
    }

    public void setIncludedTypes(final String... includedTypes) {
        explorerTree.getTreeModel().setIncludedTypes(includedTypes);
    }

    public void setIncludedRootTypes(final String... types) {
        explorerTree.getTreeModel().setIncludedRootTypes(types);
    }

    public void setTags(final String... tags) {
        explorerTree.getTreeModel().setTags(tags);
    }

    public void setNodeFlags(final NodeFlag... nodeFlags) {
        explorerTree.getTreeModel().setNodeFlags(nodeFlags);
    }

    public void setRequiredPermissions(final String... requiredPermissions) {
        explorerTree.getTreeModel().setRequiredPermissions(requiredPermissions);
    }

    public DocRef getSelectedEntityReference() {
        final ExplorerNode explorerNode = getSelectedEntityData();
        if (explorerNode == null) {
            return null;
        }
        return explorerNode.getDocRef();
    }

    public void setSelectedEntityReference(final DocRef docRef) {
        restFactory
                .create()
                .onSuccess(explorerNode -> setSelectedEntityData((ExplorerNode) explorerNode))
                .call(EXPLORER_RESOURCE)
                .getFromDocRef(docRef);
    }

    private ExplorerNode getSelectedEntityData() {
        return resolve(explorerTree.getSelectionModel().getSelected());
    }

    private void setSelectedEntityData(final ExplorerNode explorerNode) {
//        GWT.log("setSelectedEntityData: " + explorerNode);
        this.selectedExplorerNode = explorerNode;
        DataSelectionEvent.fire(this, explorerNode, false);
        refresh();
    }

    public void setAllowFolderSelection(final boolean allowFolderSelection) {
        this.allowFolderSelection = allowFolderSelection;
    }

    @Override
    public HandlerRegistration addDataSelectionHandler(final DataSelectionHandler<ExplorerNode> handler) {
        return addHandlerToSource(DataSelectionEvent.getType(), handler);
    }

    private static ExplorerNode resolve(final ExplorerNode selection) {
        if (ExplorerTreeModel.NULL_SELECTION.equals(selection)) {
            return null;
        }

        return selection;
    }

    public void setCaption(final String caption) {
        this.caption = caption;
    }

    public void setInitialQuickFilter(final String initialQuickFilter) {
        this.initialQuickFilter = initialQuickFilter;
    }

    // --------------------------------------------------------------------------------


    private static class ExtendedExplorerTree extends ExplorerTree {

        private final ExplorerDropDownTreePresenter explorerDropDownTreePresenter;

        public ExtendedExplorerTree(final ExplorerDropDownTreePresenter explorerDropDownTreePresenter,
                                    final RestFactory restFactory) {
            super(restFactory, false, false);
            this.explorerDropDownTreePresenter = explorerDropDownTreePresenter;
            this.getTreeModel().setIncludedRootTypes(ExplorerConstants.SYSTEM);
        }

        @Override
        protected void setInitialSelectedItem(final ExplorerNode selection) {
            super.setInitialSelectedItem(selection);
            explorerDropDownTreePresenter.setSelectedTreeItem(resolve(selection),
                    new SelectionType(false, false),
                    true);
        }

        @Override
        protected void doSelect(final ExplorerNode row, final SelectionType selectionType) {
            super.doSelect(row, selectionType);
            explorerDropDownTreePresenter.setSelectedTreeItem(resolve(row), selectionType, false);
        }
    }
}
