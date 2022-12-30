/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver.rest;

import io.debezium.configserver.model.TransformsInfo;
import io.debezium.configserver.rest.client.KafkaConnectClient;
import io.debezium.configserver.rest.client.KafkaConnectClientFactory;
import io.debezium.configserver.rest.client.KafkaConnectException;
import io.debezium.configserver.rest.model.ServerError;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Path(ConnectorURIs.API_PREFIX)
public class KafkaConnectResource {

    private static final Logger LOGGER = Logger.getLogger(KafkaConnectResource.class);

    public static final List<String> ENABLED_TRANSFORMS = Arrays.asList(
            "io.debezium.transforms.ByLogicalTableRouter",
            "io.debezium.transforms.ExtractNewRecordState",
            "io.debezium.transforms.ContentBasedRouter",
            "io.debezium.transforms.Filter",
            "org.apache.kafka.connect.transforms.ValueToKey",
            "org.apache.kafka.connect.transforms.TimestampRouter");

    @Path(ConnectorURIs.TOPIC_CREATION_ENABLED)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Boolean.class)
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
    public Response isKafkaConnectTopicCreationEnabled(@PathParam("cluster") int cluster)
            throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        Boolean isTopicCreationEnabled;
        try {
            isTopicCreationEnabled = kafkaConnectClient.isTopicCreationEnabled();
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }
        LOGGER.debug("Kafka Connect \"/debezium/topic-creation\" response: " + isTopicCreationEnabled);

        return Response.ok(isTopicCreationEnabled).build();
    }


    @Path(ConnectorURIs.TRANSFORMS_LIST)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TransformsInfo.class, type = SchemaType.ARRAY)
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
    public List<TransformsInfo> listTransforms(@PathParam("cluster") int cluster)
            throws KafkaConnectClientException, KafkaConnectException {
        URI kafkaConnectURI = KafkaConnectClientFactory.getKafkaConnectURIforCluster(cluster);
        KafkaConnectClient kafkaConnectClient = KafkaConnectClientFactory.getClient(cluster);

        List<TransformsInfo> transforms;
        try {
            transforms = kafkaConnectClient.listTransforms();
        }
        catch (ProcessingException | IOException e) {
            throw new KafkaConnectClientException(kafkaConnectURI, e);
        }

        LOGGER.debug("All SMTs registered in Kafka Connect: " + transforms.stream().map(TransformsInfo::getClassName).collect(Collectors.toUnmodifiableList()));

        transforms = transforms.stream().filter(transformsInfo -> ENABLED_TRANSFORMS.contains(transformsInfo.getClassName()))
                .collect(Collectors.toUnmodifiableList());

        LOGGER.debug("Found " + transforms.size() + " of " + ENABLED_TRANSFORMS.size() + " possible Kafka Connect SMTs: " + transforms);

        return transforms;
    }
}
