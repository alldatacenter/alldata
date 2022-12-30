/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.debezium.config.ConfigDefinition;
import io.debezium.config.EnumeratedValue;
import io.debezium.configserver.model.AdditionalPropertyMetadata;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.ConfigKey;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.source.SourceConnector;

import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.configserver.model.ConnectionValidationResult;
import io.debezium.configserver.model.ConnectorDefinition;
import io.debezium.configserver.model.ConnectorProperty;
import io.debezium.configserver.model.ConnectorType;
import io.debezium.configserver.model.GenericValidationResult;
import io.debezium.configserver.model.PropertiesValidationResult;
import io.debezium.configserver.model.PropertyValidationResult;
import io.debezium.relational.HistorizedRelationalDatabaseConnectorConfig;
import io.debezium.storage.kafka.history.KafkaSchemaHistory;

public abstract class ConnectorIntegratorBase implements ConnectorIntegrator {

    protected abstract ConnectorDescriptor getConnectorDescriptor();

    protected abstract SourceConnector getConnector();

    public static List<String> enumArrayToList(EnumeratedValue[] input) {
        List<String> result = new ArrayList<>();
        for (EnumeratedValue value : input) {
            result.add(value.getValue());
        }
        return result;
    }

    @Override
    public Map<String, AdditionalPropertyMetadata> allPropertiesWithAdditionalMetadata() {
        AdditionalPropertyMetadata defaultMetadata = new AdditionalPropertyMetadata(false, ConnectorProperty.Category.FILTERS);
        return getConnector().config().configKeys().values()
                .stream().collect(Collectors.toMap(configKey -> configKey.name, configKey -> defaultMetadata));
    }

    @Override
    public ConnectorDefinition getConnectorDefinition() {
        ConnectorDescriptor descriptor = getConnectorDescriptor();
        SourceConnector instance = getConnector();

        return new ConnectorDefinition(
            descriptor.id,
            instance.getClass().getName(),
            descriptor.name,
            instance.version(),
            descriptor.enabled
        );
    }

    @Override
    public ConnectorType getConnectorType() {
        ConnectorDescriptor descriptor = getConnectorDescriptor();
        SourceConnector instance = getConnector();

        Map<String, ConnectorProperty> properties = instance.config()
            .configKeys()
            .values()
            .stream()
            .filter(configKey -> allPropertiesWithAdditionalMetadata().containsKey(configKey.name))
            .filter(property -> !property.name.startsWith("internal"))
            .map(this::toConnectorProperty)
            .collect(Collectors.toMap(connectorProperty -> connectorProperty.name, connectorProperty -> connectorProperty));

        if (instance.config().configKeys().containsKey(HistorizedRelationalDatabaseConnectorConfig.SCHEMA_HISTORY.name())) {
            // todo: how to support non-Kafka storage modules?
            properties.putAll(ConfigDefinition.editor()
                .history(KafkaSchemaHistory.ALL_FIELDS.asArray())
                .create()
                .configDef()
                .configKeys()
                .values()
                .stream()
                .filter(property -> !property.name.startsWith("internal"))
                .map(this::toConnectorProperty)
                .collect(Collectors.toMap(connectorProperty -> connectorProperty.name, connectorProperty -> connectorProperty)));
        }

        // apply sorting of properties provided by {@link #allPropertiesWithAdditionalMetadata()}
        ArrayList<ConnectorProperty> sortedProperties = allPropertiesWithAdditionalMetadata()
            .keySet()
            .stream()
            .map(properties::get)
            .filter(prop -> prop != null)
            .collect(Collectors.toCollection(ArrayList::new));

        return new ConnectorType(
                descriptor.id,
                instance.getClass().getName(),
                descriptor.name,
                instance.version(),
                descriptor.enabled,
                sortedProperties
        );
    }

    @Override
    public ConnectionValidationResult validateConnection(Map<String, String> properties) {
        SourceConnector instance = getConnector();

        try {
            Config result = instance.validate(properties);
            List<PropertyValidationResult> propertyResults = toPropertyValidationResults(result);

            return propertyResults.isEmpty() ? ConnectionValidationResult.valid() : ConnectionValidationResult.invalid(propertyResults);
        }
        catch(Exception e) {
            return ConnectionValidationResult.invalid(Collections.emptyList(), Collections.singletonList(new GenericValidationResult(e.getMessage(), traceAsString(e))));
        }
    }

    public PropertiesValidationResult validateProperties(Map<String, String> properties) {
        List<Field> fields = new ArrayList<>();
        getAllConnectorFields().forEach(field -> {
            if (properties.containsKey(field.name())) {
                fields.add(field);
            }
        });

        Configuration config = Configuration.from(properties);
        Map<String, ConfigValue> results = config.validate(Field.setOf(fields));
        Config result = new Config(new ArrayList<>(results.values()));

        List<PropertyValidationResult> propertyResults = toPropertyValidationResults(result);

        return propertyResults.isEmpty() ? PropertiesValidationResult.valid() : PropertiesValidationResult.invalid(propertyResults);
    }

    private List<PropertyValidationResult> toPropertyValidationResults(Config result) {
        return result.configValues()
                .stream()
                .filter(cv -> !cv.errorMessages().isEmpty())
                .filter(cv -> !cv.errorMessages().get(0).equals(cv.name() + " is referred in the dependents, but not defined."))
                .map(cv -> new PropertyValidationResult(cv.name(), cv.errorMessages().get(0)))
                .collect(Collectors.toList());
    }

    private String traceAsString(Exception e) {
        return e.getStackTrace() != null && e.getStackTrace().length > 0 ? Arrays.toString(e.getStackTrace()) : null;
    }

    private ConnectorProperty toConnectorProperty(ConfigKey configKey) {
        boolean isMandatory = false;
        ConnectorProperty.Category category = ConnectorProperty.Category.CONNECTOR;
        List<String> allowedValues = null;

        AdditionalPropertyMetadata additionalMetadata = allPropertiesWithAdditionalMetadata().get(configKey.name);
        if (additionalMetadata != null) {
            isMandatory = additionalMetadata.isMandatory;
            category = additionalMetadata.category;
            allowedValues = additionalMetadata.allowedValues;
        }

        return new ConnectorProperty(
                configKey.name,
                configKey.displayName,
                configKey.documentation,
                toConnectorPropertyType(configKey.type()),
                configKey.defaultValue,
                isMandatory,
                category,
                allowedValues
        );
    }

    private ConnectorProperty.Type toConnectorPropertyType(ConfigDef.Type type) {
        switch(type) {
            case BOOLEAN:
                return ConnectorProperty.Type.BOOLEAN;
            case CLASS:
                return ConnectorProperty.Type.CLASS;
            case DOUBLE:
                return ConnectorProperty.Type.DOUBLE;
            case INT:
                return ConnectorProperty.Type.INT;
            case LIST:
                return ConnectorProperty.Type.LIST;
            case LONG:
                return ConnectorProperty.Type.LONG;
            case PASSWORD:
                return ConnectorProperty.Type.PASSWORD;
            case SHORT:
                return ConnectorProperty.Type.SHORT;
            case STRING:
                return ConnectorProperty.Type.STRING;
            default:
                throw new IllegalArgumentException("Unsupported property type: " + type);
        }
    }

    public static class ConnectorDescriptor {
        public String id;
        public String name;
        public boolean enabled;

        public ConnectorDescriptor(String id, String name, boolean enabled) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
        }
    }
}
