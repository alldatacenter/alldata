/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import java.util.concurrent.TimeUnit;

import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.SqlServerInfrastructureTestProfile;
import io.debezium.testing.testcontainers.Connector;
import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(SqlServerInfrastructureTestProfile.class)
public class CreateAndDeleteSqlSeverConnectorIT {

    @Test
    public void testSqlServerClustersEndpoint() {
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
    public void testSqlServerCreateConnectorEndpoint() {
        ConnectorConfiguration config = Infrastructure.getSqlServerConnectorConfiguration(1)
                .with("database.names", "testdb");

        Connector connector = Connector.from("my-sqlserver-connector", config);

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(connector.toJson())
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CREATE_CONNECTOR_ENDPOINT, 1, "sqlserver")
                .then().log().all()
                .statusCode(200)
                .assertThat().body("name", equalTo("my-sqlserver-connector"))
                .and().rootPath("config")
                .body("['connector.class']", equalTo("io.debezium.connector.sqlserver.SqlServerConnector"))
                .and().body("['database.hostname']", equalTo(Infrastructure.getSqlServerContainer().getContainerInfo().getConfig().getHostName()));
    }

    @Test
    public void testSqlServerDeleteConnectorFailed() {
        given()
                .when().delete(ConnectorURIs.API_PREFIX + ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT, 1, "wrong-connector-name-123")
                .then().log().all()
                .statusCode(404)
                .body("size()", is(2))
                .body("error_code", is(404))
                .body("message", equalTo("Connector wrong-connector-name-123 not found"));
    }

    @Test
    public void testSqlServerDeleteConnectorSuccessful() {
        ConnectorConfiguration config = Infrastructure.getSqlServerConnectorConfiguration(1)
                .with("database.names", "testdb");

        final var deleteSqlServerConnectorName = "delete-connector-sqlsever";
        Infrastructure.getDebeziumContainer().deleteAllConnectors();
        Infrastructure.getDebeziumContainer().registerConnector(
                deleteSqlServerConnectorName,
                config
        );

        // It is possible the connector has not fully started and the tasks array returns
        // no running tasks, leading to a potential NPE with this call.
        Awaitility.await()
                .atMost(60, TimeUnit.SECONDS)
                .ignoreException(NullPointerException.class)
                .until(() -> {
                           Infrastructure.getDebeziumContainer().ensureConnectorTaskState(
                                   deleteSqlServerConnectorName, 0, Connector.State.RUNNING);
                           return true;
                       });

        given()
                .when().delete(ConnectorURIs.API_PREFIX + ConnectorURIs.MANAGE_CONNECTORS_ENDPOINT, 1, deleteSqlServerConnectorName)
                .then().log().all()
                .statusCode(204);

        assertTrue(Infrastructure.getDebeziumContainer().connectorIsNotRegistered(deleteSqlServerConnectorName));
    }
}
