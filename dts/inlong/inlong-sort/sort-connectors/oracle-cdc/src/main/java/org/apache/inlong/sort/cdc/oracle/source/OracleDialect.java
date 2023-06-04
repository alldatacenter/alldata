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

package org.apache.inlong.sort.cdc.oracle.source;

import static org.apache.inlong.sort.cdc.oracle.source.utils.OracleConnectionUtils.createOracleConnection;
import static org.apache.inlong.sort.cdc.oracle.source.utils.OracleConnectionUtils.currentRedoLogOffset;

import io.debezium.connector.oracle.OracleConnection;
import io.debezium.jdbc.JdbcConnection;
import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.flink.annotation.Experimental;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.inlong.sort.cdc.base.config.JdbcSourceConfig;
import org.apache.inlong.sort.cdc.base.dialect.JdbcDataSourceDialect;
import org.apache.inlong.sort.cdc.base.relational.connection.JdbcConnectionPoolFactory;
import org.apache.inlong.sort.cdc.base.source.assigner.splitter.ChunkSplitter;
import org.apache.inlong.sort.cdc.base.source.meta.offset.Offset;
import org.apache.inlong.sort.cdc.base.source.meta.split.SourceSplitBase;
import org.apache.inlong.sort.cdc.base.source.reader.external.FetchTask;
import org.apache.inlong.sort.cdc.oracle.source.config.OracleSourceConfig;
import org.apache.inlong.sort.cdc.oracle.source.config.OracleSourceConfigFactory;
import org.apache.inlong.sort.cdc.oracle.source.reader.fetch.OracleScanFetchTask;
import org.apache.inlong.sort.cdc.oracle.source.reader.fetch.OracleSourceFetchTaskContext;
import org.apache.inlong.sort.cdc.oracle.source.reader.fetch.OracleStreamFetchTask;
import org.apache.inlong.sort.cdc.oracle.source.splitter.OracleChunkSplitter;
import org.apache.inlong.sort.cdc.oracle.source.utils.OracleConnectionUtils;
import org.apache.inlong.sort.cdc.oracle.source.utils.OracleSchema;

/** The {@link JdbcDataSourceDialect} implementation for Oracle datasource.
 *  Copy from com.ververica:flink-connector-oracle-cdc:2.3.0
 */
@Experimental
public class OracleDialect implements JdbcDataSourceDialect {

    private static final long serialVersionUID = 1L;
    private final OracleSourceConfigFactory configFactory;
    private final OracleSourceConfig sourceConfig;
    private transient OracleSchema oracleSchema;

    public OracleDialect(OracleSourceConfigFactory configFactory) {
        this.configFactory = configFactory;
        this.sourceConfig = configFactory.create(0);
    }

    @Override
    public String getName() {
        return "Oracle";
    }

    @Override
    public Offset displayCurrentOffset(JdbcSourceConfig sourceConfig) {
        try (JdbcConnection jdbcConnection = openJdbcConnection(sourceConfig)) {
            return currentRedoLogOffset(jdbcConnection);
        } catch (Exception e) {
            throw new FlinkRuntimeException("Read the redoLog offset error", e);
        }
    }

    @Override
    public boolean isDataCollectionIdCaseSensitive(JdbcSourceConfig sourceConfig) {
        try (JdbcConnection jdbcConnection = openJdbcConnection(sourceConfig)) {
            OracleConnection oracleConnection = (OracleConnection) jdbcConnection;
            return oracleConnection.getOracleVersion().getMajor() == 11;
        } catch (SQLException e) {
            throw new FlinkRuntimeException("Error reading oracle variables: " + e.getMessage(), e);
        }
    }

    @Override
    public JdbcConnection openJdbcConnection(JdbcSourceConfig sourceConfig) {
        return OracleConnectionUtils.createOracleConnection(
                sourceConfig.getDbzConnectorConfig().getJdbcConfig());
    }

    @Override
    public ChunkSplitter createChunkSplitter(JdbcSourceConfig sourceConfig) {
        return new OracleChunkSplitter(sourceConfig, this);
    }

    @Override
    public JdbcConnectionPoolFactory getPooledDataSourceFactory() {
        return new OraclePooledDataSourceFactory();
    }

    @Override
    public List<TableId> discoverDataCollections(JdbcSourceConfig sourceConfig) {
        OracleSourceConfig oracleSourceConfig = (OracleSourceConfig) sourceConfig;
        try (JdbcConnection jdbcConnection = openJdbcConnection(sourceConfig)) {
            return OracleConnectionUtils.listTables(
                    jdbcConnection, oracleSourceConfig.getTableFilters());
        } catch (SQLException e) {
            throw new FlinkRuntimeException("Error to discover tables: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<TableId, TableChange> discoverDataCollectionSchemas(JdbcSourceConfig sourceConfig) {
        final List<TableId> capturedTableIds = discoverDataCollections(sourceConfig);

        try (OracleConnection jdbc = createOracleConnection(sourceConfig.getDbzConfiguration())) {
            // fetch table schemas
            Map<TableId, TableChange> tableSchemas = new HashMap<>();
            for (TableId tableId : capturedTableIds) {
                TableChange tableSchema = queryTableSchema(jdbc, tableId);
                tableSchemas.put(tableId, tableSchema);
            }
            return tableSchemas;
        } catch (Exception e) {
            throw new FlinkRuntimeException(
                    "Error to discover table schemas: " + e.getMessage(), e);
        }
    }

    @Override
    public TableChange queryTableSchema(JdbcConnection jdbc, TableId tableId) {
        if (oracleSchema == null) {
            oracleSchema = new OracleSchema();
        }
        return oracleSchema.getTableSchema(jdbc, tableId);
    }

    @Override
    public OracleSourceFetchTaskContext createFetchTaskContext(
            SourceSplitBase sourceSplitBase, JdbcSourceConfig taskSourceConfig) {
        final OracleConnection jdbcConnection =
                createOracleConnection(taskSourceConfig.getDbzConfiguration());
        return new OracleSourceFetchTaskContext(taskSourceConfig, this, jdbcConnection);
    }

    @Override
    public FetchTask<SourceSplitBase> createFetchTask(SourceSplitBase sourceSplitBase) {
        if (sourceSplitBase.isSnapshotSplit()) {
            return new OracleScanFetchTask(sourceSplitBase.asSnapshotSplit());
        } else {
            return new OracleStreamFetchTask(sourceSplitBase.asStreamSplit());
        }
    }
}
