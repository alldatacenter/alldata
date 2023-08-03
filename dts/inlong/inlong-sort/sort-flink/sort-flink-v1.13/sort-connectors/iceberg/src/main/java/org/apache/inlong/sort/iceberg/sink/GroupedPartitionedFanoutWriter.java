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

import org.apache.flink.shaded.guava18.com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.OutputFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

public abstract class GroupedPartitionedFanoutWriter<T>
        extends
            org.apache.inlong.sort.iceberg.sink.trick.BaseTaskWriter<T> {

    private static final Logger LOG = LoggerFactory.getLogger(GroupedPartitionedFanoutWriter.class);

    private static final ExecutorService CLOSE_EXECUTOR_SERVICE = new ThreadPoolExecutor(
            10,
            20,
            100L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactoryBuilder().setNameFormat("iceberg-writer-close-thread-%s").build(),
            new CallerRunsPolicy());

    private String latestPartitionPath;

    private RollingFileWriter latestWriter;

    private final List<Future> futures = new ArrayList<>();

    private final List<Exception> failures = new ArrayList<>();

    protected GroupedPartitionedFanoutWriter(PartitionSpec spec, FileFormat format,
            FileAppenderFactory<T> appenderFactory,
            OutputFileFactory fileFactory, FileIO io, long targetFileSize) {
        super(spec, format, appenderFactory, fileFactory, io, targetFileSize);
    }

    /**
     * Create a PartitionKey from the values in row.
     * <p>
     * Any PartitionKey returned by this method can be reused by the implementation.
     *
     * @param row a data row
     */
    protected abstract PartitionKey partition(T row);

    @Override
    public void write(T row) throws IOException {
        checkFailure();
        PartitionKey partitionKey = partition(row);
        if (latestPartitionPath == null || !partitionKey.toPath().equals(latestPartitionPath)) {
            // NOTICE: we need to copy a new partition key here, in case of messing up the keys in writers.
            closeCurrentWriter();
            latestWriter = new RollingFileWriter(partitionKey.copy());
            latestPartitionPath = partitionKey.toPath();
        }

        latestWriter.write(row);
    }

    @Override
    public void close() throws IOException {
        try {
            closeCurrentWriter();
            for (Future future : futures) {
                future.get();
            }
            checkFailure();
            latestWriter = null;
            latestPartitionPath = null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Interrupted while waiting for tasks to finish", e);
            throw new RuntimeException(e);
        }
    }

    private void closeCurrentWriter() {
        if (latestWriter != null) {
            LOG.info("Start eliminated writer for partition {}", latestPartitionPath);
            final RollingFileWriter writer = latestWriter;
            final String partitionPath = latestPartitionPath;
            futures.add(CLOSE_EXECUTOR_SERVICE.submit(() -> {
                try {
                    writer.close();
                    LOG.info("End eliminated writer for partition {}", partitionPath);
                } catch (IOException | RuntimeException e) {
                    failures.add(e);
                }
            }));
        }
    }

    private void checkFailure() {
        if (!failures.isEmpty()) {
            throw new RuntimeException(
                    String.format("Failed to close equality delta writer, %d unclosed files more.", failures.size()),
                    failures.get(0));
        }
    }
}
