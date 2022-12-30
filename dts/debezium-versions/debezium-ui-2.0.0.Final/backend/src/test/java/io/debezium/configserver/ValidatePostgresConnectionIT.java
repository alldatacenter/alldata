/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.PostgresInfrastructureTestProfile;
import io.debezium.testing.testcontainers.ConnectorConfigurationTestingHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestProfile(PostgresInfrastructureTestProfile.class)
public class ValidatePostgresConnectionIT {

    @Test
    public void testValidPostgresConnection() {
        ObjectNode config = ConnectorConfigurationTestingHelper.getConfig(
                Infrastructure.getPostgresConnectorConfiguration(1)
                    .with("database.hostname", "localhost")
                    .with("database.port", Infrastructure.getPostgresContainer().getMappedPort(5432))
        );

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(config.toString())
            .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTION_VALIDATION_ENDPOINT, "postgres")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("status", equalTo("VALID"))
                .body("genericValidationResults.size()", is(0))
                .body("propertyValidationResults.size()", is(0));
    }

    @Test
    public void testInvalidHostnamePostgresConnection() {
        ObjectNode config = ConnectorConfigurationTestingHelper.getConfig(
                Infrastructure.getPostgresConnectorConfiguration(1)
                    .with("database.hostname", "zzzzzzzzzz"));

        Locale.setDefault(new Locale("en", "US"));
        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(config.toString())
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTION_VALIDATION_ENDPOINT, "postgres")
                .then().log().all()
                .statusCode(200)
                .assertThat().body("status", equalTo("INVALID"))
                .body("genericValidationResults.size()", is(0))
                .body("propertyValidationResults.size()", is(1))
                .rootPath("propertyValidationResults[0]")
                    .body("property", equalTo("database.hostname"))
                    .body("message", equalTo("Error while validating connector config: The connection attempt failed."));
    }

    @Test
    public void testInvalidPostgresConnection() {
        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body("{\"connector.class\":\"io.debezium.connector.postgresql.PostgresConnector\"}")
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.CONNECTION_VALIDATION_ENDPOINT, "postgres")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("status", equalTo("INVALID"))
                .body("genericValidationResults.size()", is(0))
                .body("propertyValidationResults.size()", is(4))
                .body("propertyValidationResults",
                    hasItems(
                        Map.of("property", "database.user", "message", "The 'database.user' value is invalid: A value is required"),
                        Map.of("property", "database.dbname", "message", "The 'database.dbname' value is invalid: A value is required"),
                        Map.of("property", "database.hostname", "message", "The 'database.hostname' value is invalid: A value is required")
                    ));
    }

}
