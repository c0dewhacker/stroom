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

package stroom.widget.popup.client.view;

import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.HidePopupRequestEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupPosition.PopupLocation;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupSupport;
import stroom.widget.popup.client.presenter.PopupType;
import stroom.widget.popup.client.presenter.Size;
import stroom.widget.util.client.Rect;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.gwtplatform.mvp.client.View;

import java.util.ArrayList;
import java.util.List;

public class PopupSupportImpl implements PopupSupport {

    private static final int POPUP_SHADOW_WIDTH = 9;
    private Popup popup;
    private String caption;
    private Boolean modal;

    private View view;
    private HidePopupRequestEvent.Handler hideRequestHandler;
    private HidePopupEvent.Handler hideHandler;
    private List<Element> autoHidePartners;
    private DialogButtons dialogButtons;

    public PopupSupportImpl(final View view, final String caption, final Boolean modal,
                            final Element... autoHidePartners) {
        setView(view);
        setCaption(caption);

        if (modal != null) {
            setModal(modal);
        }

        if (autoHidePartners != null) {
            for (final Element element : autoHidePartners) {
                addAutoHidePartner(element);
            }
        }
    }

    private void addAutoHidePartner(final Element element) {
        if (autoHidePartners == null) {
            autoHidePartners = new ArrayList<>();
        }
        autoHidePartners.add(element);
    }

    public List<Element> getAutoHidePartners() {
        return autoHidePartners;
    }

    @Override
    public void show(final ShowPopupEvent event) {
        final PopupType popupType = event.getPopupType();
        final PopupPosition popupPosition = event.getPopupPosition();
        final PopupSize popupSize = event.getPopupSize();
        hideRequestHandler = event.getHideRequestHandler();
        hideHandler = event.getHideHandler();

        if (popup == null) {
            final HideRequestUiHandlers uiHandlers = new DefaultHideRequestUiHandlers(event.getPresenterWidget());
            popup = createPopup(popupType, popupSize, uiHandlers);
        }
        final PopupPanel popupPanel = (PopupPanel) popup;
        // Hide the popup because we are going to position it before making it
        // visible.
        popupPanel.setVisible(false);
        popupPanel.setModal(false);

        // Way to set popups to be modal using glass.
        if (modal != null) {
            popupPanel.setGlassEnabled(modal);
        }

        // Add auto hide partners.
        if (autoHidePartners != null && autoHidePartners.size() > 0) {
            for (final Element element : autoHidePartners) {
                popupPanel.addAutoHidePartner(element);
            }
        }

        // Attach the popup to the DOM.
        CurrentFocus.push();
        popupPanel.show();
        // Defer the command to position and make visible because we need the
        // popup to size first.
        Scheduler.get().scheduleDeferred(() -> {
            if (popup != null) {
                // Now get the popup size.
                final int w = popupPanel.getOffsetWidth();
                final int h = popupPanel.getOffsetHeight();

                // Set the popup size.
                int newWidth = w;
                int newHeight = h;
                if (popupSize != null) {
                    newWidth = getSize(newWidth, popupSize.getWidth());
                    newHeight = getSize(newHeight, popupSize.getHeight());
                }
                if (newWidth != w) {
                    popupPanel.setWidth(newWidth + "px");
                }
                if (newHeight != h) {
                    popupPanel.setHeight(newHeight + "px");
                }

                if (popupPosition == null) {
                    // Center the popup in the client window.
                    centerPopup(popup, newWidth, newHeight);
                } else {
                    // Position the popup so it is as close as possible to
                    // the required location but is all on screen.
                    positionPopup(popup, popupType, popupPosition, newWidth, newHeight);
                }

                // Make the popup visible.
                popupPanel.setVisible(true);
                popupPanel.getElement().getStyle().setOpacity(1);

                // Tell the handler that the popup is visible.
                if (event.getShowHandler() != null) {
                    Scheduler.get().scheduleDeferred(() ->
                            event.getShowHandler().onShow(event));
                } else if (dialogButtons != null) {
                    // If no custom handler is specified then focus the buttons
                    Scheduler.get().scheduleDeferred(() -> dialogButtons.focus());
                }
            }
        });
    }

    private int getSize(final int current, Size size) {
        int newSize = current;
        if (size != null) {
            if (size.getInitial() == null) {
                size.setInitial(current);
            }

            if (size.getMin() == null) {
                size.setMin(Math.min(current, size.getInitial()));
            }

            newSize = Math.max(size.getMin(), size.getInitial());
        }
        return newSize;
    }

    private void centerPopup(final Popup popup, final int width, final int height) {
        final int parentWidth = Window.getClientWidth();
        final int parentHeight = Window.getClientHeight();
        final int left = (parentWidth - width) / 2;
        final int top = (parentHeight - height) / 2;
        popup.setPopupPosition(left, top);
    }

    /**
     * Positions the popup, called after the offset width and height of the
     * popup are known.
     */
    private void positionPopup(final Popup popup,
                               final PopupType popupType,
                               final PopupPosition popupPosition,
                               int offsetWidth, int offsetHeight) {
        int shadowWidth = 0;
        if (popupType != PopupType.POPUP) {
            shadowWidth = POPUP_SHADOW_WIDTH;
        }

        final Rect relativeRect = popupPosition.getRelativeRect();
        final Rect reference = relativeRect.grow(shadowWidth);

        double left;
        if (PopupLocation.LEFT.equals(popupPosition.getPopupLocation())) {
            // Positioned to the left but aligned with the right edge of the
            // relative box.
            left = reference.getLeft() - offsetWidth;
        } else if (PopupLocation.RIGHT.equals(popupPosition.getPopupLocation())) {
            // Positioned to the left but aligned with the right edge of the
            // relative box.
            left = reference.getRight();
        } else {
            // Positioned to the right but aligned with the left edge of the
            // relative box.
            left = reference.getLeft();
        }

        // Make sure scrolling is taken into account, since
        // box.getAbsoluteLeft() takes scrolling into account.
        final int windowRight = Window.getClientWidth() + Window.getScrollLeft();
        final int windowLeft = Window.getScrollLeft();

        // Distance from the left edge of the text box to the right edge of the
        // window
        final double distanceToWindowRight = windowRight - left;

        // Distance from the left edge of the text box to the left edge of the
        // window
        final double distanceFromWindowLeft = left - windowLeft;

        // If there is not enough space for the overflow of the popup's width to
        // the right, and there IS enough space
        // for the overflow to the left, then show the popup on the left. If
        // there is not enough space on either side, then
        // position at the far left.
        if (distanceToWindowRight < offsetWidth) {
            if (distanceFromWindowLeft >= offsetWidth) {
                // Positioned to the left but aligned with the right edge of the
                // relative box.
                left = reference.getRight() - offsetWidth;
            } else {
                left = -shadowWidth;
            }
        }

        // Calculate top position for the popup
        double top;
        if (PopupLocation.ABOVE.equals(popupPosition.getPopupLocation())) {
            // Positioned above.
            top = reference.getTop() - offsetHeight;
        } else if (PopupLocation.BELOW.equals(popupPosition.getPopupLocation())) {
            // Positioned below.
            top = reference.getBottom();
        } else {
            // Default position is to the side.
            top = reference.getTop();
        }

        // Make sure scrolling is taken into account, since box.getAbsoluteTop()
        // takes scrolling into account.
        final int windowTop = Window.getScrollTop();
        final int windowBottom = Window.getScrollTop() + Window.getClientHeight();

        // Distance from the top edge of the window to the top edge of the text
        // box.
        final double distanceFromWindowTop = top - windowTop;

        // Distance from the bottom edge of the window to the relative object.
        final double distanceToWindowBottom = windowBottom - top;

        // If there is not enough space for the popup's height below the text
        // box and there IS enough space for the
        // popup's height above the text box, then position the popup above
        // the text box. If there is not enough
        // space above or below, then position at the very top.
        if (distanceToWindowBottom < offsetHeight) {
            if (distanceFromWindowTop >= offsetHeight) {
                // Positioned above.
                top = reference.getTop() - offsetHeight;
            } else {
                top = -shadowWidth;
            }
        }

        // Make sure the left and top positions are on screen.
        if (left + offsetWidth > windowRight) {
            left = windowRight - offsetWidth;
        }
        if (top + offsetHeight > windowBottom) {
            top = windowBottom - offsetHeight;
        }

        // Make sure left and top have not ended up less than 0.
        if (left < 0) {
            left = 0;
        }
        if (top < 0) {
            top = 0;
        }

        popup.setPopupPosition((int) left, (int) top);
    }

    @Override
    public void hideRequest(final HidePopupRequestEvent event) {
        if (hideRequestHandler != null) {
            hideRequestHandler.onHideRequest(event);
        }
    }

    @Override
    public void hide(final HidePopupEvent event) {
        if (popup != null) {
            popup.forceHide(event.isAutoClose());
            popup = null;
            hideRequestHandler = null;
            if (hideHandler != null) {
                hideHandler.onHide(event);
                hideHandler = null;
            }
            CurrentFocus.pop();
        }
    }

    @Override
    public void setEnabled(final boolean enabled) {
        if (dialogButtons != null) {
            dialogButtons.setEnabled(enabled);
        }
    }

    @Override
    public void setCaption(final String caption) {
        this.caption = caption;
        if (popup != null) {
            popup.setCaption(caption);
        }
    }

    private void setModal(final boolean modal) {
        this.modal = modal;
    }

    private Popup createPopup(final PopupType popupType,
                              final PopupSize popupSize,
                              final HideRequestUiHandlers uiHandlers) {
        Popup popup = null;

        if (popupSize != null) {
            switch (popupType) {
                case POPUP:
                    popup = new SimplePopup(uiHandlers);
                    popup.setContent(view.asWidget());
                    break;
                case DIALOG:
                    popup = new ResizableDialog(uiHandlers, popupSize, popupType);
                    popup.setContent(view.asWidget());
                    break;
                case CLOSE_DIALOG:
                    popup = new ResizableDialog(uiHandlers, popupSize, popupType);
                    final ResizableCloseContent closeContent = new ResizableCloseContent(uiHandlers);
                    dialogButtons = closeContent;
                    closeContent.setContent(view.asWidget());
                    popup.setContent(closeContent);
                    break;
                case OK_CANCEL_DIALOG:
                    popup = new ResizableDialog(uiHandlers, popupSize, popupType);
                    final ResizableOkCancelContent okCancelContent = new ResizableOkCancelContent(uiHandlers);
                    dialogButtons = okCancelContent;
                    okCancelContent.setContent(view.asWidget());
                    popup.setContent(okCancelContent);
                    break;
                case ACCEPT_REJECT_DIALOG:
                    popup = new ResizableDialog(uiHandlers, popupSize, popupType);
                    final ResizableAcceptRejectContent acceptRejectContent = new ResizableAcceptRejectContent(
                            uiHandlers);
                    dialogButtons = acceptRejectContent;
                    acceptRejectContent.setContent(view.asWidget());
                    popup.setContent(acceptRejectContent);
                    break;
            }
        } else {
            switch (popupType) {
                case POPUP:
                    popup = new SimplePopup(uiHandlers);
                    popup.setContent(view.asWidget());
                    break;
                case DIALOG:
                    popup = new Dialog(uiHandlers, popupType);
                    popup.setContent(view.asWidget());
                    break;
                case CLOSE_DIALOG:
                    popup = new Dialog(uiHandlers, popupType);
                    final CloseContent closeContent = new CloseContent(uiHandlers);
                    dialogButtons = closeContent;
                    closeContent.setContent(view.asWidget());
                    popup.setContent(closeContent);
                    break;
                case OK_CANCEL_DIALOG:
                    popup = new Dialog(uiHandlers, popupType);
                    final OkCancelContent okCancelContent = new OkCancelContent(uiHandlers);
                    dialogButtons = okCancelContent;
                    okCancelContent.setContent(view.asWidget());
                    popup.setContent(okCancelContent);
                    break;
                case ACCEPT_REJECT_DIALOG:
                    popup = new Dialog(uiHandlers, popupType);
                    final AcceptRejectContent acceptRejectContent = new AcceptRejectContent(uiHandlers);
                    dialogButtons = acceptRejectContent;
                    acceptRejectContent.setContent(view.asWidget());
                    popup.setContent(acceptRejectContent);
                    break;
            }
        }

        if (caption != null) {
            popup.setCaption(caption);
        }

        return popup;
    }

    private void setView(final View view) {
        this.view = view;
    }
}
