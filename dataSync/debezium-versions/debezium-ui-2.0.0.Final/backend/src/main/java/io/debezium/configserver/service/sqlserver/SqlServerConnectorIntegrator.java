/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.service.sqlserver;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.debezium.config.Field;
import io.debezium.DebeziumException;
import io.debezium.config.Configuration;
import io.debezium.connector.sqlserver.SqlServerConnectorConfig;
import org.apache.kafka.connect.source.SourceConnector;

import io.debezium.configserver.model.AdditionalPropertyMetadata;
import io.debezium.configserver.model.ConnectorProperty;
import io.debezium.configserver.model.DataCollection;
import io.debezium.heartbeat.Heartbeat;
import io.debezium.jdbc.TemporalPrecisionMode;
import io.debezium.relational.TableId;
import io.debezium.configserver.model.FilterValidationResult;
import io.debezium.configserver.model.PropertiesValidationResult;
import io.debezium.configserver.model.PropertiesValidationResult.Status;
import io.debezium.configserver.service.ConnectorIntegratorBase;
import io.debezium.connector.sqlserver.SqlServerConnection;
import io.debezium.connector.sqlserver.SqlServerConnector;
import io.debezium.storage.kafka.history.KafkaSchemaHistory;

// TODO: This will live in the actual connector module eventually
public class SqlServerConnectorIntegrator extends ConnectorIntegratorBase {

    private static final Map<String, AdditionalPropertyMetadata> SQLSERVER_PROPERTIES;
    static {
        Map<String, AdditionalPropertyMetadata> additionalMetadata = new LinkedHashMap<>();
        // Connection properties
        additionalMetadata.put(SqlServerConnectorConfig.TOPIC_PREFIX.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(SqlServerConnectorConfig.HOSTNAME.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(SqlServerConnectorConfig.PORT.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(SqlServerConnectorConfig.USER.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(SqlServerConnectorConfig.PASSWORD.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(SqlServerConnectorConfig.DATABASE_NAME.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(SqlServerConnectorConfig.INSTANCE.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(KafkaSchemaHistory.BOOTSTRAP_SERVERS.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(KafkaSchemaHistory.TOPIC.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));

        // Filter properties
        additionalMetadata.put(SqlServerConnectorConfig.TABLE_INCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(SqlServerConnectorConfig.TABLE_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(SqlServerConnectorConfig.COLUMN_INCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(SqlServerConnectorConfig.COLUMN_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(SqlServerConnectorConfig.TABLE_IGNORE_BUILTIN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));

        // Snapshot properties
        additionalMetadata.put(SqlServerConnectorConfig.SNAPSHOT_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT, enumArrayToList(SqlServerConnectorConfig.SnapshotMode.values())));
        additionalMetadata.put(SqlServerConnectorConfig.SNAPSHOT_ISOLATION_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT, enumArrayToList(SqlServerConnectorConfig.SnapshotIsolationMode.values())));
        additionalMetadata.put(SqlServerConnectorConfig.SNAPSHOT_MODE_TABLES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(SqlServerConnectorConfig.SNAPSHOT_FETCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(SqlServerConnectorConfig.SNAPSHOT_DELAY_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(SqlServerConnectorConfig.SNAPSHOT_LOCK_TIMEOUT_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(SqlServerConnectorConfig.SNAPSHOT_MAX_THREADS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(SqlServerConnectorConfig.SNAPSHOT_SELECT_STATEMENT_OVERRIDES_BY_TABLE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(SqlServerConnectorConfig.SNAPSHOT_FULL_COLUMN_SCAN_FORCE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));

        // Data type mapping properties:
        additionalMetadata.put(SqlServerConnectorConfig.INCLUDE_SCHEMA_CHANGES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(SqlServerConnectorConfig.TOMBSTONES_ON_DELETE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(SqlServerConnectorConfig.DECIMAL_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(SqlServerConnectorConfig.DecimalHandlingMode.values())));
        additionalMetadata.put(SqlServerConnectorConfig.BINARY_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(SqlServerConnectorConfig.BinaryHandlingMode.values())));
        additionalMetadata.put(SqlServerConnectorConfig.TIME_PRECISION_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(TemporalPrecisionMode.values())));

        // Heartbeat properties
        additionalMetadata.put(Heartbeat.HEARTBEAT_INTERVAL.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED_HEARTBEAT));
        additionalMetadata.put(Heartbeat.HEARTBEAT_TOPICS_PREFIX.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED_HEARTBEAT));

        // Data type mapping properties - Advanced:
        // additional property added to UI Requirements document section for "Data type mapping properties"-advanced section:
        additionalMetadata.put(SqlServerConnectorConfig.MAX_TRANSACTIONS_PER_ITERATION.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.CUSTOM_CONVERTERS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.TRUNCATE_COLUMN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.MASK_COLUMN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.MASK_COLUMN_WITH_HASH.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.PROPAGATE_DATATYPE_SOURCE_TYPE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.PROPAGATE_COLUMN_SOURCE_TYPE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.MSG_KEY_COLUMNS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.PROVIDE_TRANSACTION_METADATA.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.SANITIZE_FIELD_NAMES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));

        // Advanced configs (aka Runtime configs based on the PoC Requirements document
        additionalMetadata.put(KafkaSchemaHistory.RECOVERY_POLL_ATTEMPTS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(KafkaSchemaHistory.RECOVERY_POLL_INTERVAL_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.SKIPPED_OPERATIONS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.EVENT_PROCESSING_FAILURE_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED, enumArrayToList(SqlServerConnectorConfig.EventProcessingFailureHandlingMode.values())));
        additionalMetadata.put(SqlServerConnectorConfig.QUERY_FETCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.MAX_BATCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.MAX_QUEUE_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.POLL_INTERVAL_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.RETRIABLE_RESTART_WAIT.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(SqlServerConnectorConfig.SIGNAL_DATA_COLLECTION.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));

        SQLSERVER_PROPERTIES = Collections.unmodifiableMap(additionalMetadata);
    }

    @Override
    public FilterValidationResult validateFilters(Map<String, String> properties) {
        PropertiesValidationResult result = validateProperties(properties);
        if (result.status == Status.INVALID) {
            return FilterValidationResult.invalid(result.propertyValidationResults);
        }

        final SqlServerConnectorConfig sqlServerConfig = new SqlServerConnectorConfig(Configuration.from(properties));
        final List<String> databaseNames = sqlServerConfig.getDatabaseNames();
        
        try (SqlServerConnection connection = connect(sqlServerConfig)) {
            Set<TableId> tables = new HashSet<>();
            databaseNames.forEach(databaseName -> {
                try {
                    tables.addAll(connection.readTableNames(databaseName, null, null, new String[]{ "TABLE" }));
                }
                catch (SQLException e) {
                    throw new DebeziumException(e);
                }
            });

            List<DataCollection> matchingTables = tables.stream()
                    .filter(tableId -> sqlServerConfig.getTableFilters().dataCollectionFilter().isIncluded(tableId))
                    .map(tableId -> new DataCollection(tableId.catalog() + "." + tableId.schema(), tableId.table()))
                    .collect(Collectors.toList());

            return FilterValidationResult.valid(matchingTables);
        }
        catch (SQLException e) {
            throw new RuntimeException("Could not retrieve real database name", e);
        }
    }
    
    private SqlServerConnection connect(SqlServerConnectorConfig sqlServerConfig) {
        return new SqlServerConnection(sqlServerConfig.getJdbcConfig(), null, Collections.emptySet(),
                sqlServerConfig.useSingleDatabase());
    }

    @Override
    protected ConnectorDescriptor getConnectorDescriptor() {
        return new ConnectorDescriptor("sqlserver", "SQL Server", true);
    }

    @Override
    protected SourceConnector getConnector() {
        return new SqlServerConnector();
    }

    @Override
    public Map<String, AdditionalPropertyMetadata> allPropertiesWithAdditionalMetadata() {
        return SQLSERVER_PROPERTIES;
    }

    @Override
    public Field.Set getAllConnectorFields() {
        return SqlServerConnectorConfig.ALL_FIELDS;
    }
}
