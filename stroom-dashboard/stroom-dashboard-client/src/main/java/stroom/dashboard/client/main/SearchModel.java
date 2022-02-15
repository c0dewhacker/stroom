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

package stroom.dashboard.client.main;

import stroom.dashboard.client.query.QueryPresenter;
import stroom.dashboard.client.table.TimeZones;
import stroom.dashboard.shared.ComponentResultRequest;
import stroom.dashboard.shared.ComponentSettings;
import stroom.dashboard.shared.DashboardQueryKey;
import stroom.dashboard.shared.DashboardResource;
import stroom.dashboard.shared.DashboardSearchRequest;
import stroom.dashboard.shared.DashboardSearchResponse;
import stroom.dashboard.shared.Search;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionParamUtil;
import stroom.query.api.v2.ExpressionUtil;
import stroom.query.api.v2.Param;
import stroom.query.api.v2.Result;
import stroom.util.shared.RandomId;

import com.google.gwt.core.client.GWT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SearchModel {

    private static final DashboardResource DASHBOARD_RESOURCE = GWT.create(DashboardResource.class);

    private final RestFactory restFactory;
    private final SearchKeepAlive searchKeepAlive;
    private final QueryPresenter queryPresenter;
    private final IndexLoader indexLoader;
    private final TimeZones timeZones;
    private String applicationInstanceId;
    private String dashboardUuid;
    private String componentId;
    private Map<String, ResultComponent> componentMap = new HashMap<>();
    private Map<String, String> currentParameterMap;
    private ExpressionOperator currentExpression;
    private DashboardSearchResponse currentResult;
    private DashboardQueryKey currentQueryKey;
    private Search currentSearch;
    private Mode mode = Mode.INACTIVE;
    private boolean searching;
    private boolean polling;

    public SearchModel(final RestFactory restFactory,
                       final SearchKeepAlive searchKeepAlive,
                       final QueryPresenter queryPresenter,
                       final IndexLoader indexLoader,
                       final TimeZones timeZones) {
        this.restFactory = restFactory;
        this.searchKeepAlive = searchKeepAlive;
        this.queryPresenter = queryPresenter;
        this.indexLoader = indexLoader;
        this.timeZones = timeZones;
    }

    public void init(final String applicationInstanceId,
                     final String dashboardUuid,
                     final String componentId) {
        this.applicationInstanceId = applicationInstanceId;
        this.dashboardUuid = dashboardUuid;
        this.componentId = componentId;
    }

    private void start() {
        searching = true;
        poll();
    }

    /**
     * Stop searching, set the search mode to inactive and tell all components
     * that they no longer want data and search has ended.
     */
    public void destroy() {
        GWT.log("SearchModel - destroy()");
        if (currentQueryKey != null) {
            searchKeepAlive.remove(currentQueryKey);
            currentQueryKey = null;
        }
        setMode(Mode.INACTIVE);

        // Stop the spinner from spinning and tell components that they no
        // longer want data.
        for (final ResultComponent resultComponent : componentMap.values()) {
            resultComponent.setWantsData(false);
            resultComponent.endSearch();
        }

        // Stop polling.
        searching = false;
        polling = false;
    }

    private void poll() {
        if (!polling) {
            polling = true;

            final DashboardQueryKey queryKey = currentQueryKey;
            final Search search = currentSearch;
            if (queryKey != null && search != null && searching) {
                final List<ComponentResultRequest> requests = new ArrayList<>();
                for (final Entry<String, ResultComponent> entry : componentMap.entrySet()) {
                    final ResultComponent resultComponent = entry.getValue();
                    final ComponentResultRequest componentResultRequest = resultComponent.getResultRequest();
                    requests.add(componentResultRequest);
                }
                final DashboardSearchRequest request = DashboardSearchRequest
                        .builder()
                        .dashboardQueryKey(queryKey)
                        .search(search)
                        .componentResultRequests(requests)
                        .dateTimeLocale(timeZones.getTimeZone())
                        .build();

                final Rest<DashboardSearchResponse> rest = restFactory.create();
                rest
                        .onSuccess(response -> {
                            try {
                                update(response);
                            } catch (final RuntimeException e) {
                                GWT.log(e.getMessage());
                            }

                            polling = false;
                            if (searching) {
                                poll();
                            }

                        })
                        .onFailure(throwable -> {
                            try {
                                if (queryKey.equals(currentQueryKey)) {
                                    queryPresenter.setErrors(Collections.singletonList(throwable.toString()));
                                    searching = false;
                                }
                            } catch (final RuntimeException e) {
                                GWT.log(e.getMessage());
                            }

                            polling = false;
                            if (searching) {
                                poll();
                            }
                        })
                        .call(DASHBOARD_RESOURCE)
                        .search(request);
            } else {
                polling = false;
            }
        }
    }

    /**
     * Run a search with the provided expression, returning results for all
     * components.
     */
    public void search(final ExpressionOperator expression,
                       final String params,
                       final boolean incremental,
                       final boolean storeHistory,
                       final String queryInfo) {
        GWT.log("SearchModel - search() - mode=" + mode);
        // Toggle the request mode or start a new search.
        switch (mode) {
            case ACTIVE:
                // Tell every component not to want data.
                setWantsData(false);
                setMode(Mode.PAUSED);
                break;
            case INACTIVE:
                startNewSearch(expression, params, incremental, storeHistory, queryInfo);
                break;
            case PAUSED:
                // Tell every component that it should want data.
                setWantsData(true);
                setMode(Mode.ACTIVE);
                break;
        }
    }

    /**
     * Prepares the necessary parts for a search without actually starting the search. Intended
     * for use when you just want the complete {@link DashboardSearchRequest} object
     */
    private Map<String, ComponentSettings> initModel(final ExpressionOperator expression,
                                                     final String params,
                                                     final boolean incremental,
                                                     final boolean storeHistory,
                                                     final String queryInfo) {

        final Map<String, ComponentSettings> componentSettingsMap = createComponentSettingsMap();
        if (componentSettingsMap != null) {
            final DocRef dataSourceRef = indexLoader.getLoadedDataSourceRef();
            if (dataSourceRef != null && expression != null) {
                // Create a parameter map.
                currentParameterMap = ExpressionParamUtil.parse(params);

                // Copy the expression.
                currentExpression = ExpressionUtil.copyOperator(expression);

                currentSearch = Search
                        .builder()
                        .dataSourceRef(dataSourceRef)
                        .expression(currentExpression)
                        .componentSettingsMap(componentSettingsMap)
                        .params(getParams(currentParameterMap))
                        .incremental(incremental)
                        .storeHistory(storeHistory)
                        .queryInfo(queryInfo)
                        .build();
            }
        }
        return componentSettingsMap;
    }

    private List<Param> getParams(final Map<String, String> parameterMap) {
        final List<Param> params = new ArrayList<>();
        for (final Entry<String, String> entry : parameterMap.entrySet()) {
            params.add(new Param(entry.getKey(), entry.getValue()));
        }
        return params;
    }

    /**
     * Begin executing a new search using the supplied query expression.
     *
     * @param expression The expression to search with.
     */
    private void startNewSearch(final ExpressionOperator expression,
                                final String params,
                                final boolean incremental,
                                final boolean storeHistory,
                                final String queryInfo) {
        // Destroy the previous search and ready all components for a new search to begin.
        destroy();

        // Tell every component that it should want data.
        setWantsData(true);

        GWT.log("SearchModel - startNewSearch()");
        final Map<String, ComponentSettings> resultComponentMap = initModel(
                expression,
                params,
                incremental,
                storeHistory,
                queryInfo);

        if (resultComponentMap != null) {
            final DocRef dataSourceRef = indexLoader.getLoadedDataSourceRef();
            if (dataSourceRef != null && expression != null) {
                // Let the query presenter know search is active.
                setMode(Mode.ACTIVE);

                // Reset all result components and tell them that search is
                // starting.
                for (final Entry<String, ResultComponent> entry : componentMap.entrySet()) {
                    final ResultComponent resultComponent = entry.getValue();
                    resultComponent.reset();
                    resultComponent.startSearch();
                }

                // Register this new query so that the bus can perform the
                // search.
                currentQueryKey = new DashboardQueryKey(
                        applicationInstanceId,
                        dashboardUuid,
                        componentId,
                        RandomId.createDiscrimiator());
                searchKeepAlive.add(currentQueryKey);

                // Start polling.
                start();
            }
        }
    }

    /**
     * Refresh the search data for the specified component.
     */
    public void refresh(final String componentId) {
        final ResultComponent resultComponent = componentMap.get(componentId);
        if (resultComponent != null) {
            final Map<String, ComponentSettings> resultComponentMap = createComponentSettingsMap();
            if (resultComponentMap != null) {
                final DocRef dataSourceRef = indexLoader.getLoadedDataSourceRef();
                if (dataSourceRef != null) {
                    currentSearch = Search
                            .builder()
                            .dataSourceRef(dataSourceRef)
                            .expression(currentExpression)
                            .componentSettingsMap(resultComponentMap)
                            .params(getParams(currentParameterMap))
                            .incremental(true)
                            .storeHistory(false)
                            .build();

                    // Tell the refreshing component that it should want data.
                    resultComponent.setWantsData(true);
                    resultComponent.startSearch();
                    start();
                }
            }
        }
    }

    /**
     * Creates a result component map for all components.
     *
     * @return A result component map.
     */
    private Map<String, ComponentSettings> createComponentSettingsMap() {
        if (componentMap.size() > 0) {
            final Map<String, ComponentSettings> resultComponentMap = new HashMap<>();
            for (final Entry<String, ResultComponent> entry : componentMap.entrySet()) {
                final String componentId = entry.getKey();
                final ResultComponent resultComponent = entry.getValue();
                final ComponentSettings componentSettings = resultComponent.getSettings();
                resultComponentMap.put(componentId, componentSettings);
            }
            return resultComponentMap;
        }
        return null;
    }

    /**
     * Method to update the wantsData state for all interested components.
     *
     * @param wantsData True if you want all components to be ready to receive data.
     */
    private void setWantsData(final boolean wantsData) {
        // Tell every component that it should want data.
        for (final Entry<String, ResultComponent> entry : componentMap.entrySet()) {
            final ResultComponent resultComponent = entry.getValue();
            resultComponent.setWantsData(wantsData);
        }
    }

    /**
     * On receiving a search result from the server update all interested
     * components with new data.
     *
     * @param result The search response.
     */
    private void update(final DashboardSearchResponse result) {
        currentResult = result;

        // Give results to the right components.
        if (result.getResults() != null) {
            for (final Result componentResult : result.getResults()) {
                final ResultComponent resultComponent = componentMap.get(componentResult.getComponentId());
                if (resultComponent != null) {
                    resultComponent.setData(componentResult);
                }
            }
        }

        // Tell all components if we are complete.
        if (result.isComplete()) {
            componentMap.values().forEach(resultComponent -> {
                // Stop the spinner from spinning and tell components that they
                // no longer want data.
                resultComponent.setWantsData(false);
                resultComponent.endSearch();
            });
        }

        queryPresenter.setErrors(result.getErrors());

        if (result.isComplete()) {
            // Let the query presenter know search is inactive.
            setMode(Mode.INACTIVE);

            // If we have completed search then stop the task spinner.
            searching = false;
        }
    }

    public Mode getMode() {
        return mode;
    }

    private void setMode(final Mode mode) {
        this.mode = mode;
        queryPresenter.setMode(mode);
    }

    /**
     * Initialises the model for passed expression and current result settings and returns
     * the corresponding {@link DashboardSearchRequest} object
     */
    public DashboardSearchRequest createDownloadQueryRequest(final ExpressionOperator expression,
                                                             final String params) {
        Search search = null;
        final Map<String, ComponentSettings> resultComponentMap = createComponentSettingsMap();
        if (resultComponentMap != null) {
            final DocRef dataSourceRef = indexLoader.getLoadedDataSourceRef();
            if (dataSourceRef != null && expression != null) {
                // Create a parameter map.
                final Map<String, String> currentParameterMap = ExpressionParamUtil.parse(params);

                // Copy the expression.
                final ExpressionOperator currentExpression = ExpressionUtil.copyOperator(expression);

                search = Search
                        .builder()
                        .dataSourceRef(dataSourceRef)
                        .expression(currentExpression)
                        .componentSettingsMap(resultComponentMap)
                        .params(getParams(currentParameterMap))
                        .build();
            }
        }

        if (search == null || componentMap.size() == 0) {
            return null;
        }

        final List<ComponentResultRequest> requests = new ArrayList<>();
        for (final Entry<String, ResultComponent> entry : componentMap.entrySet()) {
            final ResultComponent resultComponent = entry.getValue();
            final ComponentResultRequest componentResultRequest = resultComponent.createDownloadQueryRequest();
            requests.add(componentResultRequest);
        }

        return DashboardSearchRequest
                .builder()
                .search(search)
                .componentResultRequests(requests)
                .dateTimeLocale(timeZones.getTimeZone())
                .build();
    }

    public boolean isSearching() {
        return searching;
    }

    public DashboardQueryKey getCurrentQueryKey() {
        return currentQueryKey;
    }

    public Search getCurrentSearch() {
        return currentSearch;
    }

    public IndexLoader getIndexLoader() {
        return indexLoader;
    }

    public DashboardSearchResponse getCurrentResult() {
        return currentResult;
    }

    public void addComponent(final String componentId, final ResultComponent resultComponent) {
        // Create and assign a new map here to prevent concurrent modification exceptions.
        final Map<String, ResultComponent> componentMap = new HashMap<>(this.componentMap);
        componentMap.put(componentId, resultComponent);
        this.componentMap = componentMap;
    }

    public void removeComponent(final String componentId) {
        // Create and assign a new map here to prevent concurrent modification exceptions.
        final Map<String, ResultComponent> componentMap = new HashMap<>(this.componentMap);
        componentMap.remove(componentId);
        this.componentMap = componentMap;
    }

    public enum Mode {
        ACTIVE,
        INACTIVE,
        PAUSED
    }
}
