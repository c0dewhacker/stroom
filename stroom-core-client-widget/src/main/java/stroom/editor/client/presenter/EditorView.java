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

package stroom.editor.client.presenter;

import stroom.editor.client.event.HasFormatHandlers;
import stroom.editor.client.view.IndicatorLines;
import stroom.editor.client.view.Marker;
import stroom.util.shared.TextRange;
import stroom.widget.contextmenu.client.event.HasContextMenuHandlers;

import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.user.client.ui.Focus;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.RequiresResize;
import com.gwtplatform.mvp.client.View;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorTheme;

import java.util.List;
import java.util.function.Function;

public interface EditorView extends View, Focus, HasKeyDownHandlers, HasFormatHandlers, HasText, HasMouseDownHandlers,
        HasContextMenuHandlers, HasValueChangeHandlers<String>, RequiresResize {

    String getEditorId();

    void setText(final String text);

    void setText(final String text, final boolean format);

    boolean isClean();

    void markClean();

    void insertTextAtCursor(String text);

    void replaceSelectedText(String text);

    void insertSnippet(String snippet);

    void setFirstLineNumber(int firstLineNumber);

    void setIndicators(IndicatorLines indicators);

    void setMarkers(List<Marker> markers);

    void setHighlights(List<TextRange> highlights);

    public void setErrorText(String title, String errorText);

    /**
     * If the text is being formatted by this view then you can provide a function to generate
     * highlights on the formatted text as the line/col positions in the formatted text may
     * differ to those in the original input. Should be called before setText is called.
     *
     * @param highlightsFunction A function to return a list of highlight ranges from the formatted text.
     */
    void setFormattedHighlights(final Function<String, List<TextRange>> highlightsFunction);

    void setReadOnly(final boolean readOnly);

    void setMode(final AceEditorMode mode);

    void setTheme(final AceEditorTheme theme);

    void setUserKeyBindingsPreference(final boolean useVimBindings);

    void setUserLiveAutoCompletePreference(final boolean isOn);

    Action getFormatAction();

    Option getStylesOption();

    Option getLineNumbersOption();

    Option getIndicatorsOption();

    Option getLineWrapOption();

    Option getShowIndentGuides();

    Option getShowInvisiblesOption();

    Option getUseVimBindingsOption();

    Option getBasicAutoCompletionOption();

    Option getSnippetsOption();

    Option getLiveAutoCompletionOption();

    Option getHighlightActiveLineOption();

    Option getViewAsHexOption();

    void setControlsVisible(boolean visible);
}
