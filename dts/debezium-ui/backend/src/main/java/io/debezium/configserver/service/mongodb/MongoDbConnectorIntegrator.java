/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.service.mongodb;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.debezium.DebeziumException;
import io.debezium.config.CommonConnectorConfig;
import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.configserver.model.AdditionalPropertyMetadata;
import io.debezium.configserver.model.ConnectorProperty;
import io.debezium.configserver.model.DataCollection;
import io.debezium.configserver.model.PropertiesValidationResult;
import io.debezium.connector.mongodb.CollectionId;
import io.debezium.connector.mongodb.ConnectionContext;
import io.debezium.connector.mongodb.MongoDbConnectorConfig;
import io.debezium.connector.mongodb.MongoDbTaskContext;
import io.debezium.connector.mongodb.ReplicaSet;
import io.debezium.heartbeat.Heartbeat;
import io.debezium.spi.schema.DataCollectionId;
import org.apache.kafka.connect.source.SourceConnector;

import io.debezium.configserver.model.FilterValidationResult;
import io.debezium.configserver.service.ConnectorIntegratorBase;
import io.debezium.connector.mongodb.MongoDbConnector;

// TODO: This will live in the actual connector module eventually
public class MongoDbConnectorIntegrator extends ConnectorIntegratorBase {

    private static final Map<String, AdditionalPropertyMetadata> MONGODB_PROPERTIES;
    static {
        Map<String, AdditionalPropertyMetadata> additionalMetadata = new LinkedHashMap<>();
        // Connection properties
        additionalMetadata.put(MongoDbConnectorConfig.HOSTS.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MongoDbConnectorConfig.AUTO_DISCOVER_MEMBERS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MongoDbConnectorConfig.MONGODB_POLL_INTERVAL_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MongoDbConnectorConfig.USER.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MongoDbConnectorConfig.PASSWORD.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION));
        additionalMetadata.put(MongoDbConnectorConfig.TOPIC_PREFIX.name(), new AdditionalPropertyMetadata(true, ConnectorProperty.Category.CONNECTION));

        // Connection properties - advanced section incl SSL subcategory
        additionalMetadata.put(MongoDbConnectorConfig.SSL_ENABLED.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED_SSL));
        additionalMetadata.put(MongoDbConnectorConfig.SSL_ALLOW_INVALID_HOSTNAMES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED_SSL));
        additionalMetadata.put(MongoDbConnectorConfig.CONNECT_TIMEOUT_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.CONNECT_BACKOFF_INITIAL_DELAY_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.CONNECT_BACKOFF_MAX_DELAY_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.AUTH_SOURCE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.MAX_FAILED_CONNECTIONS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.SERVER_SELECTION_TIMEOUT_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.SOCKET_TIMEOUT_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTION_ADVANCED));

        // Filter properties
        additionalMetadata.put(MongoDbConnectorConfig.DATABASE_INCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MongoDbConnectorConfig.DATABASE_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MongoDbConnectorConfig.COLLECTION_INCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MongoDbConnectorConfig.COLLECTION_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));
        additionalMetadata.put(MongoDbConnectorConfig.FIELD_EXCLUDE_LIST.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS));

        // Snapshot properties
        additionalMetadata.put(MongoDbConnectorConfig.SNAPSHOT_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT, enumArrayToList(MongoDbConnectorConfig.SnapshotMode.values())));
        additionalMetadata.put(MongoDbConnectorConfig.SNAPSHOT_FETCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));
        additionalMetadata.put(MongoDbConnectorConfig.SNAPSHOT_DELAY_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_SNAPSHOT));

        // Data type mapping properties:
        additionalMetadata.put(MongoDbConnectorConfig.TOMBSTONES_ON_DELETE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));
        additionalMetadata.put(MongoDbConnectorConfig.QUERY_FETCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR));

        // Heartbeat properties
        additionalMetadata.put(Heartbeat.HEARTBEAT_INTERVAL.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED_HEARTBEAT));
        additionalMetadata.put(Heartbeat.HEARTBEAT_TOPICS_PREFIX.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED_HEARTBEAT));

        // Data type mapping properties - Advanced:
        // additional property added to UI Requirements document section for "Data type mapping properties"-advanced section:
        additionalMetadata.put(MongoDbConnectorConfig.FIELD_RENAMES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.CUSTOM_CONVERTERS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.PROVIDE_TRANSACTION_METADATA.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.SANITIZE_FIELD_NAMES.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.CONNECTOR_ADVANCED));

        // Advanced configs (aka Runtime configs based on the PoC Requirements document
        additionalMetadata.put(MongoDbConnectorConfig.SKIPPED_OPERATIONS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.EVENT_PROCESSING_FAILURE_HANDLING_MODE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED, enumArrayToList(MongoDbConnectorConfig.EventProcessingFailureHandlingMode.values())));
        additionalMetadata.put(MongoDbConnectorConfig.MAX_BATCH_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.MAX_QUEUE_SIZE.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.POLL_INTERVAL_MS.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));
        additionalMetadata.put(MongoDbConnectorConfig.RETRIABLE_RESTART_WAIT.name(), new AdditionalPropertyMetadata(false, ConnectorProperty.Category.ADVANCED));

        MONGODB_PROPERTIES =  Collections.unmodifiableMap(additionalMetadata);
    }

    protected ConnectionContext.MongoPrimary primary(MongoDbTaskContext context) throws Throwable {
        ReplicaSet replicaSet = ReplicaSet.parse(context.getConnectionContext().hosts());
        return context.getConnectionContext().primaryFor(replicaSet, context.filters(), (s, throwable) -> {
            throw new DebeziumException(s, throwable);
        });
    }

    protected <T extends DataCollectionId> Stream<T> determineDataCollectionsToBeSnapshotted(
            CommonConnectorConfig connectorConfig, final Collection<T> allDataCollections) {
        final Set<Pattern> snapshotAllowedDataCollections = connectorConfig.getDataCollectionsToBeSnapshotted();
        if (snapshotAllowedDataCollections.size() == 0) {
            return allDataCollections.stream();
        }
        else {
            return allDataCollections.stream()
                    .filter(dataCollectionId -> snapshotAllowedDataCollections.stream()
                            .anyMatch(s -> s.matcher(dataCollectionId.identifier()).matches()));
        }
    }

    @Override
    public FilterValidationResult validateFilters(Map<String, String> properties) {
        PropertiesValidationResult result = validateProperties(properties);
        if (result.status == PropertiesValidationResult.Status.INVALID) {
            return FilterValidationResult.invalid(result.propertyValidationResults);
        }

        Configuration propertiesConfig = Configuration.from(properties);
        MongoDbTaskContext context = new MongoDbTaskContext(propertiesConfig);

        MongoDbConnectorConfig config = new MongoDbConnectorConfig(propertiesConfig);
        List<CollectionId> collections;
        try {
            collections = determineDataCollectionsToBeSnapshotted(config, primary(context).collections()).collect(Collectors.toList());
        }
        catch (Throwable throwable) {
            throw new DebeziumException(throwable);
        }

        List<DataCollection> matchingTables = collections.stream()
                .map(collectionId -> new DataCollection(collectionId.replicaSetName() + "." + collectionId.dbName(), collectionId.name()))
                .collect(Collectors.toList());

        return FilterValidationResult.valid(matchingTables);
    }

    @Override
    protected ConnectorDescriptor getConnectorDescriptor() {
        return new ConnectorDescriptor("mongodb", "MongoDB", true);
    }

    @Override
    public Map<String, AdditionalPropertyMetadata> allPropertiesWithAdditionalMetadata() {
        return MONGODB_PROPERTIES;
    }

    @Override
    public Field.Set getAllConnectorFields() {
        return MongoDbConnectorConfig.ALL_FIELDS;
    }

    @Override
    protected SourceConnector getConnector() {
        return new MongoDbConnector();
    }

}
