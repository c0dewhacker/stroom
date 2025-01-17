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

import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShardKey;
import stroom.index.shared.LuceneVersion;
import stroom.util.shared.ResultPage;

public interface IndexShardService {
    IndexShard loadById(Long id);

    ResultPage<IndexShard> find(FindIndexShardCriteria criteria);

    IndexShard createIndexShard(IndexShardKey indexShardKey, String ownerNodeName);

    Boolean delete(IndexShard indexShard);

    Boolean setStatus(Long id, IndexShard.IndexShardStatus status);

    void update(long indexShardId,
                Integer documentCount,
                Long commitDurationMs,
                Long commitMs,
                Long fileSize);

    void setIndexVersion(LuceneVersion indexVersion);
}
