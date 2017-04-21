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

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;

public interface GlyphButtonView extends HasClickHandlers, HasMouseDownHandlers {
    void setIcon(String icon);

    void setColour(GlyphIcon.ColourSet colour);

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void setVisible(boolean visible);

    boolean isVisible();

    void setTitle(String title);
}
