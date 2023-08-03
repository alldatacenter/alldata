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

package org.apache.inlong.sort.cdc.mongodb.source.reader;

import org.apache.inlong.sort.base.enums.ReadPhase;
import org.apache.inlong.sort.cdc.base.debezium.DebeziumDeserializationSchema;
import org.apache.inlong.sort.cdc.base.source.meta.offset.Offset;
import org.apache.inlong.sort.cdc.base.source.meta.offset.OffsetFactory;
import org.apache.inlong.sort.cdc.base.source.meta.split.SourceSplitState;
import org.apache.inlong.sort.cdc.base.source.meta.split.StreamSplitState;
import org.apache.inlong.sort.cdc.base.source.metrics.SourceReaderMetrics;
import org.apache.inlong.sort.cdc.base.source.reader.IncrementalSourceReader;
import org.apache.inlong.sort.cdc.base.source.reader.IncrementalSourceRecordEmitter;
import org.apache.inlong.sort.cdc.mongodb.debezium.utils.RecordUtils;
import org.apache.inlong.sort.cdc.mongodb.source.offset.ChangeStreamOffset;

import com.ververica.cdc.connectors.mongodb.internal.MongoDBEnvelope;
import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.util.Collector;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ververica.cdc.connectors.base.source.meta.wartermark.WatermarkEvent.isHighWatermarkEvent;
import static com.ververica.cdc.connectors.base.source.meta.wartermark.WatermarkEvent.isWatermarkEvent;
import static com.ververica.cdc.connectors.mongodb.source.utils.MongoRecordUtils.getFetchTimestamp;
import static com.ververica.cdc.connectors.mongodb.source.utils.MongoRecordUtils.getMessageTimestamp;
import static com.ververica.cdc.connectors.mongodb.source.utils.MongoRecordUtils.getResumeToken;
import static com.ververica.cdc.connectors.mongodb.source.utils.MongoRecordUtils.isDataChangeRecord;
import static com.ververica.cdc.connectors.mongodb.source.utils.MongoRecordUtils.isHeartbeatEvent;

/**
 * The {@link RecordEmitter} implementation for {@link IncrementalSourceReader}.
 *
 * <p>The {@link RecordEmitter} buffers the snapshot records of split and call the stream reader to
 * emit records rather than emit the records directly.
 * Copy from com.ververica:flink-connector-mongodb-cdc:2.3.0.
 */
public final class MongoDBRecordEmitter<T> extends IncrementalSourceRecordEmitter<T> {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBRecordEmitter.class);

    public MongoDBRecordEmitter(
            DebeziumDeserializationSchema<T> deserializationSchema,
            SourceReaderMetrics sourceReaderMetrics,
            OffsetFactory offsetFactory) {
        super(deserializationSchema, sourceReaderMetrics, false, offsetFactory);
    }

    @Override
    protected void processElement(
            SourceRecord element, SourceOutput<T> output, SourceSplitState splitState)
            throws Exception {
        if (isWatermarkEvent(element)) {
            Offset watermark = getOffsetPosition(element);
            if (isHighWatermarkEvent(element) && splitState.isSnapshotSplitState()) {
                splitState.asSnapshotSplitState().setHighWatermark(watermark);
            }
        } else if (isHeartbeatEvent(element)) {
            if (splitState.isStreamSplitState()) {
                updatePositionForStreamSplit(element, splitState);
            }
        } else if (isDataChangeRecord(element)) {
            if (splitState.isStreamSplitState()) {
                updatePositionForStreamSplit(element, splitState);
            }
            reportMetrics(element);
            debeziumDeserializationSchema.deserialize(
                    element,
                    new Collector<T>() {

                        @Override
                        public void collect(final T t) {
                            Struct value = (Struct) element.value();
                            Struct source = value.getStruct(MongoDBEnvelope.NAMESPACE_FIELD);
                            if (null == source) {
                                source = value.getStruct(RecordUtils.DOCUMENT_TO_FIELD);
                            }
                            String dbName = source.getString(MongoDBEnvelope.NAMESPACE_DATABASE_FIELD);
                            String collectionName =
                                    source.getString(MongoDBEnvelope.NAMESPACE_COLLECTION_FIELD);
                            sourceReaderMetrics
                                    .outputMetrics(dbName, collectionName, splitState.isSnapshotSplitState(), value);
                            output.collect(t);
                        }

                        @Override
                        public void close() {
                            // do nothing
                        }
                    });
        } else {
            // unknown element
            LOG.info("Meet unknown element {}, just skip.", element);
        }
    }

    private void updatePositionForStreamSplit(SourceRecord element, SourceSplitState splitState) {
        BsonDocument resumeToken = getResumeToken(element);
        StreamSplitState streamSplitState = splitState.asStreamSplitState();
        ChangeStreamOffset offset = (ChangeStreamOffset) streamSplitState.getStartingOffset();
        if (offset != null) {
            offset.updatePosition(resumeToken);
        }
        splitState.asStreamSplitState().setStartingOffset(offset);
        // record the time metric to enter the incremental phase
        if (sourceReaderMetrics != null) {
            sourceReaderMetrics.outputReadPhaseMetrics(ReadPhase.INCREASE_PHASE);
        }
    }

    @Override
    protected void reportMetrics(SourceRecord element) {
        long now = System.currentTimeMillis();
        // record the latest process time
        sourceReaderMetrics.recordProcessTime(now);
        Long messageTimestamp = getMessageTimestamp(element);

        if (messageTimestamp != null && messageTimestamp > 0L) {
            // report fetch delay
            Long fetchTimestamp = getFetchTimestamp(element);
            if (fetchTimestamp != null && fetchTimestamp >= messageTimestamp) {
                sourceReaderMetrics.recordFetchDelay(fetchTimestamp - messageTimestamp);
            }
            // report emit delay
            sourceReaderMetrics.recordEmitDelay(now - messageTimestamp);
        }
    }
}
