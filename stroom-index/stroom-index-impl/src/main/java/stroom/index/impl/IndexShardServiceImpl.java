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

package stroom.index.impl;

import stroom.datasource.api.v2.DateField;
import stroom.datasource.api.v2.FieldInfo;
import stroom.datasource.api.v2.FindFieldInfoCriteria;
import stroom.docref.DocRef;
import stroom.entity.shared.ExpressionCriteria;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.IndexDoc;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShardFields;
import stroom.index.shared.IndexShardKey;
import stroom.index.shared.IndexVolume;
import stroom.index.shared.LuceneVersion;
import stroom.index.shared.LuceneVersionUtil;
import stroom.query.common.v2.FieldInfoResultPageBuilder;
import stroom.query.language.functions.FieldIndex;
import stroom.query.language.functions.ValuesConsumer;
import stroom.searchable.api.Searchable;
import stroom.security.api.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.PermissionNames;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.shared.ModelStringUtil;
import stroom.util.shared.PermissionException;
import stroom.util.shared.ResultPage;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class IndexShardServiceImpl implements IndexShardService, Searchable {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(IndexShardServiceImpl.class);

    private static final String PERMISSION = PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION;


    private final SecurityContext securityContext;
    private final IndexStructureCache indexStructureCache;
    private final IndexShardDao indexShardDao;
    private final IndexVolumeService indexVolumeService;

    private LuceneVersion indexVersion = LuceneVersionUtil.CURRENT_LUCENE_VERSION;

    @Inject
    IndexShardServiceImpl(final SecurityContext securityContext,
                          final IndexStructureCache indexStructureCache,
                          final IndexShardDao indexShardDao,
                          final IndexVolumeService indexVolumeService) {
        this.securityContext = securityContext;
        this.indexStructureCache = indexStructureCache;
        this.indexShardDao = indexShardDao;
        this.indexVolumeService = indexVolumeService;
    }

    @Override
    public IndexShard loadById(final Long id) {
        return securityContext.secureResult(() -> indexShardDao.fetch(id).orElse(null));
    }

    @Override
    public ResultPage<IndexShard> find(final FindIndexShardCriteria criteria) {
        return securityContext.secureResult(() -> indexShardDao.find(criteria));
    }

    @Override
    public IndexShard createIndexShard(final IndexShardKey indexShardKey,
                                       final String ownerNodeName) {
        return securityContext.secureResult(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            final IndexStructure indexStructure = indexStructureCache.get(
                    new DocRef(IndexDoc.DOCUMENT_TYPE, indexShardKey.getIndexUuid()));
            final IndexDoc index = indexStructure.getIndex();
            final IndexVolume indexVolume = indexVolumeService.selectVolume(index.getVolumeGroupName(), ownerNodeName);

            return indexShardDao.create(
                    indexShardKey,
                    indexVolume,
                    ownerNodeName,
                    indexVersion.getDisplayValue());
        });
    }

    @Override
    public Boolean delete(final IndexShard indexShard) {
        return securityContext.secureResult(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            if (!securityContext.hasDocumentPermission(indexShard.getIndexUuid(), DocumentPermissionNames.DELETE)) {
                throw new PermissionException(
                        securityContext.getUserIdentityForAudit(),
                        "You do not have permission to delete index shard");
            }

            indexShardDao.delete(indexShard.getId());

            return Boolean.TRUE;
        });
    }

    @Override
    public Boolean setStatus(final Long id,
                             final IndexShard.IndexShardStatus status) {
        return securityContext.secureResult(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            indexShardDao.setStatus(id, status);
            return Boolean.TRUE;
        });
    }

    @Override
    public void update(final long indexShardId,
                       final Integer documentCount,
                       final Long commitDurationMs,
                       final Long commitMs,
                       final Long fileSize) {
        securityContext.secure(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION, () -> {
            // Output some debug so we know how long commits are taking.
            LOGGER.debug(() -> String.format("Documents written %s (%s)",
                    documentCount,
                    ModelStringUtil.formatDurationString(commitDurationMs)));
            indexShardDao.update(indexShardId, documentCount, commitDurationMs, commitMs, fileSize);
        });
    }

    @Override
    public DocRef getDocRef() {
        return IndexShardFields.INDEX_SHARDS_PSEUDO_DOC_REF;
    }

    @Override
    public ResultPage<FieldInfo> getFieldInfo(final FindFieldInfoCriteria criteria) {
        return FieldInfoResultPageBuilder.builder(criteria).addAll(IndexShardFields.getFields()).build();
    }

    @Override
    public Optional<String> fetchDocumentation(final DocRef docRef) {
        return Optional.empty();
    }

    @Override
    public DateField getTimeField() {
        return null;
    }

    @Override
    public void search(final ExpressionCriteria criteria,
                       final FieldIndex fieldIndex,
                       final ValuesConsumer consumer) {
        securityContext.secure(PERMISSION, () ->
                indexShardDao.search(criteria, fieldIndex, consumer));
    }

    @Override
    public void setIndexVersion(final LuceneVersion indexVersion) {
        this.indexVersion = indexVersion;
    }
}
