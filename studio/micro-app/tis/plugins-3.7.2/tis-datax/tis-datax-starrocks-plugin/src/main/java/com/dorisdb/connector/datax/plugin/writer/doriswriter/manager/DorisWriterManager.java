/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.dorisdb.connector.datax.plugin.writer.doriswriter.manager;

import com.dorisdb.connector.datax.plugin.writer.doriswriter.DorisWriterOptions;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

public class DorisWriterManager {

    private static final Logger LOG = LoggerFactory.getLogger(DorisWriterManager.class);

    private final DorisStreamLoadVisitor dorisStreamLoadVisitor;
    private final DorisWriterOptions writerOptions;

    private WriterBuffer buffer = new WriterBuffer();

    // private final AtomicLong batchSize = new AtomicLong();
    private volatile boolean closed = false;
    private volatile Exception flushException;
    private final LinkedBlockingDeque<DorisFlushTuple> flushQueue;

    public DorisWriterManager(DorisWriterOptions writerOptions) {
        this.writerOptions = writerOptions;
        this.dorisStreamLoadVisitor = new DorisStreamLoadVisitor(writerOptions);
        flushQueue = new LinkedBlockingDeque<>(writerOptions.getFlushQueueLength());
        this.startAsyncFlushing();
    }

    public final synchronized void writeRecord(String record) throws IOException {
        checkFlushException();

        try {
            buffer.addRow(record);

            // batchSize.addAndGet(record.getBytes().length);
            if (buffer.rowCount >= writerOptions.getBatchRows() || buffer.size >= writerOptions.getBatchSize()) {
                String label = createBatchLabel();
                LOG.debug(String.format("Doris buffer Sinking triggered: rows[%d] label[%s].", buffer.rowCount, label));
                flush(label, false);
            }
        } catch (Exception e) {
            throw new IOException("Writing records to Doris failed.", e);
        }
    }

    public synchronized void flush(String label, boolean waitUtilDone) throws Exception {
        checkFlushException();
        if (buffer.rowCount == 0) {
            if (waitUtilDone) {
                waitAsyncFlushingDone();
            }
            return;
        }
        flushQueue.put(new DorisFlushTuple(label, buffer));
        if (waitUtilDone) {
            // wait the last flush
            waitAsyncFlushingDone();
        }
        buffer = new WriterBuffer();
    }

    public synchronized void close() {
        if (!closed) {
            closed = true;
            try {
                String label = createBatchLabel();
                if (buffer.rowCount > 0) LOG.debug(String.format("Doris Sink is about to close: label[%s].", label));
                flush(label, true);
            } catch (Exception e) {
                throw new RuntimeException("Writing records to Doris failed.", e);
            }
        }
        checkFlushException();
    }

    public String createBatchLabel() {
        return UUID.randomUUID().toString();
    }

    private void startAsyncFlushing() {
        // start flush thread
        Thread flushThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        asyncFlush();
                    } catch (Exception e) {
                        flushException = e;
                    }
                }
            }
        });
        flushThread.setDaemon(true);
        flushThread.start();
    }

    private void waitAsyncFlushingDone() throws InterruptedException {
        // wait previous flushings
        for (int i = 0; i <= writerOptions.getFlushQueueLength(); i++) {
            flushQueue.put(new DorisFlushTuple("", null));
        }
    }

    private void asyncFlush() throws Exception {
        DorisFlushTuple flushData = flushQueue.take();
        if (Strings.isNullOrEmpty(flushData.getLabel())) {
            return;
        }
        LOG.debug(String.format("Async stream load: rows[%d] bytes[%d] label[%s].", flushData.getRows().size(), flushData.getBytes(), flushData.getLabel()));
        for (int i = 0; i <= writerOptions.getMaxRetries(); i++) {
            try {
                // flush to Doris with stream load
                dorisStreamLoadVisitor.doStreamLoad(flushData);
                LOG.info(String.format("Async stream load finished: label[%s].", flushData.getLabel()));
                break;
            } catch (Exception e) {
                LOG.warn("Failed to flush batch data to doris, retry times = {}", i, e);
                if (i >= writerOptions.getMaxRetries()) {
                    throw new IOException(e);
                }
                try {
                    Thread.sleep(1000l * (i + 1));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Unable to flush, interrupted while doing another attempt", e);
                }
            }
        }
    }

    private void checkFlushException() {
        if (flushException != null) {
            throw new RuntimeException("Writing records to Doris failed.", flushException);
        }
    }

}
