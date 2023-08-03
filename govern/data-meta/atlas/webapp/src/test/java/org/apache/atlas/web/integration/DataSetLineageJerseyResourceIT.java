/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.v1.model.typedef.TraitTypeDefinition;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Hive Lineage Integration Tests.
 */
public class DataSetLineageJerseyResourceIT extends BaseResourceIT {

    private String salesFactTable;
    private String salesMonthlyTable;
    private String salesDBName;
    private static String FACT      = "Fact_Tag";
    private static String ETL       = "ETL_Tag";
    private static String DIMENSION = "Dimension_Tag";
    private static String METRIC    = "Metric_Tag";
    private static String PII       = "pii_Tag";

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        createTypeDefinitionsV1();
        setupInstances();
    }

    @Test
    public void testInputsGraph() throws Exception {
        String     tableId  = atlasClientV1.getEntity(HIVE_TABLE_TYPE, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, salesMonthlyTable).getId()._getId();
        ObjectNode response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.LINEAGE_INPUTS_GRAPH, null, tableId, "/inputs/graph");

        Assert.assertNotNull(response);
        System.out.println("inputs graph = " + response);

        Assert.assertNotNull(response.get(AtlasClient.REQUEST_ID));

        JsonNode results = response.get(AtlasClient.RESULTS);
        Assert.assertNotNull(results);

        Struct resultsInstance = AtlasType.fromV1Json(results.toString(), Struct.class);
        Map<String, Struct> vertices = (Map<String, Struct>) resultsInstance.get("vertices");
        Assert.assertEquals(vertices.size(), 4);

        Map<String, Struct> edges = (Map<String, Struct>) resultsInstance.get("edges");
        Assert.assertEquals(edges.size(), 4);
    }

    @Test
    public void testInputsGraphForEntity() throws Exception {
        String tableId = atlasClientV1.getEntity(HIVE_TABLE_TYPE, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                salesMonthlyTable).getId()._getId();
        ObjectNode results = atlasClientV1.getInputGraphForEntity(tableId);
        Assert.assertNotNull(results);

        Struct resultsInstance = AtlasType.fromV1Json(results.toString(), Struct.class);
        resultsInstance.normalize();

        Map<String, Object> vertices = (Map<String, Object>) resultsInstance.get("vertices");
        Assert.assertEquals(vertices.size(), 4);

        Object verticesObject = vertices.get(tableId);
        Struct vertex         = null;

        if (verticesObject instanceof Map) {
            vertex  = new Struct ((Map)verticesObject);
        } else if (verticesObject instanceof Struct) {
            vertex = (Struct)verticesObject;
        }

        assertEquals(((Struct) vertex.get("vertexId")).get("state"), Id.EntityState.ACTIVE.name());

        Map<String, Struct> edges = (Map<String, Struct>) resultsInstance.get("edges");
        Assert.assertEquals(edges.size(), 4);
    }

    @Test
    public void testOutputsGraph() throws Exception {
        String     tableId  = atlasClientV1.getEntity(HIVE_TABLE_TYPE, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, salesFactTable).getId()._getId();
        ObjectNode response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.LINEAGE_INPUTS_GRAPH, null, tableId, "/outputs/graph");

        Assert.assertNotNull(response);
        System.out.println("outputs graph= " + response);

        Assert.assertNotNull(response.get(AtlasClient.REQUEST_ID));

        JsonNode results = response.get(AtlasClient.RESULTS);
        Assert.assertNotNull(results);

        Struct resultsInstance = AtlasType.fromV1Json(results.toString(), Struct.class);
        Map<String, Struct> vertices = (Map<String, Struct>) resultsInstance.get("vertices");
        Assert.assertEquals(vertices.size(), 3);

        Map<String, Struct> edges = (Map<String, Struct>) resultsInstance.get("edges");
        Assert.assertEquals(edges.size(), 4);
    }

    @Test
    public void testOutputsGraphForEntity() throws Exception {
        String tableId = atlasClientV1.getEntity(HIVE_TABLE_TYPE, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, salesFactTable).getId()._getId();
        ObjectNode results = atlasClientV1.getOutputGraphForEntity(tableId);
        Assert.assertNotNull(results);

        Struct resultsInstance = AtlasType.fromV1Json(results.toString(), Struct.class);
        Map<String, Object> vertices = (Map<String, Object>) resultsInstance.get("vertices");
        Assert.assertEquals(vertices.size(), 3);

        Object verticesObject = vertices.get(tableId);
        Struct vertex         = null;

        if (verticesObject instanceof Map) {
            vertex  = new Struct ((Map)verticesObject);
        } else if (verticesObject instanceof Struct) {
            vertex = (Struct)verticesObject;
        }

        assertEquals(((Struct) vertex.get("vertexId")).get("state"), Id.EntityState.ACTIVE.name());

        Map<String, Struct> edges = (Map<String, Struct>) resultsInstance.get("edges");
        Assert.assertEquals(edges.size(), 4);
    }

    @Test
    public void testSchema() throws Exception {
        String     tableId  = atlasClientV1.getEntity(HIVE_TABLE_TYPE, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, salesFactTable).getId()._getId();
        ObjectNode response = atlasClientV1.getSchemaForEntity(tableId);
    }

    @Test
    public void testSchemaForEntity() throws Exception {
        String     tableId = atlasClientV1.getEntity(HIVE_TABLE_TYPE, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, salesFactTable).getId()._getId();
        ObjectNode results = atlasClientV1.getSchemaForEntity(tableId);
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void testSchemaForInvalidTable() throws Exception {
        ObjectNode response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.NAME_LINEAGE_SCHEMA, null, "blah", "schema");
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void testSchemaForDB() throws Exception {
        ObjectNode response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.NAME_LINEAGE_SCHEMA, null, salesDBName, "schema");
    }

    private void setupInstances() throws Exception {
        TraitTypeDefinition factTrait      = TypesUtil.createTraitTypeDef(FACT, null, Collections.<String>emptySet());
        TraitTypeDefinition etlTrait       = TypesUtil.createTraitTypeDef(ETL, null, Collections.<String>emptySet());
        TraitTypeDefinition dimensionTrait = TypesUtil.createTraitTypeDef(DIMENSION, null, Collections.<String>emptySet());
        TraitTypeDefinition metricTrait    = TypesUtil.createTraitTypeDef(METRIC, null, Collections.<String>emptySet());

        createType(getTypesDef(null, null, Arrays.asList(factTrait, etlTrait, dimensionTrait, metricTrait), null));

        salesDBName = "Sales" + randomString();
        Id salesDB = database(salesDBName, "Sales Database", "John ETL",
                "hdfs://host:8000/apps/warehouse/sales");

        List<Referenceable> salesFactColumns = Arrays.asList(column("time_id", "int", "time id"), column("product_id", "int", "product id"),
                        column("customer_id", "int", "customer id", PII),
                        column("sales", "double", "product id", METRIC));

        salesFactTable = "sales_fact" + randomString();
        Id salesFact = table(salesFactTable, "sales fact table", salesDB, "Joe", "MANAGED", salesFactColumns, FACT);

        List<Referenceable> timeDimColumns = Arrays.asList(column("time_id", "int", "time id"), column("dayOfYear", "int", "day Of Year"),
                        column("weekDay", "int", "week Day"));

        Id timeDim =
                table("time_dim" + randomString(), "time dimension table", salesDB, "John Doe", "EXTERNAL",
                        timeDimColumns, DIMENSION);

        Id reportingDB =
                database("Reporting" + randomString(), "reporting database", "Jane BI",
                        "hdfs://host:8000/apps/warehouse/reporting");

        Id salesFactDaily =
                table("sales_fact_daily_mv" + randomString(), "sales fact daily materialized view", reportingDB,
                        "Joe BI", "MANAGED", salesFactColumns, METRIC);

        loadProcess("loadSalesDaily" + randomString(), "John ETL", Arrays.asList(salesFact, timeDim),
                Collections.singletonList(salesFactDaily), "create table as select ", "plan", "id", "graph", ETL);

        salesMonthlyTable = "sales_fact_monthly_mv" + randomString();
        Id salesFactMonthly =
                table(salesMonthlyTable, "sales fact monthly materialized view", reportingDB, "Jane BI",
                        "MANAGED", salesFactColumns, METRIC);

        loadProcess("loadSalesMonthly" + randomString(), "John ETL", Collections.singletonList(salesFactDaily),
                Collections.singletonList(salesFactMonthly), "create table as select ", "plan", "id", "graph", ETL);
    }

    Id database(String name, String description, String owner, String locationUri, String... traitNames)
    throws Exception {
        Referenceable referenceable = new Referenceable(DATABASE_TYPE, traitNames);
        referenceable.set(NAME, name);
        referenceable.set(QUALIFIED_NAME, name);
        referenceable.set(CLUSTER_NAME, locationUri + name);
        referenceable.set("description", description);
        referenceable.set("owner", owner);
        referenceable.set("locationUri", locationUri);
        referenceable.set("createTime", System.currentTimeMillis());

        return createInstance(referenceable);
    }

    Referenceable column(String name, String type, String comment, String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(COLUMN_TYPE, traitNames);
        referenceable.set(NAME, name);
        referenceable.set(QUALIFIED_NAME, name);
        referenceable.set("type", type);
        referenceable.set("comment", comment);

        return referenceable;
    }

    Id table(String name, String description, Id dbId, String owner, String tableType, List<Referenceable> columns,
            String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(HIVE_TABLE_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
        referenceable.set("description", description);
        referenceable.set("owner", owner);
        referenceable.set("tableType", tableType);
        referenceable.set("createTime", System.currentTimeMillis());
        referenceable.set("lastAccessTime", System.currentTimeMillis());
        referenceable.set("retention", System.currentTimeMillis());

        referenceable.set("db", dbId);
        referenceable.set("columns", columns);

        return createInstance(referenceable);
    }

    Id loadProcess(String name, String user, List<Id> inputTables, List<Id> outputTables, String queryText,
            String queryPlan, String queryId, String queryGraph, String... traitNames) throws Exception {
        Referenceable referenceable = new Referenceable(HIVE_PROCESS_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
        referenceable.set("userName", user);
        referenceable.set("startTime", System.currentTimeMillis());
        referenceable.set("endTime", System.currentTimeMillis() + 10000);

        referenceable.set("inputs", inputTables);
        referenceable.set("outputs", outputTables);

        referenceable.set("operationType", "testOperation");
        referenceable.set("queryText", queryText);
        referenceable.set("queryPlan", queryPlan);
        referenceable.set("queryId", queryId);
        referenceable.set("queryGraph", queryGraph);

        return createInstance(referenceable);
    }
}
