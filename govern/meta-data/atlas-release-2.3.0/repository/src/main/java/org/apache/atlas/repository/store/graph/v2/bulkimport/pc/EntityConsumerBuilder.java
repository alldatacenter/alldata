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

package org.apache.atlas.repository.store.graph.v2.bulkimport.pc;

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.pc.WorkItemBuilder;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasTypeRegistry;

import java.util.concurrent.BlockingQueue;

public class EntityConsumerBuilder implements WorkItemBuilder<EntityConsumer, AtlasEntity.AtlasEntityWithExtInfo> {
    private AtlasGraph atlasGraph;
    private AtlasEntityStore entityStore;
    private AtlasGraph atlasGraphBulk;
    private AtlasEntityStore entityStoreBulk;
    private final EntityGraphRetriever entityRetriever;
    private final AtlasTypeRegistry typeRegistry;
    private EntityGraphRetriever entityRetrieverBulk;
    private int batchSize;

    public EntityConsumerBuilder(AtlasTypeRegistry typeRegistry, AtlasGraph atlasGraph, AtlasEntityStoreV2 entityStore, EntityGraphRetriever entityRetriever,
                                 AtlasGraph atlasGraphBulk, AtlasEntityStoreV2 entityStoreBulk, EntityGraphRetriever entityRetrieverBulk,
                                 int batchSize) {
        this.typeRegistry = typeRegistry;

        this.atlasGraph = atlasGraph;
        this.entityStore = entityStore;
        this.entityRetriever = entityRetriever;

        this.atlasGraphBulk = atlasGraphBulk;
        this.entityStoreBulk = entityStoreBulk;
        this.entityRetrieverBulk = entityRetrieverBulk;

        this.batchSize = batchSize;
    }

    @Override
    public EntityConsumer build(BlockingQueue<AtlasEntity.AtlasEntityWithExtInfo> queue) {
        return new EntityConsumer(typeRegistry, atlasGraph, entityStore,
                atlasGraphBulk, entityStoreBulk, entityRetrieverBulk,
                queue, this.batchSize);
    }
}
