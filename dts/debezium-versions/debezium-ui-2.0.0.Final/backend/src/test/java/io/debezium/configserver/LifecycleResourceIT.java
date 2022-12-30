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
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(PostgresInfrastructureTestProfile.class)
public class LifecycleResourceIT {

    @BeforeEach
    public void resetRunningConnectors() {
        Infrastructure.getDebeziumContainer().deleteAllConnectors();
    }
    @Test
    public void testPauseAndResumePostgresConnector() {
        final var connectorName = "pause-postgres-connector";
        Infrastructure.getDebeziumContainer().registerConnector(
                connectorName,
                Infrastructure.getPostgresConnectorConfiguration(1));

        given()
                .when().put(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_PAUSE_ENDPOINT, 1, connectorName)
                .then().log().all()
                .statusCode(202)
                .body(is(""));

        try {
            Infrastructure.getDebeziumContainer().ensureConnectorState(connectorName, Connector.State.PAUSED);
        }
        catch (ConditionTimeoutException e) {
            Assertions.fail("Connector did not pause within configured timeout.");
        }
        Assertions.assertEquals(Connector.State.PAUSED, Infrastructure.getDebeziumContainer().getConnectorState(connectorName));

        given()
                .when().put(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_RESUME_ENDPOINT, 1, connectorName)
                .then().log().all()
                .statusCode(202)
                .body(is(""));

        try {
            Infrastructure.getDebeziumContainer().ensureConnectorState(connectorName, Connector.State.RUNNING);
        }
        catch (ConditionTimeoutException e) {
            Assertions.fail("Connector did not pause within configured timeout.");
        }
        Assertions.assertEquals(Connector.State.RUNNING, Infrastructure.getDebeziumContainer().getConnectorState(connectorName));
    }

    @Test
    public void testPausePostgresConnectorNotFound() {
        final var connectorName = "not-found-postgres-connector-pause";
        given()
                .when().put(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_PAUSE_ENDPOINT, 1, connectorName)
                .then().log().all()
                .statusCode(404)
                .body("error_code", is(404))
                .body("message", is("Unknown connector " + connectorName));
    }

    @Test
    public void testResumePostgresConnectorNotFound() {
        final var connectorName = "not-found-postgres-connector-resume";
        given()
                .when().put(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_RESUME_ENDPOINT, 1, connectorName)
                .then().log().all()
                .statusCode(404)
                .body("error_code", is(404))
                .body("message", is("Unknown connector " + connectorName));
    }

    @Test
    public void testRestartPostgresConnector() {
        final var connectorName = "restart-postgres-connector";
        Infrastructure.getDebeziumContainer().registerConnector(
                connectorName,
                Infrastructure.getPostgresConnectorConfiguration(1));

        Infrastructure.getDebeziumContainer().ensureConnectorState(connectorName, Connector.State.RUNNING);

        given()
                .when().log().all()
                .accept(ContentType.JSON).contentType(ContentType.JSON)
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_RESTART_ENDPOINT, 1, connectorName)
                .then().log().all()
                .statusCode(204)
                .body(is(""));

        Infrastructure.getDebeziumContainer().ensureConnectorState(connectorName, Connector.State.RUNNING);
        Assertions.assertEquals(Connector.State.RUNNING, Infrastructure.getDebeziumContainer().getConnectorState(connectorName));
    }

    @Test
    public void testRestartPostgresConnectorNotFound() {
        final var connectorName = "not-found-postgres-connector-restart";
        given()
                .when()
                .accept(ContentType.JSON).contentType(ContentType.JSON)
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_RESTART_ENDPOINT, 1, connectorName)
                .then().log().all()
                .statusCode(404)
                .body("error_code", is(404))
                .body("message", is("Unknown connector: " + connectorName));
    }

    @Test
    public void testRestartPostgresConnectorTask() {
        final var connectorName = "restart-postgres-connector-task";
        Infrastructure.getDebeziumContainer().registerConnector(
                connectorName,
                Infrastructure.getPostgresConnectorConfiguration(1));

        Infrastructure.getDebeziumContainer().ensureConnectorState(connectorName, Connector.State.RUNNING);
        Infrastructure.waitForConnectorTaskStatus(connectorName, 0, Connector.State.RUNNING);

        given()
                .when().log().all()
                .accept(ContentType.JSON).contentType(ContentType.JSON)
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_TASK_RESTART_ENDPOINT, 1, connectorName, 0)
                .then().log().all()
                .statusCode(204)
                .body(is(""));

        Infrastructure.waitForConnectorTaskStatus(connectorName, 0, Connector.State.RUNNING);
        Assertions.assertEquals(Connector.State.RUNNING, Infrastructure.getDebeziumContainer().getConnectorTaskState(connectorName, 0));
    }

    @Test
    public void testRestartPostgresConnectorTaskNotFound() {
        final var connectorName = "not-found-postgres-connector-task-restart";
        given()
                .when()
                .accept(ContentType.JSON).contentType(ContentType.JSON)
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_TASK_RESTART_ENDPOINT, 1, connectorName, 0)
                .then().log().all()
                .statusCode(404)
                .body("error_code", is(404))
                .body("message", is("Unknown connector: " + connectorName));
    }

    @Test
    public void testRestartPostgresConnectorTaskWithInvalidTaskNumber() {
        final var connectorName = "restart-postgres-connector-task-invalid-task-number";
        final var invalidTaskNumber = 99;
        Infrastructure.getDebeziumContainer().registerConnector(
                connectorName,
                Infrastructure.getPostgresConnectorConfiguration(1));

        Infrastructure.getDebeziumContainer().ensureConnectorState(connectorName, Connector.State.RUNNING);

        // It is possible the connector has not fully started and the tasks array returns
        // no running tasks, leading to a potential NPE with this call.
        Infrastructure.waitForConnectorTaskStatus(connectorName, 0, Connector.State.RUNNING);

        given()
                .when().log().all()
                .accept(ContentType.JSON).contentType(ContentType.JSON)
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_TASK_RESTART_ENDPOINT, 1, connectorName, invalidTaskNumber)
                .then().log().all()
                .statusCode(404)
                .body("error_code", is(404))
                .body("message", is("Unknown task: " + connectorName + "-" + invalidTaskNumber));
    }

}
