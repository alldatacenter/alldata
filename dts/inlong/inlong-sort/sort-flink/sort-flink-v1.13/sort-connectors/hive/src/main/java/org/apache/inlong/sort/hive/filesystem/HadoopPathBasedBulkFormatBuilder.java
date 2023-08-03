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

package org.apache.inlong.sort.hive.filesystem;

import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.sink.PartitionPolicy;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;

import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.hadoop.bulk.HadoopFileCommitterFactory;
import org.apache.flink.formats.hadoop.bulk.HadoopPathBasedBulkWriter;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketFactory;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketWriter;
import org.apache.flink.streaming.api.functions.sink.filesystem.Buckets;
import org.apache.flink.streaming.api.functions.sink.filesystem.DefaultBucketFactoryImpl;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.SerializableConfiguration;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.CheckpointRollingPolicy;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.table.catalog.hive.client.HiveShim;
import org.apache.flink.util.Preconditions;
import org.apache.hadoop.conf.Configuration;

import javax.annotation.Nullable;

import java.io.IOException;

/** Buckets builder to create buckets that use {@link HadoopPathBasedPartFileWriter}. */
public class HadoopPathBasedBulkFormatBuilder<IN, BucketID, T extends HadoopPathBasedBulkFormatBuilder<IN, BucketID, T>>
        extends
            StreamingFileSink.BucketsBuilder<IN, BucketID, T> {

    private static final long serialVersionUID = 1L;

    private final Path basePath;

    private final HadoopPathBasedBulkWriter.Factory<IN> writerFactory;

    private final HadoopFileCommitterFactory fileCommitterFactory;

    private SerializableConfiguration serializableConfiguration;

    private BucketAssigner<IN, BucketID> bucketAssigner;

    private CheckpointRollingPolicy<IN, BucketID> rollingPolicy;

    private BucketFactory<IN, BucketID> bucketFactory;

    private OutputFileConfig outputFileConfig;

    @Nullable
    private transient SinkTableMetricData metricData;

    private final DirtyOptions dirtyOptions;

    private final @Nullable DirtySink<Object> dirtySink;

    private final SchemaUpdateExceptionPolicy schemaUpdatePolicy;
    private final PartitionPolicy partitionPolicy;
    private final HiveShim hiveShim;
    private final String hiveVersion;
    private final String inputFormat;
    private final String outputFormat;
    private final String serializationLib;

    public HadoopPathBasedBulkFormatBuilder(
            org.apache.hadoop.fs.Path basePath,
            HadoopPathBasedBulkWriter.Factory<IN> writerFactory,
            Configuration configuration,
            BucketAssigner<IN, BucketID> assigner,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink,
            SchemaUpdateExceptionPolicy schemaUpdatePolicy,
            PartitionPolicy partitionPolicy,
            HiveShim hiveShim,
            String hiveVersion,
            boolean sinkMultipleEnable,
            String inputFormat,
            String outputFormat,
            String serializationLib) {
        this(
                basePath,
                writerFactory,
                new DefaultHadoopFileCommitterFactory(sinkMultipleEnable),
                configuration,
                assigner,
                OnCheckpointRollingPolicy.build(),
                new DefaultBucketFactoryImpl<>(),
                OutputFileConfig.builder().build(),
                dirtyOptions,
                dirtySink,
                schemaUpdatePolicy,
                partitionPolicy,
                hiveShim,
                hiveVersion,
                inputFormat,
                outputFormat,
                serializationLib);
    }

    public HadoopPathBasedBulkFormatBuilder(
            org.apache.hadoop.fs.Path basePath,
            HadoopPathBasedBulkWriter.Factory<IN> writerFactory,
            HadoopFileCommitterFactory fileCommitterFactory,
            Configuration configuration,
            BucketAssigner<IN, BucketID> assigner,
            CheckpointRollingPolicy<IN, BucketID> policy,
            BucketFactory<IN, BucketID> bucketFactory,
            OutputFileConfig outputFileConfig,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink,
            SchemaUpdateExceptionPolicy schemaUpdatePolicy,
            PartitionPolicy partitionPolicy,
            HiveShim hiveShim,
            String hiveVersion,
            String inputFormat,
            String outputFormat,
            String serializationLib) {

        this.basePath = new Path(Preconditions.checkNotNull(basePath).toString());
        this.writerFactory = writerFactory;
        this.fileCommitterFactory = fileCommitterFactory;
        this.serializableConfiguration = new SerializableConfiguration(configuration);
        this.bucketAssigner = Preconditions.checkNotNull(assigner);
        this.rollingPolicy = Preconditions.checkNotNull(policy);
        this.bucketFactory = Preconditions.checkNotNull(bucketFactory);
        this.outputFileConfig = Preconditions.checkNotNull(outputFileConfig);
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
        this.schemaUpdatePolicy = schemaUpdatePolicy;
        this.partitionPolicy = partitionPolicy;
        this.hiveShim = hiveShim;
        this.hiveVersion = hiveVersion;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.serializationLib = serializationLib;
    }

    public T withBucketAssigner(BucketAssigner<IN, BucketID> assigner) {
        this.bucketAssigner = Preconditions.checkNotNull(assigner);
        return self();
    }

    public T withRollingPolicy(CheckpointRollingPolicy<IN, BucketID> rollingPolicy) {
        this.rollingPolicy = Preconditions.checkNotNull(rollingPolicy);
        return self();
    }

    public T withBucketFactory(BucketFactory<IN, BucketID> factory) {
        this.bucketFactory = Preconditions.checkNotNull(factory);
        return self();
    }

    public T withOutputFileConfig(OutputFileConfig outputFileConfig) {
        this.outputFileConfig = outputFileConfig;
        return self();
    }

    public T withConfiguration(Configuration configuration) {
        this.serializableConfiguration = new SerializableConfiguration(configuration);
        return self();
    }

    @Override
    public BucketWriter<IN, BucketID> createBucketWriter() {
        return new HadoopPathBasedPartFileWriter.HadoopPathBasedBucketWriter<>(
                serializableConfiguration.getConfiguration(),
                writerFactory,
                fileCommitterFactory,
                metricData,
                dirtyOptions,
                dirtySink,
                schemaUpdatePolicy,
                partitionPolicy,
                hiveShim,
                hiveVersion,
                inputFormat,
                outputFormat,
                serializationLib);
    }

    @Override
    public Buckets<IN, BucketID> createBuckets(int subtaskIndex) throws IOException {
        return new Buckets<>(
                basePath,
                bucketAssigner,
                bucketFactory,
                createBucketWriter(),
                rollingPolicy,
                subtaskIndex,
                outputFileConfig);
    }

    public void setMetricData(@Nullable SinkTableMetricData metricData) {
        this.metricData = metricData;
    }

}