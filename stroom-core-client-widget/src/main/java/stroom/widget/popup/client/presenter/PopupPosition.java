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

package stroom.widget.popup.client.presenter;

import stroom.widget.util.client.Rect;

public class PopupPosition {

    private final Rect relativeRect;
    private final PopupLocation popupLocation;

    public PopupPosition(final int x,
                         final int y) {
        this(new Rect(y, y, x, x), null);
    }

    public PopupPosition(final Rect relativeRect,
                         final PopupLocation popupLocation) {
        this.relativeRect = relativeRect;
        this.popupLocation = popupLocation;
    }

    public Rect getRelativeRect() {
        return relativeRect;
    }

    public PopupLocation getPopupLocation() {
        return popupLocation;
    }

    public enum PopupLocation {
        LEFT,
        RIGHT,
        ABOVE,
        BELOW
    }
}
