/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.service;

import java.util.Map;

import io.debezium.config.Field;
import io.debezium.configserver.model.AdditionalPropertyMetadata;
import io.debezium.configserver.model.ConnectionValidationResult;
import io.debezium.configserver.model.ConnectorDefinition;
import io.debezium.configserver.model.ConnectorType;
import io.debezium.configserver.model.FilterValidationResult;
import io.debezium.configserver.model.PropertiesValidationResult;

public interface ConnectorIntegrator {

    ConnectorType getConnectorType();

    ConnectorDefinition getConnectorDefinition();

    Map<String, AdditionalPropertyMetadata> allPropertiesWithAdditionalMetadata();

    /**
     * Validates the set of connection-related properties.
     */
    ConnectionValidationResult validateConnection(Map<String, String> properties);

    /**
     * Returns ALL_FIELDS from a ConnectorConfig that should be validated.
     */
    Field.Set getAllConnectorFields();

    /**
     * Validates the set of filter-related properties and returns the matching data collection(s).
     */
    FilterValidationResult validateFilters(Map<String, String> properties);

    /**
     * Validates an arbitrary set of connector properties.
     */
    PropertiesValidationResult validateProperties(Map<String, String> properties);
}
