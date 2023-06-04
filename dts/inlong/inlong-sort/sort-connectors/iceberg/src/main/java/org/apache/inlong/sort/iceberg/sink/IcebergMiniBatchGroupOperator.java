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

package org.apache.inlong.sort.iceberg.sink;

import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.operators.TableStreamOperator;
import org.apache.flink.table.runtime.util.StreamRecordCollector;
import org.apache.inlong.sort.iceberg.sink.collections.PartitionGroupBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Operator has two functional above:
 * 1. Aggregating calculation in advance, reduce downstream computational workload
 * 2. Clustering data according to partition, reduce memory pressure caused by opening multiple writers downstream
 */
public class IcebergMiniBatchGroupOperator extends TableStreamOperator<RowData>
        implements
            OneInputStreamOperator<RowData, RowData>,
            BoundedOneInput {

    private static final long serialVersionUID = 9042068324817807379L;

    private static final Logger LOG = LoggerFactory.getLogger(IcebergMiniBatchGroupOperator.class);

    private transient StreamRecordCollector<RowData> collector;
    private PartitionGroupBuffer partitionGroupBuffer;

    public IcebergMiniBatchGroupOperator(PartitionGroupBuffer partitionGroupBuffer) {
        this.partitionGroupBuffer = partitionGroupBuffer;
    }

    @Override
    public void open() throws Exception {
        super.open();
        LOG.info("Opening IcebergMiniBatchGroupOperator");
        this.collector = new StreamRecordCollector<>(output);
    }

    @Override
    public void processElement(StreamRecord<RowData> element) throws Exception {
        partitionGroupBuffer.add(element.getValue());
    }

    @Override
    public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
        super.prepareSnapshotPreBarrier(checkpointId);
        flush();
    }

    @Override
    public void close() throws Exception {
        super.close();
        partitionGroupBuffer.close();
    }

    @Override
    public void endInput() throws Exception {
        flush();
    }

    private void flush() throws Exception {
        LOG.info("Flushing IcebergMiniBatchGroupOperator.");
        // Emit the rows group by partition
        // scan range key, this range key contains all one partition data
        partitionGroupBuffer.scanPartitions(tuple -> collector.collect(tuple.f1));
        partitionGroupBuffer.clear();
    }
}
