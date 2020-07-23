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

package stroom.data.pager.client;

import stroom.svg.client.SvgPresets;
import stroom.util.shared.HasCharacterData;
import stroom.widget.button.client.SvgButton;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DataNavigator extends Composite {

    private static final String PARTS_TITLE = "Part";
    private static final String RECORDS_TITLE = "Record";
    private static final String CHARACTERS_TITLE = "Characters";
    private static final String UNKNOWN_VALUE = "?";
    private static final int ZERO_TO_ONE_BASE_INCREMENT = 1;
    private static final String NUMBER_FORMAT = "#,###";

    private static Binder binder;

    private Runnable clickHandler;

    // Part selection controls for multi-part streams (typically non-segmented)
    // May not be visible
    @UiField
    Label lblParts;
    @UiField(provided = true)
    SvgButton firstPart;
    @UiField(provided = true)
    SvgButton prevPart;
    @UiField(provided = true)
    SvgButton nextPart;
    @UiField(provided = true)
    SvgButton lastPart;

    // Segment selection controls for segmented (cooked) streams (typically single-part)
    // May not be visible
    @UiField
    Label lblSegments;
    @UiField(provided = true)
    SvgButton firstSegment;
    @UiField(provided = true)
    SvgButton prevSegment;
    @UiField(provided = true)
    SvgButton nextSegment;
    @UiField(provided = true)
    SvgButton lastSegment;

    // Selection controls for the char data in the selected record and/or part
    // Always visible
    @UiField
    Label lblCharacters;
    @UiField(provided = true)
    SvgButton showHeadCharactersBtn;
    @UiField(provided = true)
    SvgButton advanceCharactersForwardBtn;
    @UiField(provided = true)
    SvgButton advanceCharactersBackwardBtn;

    @UiField(provided = true)
    SvgButton refresh;

    private HasCharacterData display;

    // The current values
//    private int partNo = -1;
//    private int totalParts = -1;
//    private int recordNo = -1;
//    private int totalSegments = -1;
//    private long charFrom = -1; // one based
//    private long charTo = -1; // one based
//    private long totalChars = -1;

    private final Set<FocusWidget> focussed = new HashSet<>();

    public DataNavigator() {
        if (binder == null) {
            binder = GWT.create(Binder.class);
        }
        initButtons();

        initWidget(binder.createAndBindUi(this));
    }

    private void initButtons() {
        // Part btns
        firstPart = SvgButton.create(SvgPresets.FAST_BACKWARD_BLUE.title("First Part"));
        prevPart = SvgButton.create(SvgPresets.STEP_BACKWARD_BLUE.title("Previous Part"));
        nextPart = SvgButton.create(SvgPresets.STEP_FORWARD_BLUE.title("Next Part"));
        lastPart = SvgButton.create(SvgPresets.FAST_FORWARD_BLUE.title("Last Part"));

        setupButton(firstPart, true, false);
        setupButton(prevPart, true, false);
        setupButton(nextPart, true, false);
        setupButton(lastPart, true, false);

        // Segment btns
        firstSegment = SvgButton.create(SvgPresets.FAST_BACKWARD_BLUE.title("First Record"));
        prevSegment = SvgButton.create(SvgPresets.STEP_BACKWARD_BLUE.title("Previous Record"));
        nextSegment = SvgButton.create(SvgPresets.STEP_FORWARD_BLUE.title("Next Record"));
        lastSegment = SvgButton.create(SvgPresets.FAST_FORWARD_BLUE.title("Last Record"));

        setupButton(firstSegment, true, false);
        setupButton(prevSegment, true, false);
        setupButton(nextSegment, true, false);
        setupButton(lastSegment, true, false);

        // Char buttons
        showHeadCharactersBtn = SvgButton.create(
                SvgPresets.FAST_BACKWARD_BLUE.title("Show Beginning"));
        advanceCharactersBackwardBtn = SvgButton.create(
                SvgPresets.STEP_BACKWARD_BLUE.title("Advance Range Backwards"));
        advanceCharactersForwardBtn = SvgButton.create(
                SvgPresets.STEP_FORWARD_BLUE.title("Advance Range Forwards"));

        setupButton(showHeadCharactersBtn, true, false);
        setupButton(advanceCharactersBackwardBtn, true, false);
        setupButton(advanceCharactersForwardBtn, true, false);

        refresh = SvgButton.create(SvgPresets.REFRESH_BLUE);

        setupButton(refresh, true, true);

    }

    private void setupButton(final SvgButton button,
                             final boolean isEnabled,
                             final boolean isVisible) {
        button.setEnabled(isEnabled);
        button.setVisible(isVisible);
        button.getElement().getStyle().setPaddingLeft(1, Style.Unit.PX);
        button.getElement().getStyle().setPaddingRight(1, Style.Unit.PX);
    }

    private void refreshPartControls() {
        if (display != null) {
            if (display.isMultiPart() && display.getPartNo().isPresent()) {
                long partNo = display.getPartNo().get();
                GWT.log("parts: " + display.getTotalParts().map(Objects::toString).orElse("?"));

                setPartControlVisibility(true);
                final String lbl = PARTS_TITLE
                        + " "
                        + getValueForLabel(display.getPartNo(), ZERO_TO_ONE_BASE_INCREMENT)
                        + " of "
                        + getValueForLabel(display.getTotalParts());

                lblParts.setText(lbl);

                boolean hasNext = partNo < display.getTotalParts()
                        .map(count -> count - 1) // part is zero based
                        .orElse(Long.MAX_VALUE);

                // PartNo zero based
                firstPart.setEnabled(partNo > 0);
                prevPart.setEnabled(partNo > 0);
                nextPart.setEnabled(hasNext);
                lastPart.setEnabled(hasNext);
            } else {
                setPartControlVisibility(false);
            }
        }
    }

    private void refreshSegmentControls() {
        if (display != null) {
            if (display.isSegmented()
                    && display.getSegmentNoFrom().isPresent()
                    && display.getSegmentNoTo().isPresent()) {

                long segmentNoFrom = display.getSegmentNoFrom().get();
                long segmentNoTo = display.getSegmentNoTo().get();

//                GWT.log("segments: " + display.getTotalParts().map(Objects::toString).orElse("?"));

                setSegmentControlVisibility(true);
                boolean isOneSegmentPerPage = segmentNoFrom == segmentNoTo;

                final String segmentNoFromStr = getValueForLabel(
                        display.getSegmentNoFrom(), ZERO_TO_ONE_BASE_INCREMENT);
                final String totalSegmentsStr = getValueForLabel(display.getTotalSegments());
                final String lbl;
                final String name = display.getSegmentName().orElse("");

                if (isOneSegmentPerPage) {
                    lbl = name + " " + segmentNoFromStr + " of " + totalSegmentsStr;
                } else {
                    final String segmentNoToStr = getValueForLabel(
                            display.getSegmentNoTo(), ZERO_TO_ONE_BASE_INCREMENT);
                    lbl = name
                            + "s "
                            + segmentNoFromStr
                            + " to "
                            + segmentNoToStr
                            + " of "
                            + getValueForLabel(display.getTotalSegments());
                }

                lblSegments.setText(lbl);

                // segmentNO zero based
                boolean hasNext = segmentNoTo < display.getTotalSegments()
                        .map(count -> count - 1) // part is zero based
                        .orElse(Long.MAX_VALUE);

                // segmentNo zero based
                firstSegment.setEnabled(segmentNoFrom > 0);
                prevSegment.setEnabled(segmentNoFrom > 0);
                nextSegment.setEnabled(hasNext);
                lastSegment.setEnabled(hasNext);
            } else {
                setSegmentControlVisibility(false);
            }
        }
    }

    private void refreshCharacterControls() {
        if (display != null
                && display.getCharFrom().isPresent()
                && display.getCharTo().isPresent()) {

            setCharactersControlVisibility(true);

            final String lbl = CHARACTERS_TITLE
                    + " "
                    + getValueForLabel(display.getCharFrom(), ZERO_TO_ONE_BASE_INCREMENT)
                    + " to "
                    + getValueForLabel(display.getCharTo(), ZERO_TO_ONE_BASE_INCREMENT)
                    + " of "
                    + getValueForLabel(display.getTotalChars());

            lblCharacters.setText(lbl);

            final long charFrom = display.getCharFrom().get();
            final long charTo = display.getCharTo().get();

            showHeadCharactersBtn.setEnabled(charFrom > 0);
            advanceCharactersBackwardBtn.setEnabled(charFrom > 0);

            advanceCharactersForwardBtn.setEnabled(
                    charTo < display.getTotalChars().map(total -> total - 1).orElse(Long.MAX_VALUE));
        } else {
            setCharactersControlVisibility(false);
        }
    }

    private void setPartControlVisibility(final boolean isVisible) {
        lblParts.setVisible(isVisible);
        firstPart.setVisible(isVisible);
        prevPart.setVisible(isVisible);
        nextPart.setVisible(isVisible);
        lastPart.setVisible(isVisible);
    }

    private void setSegmentControlVisibility(final boolean isVisible) {
        lblSegments.setVisible(isVisible);
        firstSegment.setVisible(isVisible);
        prevSegment.setVisible(isVisible);
        nextSegment.setVisible(isVisible);
        lastSegment.setVisible(isVisible);
    }

    private void setCharactersControlVisibility(final boolean isVisible) {
        lblCharacters.setVisible(isVisible);
        showHeadCharactersBtn.setVisible(isVisible);
        advanceCharactersBackwardBtn.setVisible(isVisible);
        advanceCharactersForwardBtn.setVisible(isVisible);
    }


    private String getValueForLabel(final Optional<Long> value) {
        return getValueForLabel(value, 0);
    }

    private String getValueForLabel(final Optional<Long> value, final int increment) {
        // Increment allows for switching from zero to one based
        return value
                .map(val -> val + increment)
                .map(val -> {
                    final NumberFormat formatter = NumberFormat.getFormat(NUMBER_FORMAT);
                    return formatter.format(val);
                })
                .orElse(UNKNOWN_VALUE);
    }

    public void setVisible(final boolean isVisible) {
        setPartControlVisibility(isVisible);
        setSegmentControlVisibility(isVisible);
        setCharactersControlVisibility(isVisible);
    }

    public void setDisplay(final HasCharacterData display) {
        this.display = display;
    }


    // Parts UI handlers
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @UiHandler("lblParts")
    public void onClickPartsLabel(final ClickEvent event) {
        clickHandler.run();
    }

    @UiHandler("firstPart")
    void onClickFirstPart(final ClickEvent event) {
        if (display != null) {
            display.getPartNo()
                    .ifPresent(partNo -> display.setPartNo(0)); // zero based
        }
    }

    @UiHandler("prevPart")
    void onClickPrevPart(final ClickEvent event) {
        if (display != null) {
            display.getPartNo()
                    .ifPresent(partNo -> display.setPartNo(partNo - 1)); // zero based
        }
    }

    @UiHandler("nextPart")
    void onClickNextPart(final ClickEvent event) {
        if (display != null) {
            display.getPartNo()
                    .ifPresent(partNo -> display.setPartNo(partNo + 1)); // zero based
        }
    }

    @UiHandler("lastPart")
    void onClickLastPart(final ClickEvent event) {
        if (display != null) {
            display.getTotalParts()
                    .ifPresent(parts -> display.setPartNo(parts - 1)); // zero based
        }
    }


    // Segments UI handlers
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @UiHandler("lblSegments")
    public void onClickSegmentsLabel(final ClickEvent event) {
        clickHandler.run();
    }

    @UiHandler("firstSegment")
    void onClickFirstSegment(final ClickEvent event) {
        if (display != null) {
            display.setSegmentNoFrom(0);
        }
    }

    @UiHandler("prevSegment")
    void onClickPrevSegment(final ClickEvent event) {
        if (display != null) {
            display.getSegmentNoFrom()
                    .ifPresent(segmentNo -> display.setSegmentNoFrom(segmentNo - 1));
        }
    }

    @UiHandler("nextSegment")
    void onClickNextSegment(final ClickEvent event) {
        if (display != null) {
            display.getSegmentNoFrom()
                    .ifPresent(segmentNo -> display.setSegmentNoFrom(segmentNo + 1));
        }
    }

    @UiHandler("lastSegment")
    void onClickLastSegment(final ClickEvent event) {
        if (display != null) {
            display.getTotalSegments()
                    .ifPresent(total -> display.setSegmentNoFrom(total - 1));
        }
    }


    // Characters UI handlers
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @UiHandler("lblCharacters")
    public void onClickCharactersLabel(final ClickEvent event) {
        clickHandler.run();
    }

    @UiHandler("showHeadCharactersBtn")
    void onClickShowHeadCharacters(final ClickEvent event) {
        if (display != null) {
            display.showHeadCharacters();
        }
    }

    @UiHandler("advanceCharactersBackwardBtn")
    void onClickNextAdvanceCharsBackwards(final ClickEvent event) {
        if (display != null) {
            display.advanceCharactersBackwards();
        }
    }

    @UiHandler("advanceCharactersForwardBtn")
    void onClickAdvanceCharsForwards(final ClickEvent event) {
        if (display != null) {
            display.advanceCharactersForward();
        }
    }


    @UiHandler("refresh")
    void onClickRefresh(final ClickEvent event) {
        if (display != null) {
            display.refresh();
        }
    }

    /**
     * Let the page know that the table is loading. Call this method to clear
     * all data from the table and hide the current range when new data is being
     * loaded into the table.
     */
//    public void startLoading() {
    // TODO @AT caller can just update the values in HasCharacterData and call refresh
//        getDisplay().setRowCount(0, true);
//        lblFrom.setText("?");
//        lblTo.setText("?");
//        lblOf.setText("?");
//    }
    public void refresh() {
        refreshPartControls();
        refreshSegmentControls();
        refreshCharacterControls();
    }

//    @Override
//    protected void onRangeOrRowCountChanged() {
//        final NumberFormat formatter = NumberFormat.getFormat("#,###");
//        final HasRows display = getDisplay();
//        final Range range = display.getVisibleRange();
//        final int pageStart = range.getStart() + 1;
//        final int pageSize = range.getLength();
//        final int dataSize = display.getRowCount();
//        int endIndex = Math.min(dataSize, pageStart + pageSize - 1);
//        endIndex = Math.max(pageStart, endIndex);
//
//        final boolean isRowCountExact = display.isRowCountExact();
//        final boolean hasPreviousPage = hasPreviousPage();
//        final boolean hasNextPage = hasNextPage();
//
//        // If we aren't currently editing from or to values then turn editing off.
//        if (focussed.size() == 0) {
//            setEditing(false);
//        }
//
//        lblFrom.setText(formatter.format(pageStart));
//        lblTo.setText(formatter.format(endIndex));
//        if (isRowCountExact) {
//            lblOf.setText(formatter.format(dataSize));
//        } else {
//            lblOf.setText("?");
//        }
//
//        // Update the buttons.
//        firstPart.setEnabled(hasPreviousPage);
//        prevPart.setEnabled(hasPreviousPage);
//        nextPart.setEnabled(hasNextPage);
//        lastPart.setEnabled(hasNextPage && isRowCountExact);
//
//        refresh.setEnabled(true);
//    }

    public void setRefreshing(final boolean refreshing) {
        if (refreshing) {
            refresh.getElement().addClassName("fa-spin");
        } else {
            refresh.getElement().removeClassName("fa-spin");
        }
    }

    public void setClickHandler(final Runnable clickHandler) {
        this.clickHandler = clickHandler;
    }

    public interface Binder extends UiBinder<Widget, DataNavigator> {
    }
}
