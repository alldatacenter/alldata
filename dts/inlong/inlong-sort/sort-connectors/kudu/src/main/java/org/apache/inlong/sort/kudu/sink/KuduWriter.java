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

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.kudu.common.KuduTableInfo;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduSession;
import org.apache.kudu.client.KuduTable;
import org.apache.kudu.client.Operation;
import org.apache.kudu.client.PartialRow;
import org.apache.kudu.client.RowError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The WriteClient for kudu, which holds a client and session.
 */
public class KuduWriter {

    private static final Logger LOG = LoggerFactory.getLogger(KuduWriter.class);

    private final String[] fieldNames;
    private final DataType[] dataTypes;

    /**
     * The client of kudu.
     */
    private final transient KuduClient client;

    /**
     * The session of kudu client.
     */
    private final transient KuduSession session;

    /**
     * The kudu table object.
     */
    private final transient KuduTable kuduTable;

    private final boolean forceInUpsertMode;

    /**
     * The exception caught in asynchronous tasks.
     */
    private transient AtomicReference<Throwable> flushThrowable;

    private final List<Tuple2<Long, Integer>> consumeTimes;
    private final int timePrintingThreshold = 10;
    private int applyNum = 0;

    public KuduWriter(
            KuduClient client,
            KuduTable kuduTable,
            KuduTableInfo kuduTableInfo) {
        LOG.info("Creating new kuduWriter: {}.", kuduTableInfo);
        this.client = client;
        this.kuduTable = kuduTable;
        this.fieldNames = kuduTableInfo.getFieldNames();
        this.dataTypes = kuduTableInfo.getDataTypes();
        this.forceInUpsertMode = kuduTableInfo.isForceInUpsertMode();

        this.consumeTimes = Collections.synchronizedList(new ArrayList<>(100));
        this.session = this.client.newSession();
        session.setFlushMode(kuduTableInfo.getFlushMode());
    }

    private DataType[] getExpectedDataTypes(TableSchema flinkSchema) {
        return flinkSchema.getFieldDataTypes();
    }

    private String[] getExpectedFieldNames(TableSchema flinkSchema) {
        return flinkSchema.getFieldNames();
    }

    private void onFailure(List<RowError> failure) throws IOException {
        String errors = failure.stream()
                .map(error -> error.toString() + System.lineSeparator())
                .collect(Collectors.joining());

        throw new IOException("Error while sending value. \n " + errors);
    }

    private void checkAsyncErrors() throws IOException {
        if (session.countPendingErrors() == 0) {
            return;
        }

        List<RowError> errors = Arrays.asList(session.getPendingErrors().getRowErrors());
        onFailure(errors);
    }

    public void flushAndCheckErrors() throws IOException {
        checkAsyncErrors();
        flush();
        checkAsyncErrors();
    }

    public void flush() throws IOException {
        long startTime = System.currentTimeMillis();

        session.flush();

        Tuple2<Long, Integer> consumeTime = Tuple2.of(System.currentTimeMillis() - startTime, applyNum);
        consumeTimes.add(consumeTime);
        applyNum = 0;
        printConsumeTimes(false);
    }

    private void printConsumeTimes(boolean force) {
        if (consumeTimes.size() > 0 && (force || consumeTimes.size() >= timePrintingThreshold)) {
            StringJoiner detailTimes = new StringJoiner(",");
            StringJoiner detailNums = new StringJoiner(",");
            long summaryTime = 0;
            int summaryNum = 0;
            try {
                for (Tuple2<Long, Integer> consumeTime : consumeTimes) {
                    Long time = consumeTime.f0;
                    Integer count = consumeTime.f1;
                    detailTimes.add(time + "");
                    detailNums.add(count + "");
                    summaryTime += time;
                    summaryNum += count;
                }
                consumeTimes.clear();

                LOG.info("Flushing time spent: [{}]:{}ms.",
                        detailTimes, ((double) summaryTime / summaryNum));
                LOG.info("Flushing records: [{}]", detailNums);
            } catch (Exception e) {
                LOG.error("error when print consumeTimes", e);
            }

        }
    }

    public void close() throws KuduException {
        printConsumeTimes(true);
        session.close();
        client.close();
    }

    public void applyRow(RowData row) throws KuduException {

        Operation operation;
        if (row.getRowKind() == RowKind.UPDATE_AFTER) {
            operation = forceInUpsertMode ? kuduTable.newUpsert() : kuduTable.newInsert();
        } else {
            operation = kuduTable.newDelete();
        }
        final PartialRow kuduRow = operation.getRow();

        for (int j = 0; j < fieldNames.length; j++) {
            String columnName = fieldNames[j];
            DataType columnType = dataTypes[j];

            Object column = RowData.createFieldGetter(columnType.getLogicalType(), j).getFieldOrNull(row);
            if (column == null) {
                continue;
            }
            kuduRow.addObject(columnName, column);
        }
        session.apply(operation);
        applyNum++;
    }

    public void checkError() {
        // check session error
        try {
            checkAsyncErrors();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // check flush error
        Throwable throwable = flushThrowable.get();
        if (throwable != null) {
            throw new RuntimeException("An asynchronous exception is caught " +
                    "in the kudu sink.", throwable);
        }
    }

    private void runWithRetry(ApplyAction action, int maxRetries) {
        for (int retry = 0; retry <= maxRetries; retry++) {
            try {
                action.call();
                break;
            } catch (IOException e) {
                LOG.error("Kudu applyRow error, retryTime:" + retry, e);
                try {
                    Thread.sleep((retry + 1) * 1000L);
                } catch (InterruptedException ignore) {
                }
                if (retry >= maxRetries) {
                    flushThrowable.compareAndSet(null, e);
                }
            }
        }
    }

    @FunctionalInterface
    interface ApplyAction {

        void call() throws IOException;
    }

    public void applyRow(RowData row, int maxRetries) {
        runWithRetry(() -> applyRow(row), maxRetries);
    }

    public void flush(int maxRetries) {
        runWithRetry(this::flush, maxRetries);
    }
}
