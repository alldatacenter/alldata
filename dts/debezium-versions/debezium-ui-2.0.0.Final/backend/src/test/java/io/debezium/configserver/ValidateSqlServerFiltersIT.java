/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.configserver;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.debezium.configserver.rest.ConnectorURIs;
import io.debezium.configserver.util.Infrastructure;
import io.debezium.configserver.util.SqlServerInfrastructureTestProfile;
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
@TestProfile(SqlServerInfrastructureTestProfile.class)
public class ValidateSqlServerFiltersIT {

    @Test
    public void testEmptySqlServerFilters() {
        ObjectNode config = ConnectorConfigurationTestingHelper.getConfig(
                Infrastructure.getSqlServerConnectorConfiguration(1)
                        .with("database.hostname", "localhost")
                        .with("database.port", Infrastructure.getSqlServerContainer().getMappedPort(1433))
                        .with("database.names", "testdb")
        );

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(config.toString())
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.FILTERS_VALIDATION_ENDPOINT, "sqlserver")
                .then().log().all()
                .statusCode(200)
                .assertThat().body("status", equalTo("VALID"))
                .body("propertyValidationResults.size()", is(0))
                .body("matchedCollections.size()", is(4))
                .body("matchedCollections",
                        hasItems(
                                Map.of("namespace", "testDB.inventory", "name", "products_on_hand"),
                                Map.of("namespace", "testDB.inventory", "name", "customers"),
                                Map.of("namespace", "testDB.inventory", "name", "orders"),
                                Map.of("namespace", "testDB.inventory", "name", "products")
                        ));
    }

    @Test
    public void testValidTableIncludeList() {
        ObjectNode config = ConnectorConfigurationTestingHelper.getConfig(
                Infrastructure.getSqlServerConnectorConfiguration(1)
                        .with("database.hostname", "localhost")
                        .with("database.port", Infrastructure.getSqlServerContainer().getMappedPort(1433))
                        .with("table.include.list", "inventory\\.product.*")
                        .with("database.names", "testdb")
        );

        given().when().contentType(ContentType.JSON).accept(ContentType.JSON).body(config.toString())
                .post(ConnectorURIs.API_PREFIX + ConnectorURIs.FILTERS_VALIDATION_ENDPOINT, "sqlserver")
                .then().log().all()
                .statusCode(200)
                .assertThat().body("status", equalTo("VALID"))
                .body("propertyValidationResults.size()", is(0))
                .body("matchedCollections.size()", is(2))
                .body("matchedCollections",
                        hasItems(
                                Map.of("namespace", "testDB.inventory", "name", "products_on_hand"),
                                Map.of("namespace", "testDB.inventory", "name", "products")
                        ));
    }
}
