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

package org.apache.inlong.sort.kudu.sink;

import org.apache.inlong.sort.kudu.common.KuduTableInfo;
import org.apache.inlong.sort.kudu.source.KuduConsumerTask;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.guava18.com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.kudu.Schema;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.util.Preconditions.checkState;
import static org.apache.flink.util.TimeUtils.parseDuration;
import static org.apache.inlong.sort.kudu.common.KuduOptions.CACHE_QUEUE_MAX_LENGTH;
import static org.apache.inlong.sort.kudu.common.KuduOptions.MAX_BUFFER_TIME;
import static org.apache.inlong.sort.kudu.common.KuduOptions.SINK_FORCE_WITH_UPSERT_MODE;
import static org.apache.inlong.sort.kudu.common.KuduOptions.WRITE_THREAD_COUNT;
import static org.apache.inlong.sort.kudu.common.KuduUtils.checkSchema;

/**
 * The Flink kudu Producer in async Mode.
 */
@PublicEvolving
public class KuduAsyncSinkFunction
        extends
            AbstractKuduSinkFunction {

    private static final Logger LOG = LoggerFactory.getLogger(KuduAsyncSinkFunction.class);
    private final Duration maxBufferTime;

    private transient BlockingQueue<RowData> queue;

    private transient List<KuduConsumerTask> consumerTasks;
    private ExecutorService threadPool = null;

    public KuduAsyncSinkFunction(
            KuduTableInfo kuduTableInfo,
            Configuration configuration,
            String inlongMetric,
            String auditHostAndPorts) {
        super(kuduTableInfo, configuration, inlongMetric, auditHostAndPorts);
        this.maxBufferTime = parseDuration(configuration.getString(MAX_BUFFER_TIME));
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        int threadCnt = configuration.getInteger(WRITE_THREAD_COUNT);
        int cacheQueueMaxLength = configuration.getInteger(CACHE_QUEUE_MAX_LENGTH);
        if (cacheQueueMaxLength == -1) {
            cacheQueueMaxLength = (maxBufferSize + 1) * (threadCnt + 1);
        }
        LOG.info("Opening KuduAsyncSinkFunction, threadCount:{}, cacheQueueMaxLength:{}, maxBufferSize:{}.",
                threadCnt, cacheQueueMaxLength, maxBufferSize);

        queue = new LinkedBlockingQueue<>(cacheQueueMaxLength);
        consumerTasks = new ArrayList<>(threadCnt);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("kudu-sink-pool-%d").build();
        threadPool = new ThreadPoolExecutor(
                threadCnt + 1,
                threadCnt + 1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(4),
                namedThreadFactory,
                new ThreadPoolExecutor.AbortPolicy());

        KuduClient client = buildKuduClient();
        KuduTable kuduTable;
        try {
            String tableName = kuduTableInfo.getTableName();
            kuduTable = client.openTable(tableName);
            checkState(client.tableExists(tableName), "Can not find table with name:{} in kudu.", tableName);
        } catch (KuduException e) {
            LOG.error("Error on Open kudu table", e);
            throw new RuntimeException(e);
        }

        // check table schema
        Schema schema = kuduTable.getSchema();
        try {
            checkSchema(kuduTableInfo.getFieldNames(), kuduTableInfo.getDataTypes(), schema);
        } catch (Exception e) {
            LOG.error("The provided schema is invalid!", e);
            throw new RuntimeException(e);
        }
        boolean forceInUpsertMode = configuration.getBoolean(SINK_FORCE_WITH_UPSERT_MODE);
        for (int threadIndex = 0; threadIndex < threadCnt; threadIndex++) {
            KuduWriter kuduWriter = new KuduWriter(client, kuduTable, kuduTableInfo);
            KuduConsumerTask task = new KuduConsumerTask(
                    queue, kuduWriter, maxBufferSize, maxBufferTime, maxRetries);
            consumerTasks.add(task);
            threadPool.submit(task);
        }
    }

    @Override
    protected void addBatch(RowData in) throws Exception {
        checkError();
        try {
            if (running) {
                queue.put(in);
            } else {
                throw new FlinkRuntimeException("Canceled by KuduConsumerTask!");
            }
        } catch (InterruptedException e) {
            LOG.error("writeKuduMsgToQueue error", e);
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        for (KuduConsumerTask consumerTask : consumerTasks) {
            consumerTask.close();
        }
        if (null != threadPool) {
            threadPool.shutdown();
        }
    }

    public void flush() throws IOException {
        for (KuduConsumerTask consumerTask : consumerTasks) {
            consumerTask.flush();
        }
    }

    @Override
    protected void checkError() {
        for (KuduConsumerTask consumerTask : consumerTasks) {
            consumerTask.checkError();
        }
    }
}
