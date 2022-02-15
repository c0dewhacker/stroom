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
 */

package stroom.dashboard.impl;

import stroom.dashboard.shared.DashboardQueryKey;
import stroom.dashboard.shared.SearchKeepAliveRequest;
import stroom.dashboard.shared.SearchKeepAliveResponse;
import stroom.datasource.api.v2.DataSourceProvider;
import stroom.docref.DocRef;
import stroom.query.api.v2.QueryKey;
import stroom.security.api.SecurityContext;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class ActiveQueries {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ActiveQueries.class);

    private final String key;
    private final ConcurrentHashMap<DashboardQueryKey, ActiveQuery> activeQueries = new ConcurrentHashMap<>();
    private final SecurityContext securityContext;

    ActiveQueries(final String key,
                  final SecurityContext securityContext) {
        this.key = key;
        this.securityContext = securityContext;
    }

    public SearchKeepAliveResponse keepAlive(final SearchKeepAliveRequest request) {
        LOGGER.trace(() -> "keepAlive() " + request);

        final Set<DashboardQueryKey> activeKeys = new HashSet<>();
        final Set<DashboardQueryKey> deadKeys = new HashSet<>();

        // Kill off any searches that are no longer required by the UI.
        Iterator<Entry<DashboardQueryKey, ActiveQuery>> iterator = activeQueries.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<DashboardQueryKey, ActiveQuery> entry = iterator.next();
            final DashboardQueryKey queryKey = entry.getKey();

            final ActiveQuery activeQuery = entry.getValue();
            if (request.getDeadKeys().contains(queryKey)) {
                // Destroy the query.
                final Boolean success = securityContext.asProcessingUserResult(activeQuery::destroy);
                if (Boolean.TRUE.equals(success)) {
                    // Remove the collector from the available searches as it is no longer required by the UI.
                    iterator.remove();
                    deadKeys.add(queryKey);
                }
            } else {
                // Keep the query alive.
                if (!activeQuery.keepAlive()) {
                    LOGGER.error("Unable to keep alive: " + queryKey.toString());
                }

                // Let the UI know this query is still alive.
                activeKeys.add(queryKey);
            }
        }
        return new SearchKeepAliveResponse(activeKeys, deadKeys);
    }

    ActiveQuery getExistingQuery(final DashboardQueryKey queryKey) {
        return activeQueries.get(queryKey);
    }

    ActiveQuery addNewQuery(final DashboardQueryKey dashboardQueryKey,
                            final DocRef docRef,
                            final QueryKey queryKey,
                            final DataSourceProvider dataSourceProvider) {
        final ActiveQuery activeQuery = new ActiveQuery(dashboardQueryKey, queryKey, docRef, dataSourceProvider);
        final ActiveQuery existing = activeQueries.put(dashboardQueryKey, activeQuery);
        if (existing != null) {
            throw new RuntimeException(
                    "Existing active query found in active query map for '" + dashboardQueryKey + "'");
        }
        return activeQuery;
    }

    void destroy() {
        LOGGER.trace(() -> "destroy() - " + key, new RuntimeException("destroy"));

        // Kill off all searches.
        Iterator<Entry<DashboardQueryKey, ActiveQuery>> iterator = activeQueries.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<DashboardQueryKey, ActiveQuery> entry = iterator.next();
            final ActiveQuery activeQuery = entry.getValue();
            final Boolean success = securityContext.asProcessingUserResult(activeQuery::destroy);
            if (Boolean.TRUE.equals(success)) {
                // Remove the collector from the available searches as it is no longer required by the UI.
                iterator.remove();
            }
        }
    }

    @Override
    public String toString() {
        return key;
    }
}
