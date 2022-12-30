/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.service.mysql;

import io.debezium.DebeziumException;
import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.configserver.model.AdditionalPropertyMetadata;
import io.debezium.configserver.model.ConnectorProperty;
import io.debezium.configserver.model.DataCollection;
import io.debezium.configserver.model.FilterValidationResult;
import io.debezium.configserver.model.PropertiesValidationResult;
import io.debezium.configserver.model.PropertiesValidationResult.Status;
import io.debezium.configserver.service.ConnectorIntegratorBase;
import io.debezium.connector.mysql.MySqlConnection;
import io.debezium.connector.mysql.MySqlConnector;
import io.debezium.connector.mysql.MySqlConnectorConfig;
import io.debezium.heartbeat.Heartbeat;
import io.debezium.jdbc.TemporalPrecisionMode;
import io.debezium.relational.TableId;
import io.debezium.storage.kafka.history.KafkaSchemaHistory;
import org.apache.kafka.connect.source.SourceConnector;
import org.jboss.logging.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO: This will live in the actual connector module eventually
public class MySqlConnectorIntegrator extends ConnectorIntegratorBase {

    private static final Logger LOGGER = Logger.getLogger(MySqlConnectorIntegrator.class);

    private static final Map<String, AdditionalPropertyMetadata> MYSQL_PROPERTIES;
    static {
        Map<String, AdditionalPropertyMetadata> additionalMetadata = new LinkedHashMap<>();
        // Connection properties
        additionalMetadata.put(MySqlConnectorConfig.TOPIC_PREFIX.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MySqlConnectorConfig.SERVER_ID.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MySqlConnectorConfig.HOSTNAME.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MySqlConnectorConfig.PORT.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MySqlConnectorConfig.USER.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MySqlConnectorConfig.PASSWORD.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(KafkaSchemaHistory.BOOTSTRAP_SERVERS.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(KafkaSchemaHistory.TOPIC.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MySqlConnectorConfig.JDBC_DRIVER.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));

        // Connection properties - advanced section incl SSL subcategory
        additionalMetadata.put(MySqlConnectorConfig.SSL_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED_SSL, enumArrayToList(MySqlConnectorConfig.SecureConnectionMode.values())));
        additionalMetadata.put(MySqlConnectorConfig.SSL_KEYSTORE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED_SSL));
        additionalMetadata.put(MySqlConnectorConfig.SSL_KEYSTORE_PASSWORD.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED_SSL));
        additionalMetadata.put(MySqlConnectorConfig.SSL_TRUSTSTORE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED_SSL));
        additionalMetadata.put(MySqlConnectorConfig.SSL_TRUSTSTORE_PASSWORD.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED_SSL));
        additionalMetadata.put(MySqlConnectorConfig.SERVER_ID_OFFSET.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.CONNECTION_TIMEOUT_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.KEEP_ALIVE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.KEEP_ALIVE_INTERVAL_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.ON_CONNECT_STATEMENTS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));

        // Filter properties
        additionalMetadata.put(MySqlConnectorConfig.DATABASE_INCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MySqlConnectorConfig.DATABASE_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MySqlConnectorConfig.TABLE_INCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MySqlConnectorConfig.TABLE_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MySqlConnectorConfig.COLUMN_INCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MySqlConnectorConfig.COLUMN_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MySqlConnectorConfig.TABLES_IGNORE_BUILTIN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));

        // Snapshot properties
        additionalMetadata.put(MySqlConnectorConfig.SNAPSHOT_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT, enumArrayToList(MySqlConnectorConfig.SnapshotMode.values())));
        additionalMetadata.put(MySqlConnectorConfig.SNAPSHOT_LOCKING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT, enumArrayToList(MySqlConnectorConfig.SnapshotLockingMode.values())));
        additionalMetadata.put(MySqlConnectorConfig.SNAPSHOT_NEW_TABLES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT, enumArrayToList(MySqlConnectorConfig.SnapshotNewTables.values())));
        additionalMetadata.put(MySqlConnectorConfig.SNAPSHOT_FETCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(MySqlConnectorConfig.SNAPSHOT_DELAY_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(MySqlConnectorConfig.SNAPSHOT_SELECT_STATEMENT_OVERRIDES_BY_TABLE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));

        // Data type mapping properties:
        additionalMetadata.put(MySqlConnectorConfig.EVENT_DESERIALIZATION_FAILURE_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(MySqlConnectorConfig.EventProcessingFailureHandlingMode.values())));
        additionalMetadata.put(MySqlConnectorConfig.ENABLE_TIME_ADJUSTER.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(MySqlConnectorConfig.GTID_SOURCE_FILTER_DML_EVENTS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(MySqlConnectorConfig.GTID_SOURCE_INCLUDES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(MySqlConnectorConfig.GTID_SOURCE_EXCLUDES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(MySqlConnectorConfig.TOMBSTONES_ON_DELETE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(MySqlConnectorConfig.DECIMAL_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(MySqlConnectorConfig.DecimalHandlingMode.values())));
        additionalMetadata.put(MySqlConnectorConfig.TIME_PRECISION_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(TemporalPrecisionMode.values())));
        additionalMetadata.put(MySqlConnectorConfig.BINARY_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(MySqlConnectorConfig.BinaryHandlingMode.values())));
        additionalMetadata.put(MySqlConnectorConfig.BIGINT_UNSIGNED_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(MySqlConnectorConfig.BigIntUnsignedHandlingMode.values())));

        // Heartbeat properties
        additionalMetadata.put(Heartbeat.HEARTBEAT_INTERVAL.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED_HEARTBEAT));
        additionalMetadata.put(Heartbeat.HEARTBEAT_TOPICS_PREFIX.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED_HEARTBEAT));

        // Data type mapping properties - Advanced:
        // additional property added to UI Requirements document section for "Data type mapping properties"-advanced section:
        additionalMetadata.put(MySqlConnectorConfig.INCLUDE_SCHEMA_CHANGES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.INCLUDE_SQL_QUERY.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.SCHEMA_HISTORY.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.ROW_COUNT_FOR_STREAMING_RESULT_SETS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.BUFFER_SIZE_FOR_BINLOG_READER.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.TRUNCATE_COLUMN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.MASK_COLUMN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.MASK_COLUMN_WITH_HASH.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.MSG_KEY_COLUMNS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));

        // Advanced configs (aka Runtime configs based on the PoC Requirements document
        additionalMetadata.put(KafkaSchemaHistory.RECOVERY_POLL_ATTEMPTS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(KafkaSchemaHistory.RECOVERY_POLL_INTERVAL_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.SKIPPED_OPERATIONS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.EVENT_PROCESSING_FAILURE_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED, enumArrayToList(MySqlConnectorConfig.EventProcessingFailureHandlingMode.values())));
        additionalMetadata.put(MySqlConnectorConfig.INCONSISTENT_SCHEMA_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED, enumArrayToList(MySqlConnectorConfig.EventProcessingFailureHandlingMode.values())));
        additionalMetadata.put(MySqlConnectorConfig.MAX_BATCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.MAX_QUEUE_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(MySqlConnectorConfig.POLL_INTERVAL_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));

        MYSQL_PROPERTIES = Collections.unmodifiableMap(additionalMetadata);
    }

    @Override
    public FilterValidationResult validateFilters(Map<String, String> properties) {
        PropertiesValidationResult result = validateProperties(properties);
        if (result.status == Status.INVALID) {
            return FilterValidationResult.invalid(result.propertyValidationResults);
        }

        Configuration configuration = Configuration.from(properties);
        MySqlConnectorConfig config = new MySqlConnectorConfig(configuration);

        final MySqlConnection.MySqlConnectionConfiguration connectionConfig = new MySqlConnection.MySqlConnectionConfiguration(configuration);
        try (MySqlConnection connection = new MySqlConnection(connectionConfig)) {
            Set<TableId> tables;

            final List<String> databaseNames = new ArrayList<>();
            connection.query("SHOW DATABASES", rs -> {
                while (rs.next()) {
                    databaseNames.add(rs.getString(1));
                }
            });

            List<DataCollection> allMatchingTables = new ArrayList<>();

            for (String databaseName : databaseNames) {
                if (!config.getTableFilters().databaseFilter().test(databaseName)) {
                    continue;
                }
                tables = connection.readTableNames(databaseName, null, null, new String[]{"TABLE"});

                var matchingTables = tables.stream()
                        .filter(tableId -> config.getTableFilters().dataCollectionFilter().isIncluded(tableId))
                        .map(tableId -> new DataCollection(tableId.catalog(), tableId.table()))
                        .collect(Collectors.toList());
                allMatchingTables.addAll(matchingTables);
            }

            return FilterValidationResult.valid(allMatchingTables);
        }
        catch (SQLException e) {
            throw new DebeziumException(e);
        }
    }

    @Override
    protected ConnectorDescriptor getConnectorDescriptor() {
        return new ConnectorDescriptor("mysql", "MySQL", true);
    }

    @Override
    protected SourceConnector getConnector() {
        return new MySqlConnector();
    }

    @Override
    public Map<String, AdditionalPropertyMetadata> allPropertiesWithAdditionalMetadata() {
        return MYSQL_PROPERTIES;
    }

    @Override
    public Field.Set getAllConnectorFields() {
        return MySqlConnectorConfig.ALL_FIELDS;
    }
}
