/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.MongoDbInfrastructureTestProfile;
import io.debezium.testing.testcontainers.Connector;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(MongoDbInfrastructureTestProfile.class)
public class CreateAndDeleteMongoDbConnectorIT {

    @BeforeEach
    public void resetRunningConnectors() {
        Infrastructure.getDebeziumContainer().deleteAllConnectors();
    }

    @Test
    public void testMongoDbClustersEndpoint() {
        given()
                .when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECT_CLUSTERS_ENDPOINT)
                .then().log().all()
                .statusCode(200)
                .body("size()", is(1))
                .and().body(
                    "[0]",
                    equalTo("http://" + Infrastructure.getDebeziumContainer().getHost()
                            + ":" + Infrastructure.getDebeziumContainer().getMappedPort(8083)
                    )
        );
    }

    @Test
    public void testMongoDbCreateConnectorEndpoint() {
        Connector connector = Connector.from(
                "my-mongodb-connector",
                Infrastructure.getMongoDbConnectorConfiguration(1)
            );

        MongoDBContainer mongoDbContainer = Infrastructure.getMongoDbContainer();
        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(connector.toJson())
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CREATE_CONNECTOR_ENDPOINT, 1, "mongodb")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("name", equalTo("my-mongodb-connector"))
            .and().rootPath("config")
                .body("['connector.class']", equalTo("io.debezium.connector.mongodb.MongoDbConnector"))
                .and().body("['mongodb.hosts']",
                equalTo("rs0/"+ mongoDbContainer.getContainerInfo().getConfig().getHostName()
                    + ":" + mongoDbContainer.getExposedPorts().get(0)));
    }

    @Test
    public void testMongoDbDeleteConnectorFailed() {
        given()
                .when().delete(ConnectorURIs.API_PREFIX + ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT, 1, "wrong-connector-name-123")
                .then().log().all()
                .statusCode(404)
                .body("size()", is(2))
                .body("error_code", is(404))
                .body("message", equalTo("Connector wrong-connector-name-123 not found"));
    }

    @Test
    public void testMongoDbDeleteConnectorSuccessful() {
        final var deleteMongoDbConnectorName = "delete-connector-mongodb";
        Infrastructure.getDebeziumContainer().deleteAllConnectors();
        Infrastructure.getDebeziumContainer().registerConnector(
                deleteMongoDbConnectorName,
                Infrastructure.getMongoDbConnectorConfiguration(1));
        Infrastructure.getDebeziumContainer().ensureConnectorTaskState(
                deleteMongoDbConnectorName, 0, Connector.State.RUNNING);

        given()
                .when().delete(ConnectorURIs.API_PREFIX + ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT, 1, deleteMongoDbConnectorName)
                .then().log().all()
                .statusCode(204);

        assertTrue(Infrastructure.getDebeziumContainer().connectorIsNotRegistered(deleteMongoDbConnectorName));
    }

}
