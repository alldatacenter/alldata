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

package org.apache.inlong.sort.kudu.source;

import org.apache.flink.table.data.RowData;
import org.apache.inlong.sort.kudu.sink.KuduWriter;
import org.apache.kudu.client.KuduException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The runnable task for sink kudu.
 */
public class KuduConsumerTask implements Runnable {

    private final Duration maxBufferTime;
    private static final Logger LOG = LoggerFactory.getLogger(KuduConsumerTask.class);

    private final KuduWriter kuduWriter;
    private final BlockingQueue<RowData> queue;
    private final long writeBatch;
    private long cachedNum = 0;
    private boolean running;
    /**
     * The maximum number of retries when an exception is caught.
     */
    private final int maxRetries;

    public KuduConsumerTask(
            BlockingQueue<RowData> queue,
            KuduWriter kuduWriter,
            long writeBatch,
            Duration maxBufferTime,
            int maxRetries) {
        this.queue = queue;
        this.kuduWriter = kuduWriter;
        this.writeBatch = writeBatch;
        this.maxBufferTime = maxBufferTime;
        this.running = true;
        this.maxRetries = maxRetries;

        LOG.info("Creating KuduConsumerTask, writeBatch:{}, maxBufferTime:{}, maxRetries:{}.",
                writeBatch, maxBufferTime, maxRetries);
    }

    @Override
    public void run() {
        LOG.info("KuduConsumerTask starting: {}.", Thread.currentThread());
        try {
            while (running) {
                // Data flush conditions.
                while (running && cachedNum < writeBatch) {
                    RowData row = null;
                    try {
                        row = queue.poll(maxBufferTime.getSeconds(), TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        LOG.error("Poll message error, force flush", e);
                        flush();
                    }
                    if (row != null) {
                        applyRow(row);
                    }
                }
                flush();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        }
        LOG.info("KuduConsumerTask stopped: {}.", Thread.currentThread());
    }

    private void applyRow(RowData row) {
        kuduWriter.applyRow(row, maxRetries);
        cachedNum++;
    }

    public void flush() {
        kuduWriter.flush(maxRetries);
        cachedNum = 0;
    }

    public void close() {
        LOG.info("Closing... KuduConsumerTask");
        flush();
        running = false;
        try {
            kuduWriter.close();
        } catch (KuduException e) {
            LOG.error("Error when close kuduWrite", e);
        }
    }

    public void checkError() {
        kuduWriter.checkError();
    }
}
