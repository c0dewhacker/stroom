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

package stroom.widget.button.client;

import stroom.svg.shared.SvgImage;
import stroom.widget.util.client.KeyBinding;
import stroom.widget.util.client.KeyBinding.Action;
import stroom.widget.util.client.MouseUtil;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ButtonBase;

public class FabButton extends ButtonBase {

    private final Element face;
    /**
     * If <code>true</code>, this widget is capturing with the mouse held down.
     */
    private boolean isCapturing;
    /**
     * If <code>true</code>, this widget has focus with the space bar down.
     */
    private boolean isFocusing;
    /**
     * Used to decide whether to allow clicks to propagate up to the superclass
     * or container elements.
     */
    private boolean allowClick;

    public FabButton() {
        super(Document.get().createDivElement());
        sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.FOCUSEVENTS | Event.KEYEVENTS);

        getElement().setClassName("stroom-button-fab");

        face = Document.get().createPushButtonElement();
        face.setClassName("face");

        getElement().appendChild(face);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            face.removeClassName("face--disabled");
        } else {
            face.addClassName("face--disabled");
        }
    }

    public void setIcon(final SvgImage svgImage) {
        face.setInnerHTML("<div class=\"svgIcon " + svgImage.getClassName() + "\">" + svgImage.getSvg() + "</div>");
    }

    @Override
    public void onBrowserEvent(final Event event) {
        // Should not act on button if disabled.
        if (isEnabled() == false) {
            // This can happen when events are bubbled up from non-disabled
            // children
            return;
        }

        final int type = DOM.eventGetType(event);
        switch (type) {
            case Event.ONCLICK:
                // If clicks are currently disallowed, keep it from bubbling or
                // being passed to the superclass.
                if (!allowClick) {
                    event.stopPropagation();
                    return;
                }
                break;
            case Event.ONMOUSEDOWN:
                if (MouseUtil.isPrimary(event)) {
                    setFocus(true);
                    onClickStart();
                    DOM.setCapture(getElement());
                    isCapturing = true;
                    // Prevent dragging (on some browsers);
                    event.preventDefault();
                }
                break;
            case Event.ONMOUSEUP:
                if (isCapturing) {
                    isCapturing = false;
                    DOM.releaseCapture(getElement());
                    if (MouseUtil.isPrimary(event)) {
                        onClick();
                    }
                }
                break;
            case Event.ONMOUSEMOVE:
                if (isCapturing) {
                    // Prevent dragging (on other browsers);
                    event.preventDefault();
                }
                break;
            case Event.ONMOUSEOUT:
                final Element to = DOM.eventGetToElement(event);
                if (getElement().isOrHasChild(DOM.eventGetTarget(event))
                        && (to == null || !getElement().isOrHasChild(to))) {
                    if (isCapturing) {
                        onClickCancel();
                    }
                    setHovering(false);
                }
                break;
            case Event.ONMOUSEOVER:
                if (getElement().isOrHasChild(DOM.eventGetTarget(event))) {
                    setHovering(true);
                    if (isCapturing) {
                        onClickStart();
                    }
                }
                break;
            case Event.ONBLUR:
                if (isFocusing) {
                    isFocusing = false;
                    onClickCancel();
                }
                break;
            case Event.ONLOSECAPTURE:
                if (isCapturing) {
                    isCapturing = false;
                    onClickCancel();
                }
                break;
        }

        super.onBrowserEvent(event);

        // Synthesize clicks based on keyboard events AFTER the normal key
        // handling.
        if ((event.getTypeInt() & Event.KEYEVENTS) != 0) {
            switch (type) {
                case Event.ONKEYDOWN:
                    final Action action = KeyBinding.getAction(event);
                    if (action == Action.SELECT || action == Action.EXECUTE) {
                        onClick();
                    }
                    break;

//                case Event.ONKEYDOWN:
//                    if (keyCode == ' ') {
//                        isFocusing = true;
//                        onClickStart();
//                    }
//                    break;
//                case Event.ONKEYUP:
//                    if (isFocusing && keyCode == ' ') {
//                        isFocusing = false;
//                        onClick();
//                    }
//                    break;
//                case Event.ONKEYPRESS:
//                    if (keyCode == '\n' || keyCode == '\r') {
//                        onClickStart();
//                        onClick();
//                    }
//                    break;
            }
        }
    }

    private void onClickStart() {
        face.addClassName("face--down");
    }

    private void onClickCancel() {
        face.removeClassName("face--down");
    }

    private void onClick() {
        // Allow the click we're about to synthesize to pass through to the
        // superclass and containing elements. Element.dispatchEvent() is
        // synchronous, so we simply set and clear the flag within this method.
        allowClick = true;

        // Mouse coordinates are not always available (e.g., when the click is
        // caused by a keyboard event).
        final NativeEvent evt = Document.get().createClickEvent(1, 0, 0, 0, 0, false, false, false, false);
        getElement().dispatchEvent(evt);
        face.removeClassName("face--down");

        allowClick = false;
    }

    private void setHovering(final boolean hovering) {
        // if (hovering && hoverColor != null) {
        // face.getStyle().setBackgroundColor(hoverColor);
        // } else if (color != null) {
        // face.getStyle().setBackgroundColor(color);
        // }
    }
}
