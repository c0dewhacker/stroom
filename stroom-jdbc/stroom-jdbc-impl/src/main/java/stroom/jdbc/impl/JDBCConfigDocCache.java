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

package stroom.jdbc.impl;

import stroom.cache.api.CacheManager;
import stroom.cache.api.LoadingStroomCache;
import stroom.docref.DocRef;
import stroom.jdbc.shared.JDBCConfigDoc;
import stroom.security.api.SecurityContext;
import stroom.util.NullSafe;
import stroom.util.entityevent.EntityAction;
import stroom.util.entityevent.EntityEvent;
import stroom.util.entityevent.EntityEventHandler;
import stroom.util.shared.Clearable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
@EntityEventHandler(
        type = JDBCConfigDoc.DOCUMENT_TYPE,
        action = {EntityAction.DELETE, EntityAction.UPDATE, EntityAction.CLEAR_CACHE})
public class JDBCConfigDocCache implements Clearable, EntityEvent.Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JDBCConfigDocCache.class);

    private static final String CACHE_NAME = "JDBC Database Config Doc Cache";

    private final LoadingStroomCache<DocRef, Optional<JDBCConfigDoc>> cache;
    private final JDBCConfigStore jdbcConfigStore;
    private final SecurityContext securityContext;

    @Inject
    public JDBCConfigDocCache(final CacheManager cacheManager,
                               final JDBCConfigStore jdbcConfigStore,
                               final Provider<JDBCConfig> jdbcConfigProvider,
                               final SecurityContext securityContext) {
        this.jdbcConfigStore = jdbcConfigStore;
        this.securityContext = securityContext;
        cache = cacheManager.createLoadingCache(
                CACHE_NAME,
                () -> jdbcConfigProvider.get().getJDBCConfigDocCache(),
                this::create);
    }

    public Optional<JDBCConfigDoc> get(final DocRef jdbcConfigDocRef) {
        return cache.get(jdbcConfigDocRef);
    }

    private Optional<JDBCConfigDoc> create(final DocRef jdbcConfigDocRef) {
        Objects.requireNonNull(jdbcConfigDocRef);
        return securityContext.asProcessingUserResult(() ->
                Optional.ofNullable(jdbcConfigStore.readDocument(jdbcConfigDocRef)));
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void onChange(final EntityEvent event) {
        LOGGER.debug("Received event {}", event);
        final EntityAction eventAction = event.getAction();

        switch (eventAction) {
            case CLEAR_CACHE -> {
                LOGGER.debug("Clearing cache");
                clear();
            }
            case UPDATE, DELETE -> {
                NullSafe.consume(
                        event.getDocRef(),
                        docRef -> {
                            LOGGER.debug("Invalidating docRef {}", docRef);
                            cache.invalidate(docRef);
                        });
            }
            default -> LOGGER.debug("Unexpected event action {}", eventAction);
        }
    }
}
