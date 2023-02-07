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

package org.apache.inlong.sort.kafka.partitioner;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.base.format.AbstractDynamicSchemaFormat;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The Raw Data Hash Partitioner is used to extract partition from raw data(bytes array):
 * It needs a partitionPattern used to parse the pattern and a format {@link RawDataHashPartitioner#sinkMultipleFormat}
 * to deserialization the raw data(bytes array)
 * This partitioner will extract primary key from raw data as the partition key used hash if the 'partitionPattern'
 * equals 'PRIMARY_KEY' else it will parse partition key from raw data.
 *
 * @param <T>
 */
public class RawDataHashPartitioner<T> extends FlinkKafkaPartitioner<T> {

    /**
     * The primary key constant, this partitioner will extract primary key from raw data if the 'partitionPattern'
     * equals 'PRIMARY_KEY'
     */
    public static final String PRIMARY_KEY = "PRIMARY_KEY";
    private static final Logger LOG = LoggerFactory.getLogger(RawDataHashPartitioner.class);
    private static final long serialVersionUID = 1L;
    /**
     * The partition pattern used to extract partition
     */
    private String partitionPattern;

    /**
     * The format used to deserialization the raw data(bytes array)
     */
    private String sinkMultipleFormat;

    @SuppressWarnings({"rawtypes"})
    private AbstractDynamicSchemaFormat dynamicSchemaFormat;

    @Override
    public void open(int parallelInstanceId, int parallelInstances) {
        super.open(parallelInstanceId, parallelInstances);
        dynamicSchemaFormat = DynamicSchemaFormatFactory.getFormat(sinkMultipleFormat);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public int partition(T record, byte[] key, byte[] value, String targetTopic, int[] partitions) {
        Preconditions.checkArgument(
                partitions != null && partitions.length > 0,
                "Partitions of the target topic is empty.");
        int partition = 0;
        try {
            String partitionKey;
            if (PRIMARY_KEY.equals(partitionPattern)) {
                List<String> values = dynamicSchemaFormat.extractPrimaryKeyValues(value);
                if (values == null || values.isEmpty()) {
                    return partition;
                }
                partitionKey = StringUtils.join(values, "");
            } else {
                partitionKey = dynamicSchemaFormat.parse(value, partitionPattern);
            }
            partition = partitions[(partitionKey.hashCode() & Integer.MAX_VALUE) % partitions.length];
        } catch (Exception e) {
            LOG.warn("Extract partition failed", e);
        }
        return partition;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RawDataHashPartitioner;
    }

    @Override
    public int hashCode() {
        return RawDataHashPartitioner.class.hashCode();
    }

    public String getPartitionPattern() {
        return partitionPattern;
    }

    public void setPartitionPattern(String partitionPattern) {
        this.partitionPattern = partitionPattern;
    }

    public String getSinkMultipleFormat() {
        return sinkMultipleFormat;
    }

    public void setSinkMultipleFormat(String sinkMultipleFormat) {
        this.sinkMultipleFormat = sinkMultipleFormat;
    }
}
