/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.MySqlInfrastructureTestProfile;
import io.debezium.testing.testcontainers.ConnectorConfigurationTestingHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@TestProfile(MySqlInfrastructureTestProfile.class)
public class ValidateMySqlFiltersIT {

    @Test
    public void testEmptyMySqlFilters() {
        ObjectNode config = ConnectorConfigurationTestingHelper.getConfig(
            Infrastructure.getMySqlConnectorConfiguration(1)
                .with("database.hostname", "localhost")
                .with("database.port", Infrastructure.getMySqlContainer().getMappedPort(3306))
        );

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(config.toString())
            .post(ConnectorURIs.API_PREFIX + ConnectorURIs.FILTERS_VALIDATION_ENDPOINT, "mysql")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("status", equalTo("VALID"))
                .body("propertyValidationResults.size()", is(0))
                .body("matchedCollections.size()", is(6))
                .body("matchedCollections",
                    hasItems(
                        Map.of("namespace", "inventory", "name", "geom"),
                        Map.of("namespace", "inventory", "name", "products_on_hand"),
                        Map.of("namespace", "inventory", "name", "customers"),
                        Map.of("namespace", "inventory", "name", "addresses"),
                        Map.of("namespace", "inventory", "name", "orders"),
                        Map.of("namespace", "inventory", "name", "products")
                    ));
    }

    @Test
    public void testValidTableIncludeList() {
        ObjectNode config = ConnectorConfigurationTestingHelper.getConfig(
                Infrastructure.getMySqlConnectorConfiguration(1)
                        .with("database.hostname", "localhost")
                        .with("database.port", Infrastructure.getMySqlContainer().getMappedPort(3306))
                        .with("table.include.list", "inventory\\.product.*")
        );

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(config.toString())
            .post(ConnectorURIs.API_PREFIX + ConnectorURIs.FILTERS_VALIDATION_ENDPOINT, "mysql")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("status", equalTo("VALID"))
                .body("propertyValidationResults.size()", is(0))
                .body("matchedCollections.size()", is(2))
                .body("matchedCollections",
                    hasItems(
                        Map.of("namespace", "inventory", "name", "products_on_hand"),
                        Map.of("namespace", "inventory", "name", "products")
                    ));
    }

    @Test
    public void testValidDatabaseIncludeList() {
        ObjectNode config = ConnectorConfigurationTestingHelper.getConfig(
            Infrastructure.getMySqlConnectorConfiguration(1)
                .with("database.hostname", "localhost")
                .with("database.port", Infrastructure.getMySqlContainer().getMappedPort(3306))
                .with("database.include.list", "inventory")
        );

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(config.toString())
            .post(ConnectorURIs.API_PREFIX + ConnectorURIs.FILTERS_VALIDATION_ENDPOINT, "mysql")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("status", equalTo("VALID"))
                .body("propertyValidationResults.size()", is(0))
                .body("matchedCollections.size()", is(6))
                .body("matchedCollections",
                    hasItems(
                        Map.of("namespace", "inventory", "name", "geom"),
                        Map.of("namespace", "inventory", "name", "products_on_hand"),
                        Map.of("namespace", "inventory", "name", "customers"),
                        Map.of("namespace", "inventory", "name", "addresses"),
                        Map.of("namespace", "inventory", "name", "orders"),
                        Map.of("namespace", "inventory", "name", "products")
                    ));
    }

    @Test
    public void testDatabaseIncludeListPatternInvalid() {
        ObjectNode config = ConnectorConfigurationTestingHelper.getConfig(
            Infrastructure.getMySqlConnectorConfiguration(1)
                .with("database.hostname", "localhost")
                .with("database.port", Infrastructure.getMySqlContainer().getMappedPort(3306))
                .with("database.include.list", "+")
        );

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(config.toString())
            .post(ConnectorURIs.API_PREFIX + ConnectorURIs.FILTERS_VALIDATION_ENDPOINT, "mysql")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("status", equalTo("INVALID"))
                .body("propertyValidationResults.size()", is(1))
                .body("matchedCollections.size()", is(0))
                .rootPath("propertyValidationResults[0]")
                .body("property", equalTo("database.include.list"))
                .body("message", equalTo("The 'database.include.list' value is invalid: A comma-separated list of valid regular expressions is expected, but Dangling meta character '+' near index 0\n+\n^"));
    }

    @Test
    public void testDatabaseExcludeListPatternInvalid() {
        ObjectNode config = ConnectorConfigurationTestingHelper.getConfig(
            Infrastructure.getMySqlConnectorConfiguration(1)
                .with("database.hostname", "localhost")
                .with("database.port", Infrastructure.getMySqlContainer().getMappedPort(3306))
                .with("database.exclude.list", "+")
        );

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(config.toString())
            .post(ConnectorURIs.API_PREFIX + ConnectorURIs.FILTERS_VALIDATION_ENDPOINT, "mysql")
            .then().log().all()
            .statusCode(200)
            .assertThat().body("status", equalTo("INVALID"))
                .body("propertyValidationResults.size()", is(1))
                .body("matchedCollections.size()", is(0))
                .rootPath("propertyValidationResults[0]")
                .body("property", equalTo("database.exclude.list"))
                .body("message", equalTo("The 'database.exclude.list' value is invalid: A comma-separated list of valid regular expressions is expected, but Dangling meta character '+' near index 0\n+\n^"));
    }

}
