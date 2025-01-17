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
 *
 */

package stroom.security.client.view;

import stroom.security.client.presenter.CreateMultipleUsersPresenter.CreateMultipleUsersView;
import stroom.widget.popup.client.view.HideRequestUiHandlers;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class CreateMultipleUsersViewImpl
        extends ViewWithUiHandlers<HideRequestUiHandlers>
        implements CreateMultipleUsersView {

    private final Widget widget;

    @UiField
    TextArea userBatch;

    @Inject
    public CreateMultipleUsersViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
        widget.addAttachHandler(event -> focus());
    }

    @Override
    public void focus() {
        Scheduler.get().scheduleDeferred(() -> userBatch.setFocus(true));
    }

    @Override
    public void clear() {
        userBatch.setText(null);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public String getUsersCsvData() {
        return userBatch.getText();
    }

//    @UiHandler("userBatch")
//    void onUserBatchKeyDown(final KeyDownEvent event) {
//        handleKeyDown(event);
//    }
//
//    private void handleKeyDown(final KeyDownEvent event) {
//        if (event.getNativeKeyCode() == '\r') {
//            getUiHandlers().onHideRequest(false, true);
//        }
//    }

    public interface Binder extends UiBinder<Widget, CreateMultipleUsersViewImpl> {

    }
}
