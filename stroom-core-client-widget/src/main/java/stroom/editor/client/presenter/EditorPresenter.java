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

import stroom.editor.client.event.FormatEvent.FormatHandler;
import stroom.editor.client.event.HasFormatHandlers;
import stroom.editor.client.model.XmlFormatter;
import stroom.editor.client.view.EditorMenuPresenter;
import stroom.editor.client.view.IndicatorLines;
import stroom.editor.client.view.Marker;
import stroom.util.shared.TextRange;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import edu.ycp.cs.dh.acegwt.client.ace.AceCompletionProvider;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorTheme;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class EditorPresenter
        extends MyPresenterWidget<EditorView>
        implements HasFormatHandlers,
        HasText,
        HasValueChangeHandlers<String> {

    protected static final String VIM_KEY_BINDS_NAME = "VIM";
    private final EditorMenuPresenter contextMenu;
    private final DelegatingAceCompleter delegatingAceCompleter;

    @Inject
    public EditorPresenter(final EventBus eventBus,
                           final EditorView view,
                           final EditorMenuPresenter contextMenu,
                           final DelegatingAceCompleter delegatingAceCompleter,
                           final CurrentPreferences currentPreferences) {
        super(eventBus, view);
        this.contextMenu = contextMenu;
        this.delegatingAceCompleter = delegatingAceCompleter;
        view.setTheme(getTheme(currentPreferences));
        setEditorKeyBindings(view, currentPreferences.getEditorKeyBindings());
        view.setUserLiveAutoCompletePreference(currentPreferences.getEditorLiveAutoCompletion().isOn());

//        registerHandler(view.addMouseDownHandler(event -> contextMenu.hide()));

        registerHandler(view.addContextMenuHandler(event ->
            contextMenu.show(
                    EditorPresenter.this,
                        event.getPopupPosition())));
        registerHandler(view.addKeyDownHandler(event -> {
            if (event.isAltKeyDown() || event.isControlKeyDown()) {
                eventBus.fireEvent(event);
            }
        }));
        registerHandler(eventBus.addHandler(
                ChangeCurrentPreferencesEvent.getType(),
                this::handlePreferencesChange));
    }

    private void handlePreferencesChange(final ChangeCurrentPreferencesEvent event) {
        final EditorView view = getView();
        view.setTheme(getTheme(event.getTheme(), event.getEditorTheme()));
        // For the moment only standard and vim bindings are supported given the boolean
        // nature of the context menu
        view.setUserKeyBindingsPreference("VIM".equalsIgnoreCase(event.getEditorKeyBindings()));
        view.setUserLiveAutoCompletePreference(event.getEditorLiveAutoCompletion().isOn());
    }

    private void setEditorKeyBindings(final EditorView view, final String editorKeyBindingsName) {
        // For the moment only standard and vim bindings are supported given the boolean
        // nature of the context menu
        view.setUserKeyBindingsPreference(VIM_KEY_BINDS_NAME.equalsIgnoreCase(editorKeyBindingsName));
    }

    private AceEditorTheme getTheme(final CurrentPreferences currentPreferences) {
        return getTheme(currentPreferences.getTheme(), currentPreferences.getEditorTheme());
    }


    private AceEditorTheme getTheme(final String theme, final String editorTheme) {
        // Just in case it is null
        return Optional.ofNullable(editorTheme)
                .map(AceEditorTheme::fromName)
                .orElseGet(() ->
                        theme != null && theme.toLowerCase(Locale.ROOT).contains("dark")
                                ? AceEditorTheme.DEFAULT_DARK_THEME
                                : AceEditorTheme.DEFAULT_LIGHT_THEME);
    }

    public String getEditorId() {
        return getView().getEditorId();
    }

    public void focus() {
        getView().focus();
    }

    @Override
    public String getText() {
        return getView().getText();
    }

    /**
     * Sets the text for this control. If XML is supplied it will be turned into
     * HTML for styling.
     */
    @Override
    public void setText(final String text) {
        setText(text, false);
    }

    public boolean isClean() {
        return getView().isClean();
    }

    public void markClean() {
        getView().markClean();
    }

    /**
     * Replaces the editor with some html showing the errorText and its title
     */
    public void setErrorText(final String title, final String errorText) {
        getView().setErrorText(title, errorText);
    }

    public void insertTextAtCursor(final String text) {
        getView().insertTextAtCursor(text);
    }

    public void replaceSelectedText(final String text) {
        getView().replaceSelectedText(text);
    }

    public void insertSnippet(final String snippet) {
        getView().insertSnippet(snippet);
    }

    public void setText(final String text, final boolean format) {
        if (text == null) {
            getView().setText("");
        } else {
            if (format) {
                final String formatted = new XmlFormatter().format(text);
                getView().setText(formatted, true);
            } else {
                getView().setText(text);
            }
        }
    }

    public Action getFormatAction() {
        return getView().getFormatAction();
    }

    public Option getStylesOption() {
        return getView().getStylesOption();
    }

    public Option getLineNumbersOption() {
        return getView().getLineNumbersOption();
    }

    public Option getIndicatorsOption() {
        return getView().getIndicatorsOption();
    }

    public Option getLineWrapOption() {
        return getView().getLineWrapOption();
    }

    public Option getShowIndentGuides() {
        return getView().getShowIndentGuides();
    }

    public Option getShowInvisiblesOption() {
        return getView().getShowInvisiblesOption();
    }

    public Option getUseVimBindingsOption() {
        return getView().getUseVimBindingsOption();
    }

    public Option getBasicAutoCompletionOption() {
        return getView().getBasicAutoCompletionOption();
    }

    public Option getSnippetsOption() {
        return getView().getSnippetsOption();
    }

    public Option getLiveAutoCompletionOption() {
        return getView().getLiveAutoCompletionOption();
    }

    public Option getHighlightActiveLineOption() {
        return getView().getHighlightActiveLineOption();
    }

    public Option getViewAsHexOption() {
        return getView().getViewAsHexOption();
    }

    public void setFirstLineNumber(final int firstLineNumber) {
        getView().setFirstLineNumber(firstLineNumber);
    }

    public void setIndicators(final IndicatorLines indicators) {
        getView().setIndicators(indicators);
    }

    public void setMarkers(final List<Marker> markers) {
        getView().setMarkers(markers);
    }

    public void setHighlights(final List<TextRange> highlights) {
        getView().setHighlights(highlights);
    }

    public void setFormattedHighlights(final Function<String, List<TextRange>> highlightsFunction) {
        getView().setFormattedHighlights(highlightsFunction);
    }

    public void setControlsVisible(final boolean visible) {
        getView().setControlsVisible(visible);
    }

    public void setReadOnly(final boolean readOnly) {
        if (readOnly) {
            getFormatAction().setUnavailable();
            getBasicAutoCompletionOption().setOff();
            getBasicAutoCompletionOption().setUnavailable();
            getSnippetsOption().setOff();
            getSnippetsOption().setUnavailable();
            getLiveAutoCompletionOption().setOff();
            getLiveAutoCompletionOption().setUnavailable();
            getHighlightActiveLineOption().setOff();
        } else {
            getFormatAction().setToDefaultAvailability();
            getBasicAutoCompletionOption().setToDefaultState();
            getBasicAutoCompletionOption().setToDefaultAvailability();
            getSnippetsOption().setToDefaultState();
            getSnippetsOption().setToDefaultAvailability();
            getLiveAutoCompletionOption().setToDefaultState();
            getLiveAutoCompletionOption().setToDefaultAvailability();
            getHighlightActiveLineOption().setToDefaultState();
        }

        getView().setReadOnly(readOnly);
    }

    public void setOptionsToDefaultAvailability() {
        getView().setOptionsToDefaultAvailability();
    }

    public void setMode(final AceEditorMode mode) {
        getView().setMode(mode);
    }

    public void setTheme(final AceEditorTheme theme) {
        getView().setTheme(theme);
    }

    public EditorMenuPresenter getContextMenu() {
        return contextMenu;
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<String> handler) {
        return getView().addValueChangeHandler(handler);
    }

    @Override
    public HandlerRegistration addFormatHandler(final FormatHandler handler) {
        return getView().addFormatHandler(handler);
    }

    /**
     * Registers completion providers specific to this editor instance and mode
     */
    public void registerCompletionProviders(final AceEditorMode aceEditorMode,
                                            final AceCompletionProvider... completionProviders) {
        // scheduleDeferred to ensure editor is initialised before getId is called
        Scheduler.get().scheduleDeferred(() -> {
            delegatingAceCompleter.registerCompletionProviders(
                    getEditorId(), aceEditorMode, completionProviders);
        });
    }

    /**
     * Registers mode agnostic completion providers specific to this editor instance
     */
    public void registerCompletionProviders(final AceCompletionProvider... completionProviders) {
        // scheduleDeferred to ensure editor is initialised before getId is called
        Scheduler.get().scheduleDeferred(() -> {
            delegatingAceCompleter.registerCompletionProviders(
                    getEditorId(), completionProviders);
        });
    }

    /**
     * Removes all completion providers specific to this editor instance
     */
    public void deRegisterCompletionProviders() {
        // scheduleDeferred to ensure editor is initialised before getId is called
        Scheduler.get().scheduleDeferred(() -> {
            delegatingAceCompleter.deRegisterCompletionProviders(getEditorId());
        });
    }
}
