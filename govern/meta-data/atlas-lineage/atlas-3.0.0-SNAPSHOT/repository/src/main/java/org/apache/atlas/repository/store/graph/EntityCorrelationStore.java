/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.store.graph;

import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EntityCorrelationStore {
    private static final Logger LOG = LoggerFactory.getLogger(EntityCorrelationStore.class);

    public EntityCorrelationStore() {
    }

    @GraphTransaction
    public void add(String entityGuid, long messageTimestamp) {
        AtlasVertex v = AtlasGraphUtilsV2.findByGuid(entityGuid);
        if (v == null) {
            LOG.warn("Fetching: {} did not yield result!", entityGuid);
            return;
        }

        AtlasGraphUtilsV2.setEncodedProperty(v, Constants.ENTITY_DELETED_TIMESTAMP_PROPERTY_KEY, messageTimestamp);
        LOG.info("Updating: {}: {}", entityGuid, messageTimestamp);
    }

    public String findCorrelatedGuid(String qualifiedName, long messageTimestamp) {
        String guid = AtlasGraphUtilsV2.findFirstDeletedDuringSpooledByQualifiedName(qualifiedName, messageTimestamp);

        LOG.info("findCorrelatedGuid: {} - {} -> {}", qualifiedName, messageTimestamp, guid);
        return guid;
    }
}
