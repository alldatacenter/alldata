/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas;

import com.google.common.collect.ImmutableList;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.glossary.GlossaryService;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.glossary.relations.AtlasGlossaryHeader;
import org.apache.atlas.model.instance.*;
import org.apache.atlas.model.typedef.*;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.AtlasRelationshipStore;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.TestLoadModelUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.atlas.utils.TestLoadModelUtils.loadModelFromJson;
import static org.testng.Assert.fail;

public abstract class BasicTestSetup extends AtlasTestBase {

    // Entity type //
    protected static final String DATABASE_TYPE     = "hive_db";
    protected static final String HIVE_TABLE_TYPE   = "hive_table";
    private static final   String COLUMN_TYPE       = "hive_column";
    private static final   String HIVE_PROCESS_TYPE = "hive_process";
    private static final   String STORAGE_DESC_TYPE = "hive_storagedesc";
    private static final   String VIEW_TYPE         = "hive_process";
    protected static final String DATASET_SUBTYPE   = "Asset";

    //Classification type //
    public static final String DIMENSION_CLASSIFICATION    = "Dimension";
    public static final String FACT_CLASSIFICATION         = "Fact";
    public static final String PII_CLASSIFICATION          = "PII";
    public static final String METRIC_CLASSIFICATION       = "Metric";
    public static final String ETL_CLASSIFICATION          = "ETL";
    public static final String JDBC_CLASSIFICATION         = "JdbcAccess";
    public static final String LOGDATA_CLASSIFICATION      = "Log Data";
    public static final String DIMENSIONAL_CLASSIFICATION  = "Dimensional";

    // Glossary type //
    public static final String SALES_GLOSSARY = "salesGlossary";
    public static final String SALES_TERM     = "salesTerm";
    public static final String MODERNTRADE_TERM = "modernTrade";
    public static final String ECOMMERCE_TERM   = "ecommerce";

    @Inject
    protected AtlasTypeRegistry typeRegistry;
    @Inject
    protected AtlasTypeDefStore typeDefStore;
    @Inject
    protected AtlasEntityStore entityStore;
    @Inject
    protected AtlasRelationshipStore relationshipStore;
    @Inject
    protected GlossaryService glossaryService;

    private boolean baseLoaded = false;
    private EntityMutationResponse hiveEntities;

    private AtlasEntity timeDim;
    private AtlasEntity productDim;
    private AtlasEntity loggingFactMonthly;

    protected void setupTestData() {
        loadBaseModels();
        loadHiveDataset();
        loadEmployeeDataset();
        assignGlossary();
    }

    protected void setupRelationshipTestData() {
        if(!baseLoaded) {
            loadBaseModels();
        }

        try {
            TestLoadModelUtils.loadModelFromResourcesJson("relationship_search_def.json", typeDefStore, typeRegistry);
        } catch (IOException e) {
            fail("Failed to load relationship_search_def.json");
        } catch (AtlasBaseException e) {
           fail("Failed to load relationship_search_def.json to atlas model");
        }

        try {
            createTestDataForRelationship();
        } catch (AtlasBaseException e) {
            fail("Failed to load data for relationship_search_def.json");
        }
    }

    private void loadBaseModels() {
        try {
            loadModelFromJson("0000-Area0/0010-base_model.json", typeDefStore, typeRegistry);
            loadModelFromJson("0000-Area0/0011-glossary_model.json", typeDefStore, typeRegistry);
            baseLoaded = true;
        } catch (IOException | AtlasBaseException e) {
            fail("Base model setup is required for test to run!");
        }
    }

    protected void loadHiveDataset() {
        if (!baseLoaded) {
            loadBaseModels();
        }

        try {
            loadModelFromJson("1000-Hadoop/1030-hive_model.json", typeDefStore, typeRegistry);
        } catch (IOException | AtlasBaseException e) {
            fail("Hive model setup is required for test to run!");
        }

        AtlasEntity.AtlasEntitiesWithExtInfo hiveTestEntities = hiveTestEntities();

        try {
             hiveEntities = entityStore.createOrUpdate(new AtlasEntityStream(hiveTestEntities), false);
        } catch (AtlasBaseException e) {
            fail("Hive entities need to be created for test to run!");
        }
    }

    protected void loadEmployeeDataset() {
        if (!baseLoaded) {
            loadBaseModels();
        }

        // Define employee dataset types
        AtlasTypesDef employeeTypes = TestUtilsV2.defineDeptEmployeeTypes();

        try {
            typeDefStore.createTypesDef(employeeTypes);
        } catch (AtlasBaseException e) {
            fail("Employee Type setup is required");
        }

        // Define entities for department
        AtlasEntity.AtlasEntitiesWithExtInfo deptEg2 = TestUtilsV2.createDeptEg2();

        try {
            entityStore.createOrUpdate(new AtlasEntityStream(deptEg2), false);
        } catch (AtlasBaseException e) {
            fail("Employee entity setup should've passed");
        }
    }

    public AtlasEntity.AtlasEntitiesWithExtInfo hiveTestEntities() {
        List<AtlasEntity> entities = new ArrayList<>();
        
        createClassificationTypes();

        AtlasEntity salesDB = database("Sales", "/apps/warehouse/Sales Database", "John ETL", "hdfs://host:8000/apps/warehouse/sales");
        entities.add(salesDB);

        AtlasEntity sd =
                storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(
                        column("time_id", "int", "time id")));
        entities.add(sd);

        List<AtlasEntity> salesFactColumns = ImmutableList
                .of(column("time_id", "int", "time id"),
                        column("product_id", "int", "product id"),
                        column("customer_id", "int", "customer id", PII_CLASSIFICATION),
                        column("sales", "double", "product id", METRIC_CLASSIFICATION),
                        column("test", "int", "test 1"),
                        column("test_limit", "int", "test limit 1")
                );

        entities.addAll(salesFactColumns);

        AtlasEntity salesFact = table("sales_fact", "sales fact table", salesDB, sd, "Joe", "Managed", salesFactColumns, FACT_CLASSIFICATION);
        salesFact.setAttribute("createTime", new Date(2018, 01, 01));
        entities.add(salesFact);

        List<AtlasEntity> logFactColumns = ImmutableList
                    .of(column("time_id", "int", "time id"),
                        column("app_id", "int", "app id"),
                        column("machine_id", "int", "machine id"),
                        column("log", "string", "log data", LOGDATA_CLASSIFICATION),
                        column("test", "int", "test 2"),
                        column("test_limit", "int", "test limit 2")
                    );
        entities.addAll(logFactColumns);

        List<AtlasEntity> timeDimColumns = ImmutableList
                                                   .of(column("time_id", "int", "time id"),
                                                       column("dayOfYear", "int", "day Of Year"),
                                                       column("weekDay", "int", "week Day"));
        entities.addAll(timeDimColumns);

        sd = storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(column("time_id", "int", "time id")));
        entities.add(sd);

        timeDim = table("time_dim", "time dimension table", salesDB, sd, "John Doe", "External", timeDimColumns,
                                    DIMENSION_CLASSIFICATION);
        entities.add(timeDim);

        AtlasEntity reportingDB =
                database("Reporting", "/apps/warehouse/reporting database", "Jane BI", "hdfs://host:8000/apps/warehouse/reporting");
        entities.add(reportingDB);

        sd = storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(column("time_id", "int", "time id")));
        entities.add(sd);

        AtlasEntity salesFactDaily =
                table("sales_fact_daily_mv", "sales fact daily materialized view", reportingDB, sd, "Joe BI", "Managed",
                      salesFactColumns, METRIC_CLASSIFICATION);
        salesFactDaily.setAttribute("createTime", Date.from(LocalDate.of(2016, 8, 19).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        entities.add(salesFactDaily);

        sd = storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(column("time_id", "int", "time id")));
        entities.add(sd);

        AtlasEntity circularLineageTable1 = table("table1", "", reportingDB, sd, "Vimal", "Managed", salesFactColumns, METRIC_CLASSIFICATION);
        entities.add(circularLineageTable1);

        sd = storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(column("time_id", "int", "time id")));
        entities.add(sd);

        AtlasEntity circularLineageTable2 = table("table2", "", reportingDB, sd, "Vimal 2", "Managed", salesFactColumns, METRIC_CLASSIFICATION);
        entities.add(circularLineageTable2);

        AtlasEntity circularLineage1Process = loadProcess("circularLineage1", "hive query for daily summary", "John ETL", ImmutableList.of(circularLineageTable1),
                                         ImmutableList.of(circularLineageTable2), "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);
        entities.add(circularLineage1Process);

        AtlasEntity circularLineage2Process = loadProcess("circularLineage2", "hive query for daily summary", "John ETL", ImmutableList.of(circularLineageTable2),
                                         ImmutableList.of(circularLineageTable1), "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);
        entities.add(circularLineage2Process);

        AtlasEntity loadSalesDaily = loadProcess("loadSalesDaily", "hive query for daily summary", "John ETL", ImmutableList.of(salesFact, timeDim),
                                         ImmutableList.of(salesFactDaily), "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);
        entities.add(loadSalesDaily);

        AtlasEntity logDB = database("Logging", "/apps/warehouse/logging", "Tim ETL", "hdfs://host:8000/apps/warehouse/logging");
        entities.add(logDB);

        sd = storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(column("time_id", "int", "time id")));
        entities.add(sd);

        AtlasEntity loggingFactDaily =
                table("log_fact_daily_mv", "log fact daily materialized view", logDB, sd, "Tim ETL", "Managed",
                      logFactColumns, LOGDATA_CLASSIFICATION);
        entities.add(loggingFactDaily);

        List<AtlasEntity> productDimColumns = ImmutableList
                    .of(column("product_id", "int", "product id"),
                        column("product_name", "string", "product name"),
                        column("brand_name", "int", "brand name"));
        entities.addAll(productDimColumns);

        sd = storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(column("time_id", "int", "time id")));
        entities.add(sd);

        productDim =
                table("product_dim", "product dimension table", salesDB, sd, "John Doe 2", "Managed", productDimColumns,
                      DIMENSION_CLASSIFICATION);
        entities.add(productDim);

        AtlasEntity productDimView = view("product_dim_view", reportingDB, ImmutableList.of(productDim), DIMENSION_CLASSIFICATION, JDBC_CLASSIFICATION);
        entities.add(productDimView);

        List<AtlasEntity> customerDimColumns = ImmutableList.of(
                column("customer_id", "int", "customer id", PII_CLASSIFICATION),
                column("name", "string", "customer name", PII_CLASSIFICATION),
                column("address", "string", "customer address", PII_CLASSIFICATION));
        entities.addAll(customerDimColumns);

        sd = storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(column("time_id", "int", "time id")));
        entities.add(sd);

        AtlasEntity customerDim =
                    table("customer_dim", "customer dimension table", salesDB, sd, "fetl", "External", customerDimColumns,
                      DIMENSION_CLASSIFICATION);
        entities.add(customerDim);

        AtlasEntity customerDimView = view("customer_dim_view", reportingDB, ImmutableList.of(customerDim), DIMENSION_CLASSIFICATION, JDBC_CLASSIFICATION);
        entities.add(customerDimView);

        sd = storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(column("time_id", "int", "time id")));
        entities.add(sd);

        AtlasEntity salesFactMonthly =
                table("sales_fact_monthly_mv", "sales fact monthly materialized view", reportingDB, sd, "Jane BI",
                      "Managed", salesFactColumns, METRIC_CLASSIFICATION);
        entities.add(salesFactMonthly);

        AtlasEntity loadSalesMonthly = loadProcess("loadSalesMonthly", "hive query for monthly summary", "John ETL", ImmutableList.of(salesFactDaily),
                                         ImmutableList.of(salesFactMonthly), "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);
        entities.add(loadSalesMonthly);

        sd = storageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true, ImmutableList.of(column("time_id", "int", "time id")));
        entities.add(sd);

        loggingFactMonthly =
                table("logging_fact_monthly_mv", "logging fact monthly materialized view", logDB, sd, "Tim ETL 2",
                      "Managed", logFactColumns, LOGDATA_CLASSIFICATION);
        entities.add(loggingFactMonthly);

        AtlasEntity loadLogsMonthly = loadProcess("loadLogsMonthly", "hive query for monthly summary", "Tim ETL", ImmutableList.of(loggingFactDaily),
                                         ImmutableList.of(loggingFactMonthly), "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);
        entities.add(loadLogsMonthly);

        AtlasEntity datasetSubType = datasetSubType("dataSetSubTypeInst1", "testOwner");
        entities.add(datasetSubType);

        return new AtlasEntity.AtlasEntitiesWithExtInfo(entities);
    }

    protected void createClassificationTypes() {
        List<AtlasClassificationDef> cds =  Arrays.asList(new AtlasClassificationDef(DIMENSION_CLASSIFICATION, "Dimension Classification", "1.0",
                        Arrays.asList(new AtlasStructDef.AtlasAttributeDef("timeAttr","string"), new AtlasStructDef.AtlasAttributeDef("productAttr","string"))),
                new AtlasClassificationDef(FACT_CLASSIFICATION, "Fact Classification", "1.0"),
                new AtlasClassificationDef(PII_CLASSIFICATION, "PII Classification", "1.0"),
                new AtlasClassificationDef(METRIC_CLASSIFICATION, "Metric Classification", "1.0"),
                new AtlasClassificationDef(ETL_CLASSIFICATION, "ETL Classification", "1.0"),
                new AtlasClassificationDef(JDBC_CLASSIFICATION, "JdbcAccess Classification", "1.0"),
                new AtlasClassificationDef(LOGDATA_CLASSIFICATION, "LogData Classification", "1.0"),
                new AtlasClassificationDef(DIMENSIONAL_CLASSIFICATION,"Dimensional Classification", "1.0" ,
                        Arrays.asList(new AtlasStructDef.AtlasAttributeDef("attr1","string"))));

        AtlasTypesDef tds = new AtlasTypesDef(Collections.<AtlasEnumDef>emptyList(),
                Collections.<AtlasStructDef>emptyList(),
                cds,
                Collections.<AtlasEntityDef>emptyList());
        createUpdateClassificationDef(tds);
    }

    private void createUpdateClassificationDef(AtlasTypesDef td) {
        try {
            typeDefStore.createTypesDef(td);
        }
        catch(Exception e) {
            fail("Error creating classifications definitions.");
        }
    }

    AtlasEntity database(String name, String description, String owner, String locationUri, String... traitNames) {
        AtlasEntity database = new AtlasEntity(DATABASE_TYPE);
        database.setAttribute("name", name);
        database.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, "qualified:" + name);
        database.setAttribute("description", description);
        database.setAttribute("userDescription", description);
        database.setAttribute("owner", owner);
        database.setAttribute("location", locationUri);
        database.setAttribute("createTime", System.currentTimeMillis());
        database.setAttribute("clusterName", "cl1");
        database.setClassifications(Stream.of(traitNames).map(AtlasClassification::new).collect(Collectors.toList()));

        return database;
    }

    protected AtlasEntity storageDescriptor(String location, String inputFormat, String outputFormat, boolean compressed, List<AtlasEntity> columns) {
        AtlasEntity storageDescriptor = new AtlasEntity(STORAGE_DESC_TYPE);
        storageDescriptor.setAttribute("location", location);
        storageDescriptor.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, "qualified:" + location);
        storageDescriptor.setAttribute("inputFormat", inputFormat);
        storageDescriptor.setAttribute("outputFormat", outputFormat);
        storageDescriptor.setAttribute("compressed", compressed);
        storageDescriptor.setAttribute("cols", getAtlasObjectIds(columns));

        return storageDescriptor;
    }

    protected AtlasEntity column(String name, String dataType, String comment, String... traitNames) {
        AtlasEntity column = new AtlasEntity(COLUMN_TYPE);
        column.setAttribute("name", name);
        column.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, "qualified:" + name);
        column.setAttribute("type", dataType);
        column.setAttribute("comment", comment);
        column.setClassifications(Stream.of(traitNames).map(AtlasClassification::new).collect(Collectors.toList()));

        return column;
    }

    protected AtlasEntity table(String name, String description, AtlasEntity db, AtlasEntity sd, String owner, String tableType,
                                List<AtlasEntity> columns, String... traitNames) {
        String dbName      = db.getAttribute(AtlasClient.NAME).toString();
        String clusterName = db.getAttribute("clusterName").toString();

        AtlasEntity table = new AtlasEntity(HIVE_TABLE_TYPE);
        table.setAttribute("name", name);
        table.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName + "." + name);
        table.setAttribute("description", description);
        table.setAttribute("owner", owner);
        table.setAttribute("tableType", tableType);
        table.setAttribute("temporary", false);
        table.setAttribute("createTime", new Date(System.currentTimeMillis()));
        table.setAttribute("lastAccessTime", System.currentTimeMillis());
        table.setAttribute("retention", 5d);

        table.setAttribute("db", getAtlasObjectId(db));
        table.setAttribute("sd", getAtlasObjectId(sd));

        table.setAttribute("columns", getAtlasObjectIds(columns));
        if (name.equals("time_dim")) {
            AtlasClassification classification = new AtlasClassification(traitNames[0], new HashMap<String, Object>() {{ put("timeAttr", "timeValue"); }});
            table.setClassifications(Collections.singletonList(classification));
        } else if (name.equals("product_dim")) {
            AtlasClassification classification = new AtlasClassification(traitNames[0], new HashMap<String, Object>() {{ put("productAttr", "productValue"); }});
            table.setClassifications(Collections.singletonList(classification));
        } else {
            table.setClassifications(Stream.of(traitNames).map(AtlasClassification::new).collect(Collectors.toList()));
        }

        sd.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName + "." + name + "@" + clusterName + "_storage");

        AtlasObjectId tableId = getAtlasObjectId(table);

        sd.setAttribute("table", tableId);

        for (AtlasEntity column : columns) {
            column.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName + "." + name + "." + column.getAttribute(AtlasClient.NAME).toString() + "@" + clusterName);
            column.setAttribute("table", tableId);
        }

        return table;
    }

    private List<AtlasObjectId> getAtlasObjectIds(List<AtlasEntity> columns) {
        List<AtlasObjectId> objIds = new ArrayList<>();
        for (AtlasEntity e : columns) {
            AtlasObjectId oid = getAtlasObjectId(e);
            objIds.add(oid);
        }
        return objIds;
    }

    private AtlasObjectId getAtlasObjectId(AtlasEntity e) {
        return new AtlasObjectId(e.getGuid(), e.getTypeName());
    }

    protected AtlasEntity loadProcess(String name, String description, String user,
                                      List<AtlasEntity> inputTables, List<AtlasEntity> outputTables,
                                      String queryText, String queryPlan, String queryId, String queryGraph, String... traitNames) {
        AtlasEntity process = new AtlasEntity(HIVE_PROCESS_TYPE);
        process.setAttribute("name", name);
        process.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
        process.setAttribute("description", description);
        process.setAttribute("userName", user);
        process.setAttribute("startTime", System.currentTimeMillis());
        process.setAttribute("endTime", System.currentTimeMillis() + 10000);

        process.setAttribute("operationType", "load");
        process.setAttribute("inputs", getAtlasObjectIds(inputTables));
        process.setAttribute("outputs", getAtlasObjectIds(outputTables));

        process.setAttribute("queryText", queryText);
        process.setAttribute("queryPlan", queryPlan);
        process.setAttribute("queryId", queryId);
        process.setAttribute("queryGraph", queryGraph);

        process.setClassifications(Stream.of(traitNames).map(AtlasClassification::new).collect(Collectors.toList()));

        return process;
    }

    AtlasEntity view(String name, AtlasEntity dbId, List<AtlasEntity> inputTables, String... traitNames) {
        AtlasEntity view = new AtlasEntity(VIEW_TYPE);
        view.setAttribute("name", name);
        view.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
        view.setAttribute("userName", "testUser");
        view.setAttribute("startTime", System.currentTimeMillis());
        view.setAttribute("endTime", System.currentTimeMillis() + 10000);

        view.setAttribute("operationType", "view");
        view.setAttribute("query", "create table as select");
        view.setAttribute("queryText", "create table as select");
        view.setAttribute("queryPlan", "viewPlan");
        view.setAttribute("queryId", "view1");
        view.setAttribute("db", getAtlasObjectId(dbId));
        view.setAttribute("inputs", getAtlasObjectIds(inputTables));
        view.setClassifications(Stream.of(traitNames).map(AtlasClassification::new).collect(Collectors.toList()));

        return view;
    }

    AtlasEntity datasetSubType(final String name, String owner) {
        AtlasEntity datasetSubType = new AtlasEntity(DATASET_SUBTYPE);
        datasetSubType.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
        datasetSubType.setAttribute(AtlasClient.NAME, name);
        datasetSubType.setAttribute("owner", owner);

        return datasetSubType;
    }

    public EntityMutationResponse createDummyEntity(String name, String type, String... traitNames) throws AtlasBaseException {
        AtlasEntity entity = new AtlasEntity(type);
        entity.setAttribute("name", name);
        entity.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
        entity.setClassifications(Stream.of(traitNames).map(AtlasClassification::new).collect(Collectors.toList()));
        EntityMutationResponse resp = entityStore.createOrUpdate(new AtlasEntityStream(new AtlasEntity.AtlasEntitiesWithExtInfo(entity)), false);
        return resp;
    }

    public SearchParameters.FilterCriteria getSingleFilterCondition(String attName, SearchParameters.Operator op, String attrValue) {
        SearchParameters.FilterCriteria filterCriteria = new SearchParameters.FilterCriteria();
        filterCriteria.setCondition(SearchParameters.FilterCriteria.Condition.AND);

        List<SearchParameters.FilterCriteria> criteria = new ArrayList<>();
        SearchParameters.FilterCriteria f1 = new SearchParameters.FilterCriteria();
        f1.setAttributeName(attName);
        f1.setOperator(op);
        f1.setAttributeValue(attrValue);
        criteria.add(f1);

        filterCriteria.setCriterion(criteria);
        return filterCriteria;
    }

    public void assignGlossary() {
        try {
            AtlasGlossary glossary = new AtlasGlossary();
            glossary.setName(SALES_GLOSSARY);
            glossary = glossaryService.createGlossary(glossary);

            AtlasGlossaryTerm term = new AtlasGlossaryTerm();
            term.setAnchor(new AtlasGlossaryHeader(glossary.getGuid()));
            term.setName(SALES_TERM);
            term = glossaryService.createTerm(term);

            AtlasGlossaryTerm modernTrade = new AtlasGlossaryTerm();
            modernTrade.setAnchor(new AtlasGlossaryHeader(glossary.getGuid()));
            modernTrade.setName(MODERNTRADE_TERM);
            modernTrade = glossaryService.createTerm(modernTrade);

            AtlasGlossaryTerm ecomm = new AtlasGlossaryTerm();
            ecomm.setAnchor(new AtlasGlossaryHeader(glossary.getGuid()));
            ecomm.setName(ECOMMERCE_TERM);
            ecomm = glossaryService.createTerm(ecomm);

            List<AtlasRelatedObjectId> guids = new ArrayList<>();
            List<AtlasRelatedObjectId> mordernTradeGuids = new ArrayList<>();
            List<AtlasRelatedObjectId> ecomGuid = new ArrayList<>();

            for (AtlasEntityHeader p : hiveEntities.getCreatedEntities()) {
                if (StringUtils.equals(p.getTypeName(), HIVE_TABLE_TYPE)) {
                    AtlasRelatedObjectId obj = new AtlasRelatedObjectId();
                    obj.setGuid(p.getGuid());
                    obj.setTypeName(p.getTypeName());
                    guids.add(obj);

                    if (p.getAttribute("name").equals(timeDim.getAttribute("name"))) {
                        timeDim.setGuid(p.getGuid());
                        AtlasRelatedObjectId obj1 = new AtlasRelatedObjectId();
                        obj1.setGuid(p.getGuid());
                        obj1.setTypeName(p.getTypeName());
                        mordernTradeGuids.add(obj1);
                    } else if (p.getAttribute("name").equals(productDim.getAttribute("name"))) {
                        productDim.setGuid(p.getGuid());
                        AtlasRelatedObjectId obj2 = new AtlasRelatedObjectId();
                        obj2.setGuid(p.getGuid());
                        obj2.setTypeName(p.getTypeName());
                        ecomGuid.add(obj2);
                    } else if (p.getAttribute("name").equals(loggingFactMonthly.getAttribute("name"))) {
                        loggingFactMonthly.setGuid(p.getGuid());
                        AtlasRelatedObjectId obj3 = new AtlasRelatedObjectId();
                        obj3.setGuid(p.getGuid());
                        obj3.setTypeName(p.getTypeName());
                        mordernTradeGuids.add(obj3);
                    }
                }
            }

            glossaryService.assignTermToEntities(term.getGuid(), guids);
            glossaryService.assignTermToEntities(modernTrade.getGuid(), mordernTradeGuids);
            glossaryService.assignTermToEntities(ecomm.getGuid(), ecomGuid);
        } catch (AtlasBaseException e) {
            fail("Failed to assign glossary term");
        }

    }

    public static final String USER_TYPE           = "user";
    public static final String POST_TYPE           = "post";
    public static final String HIGHLIGHT_TYPE      = "highlight";
    public static final String USER_POST_TYPE      = "user_post";
    public static final String HIGHLIGHT_POST_TYPE = "highlight_post";

    private void createTestDataForRelationship() throws AtlasBaseException {

        List<AtlasEntity> entities = new ArrayList<>();

        //users
        AtlasEntity ajayUser    = createUser("Ajay Mishra", new Date(1985,10,9), "Male", "Content Writer");
        entities.add(ajayUser);
        AtlasEntity maryUser    = createUser("Mary Williams", new Date(1998,2,19), "Female", "Architecture");
        entities.add(maryUser);
        AtlasEntity divyaUser   = createUser("Divya Deshmukh", new Date(1980,12,10), "Female", "Designer");
        entities.add(divyaUser);

        //posts
        AtlasEntity indAugPost  = createPost("ind-post@Ajay", "Image", new Date(2021, 8, 15), "Independence Day Celebration");
        entities.add(indAugPost);
        AtlasEntity onamAugPost = createPost("onam-post@Ajay", "Image", new Date(2021, 8, 8), "Onam with Family");
        entities.add(onamAugPost);
        AtlasEntity diwaliPost  = createPost("diwali-post@Ajay", "Reel", new Date(2020, 11, 14), "Diwali get together");
        entities.add(diwaliPost);
        AtlasEntity tripPost    = createPost("trip-post@Ajay", "Video", new Date(2021, 3, 10), "Kashmir a serene");
        entities.add(tripPost);
        AtlasEntity officeParty = createPost("officeparty-post@Ajay", "Video", new Date(2021, 4, 4), "office party");
        entities.add(officeParty);

        AtlasEntity indPost     = createPost("ind-post@Mary", "Video", new Date(2021, 8, 15), "Independence Day Celebration");
        entities.add(indPost);
        AtlasEntity christPost  = createPost("christmas-post@Mary", "Image", new Date(2020, 12, 25), "Christmas Day");
        entities.add(christPost);
        AtlasEntity sraPost     = createPost("elevationSRA@Mary", "Image", new Date(2020, 12, 25), "Elevation");
        entities.add(sraPost);

        AtlasEntity ganeshPost  = createPost("ganeshchaturthi-post@Divya", "Reel", new Date(2020, 12, 25), "Ganesh chaturthi");
        entities.add(ganeshPost);

        //highlights
        AtlasEntity year2021    = createHighlight("year2021@Ajay", "Journey of 2021", new Date(2021, 9, 25));
        entities.add(year2021);
        AtlasEntity festival    = createHighlight("festives@Ajay", "Festive Celebration", new Date(2021, 9, 25));
        entities.add(festival);

        AtlasEntity projectSRA  = createHighlight("projectSRA@Mary", "Project SRA", new Date(2021, 9, 25));
        entities.add(projectSRA);


        // create entity types
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(entities), false);
        List<AtlasEntityHeader> headers = response.getCreatedEntities();
        setGuidInHeaders(entities, headers);

        // create relationship types
        createUserPostRelationship(ajayUser, indAugPost,  "create");
        createUserPostRelationship(ajayUser, onamAugPost, "create");
        createUserPostRelationship(ajayUser, diwaliPost,  "create");
        createUserPostRelationship(ajayUser, tripPost,    "create");
        createUserPostRelationship(ajayUser, officeParty, "create");
        createUserPostRelationship(ajayUser, christPost,  "like");
        createUserPostRelationship(ajayUser, ganeshPost,  "wow");

        createUserPostRelationship(maryUser, indPost,     "create");
        createUserPostRelationship(maryUser, christPost,  "create");
        createUserPostRelationship(maryUser, sraPost,     "create");
        createUserPostRelationship(maryUser, onamAugPost, "wow");

        createUserPostRelationship(divyaUser, ganeshPost, "create");
        createUserPostRelationship(divyaUser, indAugPost, "like");
        createUserPostRelationship(divyaUser, indPost,    "like");
        createUserPostRelationship(divyaUser, onamAugPost,"love");
        createUserPostRelationship(divyaUser, diwaliPost, "love");
        createUserPostRelationship(divyaUser, christPost, "like");

        createHighlightPostRelationship(year2021, indAugPost);
        createHighlightPostRelationship(year2021, onamAugPost);
        createHighlightPostRelationship(year2021, tripPost);
        createHighlightPostRelationship(year2021, officeParty);

        createHighlightPostRelationship(festival, indAugPost);
        createHighlightPostRelationship(festival, onamAugPost);
        createHighlightPostRelationship(festival, diwaliPost);

        createHighlightPostRelationship(projectSRA, sraPost);
    }

    private AtlasEntity createUser(String id, Date birthDate, String gender, String bio) {
        AtlasEntity user = new AtlasEntity(USER_TYPE);
        user.setAttribute("id", id);
        user.setAttribute("birthDate", birthDate);
        user.setAttribute("gender", gender);
        user.setAttribute("bio", bio);

        return user;
    }

    private AtlasEntity createPost(String id, String type, Date postDate, String caption) {
        AtlasEntity post = new AtlasEntity(POST_TYPE);
        post.setAttribute("type", type);
        post.setAttribute("post_date", postDate);
        post.setAttribute("caption", caption);
        post.setAttribute("id", id);

        return post;
    }

    private AtlasEntity createHighlight(String id, String title, Date createdOn) {
        AtlasEntity highlight = new AtlasEntity(HIGHLIGHT_TYPE);
        highlight.setAttribute("created_on", createdOn);
        highlight.setAttribute("title", title);
        highlight.setAttribute("id", id);

        return highlight;
    }

    private void createUserPostRelationship(AtlasEntity user, AtlasEntity post, String reaction) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user_name", user.getAttribute("id"));
        attributes.put("post_name", post.getAttribute("id"));
        attributes.put("reaction", reaction);
        try {
            AtlasRelationship relationship = new AtlasRelationship(USER_POST_TYPE, getAtlasObjectId(user), getAtlasObjectId(post), attributes);
            relationshipStore.create(relationship);
        } catch (AtlasBaseException e) {
            fail("Failed to create Relationship between , " + user.getTypeName() + ", " + post.getTypeName());
        }
    }

    private void createHighlightPostRelationship(AtlasEntity highlight, AtlasEntity post) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("highlight_name", highlight.getAttribute("id"));
        attributes.put("post_name", post.getAttribute("id"));
        try {
            AtlasRelationship relationship = new AtlasRelationship(HIGHLIGHT_POST_TYPE, getAtlasObjectId(highlight), getAtlasObjectId(post), attributes);
            relationshipStore.create(relationship);
        } catch (AtlasBaseException e) {
            fail("Failed to create Relationship between , " + highlight.getTypeName() + ", " + post.getTypeName());
        }
    }

    private List<AtlasEntity> setGuidInHeaders(List<AtlasEntity> entities, List<AtlasEntityHeader> createdEntities) {
        List<String> assignedGuids = new ArrayList<>();

        for (AtlasEntityHeader header : createdEntities) {
            String id = header.getAttribute("id").toString();

            for (AtlasEntity entity : entities) {
                if (id.equalsIgnoreCase(entity.getAttribute("id").toString())) {
                    String guid = header.getGuid();
                    entity.setGuid(guid);
                    assignedGuids.add(guid);

                    break;
                }
            }
        }
        return entities;
    }
}
