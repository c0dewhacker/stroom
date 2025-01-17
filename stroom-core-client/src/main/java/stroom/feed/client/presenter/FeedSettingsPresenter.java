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

package stroom.feed.client.presenter;

import stroom.data.client.presenter.DataTypeUiManager;
import stroom.data.store.impl.fs.shared.FsVolumeGroup;
import stroom.data.store.impl.fs.shared.FsVolumeGroupResource;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.entity.shared.ExpressionCriteria;
import stroom.feed.client.presenter.FeedSettingsPresenter.FeedSettingsView;
import stroom.feed.shared.FeedDoc;
import stroom.feed.shared.FeedDoc.FeedStatus;
import stroom.feed.shared.FeedResource;
import stroom.item.client.SelectionBox;
import stroom.util.shared.EqualsUtil;
import stroom.util.shared.ResultPage;
import stroom.widget.tickbox.client.view.CustomCheckBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.InputEvent;
import com.google.gwt.event.dom.client.InputHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;

import java.util.List;

public class FeedSettingsPresenter extends DocumentEditPresenter<FeedSettingsView, FeedDoc> {

    private static final FeedResource FEED_RESOURCE = GWT.create(FeedResource.class);
    private static final FsVolumeGroupResource VOLUME_GROUP_RESOURCE = GWT.create(FsVolumeGroupResource.class);

    private final RestFactory restFactory;

    @Inject
    public FeedSettingsPresenter(final EventBus eventBus,
                                 final FeedSettingsView view,
                                 final DataTypeUiManager dataTypeUiManager,
                                 final RestFactory restFactory) {
        super(eventBus, view);
        this.restFactory = restFactory;

        updateEncodings();
        updateVolumeGroups();

        view.getFeedStatus().addItems(FeedStatus.values());
        dataTypeUiManager.getTypes(list -> {
            view.getReceivedType().clear();
            if (list != null && !list.isEmpty()) {
                view.getReceivedType().addItems(list);
                final FeedDoc feed = getEntity();
                if (feed != null) {
                    view.getReceivedType().setValue(feed.getStreamType());
                }
            }
        });

        // Add listeners for dirty events.
        final InputHandler inputHandler = event -> setDirty(true);
        final ValueChangeHandler<Boolean> checkHandler = event -> setDirty(true);
        registerHandler(view.getClassification().addDomHandler(inputHandler, InputEvent.getType()));
        registerHandler(view.getReference().addValueChangeHandler(checkHandler));
        registerHandler(view.getDataEncoding().addValueChangeHandler(event -> {
            final String dataEncoding = ensureEncoding(view.getDataEncoding().getValue());
            getView().getDataEncoding().setValue(dataEncoding);

            if (!EqualsUtil.isEquals(dataEncoding, getEntity().getEncoding())) {
                getEntity().setEncoding(dataEncoding);
                setDirty(true);
            }
        }));
        registerHandler(view.getContextEncoding().addValueChangeHandler(event -> {
            final String contextEncoding = ensureEncoding(view.getContextEncoding().getValue());
            getView().getContextEncoding().setValue(contextEncoding);

            if (!EqualsUtil.isEquals(contextEncoding, getEntity().getContextEncoding())) {
                setDirty(true);
                getEntity().setContextEncoding(contextEncoding);
            }
        }));
        registerHandler(view.getFeedStatus().addValueChangeHandler(event -> setDirty(true)));
        registerHandler(view.getReceivedType().addValueChangeHandler(event -> {
            final String streamType = view.getReceivedType().getValue();
            getView().getReceivedType().setValue(streamType);

            if (!EqualsUtil.isEquals(streamType, getEntity().getStreamType())) {
                setDirty(true);
                getEntity().setStreamType(streamType);
            }
        }));
        registerHandler(view.getVolumeGroup().addValueChangeHandler(event -> {
            final String volumeGroup = view.getVolumeGroup().getValue();
            if (!EqualsUtil.isEquals(volumeGroup, getEntity().getVolumeGroup())) {
                setDirty(true);
                getEntity().setVolumeGroup(volumeGroup);
            }
        }));
    }

    private void updateEncodings() {
        final Rest<List<String>> rest = restFactory.create();
        rest
                .onSuccess(result -> {
                    getView().getDataEncoding().clear();
                    getView().getContextEncoding().clear();

                    if (result != null && result.size() > 0) {
                        for (final String encoding : result) {
                            getView().getDataEncoding().addItem(encoding);
                            getView().getContextEncoding().addItem(encoding);
                        }
                    }

                    final FeedDoc feed = getEntity();
                    if (feed != null) {
                        getView().getDataEncoding().setValue(ensureEncoding(feed.getEncoding()));
                        getView().getContextEncoding().setValue(ensureEncoding(feed.getContextEncoding()));
                    }
                })
                .call(FEED_RESOURCE)
                .fetchSupportedEncodings();
    }

    private void updateVolumeGroups() {
        final Rest<ResultPage<FsVolumeGroup>> rest = restFactory.create();
        rest
                .onSuccess(result -> {
                    getView().getVolumeGroup().clear();
                    getView().getVolumeGroup().setNonSelectString("");
                    if (result != null && result.getValues() != null) {
                        for (final FsVolumeGroup volumeGroup : result.getValues()) {
                            getView().getVolumeGroup().addItem(volumeGroup.getName());
                        }
                    }

                    final FeedDoc feed = getEntity();
                    if (feed != null) {
                        getView().getVolumeGroup().setValue(feed.getVolumeGroup());
                    }
                })
                .call(VOLUME_GROUP_RESOURCE)
                .find(new ExpressionCriteria());
    }

    @Override
    protected void onRead(final DocRef docRef, final FeedDoc feed, final boolean readOnly) {
        getView().getReference().setValue(feed.isReference());
        getView().getClassification().setText(feed.getClassification());
        getView().getDataEncoding().setValue(ensureEncoding(feed.getEncoding()));
        getView().getContextEncoding().setValue(ensureEncoding(feed.getContextEncoding()));
        getView().getReceivedType().setValue(feed.getStreamType());
        getView().getFeedStatus().setValue(feed.getStatus());
        getView().getVolumeGroup().setValue(feed.getVolumeGroup());
    }

    @Override
    protected FeedDoc onWrite(final FeedDoc feed) {
        feed.setReference(getView().getReference().getValue());
        feed.setClassification(getView().getClassification().getText());
        feed.setEncoding(ensureEncoding(getView().getDataEncoding().getValue()));
        feed.setContextEncoding(ensureEncoding(getView().getContextEncoding().getValue()));
        feed.setStreamType(getView().getReceivedType().getValue());
        feed.setVolumeGroup(getView().getVolumeGroup().getValue());

        // Set the process stage.
        feed.setStatus(getView().getFeedStatus().getValue());
        return feed;
    }

    private String ensureEncoding(final String encoding) {
        if (encoding == null || encoding.trim().length() == 0) {
            return "UTF-8";
        }
        return encoding;
    }

    public interface FeedSettingsView extends View {

        TextBox getClassification();

        CustomCheckBox getReference();

        SelectionBox<String> getDataEncoding();

        SelectionBox<String> getContextEncoding();

        SelectionBox<String> getReceivedType();

        SelectionBox<FeedStatus> getFeedStatus();

        SelectionBox<String> getVolumeGroup();
    }
}
