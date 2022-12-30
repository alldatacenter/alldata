/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.debezium.configserver.model.ConnectorStatus;
import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.PostgresInfrastructureTestProfile;
import io.debezium.testing.testcontainers.Connector;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(PostgresInfrastructureTestProfile.class)
public class ListPostgresConnectorIT {

    @BeforeEach
    public void resetRunningConnectors() {
        Infrastructure.getDebeziumContainer().deleteAllConnectors();
    }

    @Test
    public void testListConnectorsEndpointEmpty() {
        given().when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.LIST_CONNECTORS_ENDPOINT, "1")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testListConnectorsEndpoint() {
        final var runningConnectorName = "list-connectors-postgres-connector";
        final var pausedConnectorName = "list-connectors-postgres-connector-paused";
        final var failedConnectorName = "list-connectors-postgres-connector-failed";
        Infrastructure.getDebeziumContainer().registerConnector(
                runningConnectorName,
                Infrastructure.getPostgresConnectorConfiguration(1));
        Infrastructure.waitForConnectorTaskStatus(runningConnectorName, 0, Connector.State.RUNNING);

        Infrastructure.getDebeziumContainer().registerConnector(
                pausedConnectorName,
                Infrastructure.getPostgresConnectorConfiguration(2)
                        .with("table.include.list", ".*"));
        Infrastructure.getDebeziumContainer().pauseConnector(pausedConnectorName);
        Infrastructure.waitForConnectorTaskStatus(pausedConnectorName, 0, Connector.State.PAUSED);

        given()
                .when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.LIST_CONNECTORS_ENDPOINT, "1")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(2))
                .rootPath("find { it.name == '"+ runningConnectorName + "' }")
                    .body("connectorStatus", equalTo(ConnectorStatus.State.RUNNING.toString()))
                    .body("connectorType", equalTo("postgres"))
                    .body("databaseName", equalTo("PostgreSQL"))
                    .body("name", equalTo(runningConnectorName))
                    .body("taskStates", equalTo(Map.of("0", Map.of("taskStatus", ConnectorStatus.State.RUNNING.toString()))))
                .rootPath("find { it.name == '"+ pausedConnectorName + "' }")
                    .body("connectorStatus", equalTo(ConnectorStatus.State.PAUSED.toString()))
                    .body("connectorType", equalTo("postgres"))
                    .body("databaseName", equalTo("PostgreSQL"))
                    .body("taskStates", equalTo(Map.of("0", Map.of("taskStatus", ConnectorStatus.State.PAUSED.toString()))))
                    .body("name", equalTo(pausedConnectorName));
    }

}
