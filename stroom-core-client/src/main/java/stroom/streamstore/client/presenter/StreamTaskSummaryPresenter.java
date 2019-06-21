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

package stroom.streamstore.client.presenter;

import com.google.gwt.cell.client.TextCell;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import stroom.cell.info.client.InfoColumn;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.data.grid.client.OrderByColumn;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.entity.shared.BaseEntity;
import stroom.entity.shared.ResultList;
import stroom.feed.shared.Feed;
import stroom.pipeline.shared.PipelineEntity;
import stroom.query.api.v2.DocRef;
import stroom.streamtask.shared.ExpressionUtil;
import stroom.streamtask.shared.FetchStreamTaskSummaryAction;
import stroom.streamtask.shared.FindStreamTaskCriteria;
import stroom.streamtask.shared.StreamTaskSummary;
import stroom.util.shared.ModelStringUtil;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupView.PopupType;
import stroom.widget.tooltip.client.presenter.TooltipPresenter;
import stroom.widget.tooltip.client.presenter.TooltipUtil;
import stroom.widget.util.client.MultiSelectionModel;

public class StreamTaskSummaryPresenter extends MyPresenterWidget<DataGridView<StreamTaskSummary>>
        implements HasDocumentRead<BaseEntity> {
    private final ActionDataProvider<StreamTaskSummary> dataProvider;
    private final FetchStreamTaskSummaryAction action;
    private boolean doneDataDisplay = false;

    @Inject
    public StreamTaskSummaryPresenter(final EventBus eventBus, final ClientDispatchAsync dispatcher,
                                      final TooltipPresenter tooltipPresenter) {
        super(eventBus, new DataGridViewImpl<>(true, false));

        action = new FetchStreamTaskSummaryAction();
        dataProvider = new ActionDataProvider<StreamTaskSummary>(dispatcher, action) {
            @Override
            protected void changeData(final ResultList<StreamTaskSummary> data) {
                super.changeData(data);
                onChangeData(data);
            }
        };

        // Info column.
        final InfoColumn<StreamTaskSummary> infoColumn = new InfoColumn<StreamTaskSummary>() {
            @Override
            protected void showInfo(final StreamTaskSummary row, final int x, final int y) {
                final StringBuilder html = new StringBuilder();

                TooltipUtil.addHeading(html, "Key Data");
                TooltipUtil.addRowData(html, "Pipeline", row.getPipeline().getName()
                        + " (" + row.getPipeline().getUuid() + ")");
                TooltipUtil.addRowData(html, "Feed", row.getFeed().getName() + " ("
                        + row.getFeed().getUuid() + ")");
                TooltipUtil.addRowData(html, "Priority", row.getPriority());
                TooltipUtil.addRowData(html, "Status", row.getStatus().getDisplayValue());

                tooltipPresenter.setHTML(html.toString());

                final PopupPosition popupPosition = new PopupPosition(x, y);
                ShowPopupEvent.fire(StreamTaskSummaryPresenter.this, tooltipPresenter, PopupType.POPUP, popupPosition,
                        null);
            }
        };
        getView().addColumn(infoColumn, "<br/>", ColumnSizeConstants.ICON_COL);

        getView().addResizableColumn(
                new OrderByColumn<StreamTaskSummary, String>(new TextCell(), FindStreamTaskCriteria.FIELD_PIPELINE_UUID, true) {
                    @Override
                    public String getValue(final StreamTaskSummary row) {
                        return row.getPipeline().getName();
                    }
                }, "Pipeline", 250);

        getView().addResizableColumn(
                new OrderByColumn<StreamTaskSummary, String>(new TextCell(), FindStreamTaskCriteria.FIELD_FEED_NAME, true) {
                    @Override
                    public String getValue(final StreamTaskSummary row) {
                        return row.getFeed().getName();
                    }
                }, "Feed", 250);

        getView().addResizableColumn(
                new OrderByColumn<StreamTaskSummary, String>(new TextCell(), FindStreamTaskCriteria.FIELD_PRIORITY, false) {
                    @Override
                    public String getValue(final StreamTaskSummary row) {
                        return row.getPriority();
                    }
                }, "Priority", 100);

        getView().addResizableColumn(
                new OrderByColumn<StreamTaskSummary, String>(new TextCell(), FindStreamTaskCriteria.FIELD_STATUS, false) {
                    @Override
                    public String getValue(final StreamTaskSummary row) {
                        return row.getStatus().getDisplayValue();
                    }
                }, "Status", 100);

        getView().addResizableColumn(
                new OrderByColumn<StreamTaskSummary, String>(new TextCell(), FindStreamTaskCriteria.FIELD_COUNT, false) {
                    @Override
                    public String getValue(final StreamTaskSummary row) {
                        return ModelStringUtil.formatCsv(row.getCount());
                    }
                }, "Count", 100);

        getView().addEndColumn(new EndColumn<>());
    }

    private void onChangeData(final ResultList<StreamTaskSummary> data) {
        final StreamTaskSummary selected = getView().getSelectionModel().getSelected();
        if (selected != null) {
            // Reselect the task set.
            getView().getSelectionModel().clear();
            if (data != null && data.contains(selected)) {
                getView().getSelectionModel().setSelected(selected);
            }
        }
    }

    public MultiSelectionModel<StreamTaskSummary> getSelectionModel() {
        return getView().getSelectionModel();
    }

    private void doDataDisplay() {
        if (!doneDataDisplay) {
            doneDataDisplay = true;
            dataProvider.addDataDisplay(getView().getDataDisplay());
        } else {
            dataProvider.refresh();
        }
    }

    private void setPipeline(final PipelineEntity pipelineEntity) {
        action.setExpression(ExpressionUtil.createPipelineExpression(pipelineEntity));
        doDataDisplay();
    }

    private void setFeed(final Feed feed) {
        action.setExpression(ExpressionUtil.createFeedExpression(feed));
        doDataDisplay();
    }

    private void setFolder(final DocRef folder) {
        action.setExpression(ExpressionUtil.createFolderExpression(folder));
        doDataDisplay();
    }

    private void setNullCriteria() {
        action.setExpression(null);
        doDataDisplay();
    }

    @Override
    public void read(final DocRef docRef, final BaseEntity entity) {
        if (entity instanceof PipelineEntity) {
            setPipeline((PipelineEntity) entity);
        } else if (entity instanceof Feed) {
            setFeed((Feed) entity);
        } else if (docRef != null) {
            setFolder(docRef);
        } else {
            setNullCriteria();
        }
    }
}
