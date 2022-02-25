/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.store.graph.v2.bulkimport;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.repository.converters.AtlasFormatConverters;
import org.apache.atlas.repository.converters.AtlasInstanceConverter;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.migration.DataMigrationStatusService;
import org.apache.atlas.repository.store.graph.AtlasRelationshipStore;
import org.apache.atlas.repository.store.graph.v1.DeleteHandlerDelegate;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.store.graph.v2.AtlasRelationshipStoreV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphMapper;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.repository.store.graph.v2.EntityImportStream;
import org.apache.atlas.repository.store.graph.v2.IAtlasEntityChangeNotifier;
import org.apache.atlas.repository.store.graph.v2.bulkimport.pc.EntityConsumerBuilder;
import org.apache.atlas.repository.store.graph.v2.bulkimport.pc.EntityCreationManager;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationImport extends ImportStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationImport.class);

    private final AtlasGraph graph;
    private final AtlasGraphProvider graphProvider;
    private final AtlasTypeRegistry typeRegistry;

    public MigrationImport(AtlasGraph graph, AtlasGraphProvider graphProvider, AtlasTypeRegistry typeRegistry) {
        this.graph = graph;
        this.graphProvider = graphProvider;
        this.typeRegistry = typeRegistry;
        LOG.info("MigrationImport: Using bulkLoading...");
    }

    public EntityMutationResponse run(EntityImportStream entityStream, AtlasImportResult importResult) throws AtlasBaseException {
        if (entityStream == null || !entityStream.hasNext()) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "no entities to create/update.");
        }

        if (importResult.getRequest() == null) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "importResult should contain request");
        }

        DataMigrationStatusService dataMigrationStatusService = createMigrationStatusService(importResult);

        long index = 0;
        int streamSize = entityStream.size();
        EntityMutationResponse ret = new EntityMutationResponse();
        EntityCreationManager creationManager = createEntityCreationManager(importResult, dataMigrationStatusService);

        try {
            LOG.info("Migration Import: Size: {}: Starting...", streamSize);
            index = creationManager.read(entityStream);
            creationManager.drain();
            creationManager.extractResults();
        } catch (Exception ex) {
            LOG.error("Migration Import: Error: Current position: {}", index, ex);
        } finally {
            shutdownEntityCreationManager(creationManager);
        }

        LOG.info("Migration Import: Size: {}: Done!", streamSize);
        return ret;
    }

    private DataMigrationStatusService createMigrationStatusService(AtlasImportResult importResult) {
        DataMigrationStatusService dataMigrationStatusService = new DataMigrationStatusService();
        dataMigrationStatusService.init(importResult.getRequest().getOptions().get(AtlasImportRequest.OPTION_KEY_MIGRATION_FILE_NAME));
        return dataMigrationStatusService;
    }

    private EntityCreationManager createEntityCreationManager(AtlasImportResult importResult,
                                                              DataMigrationStatusService dataMigrationStatusService) {
        AtlasGraph graphBulk = graphProvider.getBulkLoading();

        EntityGraphRetriever entityGraphRetriever = new EntityGraphRetriever(this.graph, typeRegistry);
        EntityGraphRetriever entityGraphRetrieverBulk = new EntityGraphRetriever(graphBulk, typeRegistry);

        AtlasEntityStoreV2 entityStore = createEntityStore(this.graph, typeRegistry);
        AtlasEntityStoreV2 entityStoreBulk = createEntityStore(graphBulk, typeRegistry);

        int batchSize = importResult.getRequest().getOptionKeyBatchSize();
        int numWorkers = getNumWorkers(importResult.getRequest().getOptionKeyNumWorkers());

        EntityConsumerBuilder consumerBuilder =
                new EntityConsumerBuilder(typeRegistry, this.graph, entityStore, entityGraphRetriever, graphBulk,
                        entityStoreBulk, entityGraphRetrieverBulk, batchSize);

        LOG.info("MigrationImport: EntityCreationManager: Created!");
        return new EntityCreationManager(consumerBuilder, batchSize, numWorkers, importResult, dataMigrationStatusService);
    }

    private static int getNumWorkers(int numWorkersFromOptions) {
        int ret = (numWorkersFromOptions > 0) ? numWorkersFromOptions : 1;
        LOG.info("Migration Import: Setting numWorkers: {}", ret);
        return ret;
    }

    private AtlasEntityStoreV2 createEntityStore(AtlasGraph graph, AtlasTypeRegistry typeRegistry) {
        FullTextMapperV2Nop fullTextMapperV2 = new FullTextMapperV2Nop();
        IAtlasEntityChangeNotifier entityChangeNotifier = new EntityChangeNotifierNop();
        DeleteHandlerDelegate deleteDelegate = new DeleteHandlerDelegate(graph, typeRegistry, null);
        AtlasFormatConverters formatConverters = new AtlasFormatConverters(typeRegistry);

        AtlasInstanceConverter instanceConverter = new AtlasInstanceConverter(graph, typeRegistry, formatConverters);
        AtlasRelationshipStore relationshipStore = new AtlasRelationshipStoreV2(graph, typeRegistry, deleteDelegate, entityChangeNotifier);
        EntityGraphMapper entityGraphMapper = new EntityGraphMapper(deleteDelegate, typeRegistry, graph, relationshipStore, entityChangeNotifier, instanceConverter, fullTextMapperV2, null);

        return new AtlasEntityStoreV2(graph, deleteDelegate, typeRegistry, entityChangeNotifier, entityGraphMapper);
    }

    private void shutdownEntityCreationManager(EntityCreationManager creationManager) {
        try {
            creationManager.shutdown();
        } catch (InterruptedException e) {
            LOG.error("Migration Import: Shutdown: Interrupted!", e);
        }
    }
}
