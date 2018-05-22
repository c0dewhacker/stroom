/*
 * Copyright 2018 Crown Copyright
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

package stroom.refdata.offheapstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.query.api.v2.DocRef;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import java.util.Objects;

public class MapDefinition {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapDefinition.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(MapDefinition.class);

    private final RefStreamDefinition refStreamDefinition;
    private final String mapName;

    public MapDefinition(final RefStreamDefinition refStreamDefinition, final String mapName) {
        this.refStreamDefinition = refStreamDefinition;
        this.mapName = mapName;
    }

    DocRef getPipelineDocRef() {
        return refStreamDefinition.getPipelineDocRef();
    }

    byte getPipelineVersion() {
        return refStreamDefinition.getPipelineVersion();
    }

    long getStreamId() {
        return refStreamDefinition.getStreamId();
    }

    String getMapName() {
        return mapName;
    }

    RefStreamDefinition getRefStreamDefinition() {
        return refStreamDefinition;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MapDefinition that = (MapDefinition) o;
        return Objects.equals(refStreamDefinition, that.refStreamDefinition) &&
                Objects.equals(mapName, that.mapName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(refStreamDefinition, mapName);
    }

    @Override
    public String toString() {
        return "MapDefinition{" +
                "refStreamDefinition=" + refStreamDefinition +
                ", mapName='" + mapName + '\'' +
                '}';
    }
}
