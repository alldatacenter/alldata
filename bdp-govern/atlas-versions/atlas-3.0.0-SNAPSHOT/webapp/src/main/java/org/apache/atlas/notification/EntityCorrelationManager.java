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
package org.apache.atlas.notification;

import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.repository.store.graph.EntityCorrelationStore;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EntityCorrelationManager {
    private static final Logger LOG = LoggerFactory.getLogger(EntityCorrelationManager.class);

    private final EntityCorrelationStore entityCorrelationStore;

    public EntityCorrelationManager(EntityCorrelationStore entityCorrelationStore) {
        this.entityCorrelationStore = entityCorrelationStore;
    }

    public void add(boolean spooled, long spooledTimestamp, List<AtlasEntityHeader> entityHeaders) {
        if (this.entityCorrelationStore == null || spooled == false || CollectionUtils.isEmpty(entityHeaders)) {
            return;
        }

        for (AtlasEntityHeader entityHeader : entityHeaders) {
            String guid = entityHeader.getGuid();
            if (StringUtils.isNotEmpty(guid)) {
                entityCorrelationStore.add(guid, spooledTimestamp);
            }
        }
    }

    public String getGuidForDeletedEntityToBeCorrelated(String qualifiedName, long spooledMessageTimestamp) {
        if (this.entityCorrelationStore == null || spooledMessageTimestamp <= 0) {
            return null;
        }

        String guid = entityCorrelationStore.findCorrelatedGuid(qualifiedName, spooledMessageTimestamp);
        LOG.info("{}: spooledTimestamp: {} -> {}", qualifiedName, spooledMessageTimestamp, guid);
        return guid;
    }
}
