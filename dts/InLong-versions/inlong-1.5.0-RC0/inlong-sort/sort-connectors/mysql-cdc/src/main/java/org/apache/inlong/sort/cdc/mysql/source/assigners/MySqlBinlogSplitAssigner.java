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

package org.apache.inlong.sort.cdc.mysql.source.assigners;

import io.debezium.connector.mysql.MySqlConnection;
import io.debezium.jdbc.JdbcConnection;
import io.debezium.relational.RelationalTableFilters;
import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.inlong.sort.cdc.mysql.debezium.DebeziumUtils;
import org.apache.inlong.sort.cdc.mysql.schema.MySqlSchema;
import org.apache.inlong.sort.cdc.mysql.source.assigners.state.BinlogPendingSplitsState;
import org.apache.inlong.sort.cdc.mysql.source.assigners.state.PendingSplitsState;
import org.apache.inlong.sort.cdc.mysql.source.config.MySqlSourceConfig;
import org.apache.inlong.sort.cdc.mysql.source.config.MySqlSourceOptions;
import org.apache.inlong.sort.cdc.mysql.source.offset.BinlogOffset;
import org.apache.inlong.sort.cdc.mysql.source.split.FinishedSnapshotSplitInfo;
import org.apache.inlong.sort.cdc.mysql.source.split.MySqlBinlogSplit;
import org.apache.inlong.sort.cdc.mysql.source.split.MySqlSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ververica.cdc.connectors.mysql.source.utils.TableDiscoveryUtils.listTables;
import static org.apache.inlong.sort.cdc.mysql.debezium.DebeziumUtils.currentBinlogOffset;

/**
 * A {@link MySqlSplitAssigner} which only read binlog from current binlog position.
 */
public class MySqlBinlogSplitAssigner implements MySqlSplitAssigner {

    private static final Logger LOG = LoggerFactory.getLogger(MySqlBinlogSplitAssigner.class);
    private static final String BINLOG_SPLIT_ID = "binlog-split";

    private final MySqlSourceConfig sourceConfig;
    private final RelationalTableFilters tableFilters;

    private MySqlConnection jdbc;
    private boolean isBinlogSplitAssigned;

    public MySqlBinlogSplitAssigner(MySqlSourceConfig sourceConfig) {
        this(sourceConfig, false);
    }

    public MySqlBinlogSplitAssigner(
            MySqlSourceConfig sourceConfig, BinlogPendingSplitsState checkpoint) {
        this(sourceConfig, checkpoint.isBinlogSplitAssigned());
    }

    private MySqlBinlogSplitAssigner(
            MySqlSourceConfig sourceConfig, boolean isBinlogSplitAssigned) {
        this.sourceConfig = sourceConfig;
        this.tableFilters = DebeziumUtils.createTableFilters(sourceConfig);
        this.isBinlogSplitAssigned = isBinlogSplitAssigned;
    }

    @Override
    public void open() {
        jdbc = DebeziumUtils.createMySqlConnection(sourceConfig);
    }

    @Override
    public Optional<MySqlSplit> getNext() {
        if (isBinlogSplitAssigned) {
            return Optional.empty();
        } else {
            isBinlogSplitAssigned = true;
            return Optional.of(createBinlogSplit());
        }
    }

    @Override
    public boolean waitingForFinishedSplits() {
        return false;
    }

    @Override
    public List<FinishedSnapshotSplitInfo> getFinishedSplitInfos() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void onFinishedSplits(Map<String, BinlogOffset> splitFinishedOffsets) {
        // do nothing
    }

    @Override
    public void addSplits(Collection<MySqlSplit> splits) {
        // we don't store the split, but will re-create binlog split later
        isBinlogSplitAssigned = false;
    }

    @Override
    public PendingSplitsState snapshotState(long checkpointId) {
        return new BinlogPendingSplitsState(isBinlogSplitAssigned);
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) {
        // nothing to do
    }

    @Override
    public AssignerStatus getAssignerStatus() {
        return AssignerStatus.INITIAL_ASSIGNING_FINISHED;
    }

    @Override
    public void suspend() {

    }

    @Override
    public void wakeup() {

    }

    @Override
    public void close() {

    }

    // ------------------------------------------------------------------------------------------

    private MySqlBinlogSplit createBinlogSplit() {
        Map<TableId, TableChange> tableSchemas = discoverCapturedTableSchemas();
        try (JdbcConnection jdbc = DebeziumUtils.openJdbcConnection(sourceConfig)) {
            return new MySqlBinlogSplit(
                    BINLOG_SPLIT_ID,
                    currentBinlogOffset(jdbc),
                    BinlogOffset.NO_STOPPING_OFFSET,
                    new ArrayList<>(),
                    tableSchemas,
                    0);
        } catch (Exception e) {
            throw new FlinkRuntimeException("Read the binlog offset error", e);
        }
    }

    private Map<TableId, TableChange> discoverCapturedTableSchemas() {
        final List<TableId> capturedTableIds;
        try {
            capturedTableIds = listTables(jdbc, tableFilters);
        } catch (SQLException e) {
            throw new FlinkRuntimeException("Failed to discover captured tables", e);
        }
        if (capturedTableIds.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Can't find any matched tables,"
                                    + " please check your configured database-name: %s and table-name: %s",
                            sourceConfig.getDbzConfiguration().getString(MySqlSourceOptions.DATABASE_NAME.key()),
                            sourceConfig.getDbzConfiguration().getString(MySqlSourceOptions.TABLE_NAME.key())));
        }

        // fetch table schemas
        MySqlSchema mySqlSchema = new MySqlSchema(sourceConfig, false);
        Map<TableId, TableChange> tableSchemas = new HashMap<>();
        for (TableId tableId : capturedTableIds) {
            TableChange tableSchema = mySqlSchema.getTableSchema(jdbc, tableId);
            tableSchemas.put(tableId, tableSchema);
        }
        return tableSchemas;
    }
}
