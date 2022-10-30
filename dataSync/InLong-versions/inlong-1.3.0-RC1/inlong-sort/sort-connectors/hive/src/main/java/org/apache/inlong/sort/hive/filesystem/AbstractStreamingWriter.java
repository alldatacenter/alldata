/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.inlong.sort.hive.filesystem;

import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.StateSnapshotContext;
import org.apache.flink.streaming.api.functions.sink.filesystem.Bucket;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketLifeCycleListener;
import org.apache.flink.streaming.api.functions.sink.filesystem.Buckets;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSinkHelper;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.metric.ThreadSafeCounter;
import javax.annotation.Nullable;

import static org.apache.inlong.sort.base.Constants.DELIMITER;

/**
 * Operator for file system sink. It is a operator version of {@link StreamingFileSink}. It can send
 * file and bucket information to downstream.
 */
public abstract class AbstractStreamingWriter<IN, OUT> extends AbstractStreamOperator<OUT>
        implements OneInputStreamOperator<IN, OUT>, BoundedOneInput {

    private static final long serialVersionUID = 1L;

    // ------------------------ configuration fields --------------------------

    private final long bucketCheckInterval;

    private final StreamingFileSink.BucketsBuilder<
            IN, String, ? extends StreamingFileSink.BucketsBuilder<IN, String, ?>>
            bucketsBuilder;

    @Nullable
    private String inLongMetric;

    @Nullable
    private String auditHostAndPorts;

    // --------------------------- runtime fields -----------------------------

    private transient Buckets<IN, String> buckets;

    private transient StreamingFileSinkHelper<IN> helper;

    private transient long currentWatermark;

    @Nullable
    private transient SinkMetricData metricData;

    public AbstractStreamingWriter(
            long bucketCheckInterval,
            StreamingFileSink.BucketsBuilder<
                    IN, String, ? extends StreamingFileSink.BucketsBuilder<IN, String, ?>>
                    bucketsBuilder,
            String inLongMetric,
            String auditHostAndPorts) {
        this.bucketCheckInterval = bucketCheckInterval;
        this.bucketsBuilder = bucketsBuilder;
        this.inLongMetric = inLongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
        setChainingStrategy(ChainingStrategy.ALWAYS);
    }

    /** Notifies a partition created. */
    protected abstract void partitionCreated(String partition);

    /**
     * Notifies a partition become inactive. A partition becomes inactive after all the records
     * received so far have been committed.
     */
    protected abstract void partitionInactive(String partition);

    /**
     * Notifies a new file has been opened.
     *
     * <p>Note that this does not mean that the file has been created in the file system. It is only
     * created logically and the actual file will be generated after it is committed.
     */
    protected abstract void onPartFileOpened(String partition, Path newPath);

    /** Commit up to this checkpoint id. */
    protected void commitUpToCheckpoint(long checkpointId) throws Exception {
        helper.commitUpToCheckpoint(checkpointId);
    }

    @Override
    public void open() throws Exception {
        super.open();
        if (inLongMetric != null) {
            String[] inLongMetricArray = inLongMetric.split(DELIMITER);
            String inLongGroupId = inLongMetricArray[0];
            String inLongStreamId = inLongMetricArray[1];
            String nodeId = inLongMetricArray[2];
            metricData = new SinkMetricData(
                    inLongGroupId, inLongStreamId, nodeId, getRuntimeContext().getMetricGroup(), auditHostAndPorts);
            metricData.registerMetricsForDirtyBytes(new ThreadSafeCounter());
            metricData.registerMetricsForDirtyRecords(new ThreadSafeCounter());
            metricData.registerMetricsForNumBytesOut(new ThreadSafeCounter());
            metricData.registerMetricsForNumRecordsOut(new ThreadSafeCounter());
            metricData.registerMetricsForNumBytesOutPerSecond();
            metricData.registerMetricsForNumRecordsOutPerSecond();
        }
    }

    @Override
    public void initializeState(StateInitializationContext context) throws Exception {
        super.initializeState(context);
        buckets = bucketsBuilder.createBuckets(getRuntimeContext().getIndexOfThisSubtask());

        // Set listener before the initialization of Buckets.
        buckets.setBucketLifeCycleListener(
                new BucketLifeCycleListener<IN, String>() {

                    @Override
                    public void bucketCreated(Bucket<IN, String> bucket) {
                        partitionCreated(bucket.getBucketId());
                    }

                    @Override
                    public void bucketInactive(Bucket<IN, String> bucket) {
                        partitionInactive(bucket.getBucketId());
                    }
                });

        buckets.setFileLifeCycleListener(this::onPartFileOpened);

        helper =
                new StreamingFileSinkHelper<>(
                        buckets,
                        context.isRestored(),
                        context.getOperatorStateStore(),
                        getRuntimeContext().getProcessingTimeService(),
                        bucketCheckInterval);

        currentWatermark = Long.MIN_VALUE;
    }

    @Override
    public void snapshotState(StateSnapshotContext context) throws Exception {
        super.snapshotState(context);
        helper.snapshotState(context.getCheckpointId());
    }

    @Override
    public void processWatermark(Watermark mark) throws Exception {
        super.processWatermark(mark);
        currentWatermark = mark.getTimestamp();
    }

    @Override
    public void processElement(StreamRecord<IN> element) throws Exception {
        helper.onElement(
                element.getValue(),
                getProcessingTimeService().getCurrentProcessingTime(),
                element.hasTimestamp() ? element.getTimestamp() : null,
                currentWatermark);
        if (metricData != null) {
            metricData.invokeWithEstimate(element.getValue());
        }
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {
        super.notifyCheckpointComplete(checkpointId);
        commitUpToCheckpoint(checkpointId);
    }

    @Override
    public void endInput() throws Exception {
        buckets.onProcessingTime(Long.MAX_VALUE);
        helper.snapshotState(Long.MAX_VALUE);
        output.emitWatermark(new Watermark(Long.MAX_VALUE));
        commitUpToCheckpoint(Long.MAX_VALUE);
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
        if (helper != null) {
            helper.close();
        }
    }
}

