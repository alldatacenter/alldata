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

import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.pc.StatusReporter;
import org.apache.atlas.pc.WorkItemBuilder;
import org.apache.atlas.pc.WorkItemManager;
import org.apache.atlas.repository.migration.DataMigrationStatusService;
import org.apache.atlas.repository.store.graph.v2.BulkImporterImpl;
import org.apache.atlas.repository.store.graph.v2.EntityImportStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityCreationManager<AtlasEntityWithExtInfo> extends WorkItemManager {
    private static final Logger LOG = LoggerFactory.getLogger(EntityCreationManager.class);
    private static final String WORKER_PREFIX = "migration-import";
    private static final long STATUS_REPORT_TIMEOUT_DURATION = 1 * 60 * 1000; // 5 min

    private final StatusReporter<String, Long> statusReporter;
    private final AtlasImportResult importResult;
    private final DataMigrationStatusService dataMigrationStatusService;
    private String currentTypeName;
    private float currentPercent;
    private EntityImportStream entityImportStream;

    public EntityCreationManager(WorkItemBuilder builder, int batchSize, int numWorkers, AtlasImportResult importResult, DataMigrationStatusService dataMigrationStatusService) {
        super(builder, WORKER_PREFIX, batchSize, numWorkers, true);
        this.importResult = importResult;
        this.dataMigrationStatusService = dataMigrationStatusService;

        this.statusReporter = new StatusReporter<>(STATUS_REPORT_TIMEOUT_DURATION);
    }

    public long read(EntityImportStream entityStream) {
        long currentIndex = entityStream.getPosition();
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo;
        this.entityImportStream = entityStream;
        this.dataMigrationStatusService.setStatus("IN_PROGRESS");
        while ((entityWithExtInfo = entityStream.getNextEntityWithExtInfo()) != null) {
            AtlasEntity entity = entityWithExtInfo != null ? entityWithExtInfo.getEntity() : null;
            if (entity == null) {
                continue;
            }

            try {
                produce(currentIndex++, entity.getTypeName(), entityWithExtInfo);
            } catch (Throwable e) {
                LOG.warn("Exception: {}", entity.getGuid(), e);
                break;
            }
        }

        this.dataMigrationStatusService.setStatus("DONE");
        return currentIndex;
    }

    private void produce(long currentIndex, String typeName, AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        String previousTypeName = getCurrentTypeName();

        if (StringUtils.isNotEmpty(typeName)
                && StringUtils.isNotEmpty(previousTypeName)
                && !StringUtils.equals(previousTypeName, typeName)) {
            LOG.info("Waiting: '{}' to complete...", previousTypeName);
            super.drain();
            LOG.info("Switching entity type processing: From: '{}' To: '{}'...", previousTypeName, typeName);
        }

        setCurrentTypeName(typeName);
        statusReporter.produced(entityWithExtInfo.getEntity().getGuid(), currentIndex);
        super.checkProduce(entityWithExtInfo);
        extractResults();
    }

    public void extractResults() {
        Object result;
        while (((result = getResults().poll())) != null) {
            statusReporter.processed((String) result);
        }

        logStatus();
    }

    private void logStatus() {
        Long ack = statusReporter.ack();
        if (ack == null) {
            return;
        }

        importResult.incrementMeticsCounter(getCurrentTypeName());
        dataMigrationStatusService.savePosition(ack);
        this.currentPercent = updateImportMetrics(getCurrentTypeName(), ack, this.entityImportStream.size(), getCurrentPercent());
    }

    private static float updateImportMetrics(String typeNameGuid, long currentIndex, int streamSize, float currentPercent) {
        String lastEntityImported = String.format("entity:last-imported:%s:(%s)", typeNameGuid, currentIndex);
        return BulkImporterImpl.updateImportProgress(LOG, (int) currentIndex, streamSize, currentPercent, lastEntityImported);
    }

    private String getCurrentTypeName() {
        return this.currentTypeName;
    }

    private void setCurrentTypeName(String typeName) {
        this.currentTypeName = typeName;
    }

    private float getCurrentPercent() {
        return this.currentPercent;
    }
}
