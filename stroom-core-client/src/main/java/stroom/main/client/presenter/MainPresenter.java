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

package stroom.main.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.content.client.event.RefreshCurrentContentTabEvent;
import stroom.core.client.MenuKeys;
import stroom.core.client.presenter.CorePresenter;
import stroom.menubar.client.event.BeforeRevealMenubarEvent;
import stroom.task.client.TaskEndEvent;
import stroom.task.client.TaskStartEvent;
import stroom.task.client.event.OpenTaskManagerEvent;
import stroom.ui.config.client.UiConfigCache;
import stroom.ui.config.shared.ExtendedUiConfig;
import stroom.widget.menu.client.presenter.Item;
import stroom.widget.menu.client.presenter.MenuItems;
import stroom.widget.menu.client.presenter.ShowMenuEvent;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.tab.client.event.MaximiseEvent;
import stroom.widget.util.client.DoubleSelectTester;
import stroom.widget.util.client.KeyBinding;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.MyPresenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;

import java.util.List;

public class MainPresenter
        extends MyPresenter<MainPresenter.MainView, MainPresenter.MainProxy>
        implements MainUiHandlers {

    @ContentSlot
    public static final Type<RevealContentHandler<?>> EXPLORER = new Type<>();
    @ContentSlot
    public static final Type<RevealContentHandler<?>> CONTENT = new Type<>();
    private final Timer refreshTimer;
    private boolean click;
    private final MenuItems menuItems;

    @Inject
    public MainPresenter(final EventBus eventBus,
                         final MainView view,
                         final MainProxy proxy,
                         final MenuItems menuItems,
                         final UiConfigCache uiConfigCache) {
        super(eventBus, view, proxy);
        this.menuItems = menuItems;
        view.setUiHandlers(this);

        // Handle key presses.
        view.asWidget().addDomHandler(event ->
                KeyBinding.getAction(event.getNativeEvent()), KeyDownEvent.getType());

        addRegisteredHandler(TaskStartEvent.getType(), event -> {
            // DebugPane.debug("taskStart:" + event.getTaskCount());

            // Always try and start spinner even if it might be spinning
            // already.
            if (view.getSpinner() != null) {
                view.getSpinner().start();
            }
        });
        addRegisteredHandler(TaskEndEvent.getType(), event -> {
            // DebugPane.debug("taskEnd:" + event.getTaskCount());

            // Only stop spinner if we are no longer executing any tasks.
            if (event.getTaskCount() == 0) {
                if (view.getSpinner() != null) {
                    view.getSpinner().stop();
                }
            }
        });
        registerHandler(uiConfigCache.addPropertyChangeHandler(
                event -> {
                    final ExtendedUiConfig uiConfig = event.getProperties();
                    if (uiConfig.getTheme() != null) {
                        getView().setBorderStyle(uiConfig.getTheme().getPageBorder());
                    }
                    getView().setBanner(uiConfig.getMaintenanceMessage());
                    if (uiConfig.getRequireReactWrapper()) {
                        final Object parentIframe = getParentIframe();
                        if (parentIframe == null) {
                            AlertEvent.fireWarn(
                                    this,
                                    "You have reached Stroom outside of an IFrame you will now be redirected",
                                    () -> {
                                        UrlBuilder builder = new UrlBuilder();
                                        builder.setProtocol(Window.Location.getProtocol());
                                        builder.setHost(Window.Location.getHost());
                                        Window.Location.replace(builder.buildString());
                                    });
                        }
                    }
                }
        ));

        registerHandler(view.getSpinner().addClickHandler(event -> {
            if (click) {
                click = false;
                OpenTaskManagerEvent.fire(MainPresenter.this);

            } else {
                final Timer clickTimer = new Timer() {
                    @Override
                    public void run() {
                        if (click) {
                            stopAutoRefresh();
                            // refresh();
                            startAutoRefresh();
                        }

                        click = false;
                    }
                };
                click = true;
                clickTimer.schedule(DoubleSelectTester.DOUBLE_SELECT_DELAY);
            }
        }));
        addRegisteredHandler(MaximiseEvent.getType(), event -> view.maximise(event.getView()));

        refreshTimer = new Timer() {
            @Override
            public void run() {
                RefreshCurrentContentTabEvent.fire(MainPresenter.this);
            }
        };

        // Start the auto refresh timer.
        startAutoRefresh();
    }

    @Override
    public void showMenu(final NativeEvent event, final Element target) {
        final PopupPosition popupPosition = new PopupPosition(target.getAbsoluteRight(),
                target.getAbsoluteBottom());
        showMenuItems(
                popupPosition,
                target);
    }

    public void showMenuItems(final PopupPosition popupPosition,
                              final Element autoHidePartner) {
        // Clear the current menus.
        menuItems.clear();
        // Tell all plugins to add new menu items.
        BeforeRevealMenubarEvent.fire(this, menuItems);
        final List<Item> items = menuItems.getMenuItems(MenuKeys.MAIN_MENU);
        if (items != null && items.size() > 0) {
            ShowMenuEvent
                    .builder()
                    .items(items)
                    .popupPosition(popupPosition)
                    .addAutoHidePartner(autoHidePartner)
                    .fire(this);
        }
    }

    private void startAutoRefresh() {
        refreshTimer.scheduleRepeating(30000); // 30 second default.
    }

    private void stopAutoRefresh() {
        refreshTimer.cancel();
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, CorePresenter.CORE, this);
        RootPanel.get("logo").setVisible(false);
    }

    public native Object getParentIframe() /*-{
        return window.parent.frameElement;
    }-*/;

    @ProxyCodeSplit
    public interface MainProxy extends Proxy<MainPresenter> {

    }


    // --------------------------------------------------------------------------------


    public interface MainView extends View, HasUiHandlers<MainUiHandlers> {

        SpinnerDisplay getSpinner();

        void maximise(View view);

        void setBorderStyle(String style);

        void setBanner(String text);
    }


    // --------------------------------------------------------------------------------


    public interface SpinnerDisplay extends HasClickHandlers, HasDoubleClickHandlers {

        void start();

        void stop();
    }
}
