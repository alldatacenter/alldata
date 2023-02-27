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

import org.apache.atlas.GraphTransactionInterceptor;
import org.apache.atlas.RequestContext;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.pc.WorkItemConsumer;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasSchemaViolationException;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStreamForImport;
import org.apache.atlas.repository.store.graph.v2.BulkImporterImpl;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.repository.store.graph.v2.EntityStream;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.AtlasPerfMetrics;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class EntityConsumer extends WorkItemConsumer<AtlasEntity.AtlasEntityWithExtInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(EntityConsumer.class);
    private static final int MAX_COMMIT_RETRY_COUNT = 3;

    private final int batchSize;
    private AtomicLong counter = new AtomicLong(1);
    private AtomicLong currentBatch = new AtomicLong(1);

    private AtlasGraph atlasGraph;
    private final AtlasEntityStore entityStore;
    private final AtlasGraph atlasGraphBulk;
    private final AtlasEntityStore entityStoreBulk;
    private final AtlasTypeRegistry typeRegistry;
    private final EntityGraphRetriever entityRetrieverBulk;

    private List<AtlasEntity.AtlasEntityWithExtInfo> entityBuffer = new ArrayList<>();
    private List<String> localResults = new ArrayList<>();

    public EntityConsumer(AtlasTypeRegistry typeRegistry,
                          AtlasGraph atlasGraph, AtlasEntityStore entityStore,
                          AtlasGraph atlasGraphBulk, AtlasEntityStore entityStoreBulk, EntityGraphRetriever entityRetrieverBulk,
                          BlockingQueue queue, int batchSize) {
        super(queue);
        this.typeRegistry = typeRegistry;

        this.atlasGraph = atlasGraph;
        this.entityStore = entityStore;

        this.atlasGraphBulk = atlasGraphBulk;
        this.entityStoreBulk = entityStoreBulk;
        this.entityRetrieverBulk = entityRetrieverBulk;

        this.batchSize = batchSize;
    }

    @Override
    protected void processItem(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        int delta = MapUtils.isEmpty(entityWithExtInfo.getReferredEntities())
                ? 1
                : entityWithExtInfo.getReferredEntities().size() + 1;

        long currentCount = counter.addAndGet(delta);
        currentBatch.addAndGet(delta);

        try {
            processEntity(entityWithExtInfo, currentCount);
            attemptCommit();
        } catch (Exception e) {
            LOG.info("Invalid entities. Possible data loss: Please correct and re-submit!", e);
        }
    }

    private void processEntity(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo, long currentCount) {
        RequestContext.get().setImportInProgress(true);
        RequestContext.get().setCreateShellEntityForNonExistingReference(true);

        try {
            LOG.debug("Processing: {}", currentCount);
            importUsingBulkEntityStore(entityWithExtInfo);
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.warn("{}: {} - {}", e.getClass().getSimpleName(), entityWithExtInfo.getEntity().getTypeName(), entityWithExtInfo.getEntity().getGuid(), e);
            importUsingRegularEntityStore(entityWithExtInfo, e);
        } catch (AtlasBaseException e) {
            LOG.warn("AtlasBaseException: {} - {}", entityWithExtInfo.getEntity().getTypeName(), entityWithExtInfo.getEntity().getGuid(), e);
        } catch (AtlasSchemaViolationException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Entity: {}", entityWithExtInfo.getEntity().getGuid(), e);
            }

            BulkImporterImpl.updateVertexGuid(this.atlasGraphBulk, typeRegistry, entityRetrieverBulk, entityWithExtInfo.getEntity());
        }
    }

    private void importUsingBulkEntityStore(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) throws AtlasBaseException {
        EntityStream oneEntityStream = new AtlasEntityStreamForImport(entityWithExtInfo, null);
        EntityMutationResponse result = entityStoreBulk.createOrUpdateForImportNoCommit(oneEntityStream);
        localResults.add(entityWithExtInfo.getEntity().getGuid());
        entityBuffer.add(entityWithExtInfo);
    }

    private void importUsingRegularEntityStore(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo, Exception ex) {
        commitValidatedEntities(ex);
        performRegularImport(entityWithExtInfo);
    }

    private void performRegularImport(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        synchronized (atlasGraph) {
            try {
                LOG.info("Regular: EntityStore: {}: Starting...", this.counter.get());
                AtlasEntityStreamForImport oneEntityStream = new AtlasEntityStreamForImport(entityWithExtInfo, null);
                this.entityStore.createOrUpdateForImportNoCommit(oneEntityStream);
                atlasGraph.commit();
                localResults.add(entityWithExtInfo.getEntity().getGuid());
                dispatchResults();
            } catch (Exception e) {
                atlasGraph.rollback();
                LOG.error("Regular: EntityStore: Rollback!: Entity creation using regular (non-bulk) failed! Please correct entity and re-submit!", e);
            } finally {
                LOG.info("Regular: EntityStore: {}: Commit: Done!", this.counter.get());
                atlasGraph.commit();
                addResult(entityWithExtInfo.getEntity().getGuid());
                clear();
                LOG.info("Regular: EntityStore: {}: Done!", this.counter.get());
            }
        }
    }

    private void commitValidatedEntities(Exception ex) {
        try {
            LOG.info("Validated Entities: Commit: Starting...");
            rollbackPauseRetry(1, ex);
            doCommit();
        }
        finally {
            LOG.info("Validated Entities: Commit: Done!");
        }
    }

    private void attemptCommit() {
        if (currentBatch.get() < batchSize) {
            return;
        }

        doCommit();
    }

    @Override
    protected void doCommit() {
        for (int retryCount = 1; retryCount <= MAX_COMMIT_RETRY_COUNT; retryCount++) {
            if (commitWithRetry(retryCount)) {
                return;
            }
        }

        LOG.error("Retries exceeded! Potential data loss! Please correct data and re-attempt. Buffer: {}: Counter: {}", entityBuffer.size(), counter.get());
        clear();
    }

    @Override
    protected void commitDirty() {
        super.commitDirty();
        LOG.info("Total: Commit: {}", counter.get());
        counter.set(0);
    }

    private boolean commitWithRetry(int retryCount) {
        AtlasPerfMetrics.MetricRecorder metric = RequestContext.get().startMetricRecord("commitWithRetry");

        try {
            atlasGraphBulk.commit();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Commit: Done!: Buffer: {}: Batch: {}: Counter: {}", entityBuffer.size(), currentBatch.get(), counter.get());
            }

            dispatchResults();
            return true;
        } catch (Exception ex) {
            rollbackPauseRetry(retryCount, ex);
            return false;
        } finally {
            RequestContext.get().endMetricRecord(metric);
        }
    }

    private void rollbackPauseRetry(int retryCount, Exception ex) {
        bulkGraphRollback(retryCount);

        LOG.warn("Rollback: Done! Buffer: {}: Counter: {}: Retry count: {}", entityBuffer.size(), counter.get(), retryCount);
        pause(retryCount);
        String exceptionClass = ex.getClass().getSimpleName();
        if (!exceptionClass.equals("JanusGraphException") && !exceptionClass.equals("PermanentLockingException")) {
            LOG.warn("Commit error! Will pause and retry: Buffer: {}: Counter: {}: Retry count: {}", entityBuffer.size(), counter.get(), retryCount, ex);
        }
        retryProcessEntity(retryCount);
    }

    private void bulkGraphRollback(int retryCount) {
        try {
            atlasGraphBulk.rollback();
            clearCache();
        } catch (Exception e) {
            LOG.error("Rollback: Exception! Buffer: {}: Counter: {}: Retry count: {}", entityBuffer.size(), counter.get(), retryCount);
        }
    }

    private void retryProcessEntity(int retryCount) {
        if (LOG.isDebugEnabled() || retryCount > 1) {
            LOG.info("Replaying: Starting!: Buffer: {}: Retry count: {}", entityBuffer.size(), retryCount);
        }

        List<AtlasEntity.AtlasEntityWithExtInfo> localBuffer = new ArrayList<>(entityBuffer);
        entityBuffer.clear();

        for (AtlasEntity.AtlasEntityWithExtInfo e : localBuffer) {
            processEntity(e, counter.get());
        }

        LOG.info("Replaying: Done!: Buffer: {}: Retry count: {}", entityBuffer.size(), retryCount);
    }

    private void dispatchResults() {
        localResults.stream().forEach(x -> addResult(x));
        clear();
    }

    private void pause(int retryCount) {
        try {
            Thread.sleep(1000 * retryCount);
        } catch (InterruptedException e) {
            LOG.error("pause: Interrupted!", e);
        }
    }

    private void clear() {
        localResults.clear();
        entityBuffer.clear();
        clearCache();
        currentBatch.set(0);
    }

    private void clearCache() {
        GraphTransactionInterceptor.clearCache();
        RequestContext.get().clearCache();
    }
}
