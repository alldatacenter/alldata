/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.service.oracle;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.debezium.config.Field;
import io.debezium.DebeziumException;
import io.debezium.config.Configuration;
import io.debezium.connector.oracle.OracleConnectorConfig;
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
import io.debezium.connector.oracle.OracleConnection;
import io.debezium.connector.oracle.OracleConnector;
import io.debezium.storage.kafka.history.KafkaSchemaHistory;
import io.debezium.util.Strings;

// TODO: This will live in the actual connector module eventually
public class OracleConnectorIntegrator extends ConnectorIntegratorBase {

    private static final Map<String, AdditionalPropertyMetadata> ORACLE_PROPERTIES;
    static {
        Map<String, AdditionalPropertyMetadata> additionalMetadata = new LinkedHashMap<>();
        // Connection properties
        additionalMetadata.put(OracleConnectorConfig.TOPIC_PREFIX.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(OracleConnectorConfig.HOSTNAME.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(OracleConnectorConfig.PORT.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(OracleConnectorConfig.USER.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(OracleConnectorConfig.PASSWORD.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(OracleConnectorConfig.DATABASE_NAME.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(OracleConnectorConfig.PDB_NAME.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(OracleConnectorConfig.URL.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(OracleConnectorConfig.RAC_NODES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(KafkaSchemaHistory.BOOTSTRAP_SERVERS.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(KafkaSchemaHistory.TOPIC.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));

        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_STRATEGY.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_ARCHIVE_LOG_ONLY_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_ARCHIVE_LOG_HOURS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_BATCH_SIZE_DEFAULT.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_BATCH_SIZE_MIN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_BATCH_SIZE_MAX.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_SLEEP_TIME_DEFAULT_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_SLEEP_TIME_MIN_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_SLEEP_TIME_MAX_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_SLEEP_TIME_INCREMENT_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_TRANSACTION_RETENTION.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOG_MINING_ARCHIVE_DESTINATION_NAME.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));

        // Filter properties
        additionalMetadata.put(OracleConnectorConfig.TABLE_INCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(OracleConnectorConfig.TABLE_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(OracleConnectorConfig.COLUMN_INCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(OracleConnectorConfig.COLUMN_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));

        // Snapshot properties
        additionalMetadata.put(OracleConnectorConfig.SNAPSHOT_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT, enumArrayToList(OracleConnectorConfig.SnapshotMode.values())));
        additionalMetadata.put(OracleConnectorConfig.SNAPSHOT_LOCKING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(OracleConnectorConfig.SNAPSHOT_FETCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(OracleConnectorConfig.SNAPSHOT_DELAY_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(OracleConnectorConfig.SNAPSHOT_LOCK_TIMEOUT_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(OracleConnectorConfig.SNAPSHOT_MAX_THREADS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(OracleConnectorConfig.SNAPSHOT_SELECT_STATEMENT_OVERRIDES_BY_TABLE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(OracleConnectorConfig.SNAPSHOT_ENHANCEMENT_TOKEN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));

        // Data type mapping properties:
        additionalMetadata.put(OracleConnectorConfig.INCLUDE_SCHEMA_CHANGES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(OracleConnectorConfig.TOMBSTONES_ON_DELETE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(OracleConnectorConfig.DECIMAL_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(OracleConnectorConfig.DecimalHandlingMode.values())));
        additionalMetadata.put(OracleConnectorConfig.BINARY_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(OracleConnectorConfig.BinaryHandlingMode.values())));
        additionalMetadata.put(OracleConnectorConfig.TIME_PRECISION_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR, enumArrayToList(TemporalPrecisionMode.values())));

        // Heartbeat properties
        additionalMetadata.put(Heartbeat.HEARTBEAT_INTERVAL.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED_HEARTBEAT));
        additionalMetadata.put(Heartbeat.HEARTBEAT_TOPICS_PREFIX.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED_HEARTBEAT));

        // Data type mapping properties - Advanced:
        // additional property added to UI Requirements document section for "Data type mapping properties"-advanced section:
        additionalMetadata.put(OracleConnectorConfig.CUSTOM_CONVERTERS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.TRUNCATE_COLUMN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.MASK_COLUMN.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.MASK_COLUMN_WITH_HASH.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.PROPAGATE_DATATYPE_SOURCE_TYPE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.PROPAGATE_COLUMN_SOURCE_TYPE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.MSG_KEY_COLUMNS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.PROVIDE_TRANSACTION_METADATA.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.SANITIZE_FIELD_NAMES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.LOB_ENABLED.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));

        // Advanced configs (aka Runtime configs based on the PoC Requirements document
        additionalMetadata.put(KafkaSchemaHistory.RECOVERY_POLL_ATTEMPTS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(KafkaSchemaHistory.RECOVERY_POLL_INTERVAL_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.SKIPPED_OPERATIONS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.EVENT_PROCESSING_FAILURE_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED, enumArrayToList(OracleConnectorConfig.EventProcessingFailureHandlingMode.values())));
        additionalMetadata.put(OracleConnectorConfig.QUERY_FETCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.MAX_QUEUE_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.POLL_INTERVAL_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.RETRIABLE_RESTART_WAIT.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(OracleConnectorConfig.SIGNAL_DATA_COLLECTION.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));

        ORACLE_PROPERTIES = Collections.unmodifiableMap(additionalMetadata);
    }

    @Override
    public FilterValidationResult validateFilters(Map<String, String> properties) {
        PropertiesValidationResult result = validateProperties(properties);
        if (result.status == Status.INVALID) {
            return FilterValidationResult.invalid(result.propertyValidationResults);
        }

        final OracleConnectorConfig oracleConfig = new OracleConnectorConfig(Configuration.from(properties));
        final String databaseName = oracleConfig.getCatalogName();

        try (OracleConnection connection = connect(oracleConfig)) {
            if (!Strings.isNullOrBlank(oracleConfig.getPdbName())) {
                connection.setSessionToPdb(oracleConfig.getPdbName());
            }
            Set<TableId> tables;
            try {
                // todo: we need to expose a better method from the connector, particularly getAllTableIds
                //          the following's performance is acceptable when using PDBs but not as ideal with non-PDB
                tables = connection.readTableNames(databaseName, null, null, new String[]{ "TABLE" });

                List<DataCollection> matchingTables = tables.stream()
                        .filter(tableId -> oracleConfig.getTableFilters().dataCollectionFilter().isIncluded(tableId))
                        .map(tableId -> new DataCollection(tableId.schema(), tableId.table()))
                        .collect(Collectors.toList());

                return FilterValidationResult.valid(matchingTables);
            }
            catch (SQLException e) {
                throw new DebeziumException(e);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Could not retrieve real database name", e);
        }
    }
    
    private OracleConnection connect(OracleConnectorConfig oracleConfig) {
        return new OracleConnection(oracleConfig.getJdbcConfig(), false);
    }

    @Override
    protected ConnectorDescriptor getConnectorDescriptor() {
        return new ConnectorDescriptor("oracle", "Oracle", true);
    }

    @Override
    protected SourceConnector getConnector() {
        return new OracleConnector();
    }

    @Override
    public Map<String, AdditionalPropertyMetadata> allPropertiesWithAdditionalMetadata() {
        return ORACLE_PROPERTIES;
    }

    @Override
    public Field.Set getAllConnectorFields() {
        return OracleConnectorConfig.ALL_FIELDS;
    }
}
