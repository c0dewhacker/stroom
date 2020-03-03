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

package stroom.config.global.client.presenter;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.view.client.Range;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import stroom.alert.client.event.AlertEvent;
import stroom.config.global.shared.ConfigProperty;
import stroom.config.global.shared.GlobalConfigResource;
import stroom.config.global.shared.ListConfigResponse;
import stroom.data.client.presenter.RestDataProvider;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.data.table.client.Refreshable;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.node.shared.FetchNodeStatusResponse;
import stroom.node.shared.NodeResource;
import stroom.svg.client.SvgPreset;
import stroom.widget.button.client.ButtonView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ManageGlobalPropertyListPresenter
    extends MyPresenterWidget<DataGridView<ConfigProperty>>
    implements Refreshable, ColumnSortEvent.Handler{

    private static final String MULTIPLE_VALUES_MSG = "[Multiple values exist in the cluster]";
    private static final String MULTIPLE_SOURCES_MSG = "[Multiple]";

    private static final NodeResource NODE_RESOURCE = GWT.create(NodeResource.class);
    private static final GlobalConfigResource GLOBAL_CONFIG_RESOURCE_RESOURCE = GWT.create(GlobalConfigResource.class);

    private final RestDataProvider<ConfigProperty, ListConfigResponse> dataProvider;
    private final RestFactory restFactory;
    private String partialName;

    // propName => (node => effectiveValue)
    private final Map<String, Map<String, String>> propertyClusterEffectiveValuesMap = new HashMap<>();
    // propName => (effectiveValues)
    private Map<String, Set<String>> propertyClusterEffectiveValues = new HashMap<>();
    // propName => (node => source)
    private final Map<String, Map<String, String>> propertyClusterSourceMap = new HashMap<>();
    // propName => (sources)
    private Map<String, Set<String>> propertyClusterSources = new HashMap<>();

    private final Timer refreshAllNodesTimer = new Timer() {
        @Override
        public void run() {
            refreshPropertiesForAllNodes();
        }
    };
    private final Timer updateChildMapsTimer = new Timer() {
        @Override
        public void run() {
            updateChildMaps();
        }
    };

    @Inject
    public ManageGlobalPropertyListPresenter(final EventBus eventBus,
                                             final RestFactory restFactory) {
        super(eventBus, new DataGridViewImpl<>(true));
        this.restFactory = restFactory;

        initColumns();

        dataProvider = new RestDataProvider<ConfigProperty, ListConfigResponse>(eventBus) {
            @Override
            protected void exec(final Consumer<ListConfigResponse> dataConsumer,
                                final Consumer<Throwable> throwableConsumer) {
                final Rest<ListConfigResponse> rest = restFactory.create();
                rest
                    .onSuccess(listConfigResponse -> {
                        dataConsumer.accept(listConfigResponse);

                        // now we have the props from one node, go off and get all the values/sources
                        // from all the nodes. Use a timer to delay it a bit
                        if (!refreshAllNodesTimer.isRunning()) {
                            refreshAllNodesTimer.schedule(300);
                        }
                    })
                    .onFailure(throwableConsumer)
                    .call(GLOBAL_CONFIG_RESOURCE_RESOURCE)
                    .list(
                        partialName,
                        getView().getDataDisplay().getVisibleRange().getStart(),
                        getView().getDataDisplay().getVisibleRange().getLength());
            }

            @Override
            protected void changeData(final ListConfigResponse data) {
                super.changeData(data);
            }
        };
        dataProvider.addDataDisplay(getView().getDataDisplay());

    }

    private void refreshPropertiesForAllNodes() {
        final Rest<FetchNodeStatusResponse> fetchAllNodes = restFactory.create();

        // For each node fire off a request to get the yaml override for that node
        fetchAllNodes
            .onSuccess(response -> {
                response.getValues().forEach(nodeStatus -> {
                    refreshPropertiesForNode(nodeStatus.getNode().getName());
                });
            })
            .onFailure(throwable -> {
                showError(throwable, "Error getting list of all nodes");
            })
            .call(NODE_RESOURCE)
            .list();
    }

    private void refreshPropertiesForNode(final String nodeName) {
        final Rest<ListConfigResponse> listPropertiesRest = restFactory.create();

        listPropertiesRest
            .onSuccess(listConfigResponse -> {

                // Add the node's result to our maps
                listConfigResponse.getValues().forEach(configProperty -> {
                    final String effectiveValue = configProperty.getEffectiveValue().orElse(null);
                    final String source = configProperty.getSource().getName();

                    updateMaps(nodeName, configProperty.getNameAsString(), effectiveValue, source);
                });
            })
            .onFailure(throwable -> {
                propertyClusterEffectiveValuesMap.keySet().forEach(propName -> {
                    updateMaps(nodeName, propName, "[ERROR]", "[ERROR]");
                });
            })
            .call(GLOBAL_CONFIG_RESOURCE_RESOURCE)
            .listByNode(
                nodeName,
                partialName,
                getView().getDataDisplay().getVisibleRange().getStart(),
                getView().getDataDisplay().getVisibleRange().getLength());
    }

    private void updateMaps(final String nodeName,
                            final String propName,
                            final String effectiveValue,
                            final String source) {

        propertyClusterEffectiveValuesMap.computeIfAbsent(
            propName,
            k -> new HashMap<>())
            .put(nodeName, effectiveValue);

        propertyClusterSourceMap.computeIfAbsent(
            propName,
            k -> new HashMap<>())
            .put(nodeName, source);

        if (!updateChildMapsTimer.isRunning()) {
            updateChildMapsTimer.schedule(200);
        }
    }

    private void updateChildMaps() {
        propertyClusterEffectiveValues = propertyClusterEffectiveValuesMap.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    new HashSet<>(entry.getValue().values())
            ));

        propertyClusterSources = propertyClusterSourceMap.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    new HashSet<>(entry.getValue().values())
            ));
    }

    private void initColumns() {
        // Name.
        addColumn(buildBasicColumn(
            row ->
                row.getName().toString()),
            "Name",
            450);
        addColumn(buildBasicColumn(
            row -> {
//                row.getEffectiveValueMasked().orElse(null)),
                final Set<String> effectiveValues = propertyClusterEffectiveValues.get(row.getNameAsString());
                if (effectiveValues == null || effectiveValues.size() <= 1) {
                    return row.getEffectiveValueMasked().orElse(null);
                } else {
                    return MULTIPLE_VALUES_MSG;
                }
            }),
            "Effective Value",
            300);
        addColumn(buildBasicColumn(
            row -> {
                final Set<String> sources = propertyClusterSources.get(row.getNameAsString());
                if (sources == null || sources.size() <= 1) {
                    return row.getSource().getName();
                } else {
                    return MULTIPLE_SOURCES_MSG;
                }
            }),
            "Source",
            75);

        addColumn(buildDescriptionColumn(), "Description", 750);
        getView().addEndColumn(new EndColumn<>());
    }

    private Column<ConfigProperty, String> buildDescriptionColumn() {
        return new Column<ConfigProperty, String>(new TextCell()) {
            @Override
            public String getValue(final ConfigProperty row) {
                if (row == null) {
                    return null;
                }
                return row.getDescription();
            }
            @Override
            public String getCellStyleNames(Cell.Context context, ConfigProperty object) {
                return super.getCellStyleNames(context, object) + " "
                    + getView().getResources().dataGridStyle().dataGridCellWrapText() + " "
                    + getView().getResources().dataGridStyle().dataGridCellVerticalTop();
            }
        };
    }

    private Column<ConfigProperty, String> buildBasicColumn(final Function<ConfigProperty, String> valueFunc) {
        // TODO use OrderByColumn
        return new Column<ConfigProperty, String>(new TextCell()) {
            @Override
            public String getValue(final ConfigProperty row) {
                if (row == null) {
                    return null;
                }
                return valueFunc.apply(row);
            }
            @Override
            public String getCellStyleNames(Cell.Context context, ConfigProperty object) {
                return super.getCellStyleNames(context, object) + " "
                    + getView().getResources().dataGridStyle().dataGridCellVerticalTop();
            }
        };
    }

    private void addColumn(Column<ConfigProperty, String> column, String name, int width) {
        column.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
        getView().addResizableColumn(column, name, width);
    }

    public ButtonView addButton(final SvgPreset preset) {
        return getView().addButton(preset);
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        refresh();
    }

    @Override
    public void refresh() {
        dataProvider.refresh();
    }

    public ConfigProperty getSelectedItem() {
        return getView().getSelectionModel().getSelected();
    }

    public void setSelectedItem(final ConfigProperty row) {
        getView().getSelectionModel().setSelected(row);
    }

    void setPartialName(final String partialName) {
        this.partialName = partialName;
        // Need to reset the range else the name criteria can push us outside the page we are on
        Range range = getView().getVisibleRange();
        getView().getDataDisplay().setVisibleRange(0, range.getLength());
        refresh();
    }
    void clearPartialName() {
        this.partialName = null;
        refresh();
    }

    private void showError(final Throwable throwable, final String message) {
        AlertEvent.fireError(
            ManageGlobalPropertyListPresenter.this,
            message + " - " + throwable.getMessage(),
            null,
            null);
    }

    @Override
    public void onColumnSort(final ColumnSortEvent event) {
        // TODO implement sorting for Name and Source
    }
}
