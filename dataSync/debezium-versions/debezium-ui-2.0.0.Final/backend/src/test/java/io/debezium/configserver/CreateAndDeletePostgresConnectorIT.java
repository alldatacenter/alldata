/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.PostgresInfrastructureTestProfile;
import io.debezium.testing.testcontainers.Connector;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(PostgresInfrastructureTestProfile.class)
public class CreateAndDeletePostgresConnectorIT {

    @BeforeEach
    public void resetRunningConnectors() {
        Infrastructure.getDebeziumContainer().deleteAllConnectors();
    }

    @Test
    public void testPostgresClustersEndpoint() {
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
    public void testPostgresCreateConnectorEndpoint() {
        Connector connector = Connector.from(
                "my-postgres-connector",
                Infrastructure.getPostgresConnectorConfiguration(1)
                .with("slot.drop.on.stop", true)
            );

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(connector.toJson())
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CREATE_CONNECTOR_ENDPOINT, 1, "postgres")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("name", equalTo("my-postgres-connector"))
            .and().rootPath("config")
                .body("['connector.class']", equalTo("io.debezium.connector.postgresql.PostgresConnector"))
                .and().body("['database.hostname']", equalTo(Infrastructure.getPostgresContainer().getContainerInfo().getConfig().getHostName()));
    }

    @Test
    public void testPostgresDeleteConnectorFailed() {
        given()
                .when().delete(ConnectorURIs.API_PREFIX + ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT, 1, "wrong-connector-name-123")
                .then().log().all()
                .statusCode(404)
                .body("size()", is(2))
                .body("error_code", is(404))
                .body("message", equalTo("Connector wrong-connector-name-123 not found"));
    }

    @Test
    public void testPostgresDeleteConnectorSuccessful() {
        final var deletePostgresConnectorName = "delete-connector-postgres";
        Infrastructure.getDebeziumContainer().registerConnector(
                deletePostgresConnectorName,
                Infrastructure.getPostgresConnectorConfiguration(1));

        // It is possible the connector has not fully started and the tasks array returns
        // no running tasks, leading to a potential NPE with this call.
        Infrastructure.waitForConnectorTaskStatus(deletePostgresConnectorName, 0, Connector.State.RUNNING);

        given()
                .when().delete(ConnectorURIs.API_PREFIX + ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT, 1, deletePostgresConnectorName)
                .then().log().all()
                .statusCode(204);

        assertTrue(Infrastructure.getDebeziumContainer().connectorIsNotRegistered(deletePostgresConnectorName));
    }

}
