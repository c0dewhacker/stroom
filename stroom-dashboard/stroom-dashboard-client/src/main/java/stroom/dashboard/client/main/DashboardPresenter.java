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
 */

package stroom.dashboard.client.main;

import stroom.alert.client.event.AlertEvent;
import stroom.alert.client.event.ConfirmEvent;
import stroom.content.client.event.RefreshContentTabEvent;
import stroom.dashboard.client.flexlayout.FlexLayoutChangeHandler;
import stroom.dashboard.client.flexlayout.PositionAndSize;
import stroom.dashboard.client.main.ComponentRegistry.ComponentType;
import stroom.dashboard.client.main.ComponentRegistry.ComponentUse;
import stroom.dashboard.client.main.DashboardPresenter.DashboardView;
import stroom.dashboard.client.query.QueryInfoPresenter;
import stroom.dashboard.client.query.QueryUiHandlers;
import stroom.dashboard.shared.ComponentConfig;
import stroom.dashboard.shared.DashboardConfig;
import stroom.dashboard.shared.DashboardConfig.TabVisibility;
import stroom.dashboard.shared.DashboardDoc;
import stroom.dashboard.shared.LayoutConfig;
import stroom.dashboard.shared.Size;
import stroom.dashboard.shared.SplitLayoutConfig;
import stroom.dashboard.shared.SplitLayoutConfig.Direction;
import stroom.dashboard.shared.TabConfig;
import stroom.dashboard.shared.TabLayoutConfig;
import stroom.docref.DocRef;
import stroom.document.client.DocumentTabData;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.explorer.shared.DocumentType;
import stroom.query.api.v2.TimeRange;
import stroom.security.client.api.ClientSecurityContext;
import stroom.svg.client.Icon;
import stroom.util.shared.RandomId;
import stroom.widget.menu.client.presenter.Item;
import stroom.widget.menu.client.presenter.ShowMenuEvent;
import stroom.widget.menu.client.presenter.SimpleMenuItem;
import stroom.widget.popup.client.presenter.PopupPosition;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.View;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardPresenter extends DocumentEditPresenter<DashboardView, DashboardDoc>
        implements FlexLayoutChangeHandler, DocumentTabData, DashboardUiHandlers, QueryUiHandlers,
        Consumer<Boolean> {

    private static final Logger logger = Logger.getLogger(DashboardPresenter.class.getName());
    private final DashboardLayoutPresenter layoutPresenter;
    private final Components components;
    private final Provider<QueryInfoPresenter> queryInfoPresenterProvider;
    private String lastLabel;
    private boolean loaded;
    private String customTitle;
    private DocRef docRef;

    private TimeRange currentTimeRange;
    private String currentParams;
    private String lastUsedQueryInfo;
    private boolean embedded;
    private boolean queryOnOpen;

    @Inject
    public DashboardPresenter(final EventBus eventBus,
                              final DashboardView view,
                              final DashboardLayoutPresenter layoutPresenter,
                              final Components components,
                              final Provider<RenameTabPresenter> renameTabPresenterProvider,
                              final Provider<QueryInfoPresenter> queryInfoPresenterProvider,
                              final ClientSecurityContext securityContext) {
        super(eventBus, view, securityContext);
        this.layoutPresenter = layoutPresenter;
        this.components = components;
        this.queryInfoPresenterProvider = queryInfoPresenterProvider;

        final TabManager tabManager = new TabManager(components, renameTabPresenterProvider, this);
        layoutPresenter.setTabManager(tabManager);

        layoutPresenter.setFlexLayoutChangeHandler(this);
        layoutPresenter.setComponents(components);
        view.setContent(layoutPresenter.getView());
        view.setUiHandlers(this);

//        view.getQueryButtons().setUiHandlers(this);
    }

    @Override
    protected void onUnbind() {
        super.onUnbind();

        // Remove all components. This should have been done already in the
        // onClose() method.
        components.removeAll();
    }

    @Override
    public void onAddPanel(final ClickEvent event) {
        onAdd(event, ComponentUse.PANEL);
    }

    @Override
    public void onAddInput(final ClickEvent event) {
        onAdd(event, ComponentUse.INPUT);
    }

    private void onAdd(final ClickEvent event, final ComponentUse componentUse) {
        final com.google.gwt.dom.client.Element target = event.getNativeEvent().getEventTarget().cast();

        final PopupPosition popupPosition = new PopupPosition(target.getAbsoluteLeft() - 3,
                target.getAbsoluteTop() + target.getClientHeight() + 1);

        final List<Item> menuItems = new ArrayList<>();
        for (final ComponentType type : components.getComponentTypes()) {
            if (componentUse.equals(type.getUse())) {
                menuItems.add(new SimpleMenuItem.Builder()
                        .text(type.getName())
                        .command(() -> addComponent(type))
                        .build());
            }
        }

        ShowMenuEvent
                .builder()
                .items(menuItems)
                .popupPosition(popupPosition)
                .fire(this);
    }

    @Override
    public void onTimeRange(final TimeRange timeRange) {
        setTimeRange(timeRange);
    }

    private void setTimeRange(final TimeRange timeRange) {
        currentTimeRange = timeRange;
        getView().setTimeRange(timeRange);
        for (final Component component : components) {
            if (component instanceof Queryable) {
                final Queryable queryable = (Queryable) component;
                queryable.setTimeRange(timeRange);
            }
        }
    }

    public void setParams(final String params) {
        logger.log(Level.INFO, "Dashboard Presenter setParams " + params);
        this.currentParams = params;
    }

    void setEmbedded(final boolean embedded) {
        this.embedded = embedded;
        getView().setEmbedded(embedded);
    }

    public void setQueryOnOpen(final boolean queryOnOpen) {
        this.queryOnOpen = queryOnOpen;
    }

    @Override
    protected void onRead(final DocRef docRef, final DashboardDoc dashboard) {
        this.docRef = docRef;
        if (!loaded) {
            loaded = true;

            components.setDashboard(dashboard);
            components.clear();
            LayoutConfig layoutData = null;

            final DashboardConfig dashboardConfig = dashboard.getDashboardConfig();
            if (dashboardConfig != null) {
                if (null == currentParams) {
                    currentParams = "";
                    if (dashboardConfig.getParameters() != null
                            && dashboardConfig.getParameters().trim().length() > 0) {
                        currentParams = dashboardConfig.getParameters().trim();
                    }
                }
//                getView().setParams(currentParams);

                if (currentTimeRange == null) {
                    if (dashboardConfig.getTimeRange() != null) {
                        setTimeRange(dashboardConfig.getTimeRange());
                    }
                }

                layoutData = dashboardConfig.getLayout();
                final List<ComponentConfig> componentDataList = dashboardConfig.getComponents();
                if (componentDataList != null) {
                    for (final ComponentConfig componentData : componentDataList) {
                        addComponent(componentData.getType(), componentData);
                    }
                    for (final ComponentConfig componentData : componentDataList) {
                        final Component component = components.get(componentData.getId());
                        if (component != null) {
                            component.link();
                        }
                    }
                }
            } else {
                // /**
                // * ADD TEST DATA
                // */
                // final SplitLayoutData down = new
                // SplitLayoutData(Direction.DOWN.getDimension());
                // for (int i = 0; i < 3; i++) {
                // final SplitLayoutData across = new
                // SplitLayoutData(Direction.ACROSS.getDimension());
                // down.add(across);
                //
                // for (int l = 0; l < 2; l++) {
                // final SplitLayoutData down2 = new
                // SplitLayoutData(Direction.DOWN.getDimension());
                // across.add(down2);
                //
                // for (int j = 0; j < 3; j++) {
                // final TabLayoutData tablayout = new TabLayoutData();
                // down2.add(tablayout);
                //
                // for (int k = 0; k < 2; k++) {
                // final String type = TablePresenter.TYPE;
                // final String id = type + "_" +
                // String.valueOf(System.currentTimeMillis());
                //
                // final ComponentData componentData = new ComponentData();
                // componentData.setId(id);
                //
                // final ComponentPresenter component =
                // componentRegistry.getComponent(type);
                // component.read(componentData);
                //
                // components.add(component);
                // componentViews.put(id, component.getView());
                //
                // final TabData tabData = new TabData();
                // tabData.setId(id);
                // tabData.setName(component.getType() + " " + i + ":" + j + ":"
                // + k);
                //
                // tablayout.add(tabData);
                // }
                // }
                // }
                // }
                // dashboardData.setLayout(down);
                // /**
                // * DONE - ADD TEST DATA
                // */
            }

            // if (dashboardData.getTabVisibility() != null) {
            // tabVisibility.setSelectedItem(dashboardData.getTabVisibility());
            // }

            layoutPresenter.setLayoutData(layoutData);

            // Tell all queryable components whether we want them to query on open.
            for (final Component component : components) {
                if (component instanceof Queryable) {
                    ((Queryable) component).setQueryOnOpen(queryOnOpen);
                }
            }
        }
    }

    private Component addComponent(final String type, final ComponentConfig componentData) {
        final Component component = components.add(type, componentData.getId());
        if (component != null) {
            if (component instanceof HasDirtyHandlers) {
                ((HasDirtyHandlers) component).addDirtyHandler(event -> setDirty(true));
            }

            // Set params on the component if it needs them.
            if (component instanceof Queryable) {
                final Queryable queryable = (Queryable) component;
                queryable.setParams(currentParams);
                queryable.setTimeRange(currentTimeRange);
                queryable.addModeListener(this);
            }

            component.read(componentData);
        }

        enableQueryButtons();

        return component;
    }

    private void enableQueryButtons() {
//        getView().getQueryButtons().setEnabled(getQueryableComponents().size() > 0);
//        getView().getQueryButtons().setMode(getCombinedMode());
    }

    @Override
    public void accept(final Boolean mode) {
//        getView().getQueryButtons().setMode(getCombinedMode());
    }

    private boolean getCombinedMode() {
        final List<Queryable> queryableComponents = getQueryableComponents();
        boolean combinedMode = false;
        for (final Queryable queryable : queryableComponents) {
            if (queryable.getMode()) {
                combinedMode = true;
            }
        }
        return combinedMode;
    }

    @Override
    protected void onWrite(final DashboardDoc dashboard) {
        String params = null;//getView().getParams();
        if (params != null && params.trim().length() == 0) {
            params = null;
        }

        final List<ComponentConfig> componentDataList = new ArrayList<>(components.size());
        for (final Component component : components) {
            final ComponentConfig componentConfig = component.write();
            componentDataList.add(componentConfig);
        }

        final DashboardConfig dashboardConfig = new DashboardConfig();
        dashboardConfig.setParameters(params);
        dashboardConfig.setTimeRange(currentTimeRange);
        dashboardConfig.setComponents(componentDataList);
        dashboardConfig.setLayout(layoutPresenter.getLayoutData());
        dashboardConfig.setTabVisibility(TabVisibility.SHOW_ALL);
        dashboard.setDashboardConfig(dashboardConfig);
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        super.onReadOnly(readOnly || embedded);
        getView().setReadOnly(readOnly || embedded);
    }

    @Override
    public void onClose() {
        // Remove all components.
        components.removeAll();
        super.onClose();
    }

    @Override
    public String getType() {
        return DashboardDoc.DOCUMENT_TYPE;
    }

    @Override
    public void onDirty() {
        setDirty(true);
    }

    @Override
    public void requestTabClose(final TabLayoutConfig tabLayoutConfig, final TabConfig tabConfig) {
        // Figure out what tabs would remain after removal.
        int hiddenCount = 0;
        int totalCount = 0;
        for (final TabConfig tab : tabLayoutConfig.getTabs()) {
            if (tab != tabConfig) {
                if (!tab.visible()) {
                    hiddenCount++;
                }
                totalCount++;
            }
        }

        // If all remaining tabs are hidden then we can't allow removal.
        if (totalCount > 0 && totalCount == hiddenCount) {
            AlertEvent.fireError(this, "You cannot remove or hide all tabs", null);
        } else {
            ConfirmEvent.fire(this, "Are you sure you want to close this tab?", ok -> {
                if (ok) {
                    layoutPresenter.closeTab(tabConfig);
                    final Component component = components.get(tabConfig.getId());
                    if (component != null) {
                        if (component instanceof Queryable) {
                            final Queryable queryable = (Queryable) component;
                            queryable.removeModeListener(this);
                        }
                        components.remove(tabConfig.getId(), true);
                        enableQueryButtons();
                    }
                }
            });
        }
    }

    @Override
    public void start() {
        // Get a sub list of components that can be queried.
        final List<Queryable> queryableComponents = getQueryableComponents();
        final boolean combinedMode = getCombinedMode();

        if (combinedMode) {
            for (final Queryable queryable : getQueryableComponents()) {
                queryable.stop();
            }
        } else {

            // If we have some queryable components then make sure we get query info for them.
            if (queryableComponents.size() > 0) {
                queryInfoPresenterProvider.get().show(lastUsedQueryInfo, state -> {
                    if (state.isOk()) {
                        lastUsedQueryInfo = state.getQueryInfo();

                        for (final Queryable queryable : queryableComponents) {
                            queryable.setParams(currentParams);
                            queryable.setTimeRange(currentTimeRange);
                            queryable.setQueryInfo(lastUsedQueryInfo);
                            queryable.start();
                        }
                    }
                });
            }
        }
    }

    private List<Queryable> getQueryableComponents() {
        // Get a sub list of components that can be queried.
        final List<Queryable> queryableComponents = new ArrayList<>();
        for (final Component component : components) {
            if (component instanceof Queryable) {
                queryableComponents.add((Queryable) component);
            }
        }
        return queryableComponents;
    }

    @Override
    public String getLabel() {
        String label = getTitle();
        if (isDirty()) {
            label = "* " + label;
        }
        return label;
    }

    public String getTitle() {
        String title = "";
        if (docRef != null) {
            title = docRef.getName();
        }
        if (customTitle != null && customTitle.length() > 0) {
            title = customTitle.replaceAll("\\$\\{name\\}", title);
        }
        return title;
    }

    @Override
    public boolean isCloseable() {
        return true;
    }

    @Override
    public Icon getIcon() {
        return Icon.create(DocumentType.DOC_IMAGE_CLASS_NAME + getType());
    }

    @Override
    public void onDirtyChange() {
        // Only fire tab refresh if the tab has changed.
        if (lastLabel == null || !lastLabel.equals(getLabel())) {
            lastLabel = getLabel();
            RefreshContentTabEvent.fire(this, this);
        }
    }

    public void setCustomTitle(final String customTitle) {
        this.customTitle = customTitle;
    }

    public interface DashboardView extends View, HasUiHandlers<DashboardUiHandlers> {

        TimeRange getTimeRange();

        void setTimeRange(TimeRange timeRange);

        void setContent(View view);

        void setEmbedded(boolean embedded);

        void setReadOnly(boolean readOnly);
    }

    private void addComponent(final ComponentType type) {
        if (type != null) {
            String id = type.getId() + "-" + RandomId.createId(5);
            // Make sure we don't duplicate ids.
            while (components.idExists(id)) {
                id = type.getId() + "-" + RandomId.createId(5);
            }

            final ComponentConfig componentData = ComponentConfig
                    .builder()
                    .type(type.getId())
                    .id(id)
                    .name(type.getName())
                    .build();

            final Component componentPresenter = addComponent(componentData.getType(), componentData);
            if (componentPresenter != null) {
                componentPresenter.link();
            }

            final TabConfig tabConfig = new TabConfig(id, true);
            final TabLayoutConfig tabLayoutConfig = new TabLayoutConfig(tabConfig);

            // Choose where to put the new component in the layout data.
            LayoutConfig layoutData = layoutPresenter.getLayoutData();
            if (layoutData == null) {
                // There is no existing layout so add the new item as a
                // single item layout.

                layoutData = tabLayoutConfig;

            } else if (layoutData instanceof TabLayoutConfig) {
                // If the layout is a single item then replace it with a
                // split layout.
                layoutData = new SplitLayoutConfig(Direction.DOWN.getDimension(),
                        layoutData, tabLayoutConfig);
            } else {
                // If the layout is already a split then add a new component
                // to the split.
                final SplitLayoutConfig parent = (SplitLayoutConfig) layoutData;

                // Add the new component.
                parent.add(tabLayoutConfig);

                // Fix the heights of the components to fit the new
                // component in.
                fixHeights(parent);
            }

            layoutPresenter.setLayoutData(layoutData);
            setDirty(true);
        }
    }

    private void fixHeights(final SplitLayoutConfig parent) {
        // Create a default size to use.
        final Size defaultSize = new Size();

        if (parent.count() > 1) {
            final LayoutConfig previousComponent = parent.get(parent.count() - 2);
            final int height = previousComponent.getPreferredSize().getHeight();

            // See if the previous component has enough height to be split
            // to include the new component.
            if (height > (defaultSize.getHeight() * 2)) {
                previousComponent.getPreferredSize().setHeight(height - defaultSize.getHeight());
            } else {
                // The previous component isn't high enough so resize all
                // components to fit.
                lazyRedistribution(parent);
            }
        }
    }

    private void lazyRedistribution(final SplitLayoutConfig parent) {
        // Create a default size to use.
        final Size defaultSize = new Size();

        // See if we can get the currently presented position and size for
        // the parent layout.
        final PositionAndSize positionAndSize = layoutPresenter.getPositionAndSize(parent);
        if (positionAndSize != null) {
            // Get the current height of the split layout.
            final int height = positionAndSize.getHeight();

            final int totalHeight = getTotalHeight(parent);
            if (height > 0 && totalHeight > height) {
                int amountToSave = totalHeight - height;

                // Try and set heights to the default height to claw back
                // space we want to save.
                for (int i = parent.count() - 1; i >= 0; i--) {
                    final LayoutConfig ld = parent.get(i);
                    final Size size = ld.getPreferredSize();
                    final int diff = size.getHeight() - defaultSize.getHeight();
                    if (diff > 0) {
                        if (diff > amountToSave) {
                            size.setHeight(size.getHeight() - amountToSave);
                            amountToSave = 0;
                            break;
                        } else {
                            size.setHeight(defaultSize.getHeight());
                            amountToSave -= diff;
                        }
                    }
                }

                // If we have more space we need to save then try and
                // distribute space evenly between widgets.
                if (amountToSave > 0) {
                    fairRedistribution(parent, height);
                }
            }
        } else {
            // We have no idea what size the parnet container is occupying
            // so just reset all heights.
            resetAllHeights(parent);
        }
    }

    private void fairRedistribution(final SplitLayoutConfig parent, final int height) {
        // Find out how high each component could be if they were all the
        // same height.
        int fairHeight = (height / parent.count());
        fairHeight = Math.max(0, fairHeight);

        int used = 0;
        int count = 0;

        // Try and find the components that are bigger than their fair size
        // and remember the amount of space used by smaller components.
        for (int i = parent.count() - 1; i >= 0; i--) {
            final LayoutConfig ld = parent.get(i);
            final Size size = ld.getPreferredSize();
            if (size.getHeight() > fairHeight) {
                count++;
            } else {
                used += size.getHeight();
            }
        }

        // Calculate the height to set all components that are bigger than
        // the available height.
        if (count > 0) {
            final int newHeight = ((height - used) / count);
            for (int i = parent.count() - 1; i >= 0; i--) {
                final LayoutConfig ld = parent.get(i);
                final Size size = ld.getPreferredSize();
                if (size.getHeight() > fairHeight) {
                    size.setHeight(newHeight);
                }
            }
        }
    }

    private void resetAllHeights(final SplitLayoutConfig parent) {
        final Size defaultSize = new Size();
        for (int i = 0; i < parent.count(); i++) {
            final LayoutConfig ld = parent.get(i);
            final Size size = ld.getPreferredSize();
            if (size.getHeight() > defaultSize.getHeight()) {
                size.setHeight(defaultSize.getHeight());
            }
        }
    }

    private int getTotalHeight(final SplitLayoutConfig parent) {
        int totalHeight = 0;
        for (int i = parent.count() - 1; i >= 0; i--) {
            final LayoutConfig ld = parent.get(i);
            final Size size = ld.getPreferredSize();
            totalHeight += size.getHeight();
        }
        return totalHeight;
    }
}

