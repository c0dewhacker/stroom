package stroom.data.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.data.client.presenter.CharacterRangeSelectionPresenter.CharacterRangeSelectionView;
import stroom.util.shared.Count;
import stroom.util.shared.DataRange;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.DefaultPopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.function.Consumer;

public class CharacterRangeSelectionPresenter extends MyPresenterWidget<CharacterRangeSelectionView> {

    private DataRange dataRange;

    @Inject
    public CharacterRangeSelectionPresenter(final EventBus eventBus,
                                            final CharacterRangeSelectionView view) {
        super(eventBus, view);
    }

    public DataRange getDataRange() {
        return dataRange;
    }

    public void setDataRange(final DataRange dataRange) {
        this.dataRange = dataRange;
    }

    public void setTotalCharsCount(final Count<Long> totalCharsCount) {
        getView().setTotalCharsCount(totalCharsCount);
    }

    private void write() {
        dataRange = getView().getDataRange();
    }

    private void read() {
        getView().setDataRange(dataRange);
    }

    public void show(final Consumer<DataRange> dataRangeConsumer) {
        read();
        final PopupUiHandlers popupUiHandlers = new DefaultPopupUiHandlers(this) {
            @Override
            public void onHideRequest(final boolean autoClose, final boolean ok) {
                if (!ok || isDataRangeValid()) {
                    if (ok) {
                        write();
                        dataRangeConsumer.accept(getDataRange());
                    }
                    hide(autoClose, ok);
                }
            }
        };
        ShowPopupEvent.fire(
                this,
                this,
                PopupType.OK_CANCEL_DIALOG,
                "Set Source Range",
                popupUiHandlers);
    }

    private boolean isDataRangeValid() {
        boolean result = true;
        String msg = "";

        final DataRange dataRange = getView().getDataRange();

        if (dataRange.getLocationFrom() != null
                && dataRange.getLocationTo() != null
                && dataRange.getLocationTo().isBefore(dataRange.getLocationFrom())) {
            msg = "To location " + dataRange.getLocationTo()
                    + " is before from location " + dataRange.getLocationFrom();
            result = false;
        } else if (dataRange.getLocationFrom() != null
                && dataRange.getLocationTo() != null
                && dataRange.getLocationTo().equals(dataRange.getLocationFrom())) {
            msg = "From location " + dataRange.getLocationFrom()
                    + " is equal to the to location " + dataRange.getLocationTo();
            result = false;
        } else if (dataRange.getCharOffsetFrom() != null
                && dataRange.getCharOffsetTo() != null
                && dataRange.getCharOffsetTo() < dataRange.getCharOffsetFrom()) {
            msg = "To offset " + dataRange.getCharOffsetTo() + 1 // to one based
                    + " is before from offset " + dataRange.getCharOffsetFrom() + 1; // to one based
            result = false;
        } else if (dataRange.getCharOffsetFrom() != null
                && dataRange.getCharOffsetTo() != null
                && dataRange.getCharOffsetTo().equals(dataRange.getCharOffsetFrom())) {
            msg = "From offset " + dataRange.getCharOffsetFrom() + 1 // to one based
                    + " is equal to to offset" + dataRange.getCharOffsetTo() + 1; // to one based
            result = false;
        }

        if (!result) {
            AlertEvent.fireError(CharacterRangeSelectionPresenter.this, msg, null);
        }

        return result;
    }

    public interface CharacterRangeSelectionView extends View {

        DataRange getDataRange();

        void setDataRange(final DataRange dataRange);

        void setTotalCharsCount(final Count<Long> totalCharCount);
    }
}
