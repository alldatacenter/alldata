/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.MySqlInfrastructureTestProfile;
import io.debezium.testing.testcontainers.Connector;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(MySqlInfrastructureTestProfile.class)
public class CreateAndDeleteMySqlConnectorIT {

    @Test
    public void testMySqlClustersEndpoint() {
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
    public void testMySqlCreateConnectorEndpoint() {
        Connector connector = Connector.from("my-mysql-connector", Infrastructure.getMySqlConnectorConfiguration(1));

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(connector.toJson())
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CREATE_CONNECTOR_ENDPOINT, 1, "mysql")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("name", equalTo("my-mysql-connector"))
            .and().rootPath("config")
                .body("['connector.class']", equalTo("io.debezium.connector.mysql.MySqlConnector"))
                .and().body("['database.hostname']", equalTo(Infrastructure.getMySqlContainer().getContainerInfo().getConfig().getHostName()));
    }

    @Test
    public void testMySqlDeleteConnectorFailed() {
        Infrastructure.getDebeziumContainer().deleteAllConnectors();
        given()
                .when().delete(ConnectorURIs.API_PREFIX + ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT, 1, "wrong-connector-name-123")
                .then().log().all()
                .statusCode(404)
                .body("size()", is(2))
                .body("error_code", is(404))
                .body("message", equalTo("Connector wrong-connector-name-123 not found"));
    }

    @Test
    public void testMySqlDeleteConnectorSuccessful() {
        final var deleteMySqlConnectorName = "delete-connector-mysql";
        Infrastructure.getDebeziumContainer().deleteAllConnectors();
        Infrastructure.getDebeziumContainer().registerConnector(
                deleteMySqlConnectorName,
                Infrastructure.getMySqlConnectorConfiguration(1)
        );
        Infrastructure.waitForConnectorTaskStatus(deleteMySqlConnectorName, 0, Connector.State.RUNNING);

        given()
                .when().delete(ConnectorURIs.API_PREFIX + ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT, 1, deleteMySqlConnectorName)
                .then().log().all()
                .statusCode(204);

        assertTrue(Infrastructure.getDebeziumContainer().connectorIsNotRegistered(deleteMySqlConnectorName));
    }

}
