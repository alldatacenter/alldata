/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.rest;

public final class ConnectorURIs {
    public static final String API_PREFIX = "/api";
    public static final String CONNECT_CLUSTERS_ENDPOINT = "/connect-clusters";
    public static final String CONNECTOR_TYPES_ENDPOINT = "/connector-types";
    public static final String CONNECTOR_TYPES_ENDPOINT_FOR_CONNECTOR = "/connector-types/{id}";
    public static final String CONNECTION_VALIDATION_ENDPOINT = "/connector-types/{id}/validation/connection";
    public static final String FILTERS_VALIDATION_ENDPOINT = "/connector-types/{id}/validation/filters";
    public static final String PROPERTIES_VALIDATION_ENDPOINT = "/connector-types/{id}/validation/properties";
    public static final String CREATE_CONNECTOR_ENDPOINT = "/connector/{cluster}/{connector-type-id}";
    public static final String LIST_CONNECTORS_ENDPOINT = "/connectors/{cluster}";
    public static final String MANAGE_CONNECTORS_ENDPOINT = "/connectors/{cluster}/{connector-name}";
    public static final String CONNECTOR_PAUSE_ENDPOINT = "/connector/{cluster}/{connectorname}/pause";
    public static final String CONNECTOR_RESUME_ENDPOINT = "/connector/{cluster}/{connectorname}/resume";
    public static final String CONNECTOR_RESTART_ENDPOINT = "/connector/{cluster}/{connectorname}/restart";
    public static final String CONNECTOR_TASK_RESTART_ENDPOINT = "/connector/{cluster}/{connectorname}/task/{tasknumber}/restart";
    public static final String CONNECTOR_CONFIG_ENDPOINT = "/connectors/{cluster}/{connector-name}/config";
    public static final String TRANSFORMS_LIST = "/{cluster}/transforms.json";
    public static final String TOPIC_CREATION_ENABLED = "/{cluster}/topic-creation-enabled";
}
