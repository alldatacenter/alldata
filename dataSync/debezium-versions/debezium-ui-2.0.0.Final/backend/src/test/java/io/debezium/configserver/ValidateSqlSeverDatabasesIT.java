/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.SqlServerInfrastructureTestProfile;
import io.debezium.testing.testcontainers.Connector;
import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@TestProfile(SqlServerInfrastructureTestProfile.class)
public class ValidateSqlSeverDatabasesIT {

    @Test
    public void testMultipleDatabases() {
        ConnectorConfiguration config = Infrastructure.getSqlServerConnectorConfiguration(1)
                .with("database.names", "testDB,testDB2");

        Connector connector = Connector.from("custom-sqlserver-connector", config);

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(connector.toJson())
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CREATE_CONNECTOR_ENDPOINT, 1, "sqlserver")
                .then().log().all()
                .statusCode(200)
                .assertThat().body("name", equalTo("custom-sqlserver-connector"))
                .and().rootPath("config")
                .body("['connector.class']", equalTo("io.debezium.connector.sqlserver.SqlServerConnector"))
                .body("['database.names']", equalTo("testDB,testDB2"))
                .and().body("['database.hostname']", equalTo(Infrastructure.getSqlServerContainer().getContainerInfo().getConfig().getHostName()));
    }
}
