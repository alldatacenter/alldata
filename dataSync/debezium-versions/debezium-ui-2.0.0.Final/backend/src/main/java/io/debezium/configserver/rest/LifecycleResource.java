/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.rest;

import io.debezium.configserver.model.ConnectionValidationResult;
import io.debezium.configserver.rest.client.KafkaConnectClient;
import io.debezium.configserver.rest.client.KafkaConnectClientFactory;
import io.debezium.configserver.rest.client.KafkaConnectException;
import io.debezium.configserver.rest.model.NotFoundResponse;
import io.debezium.configserver.rest.model.ServerError;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;

@Path(ConnectorURIs.API_PREFIX)
public class LifecycleResource {

    private static final Logger LOGGER = Logger.getLogger(LifecycleResource.class);

    @Path(ConnectorURIs.CONNECTOR_PAUSE_ENDPOINT)
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ConnectionValidationResult.class)
            ))
    @APIResponse(
            responseCode = "404",
            description = "Invalid connector name provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = NotFoundResponse.class)
            ))
    @APIResponse(
            responseCode = "500",
            description = "Exception during action",
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
    public Response pauseConnector(
            @PathParam("cluster") int cluster,
            @PathParam("connectorname") String connectorName
    ) throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        Response result;
        try {
            result = kafkaConnectClient.pauseConnector(connectorName);
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }
        LOGGER.debug("Kafka Connect response: " + result.readEntity(String.class));

        return result;
    }

    @Path(ConnectorURIs.CONNECTOR_RESUME_ENDPOINT)
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ConnectionValidationResult.class)
            ))
    @APIResponse(
            responseCode = "404",
            description = "Invalid connector name provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = NotFoundResponse.class)
            ))
    @APIResponse(
            responseCode = "500",
            description = "Exception during action",
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
    public Response resumeConnector(
            @PathParam("cluster") int cluster,
            @PathParam("connectorname") String connectorName
    ) throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        Response result;
        try {
            result = kafkaConnectClient.resumeConnector(connectorName);
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }
        LOGGER.debug("Kafka Connect response: " + result.readEntity(String.class));

        return result;
    }

    @Path(ConnectorURIs.CONNECTOR_RESTART_ENDPOINT)
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
            responseCode = "404",
            description = "Invalid connector name provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = NotFoundResponse.class)
            ))
    @APIResponse(
            responseCode = "409",
            description = "Could not restart while a rebelancing is in progress",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Exception during action",
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
    public Response restartConnector(
            @PathParam("cluster") int cluster,
            @PathParam("connectorname") String connectorName
    ) throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        Response result;
        try {
            result = kafkaConnectClient.restartConnector(connectorName);
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }
        LOGGER.debug("Kafka Connect response: " + result.readEntity(String.class));

        return Response.fromResponse(result).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Path(ConnectorURIs.CONNECTOR_TASK_RESTART_ENDPOINT)
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
            responseCode = "404",
            description = "Invalid connector name provided",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = NotFoundResponse.class)
            ))
    @APIResponse(
            responseCode = "409",
            description = "Could not restart while a rebelancing is in progress",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Exception during action",
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
    public Response restartTask(
            @PathParam("cluster") int cluster,
            @PathParam("connectorname") String connectorName,
            @PathParam("tasknumber") int taskNumber
    ) throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        Response result;
        try {
            result = kafkaConnectClient.restartConnectorTask(connectorName, taskNumber);
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }
        LOGGER.debug("Kafka Connect response: " + result.readEntity(String.class));

        return Response.fromResponse(result).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

}
