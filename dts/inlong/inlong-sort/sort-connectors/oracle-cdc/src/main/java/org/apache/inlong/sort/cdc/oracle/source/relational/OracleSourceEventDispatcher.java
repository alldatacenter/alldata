/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.cdc.oracle.source.relational;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.connector.base.ChangeEventQueue;
import io.debezium.document.DocumentWriter;
import io.debezium.pipeline.DataChangeEvent;
import io.debezium.pipeline.EventDispatcher;
import io.debezium.pipeline.source.spi.EventMetadataProvider;
import io.debezium.pipeline.spi.ChangeEventCreator;
import io.debezium.pipeline.spi.SchemaChangeEventEmitter;
import io.debezium.relational.TableId;
import io.debezium.relational.history.HistoryRecord;
import io.debezium.schema.DataCollectionFilters;
import io.debezium.schema.DatabaseSchema;
import io.debezium.schema.TopicSelector;
import io.debezium.util.SchemaNameAdjuster;
import java.util.Collection;
import org.apache.inlong.sort.cdc.base.relational.JdbcSourceEventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A subclass implementation of {@link EventDispatcher}.
 *
 * <pre>
 *  1. This class shares one {@link ChangeEventQueue} between multiple readers.
 *  2. This class override some methods for dispatching {@link HistoryRecord} directly,
 *     this is useful for downstream to deserialize the {@link HistoryRecord} back.
 * </pre>
 * This class extends JdbcSourceEventDispatcher in cdc-base,
 * because there is a conflict between the io-debezium-core:1.5.4-final depended on in cdc-base
 * and the io-debezium-core:1.6.4-final relied on in this module.
 */
public class OracleSourceEventDispatcher extends JdbcSourceEventDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(OracleSourceEventDispatcher.class);

    public static final String HISTORY_RECORD_FIELD = "historyRecord";

    private static final DocumentWriter DOCUMENT_WRITER = DocumentWriter.defaultWriter();

    public OracleSourceEventDispatcher(
            CommonConnectorConfig connectorConfig,
            TopicSelector<TableId> topicSelector,
            DatabaseSchema<TableId> schema,
            ChangeEventQueue<DataChangeEvent> queue,
            DataCollectionFilters.DataCollectionFilter<TableId> filter,
            ChangeEventCreator changeEventCreator,
            EventMetadataProvider metadataProvider,
            SchemaNameAdjuster schemaNameAdjuster) {
        super(
                connectorConfig,
                topicSelector,
                schema,
                queue,
                filter,
                changeEventCreator,
                metadataProvider,
                schemaNameAdjuster);
    }

    @Override
    public void dispatchSchemaChangeEvent(
            TableId dataCollectionId, SchemaChangeEventEmitter schemaChangeEventEmitter)
            throws InterruptedException {
        if (dataCollectionId != null && !filter.isIncluded(dataCollectionId)) {
            if (historizedSchema == null || historizedSchema.storeOnlyCapturedTables()) {
                LOG.trace("Filtering schema change event for {}", dataCollectionId);
                return;
            }
        }
        schemaChangeEventEmitter.emitSchemaChangeEvent(new SchemaChangeEventReceiver());
    }

    @Override
    public void dispatchSchemaChangeEvent(
            Collection<TableId> dataCollectionIds,
            SchemaChangeEventEmitter schemaChangeEventEmitter)
            throws InterruptedException {
        boolean anyNonfilteredEvent = false;
        if (dataCollectionIds == null || dataCollectionIds.isEmpty()) {
            anyNonfilteredEvent = true;
        } else {
            for (TableId dataCollectionId : dataCollectionIds) {
                if (filter.isIncluded(dataCollectionId)) {
                    anyNonfilteredEvent = true;
                    break;
                }
            }
        }
        if (!anyNonfilteredEvent) {
            if (historizedSchema == null || historizedSchema.storeOnlyCapturedTables()) {
                LOG.trace("Filtering schema change event for {}", dataCollectionIds);
                return;
            }
        }

        schemaChangeEventEmitter.emitSchemaChangeEvent(new SchemaChangeEventReceiver());
    }

}
