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

package org.apache.inlong.sort.jdbc.internal;

import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.util.CalculateObjectSizeUtils;

import org.apache.flink.connector.jdbc.internal.converter.JdbcRowConverter;
import org.apache.flink.connector.jdbc.internal.executor.JdbcBatchStatementExecutor;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatement;
import org.apache.flink.connector.jdbc.statement.StatementFactory;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.table.data.RowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * A {@link JdbcBatchStatementExecutor} that simply adds the records into batches of {@link
 * java.sql.PreparedStatement} and doesn't buffer records in memory. Only used in Table/SQL API.
 * Supported executors:TableBufferedStatementExecutor, TableBufferReducedStatementExecutor, TableSimpleStatementExecutor
 */
public final class TableMetricStatementExecutor implements JdbcBatchStatementExecutor<RowData> {

    private static final Pattern pattern = Pattern.compile("Batch entry (\\d+)");
    private static final Logger LOG = LoggerFactory.getLogger(TableMetricStatementExecutor.class);
    private final StatementFactory stmtFactory;
    private final JdbcRowConverter converter;
    private List<RowData> batch;
    private final DirtySinkHelper<Object> dirtySinkHelper;
    private final SinkMetricData sinkMetricData;
    private final AtomicInteger counter = new AtomicInteger();
    private transient FieldNamedPreparedStatement st;
    private boolean multipleSink;
    private String label;
    private String logtag;
    private String identifier;
    private Function<RowData, RowData> valueTransform = null;
    // counters used for table level metric calculation for multiple sink
    public long[] metric = new long[4];

    public TableMetricStatementExecutor(StatementFactory stmtFactory, JdbcRowConverter converter,
            DirtySinkHelper<Object> dirtySinkHelper, SinkMetricData sinkMetricData) {
        this.stmtFactory = checkNotNull(stmtFactory);
        this.converter = checkNotNull(converter);
        this.batch = new CopyOnWriteArrayList<>();
        this.dirtySinkHelper = dirtySinkHelper;
        this.sinkMetricData = sinkMetricData;
    }

    public void setDirtyMetaData(String label, String logtag, String identifier) {
        this.label = label;
        this.logtag = logtag;
        this.identifier = identifier;
    }

    public void setMultipleSink(boolean multipleSink) {
        this.multipleSink = multipleSink;
    }

    @Override
    public void prepareStatements(Connection connection) throws SQLException {
        st = stmtFactory.createStatement(connection);
    }

    public void setValueTransform(Function<RowData, RowData> valueTransform) {
        this.valueTransform = valueTransform;
    }

    @Override
    public void addToBatch(RowData record) throws SQLException {
        if (valueTransform != null) {
            record = valueTransform.apply(record); // copy or not
        }
        batch.add(record);
        converter.toExternal(record, st);
        st.addBatch();
    }

    @Override
    public void executeBatch() throws SQLException {
        try {
            st.executeBatch();

            long writtenSize = batch.size();
            // approximate since it may be inefficient to iterate over all writtenSize-1 elements.
            long writtenBytes = 0L;
            if (writtenSize > 0) {
                writtenBytes = CalculateObjectSizeUtils.getDataSize(batch.get(0)) * writtenSize;
            }
            batch.clear();
            if (!multipleSink) {
                sinkMetricData.invoke(writtenSize, writtenBytes);
            } else {
                metric[0] += writtenSize;
                metric[1] += writtenBytes;
            }

        } catch (SQLException e) {
            // clear the prepared statement first to avoid exceptions
            st.clearParameters();
            try {
                processErrorPosition(e);
            } catch (Exception ex) {
                try {
                    retryEntireBatch();
                } catch (JsonProcessingException exc) {
                    LOG.error("dirty data archive failed");
                }
            }
        }
    }

    private void processErrorPosition(SQLException e) throws SQLException {
        List<Integer> errorPositions = parseError(e);
        // the data before the first sqlexception are already written, handle those and remove them.
        int writtenSize = errorPositions.get(0);
        long writtenBytes = 0L;
        if (writtenSize > 0) {
            writtenBytes = CalculateObjectSizeUtils.getDataSize(batch.get(0)) * writtenSize;
        }
        if (!multipleSink) {
            sinkMetricData.invoke(writtenSize, writtenBytes);
        } else {
            metric[0] += writtenSize;
            metric[1] += writtenBytes;
        }

        batch = batch.subList(writtenSize, batch.size());

        // for the unwritten data, remove the dirty ones
        for (int pos : errorPositions) {
            pos -= writtenSize;
            RowData record = batch.get(pos);
            batch.remove(record);
            invokeDirty(record, e);
        }

        // try to execute the supposedly clean batch, throw exception on failure
        for (RowData record : batch) {
            addToBatch(record);
        }
        st.executeBatch();
        batch.clear();
        st.clearParameters();
    }

    private void retryEntireBatch() throws SQLException, JsonProcessingException {
        // clear parameters to make sure the batch is always clean in the end.
        st.clearParameters();
        for (RowData rowData : batch) {
            try {
                converter.toExternal(rowData, st);
                st.addBatch();
                st.executeBatch();
                if (!multipleSink) {
                    sinkMetricData.invokeWithEstimate(rowData);
                } else {
                    metric[0] += 1;
                    metric[1] += CalculateObjectSizeUtils.getDataSize(rowData);
                }
            } catch (Exception e) {
                st.clearParameters();
                invokeDirty(rowData, e);
            }
        }
        batch.clear();
        st.clearParameters();
    }

    private void invokeDirty(RowData rowData, Exception e) {
        if (!multipleSink) {
            if (dirtySinkHelper != null) {
                dirtySinkHelper.invoke(rowData.toString(), DirtyType.BATCH_LOAD_ERROR, e);
            }
            sinkMetricData.invokeDirtyWithEstimate(rowData);
        } else {
            if (dirtySinkHelper != null) {
                dirtySinkHelper.invoke(rowData.toString(), DirtyType.BATCH_LOAD_ERROR, label, logtag, identifier, e);
            }
            metric[2] += 1;
            metric[3] += CalculateObjectSizeUtils.getDataSize(rowData);
        }
    }

    private List<Integer> parseError(SQLException e) throws SQLException {
        List<Integer> errors = new ArrayList<>();
        int pos = getPosFromMessage(e.getMessage());
        if (pos != -1) {
            errors.add(getPosFromMessage(e.getMessage()));
        } else {
            throw new SQLException(e);
        }
        SQLException next = e.getNextException();
        if (next != null) {
            errors.addAll(parseError(next));
        }
        return errors;
    }

    private int getPosFromMessage(String message) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            int pos = Integer.parseInt(matcher.group(1));
            // duplicate key is a special case，can't just return the first instance
            if (message.contains("duplicate key")) {
                return -1;
            }
            return pos;
        }
        LOG.error("The dirty message {} can't be parsed", message);
        return -1;
    }

    @Override
    public void closeStatements() throws SQLException {
        if (st != null) {
            st.close();
            st = null;
        }
    }
}
