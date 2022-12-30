/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import io.debezium.configserver.model.ConnectorProperty;
import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.PostgresInfrastructureTestProfile;
import io.debezium.testing.testcontainers.Connector;
import io.debezium.configserver.service.ConnectorIntegratorBase;
import io.debezium.connector.mongodb.MongoDbConnectorConfig;
import io.debezium.connector.mysql.MySqlConnectorConfig;
import io.debezium.connector.postgresql.PostgresConnectorConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(PostgresInfrastructureTestProfile.class)
public class ConnectorResourceIT {

    @Test
    public void testConnectorTypesEndpoint() {
        given()
          .when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_TYPES_ENDPOINT)
          .then().log().all()
             .statusCode(200)
             .body("className", hasItems(
                     equalTo("io.debezium.connector.sqlserver.SqlServerConnector"),
                     equalTo("io.debezium.connector.postgresql.PostgresConnector"),
                     equalTo("io.debezium.connector.mysql.MySqlConnector"),
                     equalTo("io.debezium.connector.mongodb.MongoDbConnector")
                ));
    }

    @Test
    public void testPostgresConnectorTypesEndpoint() {
        given()
            .when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_TYPES_ENDPOINT_FOR_CONNECTOR, "postgres")
            .then().log().all()
            .statusCode(200)
            .body("className", equalTo("io.debezium.connector.postgresql.PostgresConnector"))
            .body("properties.find { it.name == 'snapshot.mode' }.allowedValues",
                    equalTo(ConnectorIntegratorBase.enumArrayToList(PostgresConnectorConfig.SnapshotMode.values())))
            .body("properties.find { it.name == 'decimal.handling.mode' }.allowedValues",
                    equalTo(ConnectorIntegratorBase.enumArrayToList(PostgresConnectorConfig.DecimalHandlingMode.values())))
            .body("properties.contains(null)", is(false))
            .body("enabled", is(true));
    }

    @Test
    public void testMySqlConnectorTypesEndpoint() {
        given()
            .when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_TYPES_ENDPOINT_FOR_CONNECTOR, "mysql")
            .then().log().all()
            .statusCode(200)
            .body("className", equalTo("io.debezium.connector.mysql.MySqlConnector"))
            .body("properties.find { it.name == 'snapshot.mode' }.allowedValues",
                    equalTo(ConnectorIntegratorBase.enumArrayToList(MySqlConnectorConfig.SnapshotMode.values())))
            .body("properties.find { it.name == 'snapshot.locking.mode' }.allowedValues",
                    equalTo(ConnectorIntegratorBase.enumArrayToList(MySqlConnectorConfig.SnapshotLockingMode.values())))
            .body("properties.find { it.name == 'snapshot.new.tables' }.allowedValues",
                    equalTo(ConnectorIntegratorBase.enumArrayToList(MySqlConnectorConfig.SnapshotNewTables.values())))
            .body("properties.contains(null)", is(false))
            .body("enabled", is(true));
    }

    @Test
    public void testMongoDbConnectorTypesEndpoint() {
        given()
            .when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_TYPES_ENDPOINT_FOR_CONNECTOR, "mongodb")
            .then().log().all()
            .statusCode(200)
            .body("className", equalTo("io.debezium.connector.mongodb.MongoDbConnector"))
            .body("properties.find { it.name == 'snapshot.mode' }.allowedValues",
                    equalTo(ConnectorIntegratorBase.enumArrayToList(MongoDbConnectorConfig.SnapshotMode.values())))
            .body("properties.find { it.name == 'field.renames' }.category", is(ConnectorProperty.Category.CONNECTOR_ADVANCED.name()))
            .body("properties.contains(null)", is(false))
            .body("enabled", is(true));
    }

    @Test
    public void testClustersEndpoint() {
        given()
          .when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECT_CLUSTERS_ENDPOINT)
          .then().log().all()
             .statusCode(200)
             .body("size()", is(1))
             .and().body("[0]", equalTo("http://localhost:" + Infrastructure.getDebeziumContainer().getMappedPort(8083)));
    }

    @Test
    public void testGetPostgresConnectorConfigNotFound() {
        final var connectorName = "not-found-postgres-connector-get-config";

        given()
                .when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_CONFIG_ENDPOINT, 1, connectorName)
                .then().log().all()
                .statusCode(404)
                .body("error_code", is(404))
                .body("message", is("Connector " + connectorName + " not found"));
    }

    @Test
    public void testGetPostgresConnectorConfig() {
        final var connectorName = "postgres-connector-get-config";
        Infrastructure.getDebeziumContainer().registerConnector(
                connectorName,
                Infrastructure.getPostgresConnectorConfiguration(1));

        Infrastructure.getDebeziumContainer().ensureConnectorState(connectorName, Connector.State.RUNNING);

        given()
                .when().get(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTOR_CONFIG_ENDPOINT, 1, connectorName)
                .then().log().all()
                .statusCode(200)
                .body("name", equalTo("postgres-connector-get-config"))
                .body("'connector.class'", equalTo("io.debezium.connector.postgresql.PostgresConnector"))
                .body("'topic.prefix'", equalTo("dbserver1"))
                .body("'database.dbname'", equalTo("test"))
                .body("'database.user'", equalTo("test"));
    }

}
