/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.rest;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import io.debezium.configserver.model.ConnectConnectorConfigResponse;
import io.debezium.configserver.model.ConnectorStatus;
import io.debezium.configserver.model.KafkaConnectClusterList;
import io.debezium.configserver.rest.client.InvalidClusterException;
import io.debezium.configserver.rest.client.KafkaConnectClientFactory;
import io.debezium.configserver.rest.client.KafkaConnectException;
import io.debezium.configserver.service.StacktraceHelper;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.debezium.DebeziumException;
import io.debezium.configserver.model.ConnectionValidationResult;
import io.debezium.configserver.model.ConnectorType;
import io.debezium.configserver.model.ConnectorDefinition;
import io.debezium.configserver.model.FilterValidationResult;
import io.debezium.configserver.model.PropertiesValidationResult;
import io.debezium.configserver.rest.client.KafkaConnectClient;
import io.debezium.configserver.rest.model.BadRequestResponse;
import io.debezium.configserver.rest.model.ServerError;
import io.debezium.configserver.service.ConnectorIntegrator;

@Path(ConnectorURIs.API_PREFIX)
public class ConnectorResource {

    private static final Logger LOGGER = Logger.getLogger(ConnectorResource.class);

    private final Map<String, ConnectorIntegrator> integrators;

    public ConnectorResource() {
        Map<String, ConnectorIntegrator> integrators = new HashMap<>();

        ServiceLoader.load(ConnectorIntegrator.class)
                .forEach(integrator -> integrators.put(integrator.getConnectorType().id, integrator));

        this.integrators = Collections.unmodifiableMap(integrators);
    }

    @Path(ConnectorURIs.CONNECT_CLUSTERS_ENDPOINT)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = KafkaConnectClusterList.class)
            ))
    @APIResponse(
            responseCode = "500",
            description = "Exception during Kafka Connect URI validation",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    public Response getClusters() throws InvalidClusterException {
        return Response.ok(KafkaConnectClientFactory.getAllKafkaConnectClusters()).build();
    }

    @Path(ConnectorURIs.CONNECTOR_TYPES_ENDPOINT)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ConnectorDefinition> getConnectorTypes() {
        return integrators.values()
                .stream()
                .map(ConnectorIntegrator::getConnectorDefinition)
                .collect(Collectors.toList());
    }

    @Path(ConnectorURIs.CONNECTOR_TYPES_ENDPOINT_FOR_CONNECTOR)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ConnectorType.class)
            ))
    @APIResponse(
            responseCode = "400",
            description = "Invalid connector type provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = BadRequestResponse.class)
            ))
    public Response getConnectorTypes(@PathParam("id") String connectorTypeId) {
        if (null == connectorTypeId || "".equals(connectorTypeId)) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new BadRequestResponse("You have to specify a connector type!"))
                    .build();
        }

        ConnectorIntegrator integrator = integrators.get(connectorTypeId);

        if (integrator == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new BadRequestResponse("Unknown connector type: " + connectorTypeId))
                    .build();
        }

        return Response.ok(integrator.getConnectorType()).build();
    }

    private Map<String, String> convertPropertiesToStrings(Map<String, ?> properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
    }

    @Path(ConnectorURIs.CONNECTION_VALIDATION_ENDPOINT)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ConnectionValidationResult.class)
            ))
    @APIResponse(
            responseCode = "400",
            description = "Invalid connector type provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = BadRequestResponse.class)
            ))
    public Response validateConnectionProperties(@PathParam("id") String connectorTypeId, Map<String, ?> properties) {
        ConnectorIntegrator integrator = integrators.get(connectorTypeId);

        if (integrator == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new BadRequestResponse("Unknown connector type: " + connectorTypeId))
                    .build();
        }

        ConnectionValidationResult validationResult = integrator.validateConnection(convertPropertiesToStrings(properties));

        return Response.ok(validationResult)
                .build();
    }

    @Path(ConnectorURIs.FILTERS_VALIDATION_ENDPOINT)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = FilterValidationResult.class)
            ))
    @APIResponse(
            responseCode = "400",
            description = "Invalid connector type provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = BadRequestResponse.class)
            ))
    @APIResponse(
            responseCode = "500",
            description = "Exception during validation",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    public Response validateFilters(@PathParam("id") String connectorTypeId, Map<String, ?> properties) {
        ConnectorIntegrator integrator = integrators.get(connectorTypeId);

        if (integrator == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new BadRequestResponse("Unknown connector type: " + connectorTypeId))
                    .build();
        }

        try {
            FilterValidationResult validationResult = integrator.validateFilters(convertPropertiesToStrings(properties));

            return Response.ok(validationResult)
                    .build();
        }
        catch(DebeziumException e) {
            e.printStackTrace();
            return Response.serverError()
                    .entity(new ServerError("Failed to apply table filters", StacktraceHelper.traceAsString(e)))
                    .build();
        }
    }

    @Path(ConnectorURIs.PROPERTIES_VALIDATION_ENDPOINT)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PropertiesValidationResult.class)
            ))
    @APIResponse(
            responseCode = "400",
            description = "Invalid connector type provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = BadRequestResponse.class)
            ))
    public Response validateConnectorProperties(@PathParam("id") String connectorTypeId, Map<String, ?> properties) {
        ConnectorIntegrator integrator = integrators.get(connectorTypeId);

        if (integrator == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new BadRequestResponse("Unknown connector type: " + connectorTypeId))
                    .build();
        }

        PropertiesValidationResult validationResult = integrator.validateProperties(convertPropertiesToStrings(properties));

        return Response.ok(validationResult)
                .build();
    }

    @Path(ConnectorURIs.CREATE_CONNECTOR_ENDPOINT)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = FilterValidationResult.class)
            ))
    @APIResponse(
            responseCode = "400",
            description = "Missing or invalid properties or invalid connector type provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = BadRequestResponse.class)
            ))
    @APIResponse(
            responseCode = "500",
            description = "Exception during Kafka Connect URI validation",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    @APIResponse(
            responseCode = "503",
            description = "Exception while trying to connect to the selected Kafka Connect cluster",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    public Response createConnector(
            @PathParam("cluster") int cluster,
            @PathParam("connector-type-id") String connectorTypeId,
            ConnectConnectorConfigResponse kafkaConnectConfig
            ) throws KafkaConnectClientException, KafkaConnectException {
        if (kafkaConnectConfig.getConfig() == null || kafkaConnectConfig.getConfig().isEmpty()) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new BadRequestResponse("Connector \"config\" property is not set!"))
                    .build();
        }

        if (null == kafkaConnectConfig.getName() || kafkaConnectConfig.getName().isBlank()) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new BadRequestResponse("Connector \"name\" property is not set!"))
                    .build();
        }

        ConnectorIntegrator integrator = integrators.get(connectorTypeId);
        if (integrator == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new BadRequestResponse("Unknown connector type: " + connectorTypeId))
                    .build();
        }

        PropertiesValidationResult validationResult = integrator.validateProperties(kafkaConnectConfig.getConfig());

        if (validationResult.status == PropertiesValidationResult.Status.INVALID) {
            return Response.status(Status.BAD_REQUEST).entity(validationResult).build();
        }

        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        kafkaConnectConfig.getConfig().put("connector.class", integrator.getConnectorType().className);

        String result;
        LOGGER.debug("Sending valid connector config: " + kafkaConnectConfig.getConfig());
        try {
            result = kafkaConnectClient.createConnector(kafkaConnectConfig);
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }
        LOGGER.debug("Kafka Connect response: " + result);

        return Response.ok(result).build();
    }

    @Path(ConnectorURIs.LIST_CONNECTORS_ENDPOINT)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ConnectorStatus.class, type = SchemaType.ARRAY)
            ))
    @APIResponse(
            responseCode = "500",
            description = "Exception during Kafka Connect URI validation",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    @APIResponse(
            responseCode = "503",
            description = "Exception while trying to connect to the selected Kafka Connect cluster",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    public Response listConnectors(@PathParam("cluster") int cluster)
            throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        List<String> activeConnectors;
        try {
            activeConnectors = kafkaConnectClient.listConnectors();
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }

        LOGGER.debug("Kafka Connect response: " + activeConnectors);

        List<ConnectorStatus> connectorData = Collections.emptyList();
        if (!activeConnectors.isEmpty()) {
            connectorData = activeConnectors.stream().map(
                            connectorName -> {
                                try {
                                    var connectorInfo = kafkaConnectClient.getConnectorInfo(connectorName);
                                    String connectorType = connectorInfo.getConfig().get("connector.class");
                                    if (!connectorType.startsWith("io.debezium")) {
                                        return null;
                                    }
                                    LOGGER.debug("Kafka Connect connector status details: " + connectorInfo);
                                    var connectorStatus = kafkaConnectClient.getConnectorStatus(connectorName);
                                    var connectorState = new ConnectorStatus(connectorName);
                                    connectorState.setConnectorType(connectorType);
                                    connectorState.setConnectorStatus(connectorStatus.connectorStatus.status);
                                    connectorStatus.taskStates.forEach(
                                            taskStatus -> connectorState.setTaskState(
                                                    taskStatus.id,
                                                    taskStatus.status,
                                                    (taskStatus.getErrorsAsList() != null
                                                            ? taskStatus.getErrorsAsList().stream().filter(s -> s.startsWith("Caused by:")).collect(Collectors.toList())
                                                            : null
                                                    )
                                                ));
                                    return connectorState;
                                }
                                catch (ProcessingException | IOException e) {
                                    LOGGER.error(e.getMessage(), e);
                                }
                                return null;
                            }).collect(Collectors.toList());
        }

        LOGGER.debug("Registered Connectors: " + connectorData);

        return Response.ok(connectorData).build();
    }

    @Path(ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT)
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "204",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "404",
            description = "Connector with specified name not found")
    @APIResponse(
            responseCode = "500",
            description = "Exception during Kafka Connect URI validation",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    @APIResponse(
            responseCode = "503",
            description = "Exception while trying to connect to the selected Kafka Connect cluster",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    public Response deleteConnector(
            @PathParam("cluster") int cluster,
            @PathParam("connector-name") String connectorName
    ) throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        Response deleteResponse;
        try {
            Response originalKafkaConnectResponse = kafkaConnectClient.deleteConnector(connectorName);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Kafka Connect response: " + originalKafkaConnectResponse.readEntity(String.class));
            }
            deleteResponse = Response.fromResponse(originalKafkaConnectResponse).type(MediaType.APPLICATION_JSON).build();
            originalKafkaConnectResponse.close();
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }
        return deleteResponse;
    }

    @Path(ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT)
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "404",
            description = "Connector with specified name not found")
    @APIResponse(
            responseCode = "500",
            description = "Exception during Kafka Connect URI validation",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    @APIResponse(
            responseCode = "503",
            description = "Exception while trying to connect to the selected Kafka Connect cluster",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    public Response updateConnectorConfig(
            @PathParam("cluster") int cluster,
            @PathParam("connector-name") String connectorName,
            Map<String, String> updatedConfig
    ) throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        try {
            Response originalKafkaConnectResponse = kafkaConnectClient.updateConnectorConfig(connectorName, updatedConfig);
            originalKafkaConnectResponse.bufferEntity();
            Response updateResponse = Response.fromResponse(originalKafkaConnectResponse).type(MediaType.APPLICATION_JSON).build();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Kafka Connect response: " + originalKafkaConnectResponse.readEntity(String.class));
            }
            originalKafkaConnectResponse.close();
            return updateResponse;
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }
    }

    @Path(ConnectorURIs.CONNECTOR_CONFIG_ENDPOINT)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "204",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "404",
            description = "Connector with specified name not found")
    @APIResponse(
            responseCode = "500",
            description = "Exception during Kafka Connect URI validation",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    @APIResponse(
            responseCode = "503",
            description = "Exception while trying to connect to the selected Kafka Connect cluster",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServerError.class)
            ))
    public Response getConnectorConfig(
            @PathParam("cluster") int cluster,
            @PathParam("connector-name") String connectorName
    ) throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        Response connectorConfigResponse;
        try {
            Response originalConfigResponse = kafkaConnectClient.getConnectorConfig(connectorName);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Kafka Connect response: " + originalConfigResponse.readEntity(String.class));
            }

            final Map configResponse = originalConfigResponse.readEntity(Map.class);
            if (!configResponse.containsKey("connector.class")) {
                throw new ProcessingException("Failed to locate connector.class in response");
            }

            Optional<ConnectorDefinition> definition = getConnectorDefinition((String) configResponse.get("connector.class"));
            if (definition.isPresent()) {
                configResponse.put("connector.displayName", definition.get().displayName);
                configResponse.put("connector.id", definition.get().id);
            }

            connectorConfigResponse = Response.ok().entity(configResponse).type(MediaType.APPLICATION_JSON).build();
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }
        return connectorConfigResponse;
    }

    private Optional<ConnectorDefinition> getConnectorDefinition(String className) {
        for (ConnectorDefinition definition : getConnectorTypes()) {
            if (definition.className.equals(className)) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }
}
