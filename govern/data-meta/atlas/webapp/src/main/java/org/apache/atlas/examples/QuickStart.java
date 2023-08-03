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

package org.apache.atlas.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.typedef.*;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.configuration.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A driver that sets up sample types and data for testing purposes.
 * Please take a look at QueryDSL in docs for the Meta Model.
 * todo - move this to examples module.
 */
public class QuickStart {
    public static final String ATLAS_REST_ADDRESS = "atlas.rest.address";
    public static final String SALES_DB = "Sales";
    public static final String SALES_DB_DESCRIPTION = "Sales Database";
    public static final String SALES_FACT_TABLE = "sales_fact";
    public static final String FACT_TRAIT = "Fact_v1";
    public static final String COLUMNS_ATTRIBUTE = "columns";
    public static final String TIME_ID_COLUMN = "time_id";
    public static final String DB_ATTRIBUTE = "db";
    public static final String SALES_FACT_TABLE_DESCRIPTION = "sales fact table";
    public static final String LOAD_SALES_DAILY_PROCESS = "loadSalesDaily";
    public static final String LOAD_SALES_DAILY_PROCESS_DESCRIPTION = "hive query for daily summary";
    public static final String INPUTS_ATTRIBUTE = "inputs";
    public static final String OUTPUTS_ATTRIBUTE = "outputs";
    public static final String TIME_DIM_TABLE = "time_dim";
    public static final String SALES_FACT_DAILY_MV_TABLE = "sales_fact_daily_mv";
    public static final String PRODUCT_DIM_VIEW = "product_dim_view";
    public static final String PRODUCT_DIM_TABLE = "product_dim";
    public static final String INPUT_TABLES_ATTRIBUTE = "inputTables";

    public static void main(String[] args) throws Exception {
        String[] basicAuthUsernamePassword = null;
        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            basicAuthUsernamePassword = AuthenticationUtil.getBasicAuthenticationInput();
        }

        runQuickstart(args, basicAuthUsernamePassword);
    }

    @VisibleForTesting
    static void runQuickstart(String[] args, String[] basicAuthUsernamePassword) throws Exception {
        String[] urls = getServerUrl(args);
        QuickStart quickStart = null;

        try {
            if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
                quickStart = new QuickStart(urls, basicAuthUsernamePassword);
            } else {
                quickStart = new QuickStart(urls);
            }

            // Shows how to create types in Atlas for your meta model
            quickStart.createTypes();

            // Shows how to create entities (instances) for the added types in Atlas
            quickStart.createEntities();

            // Shows some search queries using DSL based on types
            quickStart.search();
        } finally {
            if(quickStart!=null) {
                quickStart.closeConnection();
            }
        }
    }

    static String[] getServerUrl(String[] args) throws AtlasException {
        if (args.length > 0) {
            return args[0].split(",");
        }

        Configuration configuration = ApplicationProperties.get();
        String[] urls = configuration.getStringArray(ATLAS_REST_ADDRESS);
        if (urls == null || urls.length == 0) {
            System.out.println("Usage: quick_start_v1.py <atlas endpoint of format <http/https>://<atlas-fqdn>:<atlas port> like http://localhost:21000>");
            System.exit(-1);
        }

        return urls;
    }

    static final String DATABASE_TYPE = "DB_v1";
    static final String COLUMN_TYPE = "Column_v1";
    static final String TABLE_TYPE = "Table_v1";
    static final String VIEW_TYPE = "View_v1";
    static final String LOAD_PROCESS_TYPE = "LoadProcess_v1";
    static final String STORAGE_DESC_TYPE = "StorageDesc_v1";

    private static final String[] TYPES =
            {DATABASE_TYPE, TABLE_TYPE, STORAGE_DESC_TYPE, COLUMN_TYPE, LOAD_PROCESS_TYPE, VIEW_TYPE, "JdbcAccess_v1",
                    "ETL_v1", "Metric_v1", "PII_v1", "Fact_v1", "Dimension_v1", "Log Data_v1"};

    private final AtlasClient metadataServiceClient;

    QuickStart(String[] urls,String[] basicAuthUsernamePassword) {
        metadataServiceClient = new AtlasClient(urls,basicAuthUsernamePassword);
    }

    QuickStart(String[] urls) throws AtlasException {
        metadataServiceClient = new AtlasClient(urls);
    }


    void createTypes() throws Exception {
        TypesDef typesDef = createTypeDefinitions();

        String typesAsJSON = AtlasType.toV1Json(typesDef);
        System.out.println("typesAsJSON = " + typesAsJSON);
        metadataServiceClient.createType(typesAsJSON);

        // verify types created
        verifyTypesCreated();
    }

    TypesDef createTypeDefinitions() throws Exception {
        ClassTypeDefinition dbClsDef = TypesUtil
                .createClassTypeDef(DATABASE_TYPE, DATABASE_TYPE, null,
                        TypesUtil.createUniqueRequiredAttrDef("name", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("description", AtlasBaseTypeDef.ATLAS_TYPE_STRING), attrDef("locationUri", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("owner", AtlasBaseTypeDef.ATLAS_TYPE_STRING), attrDef("createTime", AtlasBaseTypeDef.ATLAS_TYPE_LONG));

        ClassTypeDefinition storageDescClsDef = TypesUtil
                .createClassTypeDef(STORAGE_DESC_TYPE, STORAGE_DESC_TYPE, null, attrDef("location", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("inputFormat", AtlasBaseTypeDef.ATLAS_TYPE_STRING), attrDef("outputFormat", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("compressed", AtlasBaseTypeDef.ATLAS_TYPE_STRING, Multiplicity.REQUIRED, false, null));

        ClassTypeDefinition columnClsDef = TypesUtil
                .createClassTypeDef(COLUMN_TYPE, COLUMN_TYPE, null, attrDef("name", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("dataType", AtlasBaseTypeDef.ATLAS_TYPE_STRING), attrDef("comment", AtlasBaseTypeDef.ATLAS_TYPE_STRING));

        ClassTypeDefinition tblClsDef = TypesUtil
                .createClassTypeDef(TABLE_TYPE, TABLE_TYPE, Collections.singleton("DataSet"),
                        new AttributeDefinition(DB_ATTRIBUTE, DATABASE_TYPE, Multiplicity.REQUIRED, false, null),
                        new AttributeDefinition("sd", STORAGE_DESC_TYPE, Multiplicity.REQUIRED, true, null),
                        attrDef("createTime", AtlasBaseTypeDef.ATLAS_TYPE_LONG),
                        attrDef("lastAccessTime", AtlasBaseTypeDef.ATLAS_TYPE_LONG), attrDef("retention", AtlasBaseTypeDef.ATLAS_TYPE_LONG),
                        attrDef("viewOriginalText", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("viewExpandedText", AtlasBaseTypeDef.ATLAS_TYPE_STRING), attrDef("tableType", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        attrDef("temporary", AtlasBaseTypeDef.ATLAS_TYPE_BOOLEAN),
                        new AttributeDefinition(COLUMNS_ATTRIBUTE, AtlasBaseTypeDef.getArrayTypeName(COLUMN_TYPE),
                                Multiplicity.COLLECTION, true, null));

        ClassTypeDefinition loadProcessClsDef = TypesUtil
                .createClassTypeDef(LOAD_PROCESS_TYPE, LOAD_PROCESS_TYPE, Collections.singleton("Process"),
                        attrDef("userName", AtlasBaseTypeDef.ATLAS_TYPE_STRING), attrDef("startTime", AtlasBaseTypeDef.ATLAS_TYPE_LONG),
                        attrDef("endTime", AtlasBaseTypeDef.ATLAS_TYPE_LONG),
                        attrDef("queryText", AtlasBaseTypeDef.ATLAS_TYPE_STRING, Multiplicity.REQUIRED),
                        attrDef("queryPlan", AtlasBaseTypeDef.ATLAS_TYPE_STRING, Multiplicity.REQUIRED),
                        attrDef("queryId", AtlasBaseTypeDef.ATLAS_TYPE_STRING, Multiplicity.REQUIRED),
                        attrDef("queryGraph", AtlasBaseTypeDef.ATLAS_TYPE_STRING, Multiplicity.REQUIRED));

        ClassTypeDefinition viewClsDef = TypesUtil
            .createClassTypeDef(VIEW_TYPE, VIEW_TYPE, Collections.singleton("DataSet"),
                new AttributeDefinition("db", DATABASE_TYPE, Multiplicity.REQUIRED, false, null),
                new AttributeDefinition("inputTables", AtlasBaseTypeDef.getArrayTypeName(TABLE_TYPE),
                    Multiplicity.COLLECTION, false, null));

        TraitTypeDefinition dimTraitDef = TypesUtil.createTraitTypeDef("Dimension_v1",  "Dimension Trait", null);

        TraitTypeDefinition factTraitDef = TypesUtil.createTraitTypeDef("Fact_v1", "Fact Trait", null);

        TraitTypeDefinition piiTraitDef = TypesUtil.createTraitTypeDef("PII_v1", "PII Trait", null);

        TraitTypeDefinition metricTraitDef = TypesUtil.createTraitTypeDef("Metric_v1", "Metric Trait", null);

        TraitTypeDefinition etlTraitDef = TypesUtil.createTraitTypeDef("ETL_v1", "ETL Trait", null);

        TraitTypeDefinition jdbcTraitDef = TypesUtil.createTraitTypeDef("JdbcAccess_v1", "JdbcAccess Trait", null);

        TraitTypeDefinition logTraitDef = TypesUtil.createTraitTypeDef("Log Data_v1", "LogData Trait",  null);

        return new TypesDef(Collections.<EnumTypeDefinition>emptyList(), Collections.<StructTypeDefinition>emptyList(),
                Arrays.asList(dimTraitDef, factTraitDef, piiTraitDef, metricTraitDef, etlTraitDef, jdbcTraitDef, logTraitDef),
                Arrays.asList(dbClsDef, storageDescClsDef, columnClsDef, tblClsDef, loadProcessClsDef, viewClsDef));
    }

    AttributeDefinition attrDef(String name, String dT) {
        return attrDef(name, dT, Multiplicity.OPTIONAL, false, null);
    }

    AttributeDefinition attrDef(String name, String dT, Multiplicity m) {
        return attrDef(name, dT, m, false, null);
    }

    AttributeDefinition attrDef(String name, String dT, Multiplicity m, boolean isComposite,
            String reverseAttributeName) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(dT);
        return new AttributeDefinition(name, dT, m, isComposite, reverseAttributeName);
    }

    void createEntities() throws Exception {
        Id salesDB = database(SALES_DB, SALES_DB_DESCRIPTION, "John ETL", "hdfs://host:8000/apps/warehouse/sales");


        Referenceable sd =
                rawStorageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat",
                        true);

        List<Referenceable> salesFactColumns = Arrays.asList(rawColumn(TIME_ID_COLUMN, "int", "time id"), rawColumn("product_id", "int", "product id"),
                        rawColumn("customer_id", "int", "customer id", "PII_v1"),
                        rawColumn("sales", "double", "product id", "Metric_v1"));

        List<Referenceable> logFactColumns = Arrays.asList(rawColumn("time_id", "int", "time id"), rawColumn("app_id", "int", "app id"),
                        rawColumn("machine_id", "int", "machine id"), rawColumn("log", "string", "log data", "Log Data_v1"));

        Id salesFact = table(SALES_FACT_TABLE, SALES_FACT_TABLE_DESCRIPTION, salesDB, sd, "Joe", "Managed",
                salesFactColumns, FACT_TRAIT);

        List<Referenceable> productDimColumns = Arrays.asList(rawColumn("product_id", "int", "product id"), rawColumn("product_name", "string", "product name"),
                        rawColumn("brand_name", "int", "brand name"));

        Id productDim =
                table(PRODUCT_DIM_TABLE, "product dimension table", salesDB, sd, "John Doe", "Managed",
                        productDimColumns, "Dimension_v1");

        List<Referenceable> timeDimColumns = Arrays.asList(rawColumn("time_id", "int", "time id"), rawColumn("dayOfYear", "int", "day Of Year"),
                        rawColumn("weekDay", "int", "week Day"));

        Id timeDim = table(TIME_DIM_TABLE, "time dimension table", salesDB, sd, "John Doe", "External", timeDimColumns,
                "Dimension_v1");


        List<Referenceable> customerDimColumns = Arrays.asList(rawColumn("customer_id", "int", "customer id", "PII_v1"),
                rawColumn("name", "string", "customer name", "PII_v1"),
                rawColumn("address", "string", "customer address", "PII_v1"));

        Id customerDim =
                table("customer_dim", "customer dimension table", salesDB, sd, "fetl", "External", customerDimColumns,
                        "Dimension_v1");


        Id reportingDB =
                database("Reporting", "reporting database", "Jane BI", "hdfs://host:8000/apps/warehouse/reporting");

        Id logDB = database("Logging", "logging database", "Tim ETL", "hdfs://host:8000/apps/warehouse/logging");

        Id salesFactDaily =
                table(SALES_FACT_DAILY_MV_TABLE, "sales fact daily materialized view", reportingDB, sd, "Joe BI",
                        "Managed", salesFactColumns, "Metric_v1");

        Id loggingFactDaily =
                table("log_fact_daily_mv", "log fact daily materialized view", logDB, sd, "Tim ETL", "Managed",
                        logFactColumns, "Log Data_v1");

        loadProcess(LOAD_SALES_DAILY_PROCESS, LOAD_SALES_DAILY_PROCESS_DESCRIPTION, "John ETL",
                Arrays.asList(salesFact, timeDim),
                Collections.singletonList(salesFactDaily), "create table as select ", "plan", "id", "graph", "ETL_v1");

        view(PRODUCT_DIM_VIEW, reportingDB, Collections.singletonList(productDim), "Dimension_v1", "JdbcAccess_v1");

        view("customer_dim_view", reportingDB, Collections.singletonList(customerDim), "Dimension_v1", "JdbcAccess_v1");

        Id salesFactMonthly =
                table("sales_fact_monthly_mv", "sales fact monthly materialized view", reportingDB, sd, "Jane BI",
                        "Managed", salesFactColumns, "Metric_v1");

        loadProcess("loadSalesMonthly", "hive query for monthly summary", "John ETL", Collections.singletonList(salesFactDaily),
                Collections.singletonList(salesFactMonthly), "create table as select ", "plan", "id", "graph", "ETL_v1");

        Id loggingFactMonthly =
                table("logging_fact_monthly_mv", "logging fact monthly materialized view", logDB, sd, "Tim ETL",
                        "Managed", logFactColumns, "Log Data_v1");

        loadProcess("loadLogsMonthly", "hive query for monthly summary", "Tim ETL", Collections.singletonList(loggingFactDaily),
                Collections.singletonList(loggingFactMonthly), "create table as select ", "plan", "id", "graph", "ETL_v1");
    }

    private Id createInstance(Referenceable referenceable) throws Exception {
        String typeName = referenceable.getTypeName();

        String entityJSON = AtlasType.toV1Json(referenceable);
        System.out.println("Submitting new entity= " + entityJSON);
        List<String> guids = metadataServiceClient.createEntity(entityJSON);
        System.out.println("created instance for type " + typeName + ", guid: " + guids);

        // return the Id for created instance with guid
        if (guids.size() > 0) {
            return new Id(guids.get(guids.size() - 1), referenceable.getId().getVersion(), referenceable.getTypeName());
        }

        return null;
    }

    Id database(String name, String description, String owner, String locationUri, String... traitNames)
            throws AtlasBaseException {
        try {
            Referenceable referenceable = new Referenceable(DATABASE_TYPE, traitNames);
            referenceable.set("name", name);
            referenceable.set("description", description);
            referenceable.set("owner", owner);
            referenceable.set("locationUri", locationUri);
            referenceable.set("createTime", System.currentTimeMillis());

            return createInstance(referenceable);
        } catch (Exception e) {
            throw new AtlasBaseException(AtlasErrorCode.QUICK_START, e, String.format("%s database entity creation failed", name));
        }
    }

    Referenceable rawStorageDescriptor(String location, String inputFormat, String outputFormat, boolean compressed) {
            Referenceable referenceable = new Referenceable(STORAGE_DESC_TYPE);
            referenceable.set("location", location);
            referenceable.set("inputFormat", inputFormat);
            referenceable.set("outputFormat", outputFormat);
            referenceable.set("compressed", compressed);

            return referenceable;
    }

    Referenceable rawColumn(String name, String dataType, String comment, String... traitNames) throws AtlasBaseException {
        try {
            Referenceable referenceable = new Referenceable(COLUMN_TYPE, traitNames);
            referenceable.set("name", name);
            referenceable.set("dataType", dataType);
            referenceable.set("comment", comment);

            return referenceable;
        }
        catch(Exception e) {
            throw new AtlasBaseException(AtlasErrorCode.QUICK_START, e, String.format("%s, column entity creation failed", name));
        }
    }

    Id table(String name, String description, Id dbId, Referenceable sd, String owner, String tableType,
            List<Referenceable> columns, String... traitNames) throws AtlasBaseException {
        try {
        Referenceable referenceable = new Referenceable(TABLE_TYPE, traitNames);
        referenceable.set("name", name);
        referenceable.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
        referenceable.set("description", description);
        referenceable.set("owner", owner);
        referenceable.set("tableType", tableType);
        referenceable.set("createTime", System.currentTimeMillis());
        referenceable.set("lastAccessTime", System.currentTimeMillis());
        referenceable.set("retention", System.currentTimeMillis());
        referenceable.set("db", dbId);
        referenceable.set("sd", sd);
        referenceable.set("columns", columns);

        return createInstance(referenceable);
        } catch (Exception e) {
            throw new AtlasBaseException(AtlasErrorCode.QUICK_START, e, String.format("%s table entity creation failed", name));
        }
    }

    Id loadProcess(String name, String description, String user, List<Id> inputTables, List<Id> outputTables,
                   String queryText, String queryPlan, String queryId, String queryGraph, String... traitNames)
            throws AtlasBaseException {
        try {
            Referenceable referenceable = new Referenceable(LOAD_PROCESS_TYPE, traitNames);
            // super type attributes
            referenceable.set(AtlasClient.NAME, name);
            referenceable.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
            referenceable.set("description", description);
            referenceable.set(INPUTS_ATTRIBUTE, inputTables);
            referenceable.set(OUTPUTS_ATTRIBUTE, outputTables);

            referenceable.set("user", user);
            referenceable.set("startTime", System.currentTimeMillis());
            referenceable.set("endTime", System.currentTimeMillis() + 10000);

            referenceable.set("queryText", queryText);
            referenceable.set("queryPlan", queryPlan);
            referenceable.set("queryId", queryId);
            referenceable.set("queryGraph", queryGraph);

            return createInstance(referenceable);
        } catch (Exception e) {
            throw new AtlasBaseException(AtlasErrorCode.QUICK_START, e, String.format("%s process entity creation failed", name));
        }
    }

    Id view(String name, Id dbId, List<Id> inputTables, String... traitNames) throws AtlasBaseException {
        try {
            Referenceable referenceable = new Referenceable(VIEW_TYPE, traitNames);
            referenceable.set("name", name);
            referenceable.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
            referenceable.set("db", dbId);

            referenceable.set(INPUT_TABLES_ATTRIBUTE, inputTables);

            return createInstance(referenceable);
        } catch (Exception e) {
            throw new AtlasBaseException(AtlasErrorCode.QUICK_START, e, String.format("%s Id creation", name));
        }
    }

    private void verifyTypesCreated() throws AtlasBaseException {
        try {
            List<String> types = metadataServiceClient.listTypes();
            for (String type : TYPES) {
                assert types.contains(type);
            }
        } catch (Exception e) {
            throw new AtlasBaseException(AtlasErrorCode.QUICK_START, e, "view creation failed.");
        }
    }

    private String[] getDSLQueries() {
        return new String[]{"from DB_v1", "DB_v1", "DB_v1 where name=\"Reporting\"", "DB_v1 where DB_v1.name=\"Reporting\"",
                "DB_v1 name = \"Reporting\"", "DB_v1 DB_v1.name = \"Reporting\"",
                "DB_v1 where name=\"Reporting\" select name, owner", "DB_v1 where DB_v1.name=\"Reporting\" select name, owner",
                "DB_v1 has name", "DB_v1 where DB_v1 has name",
                // "DB_v1, Table_v1", TODO: Fix "DB, Table", Table, db; Table db works
                "DB_v1 is JdbcAccess",
            /*
            "DB, hive_process has name",
            "DB as db1, Table where db1.name = \"Reporting\"",
            "DB where DB.name=\"Reporting\" and DB.createTime < " + System.currentTimeMillis()},
            */
                "from Table_v1", "Table_v1", "Table_v1 is Dimension_v1", "Column_v1 where Column_v1 isa PII_v1", "View_v1 is Dimension_v1",
            /*"Column where Column isa PII select Column.name",*/
                "Column_v1 select Column_v1.name", "Column_v1 select name", "Column_v1 where Column_v1.name=\"customer_id\"",
                "from Table_v1 select Table_v1.name", "DB_v1 where (name = \"Reporting\")",
                "DB_v1 where (name = \"Reporting\") select name as _col_0, owner as _col_1", "DB_v1 where DB_v1 is JdbcAccess_v1",
                "DB_v1 where DB_v1 has name", "DB_v1 Table_v1", "DB_v1 where DB_v1 has name",
                "DB_v1 as db1 Table where (db1.name = \"Reporting\")",
                "DB_v1 where (name = \"Reporting\") select name as _col_0, (createTime + 1) as _col_1 ",
            /*
            todo: does not work
            "DB where (name = \"Reporting\") and ((createTime + 1) > 0)",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") select db1.name
            as dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) or (db1.name = \"Reporting\") select db1.name as
             dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") or db1 has owner
            select db1.name as dbName, tab.name as tabName",
            "DB as db1 Table as tab where ((db1.createTime + 1) > 0) and (db1.name = \"Reporting\") or db1 has owner
            select db1.name as dbName, tab.name as tabName",
            */
                // trait searches
                "Dimension_v1",
            /*"Fact", - todo: does not work*/
                "JdbcAccess_v1", "ETL_v1", "Metric_v1", "PII_v1", "`Log Data_v1`",
            /*
            // Lineage - todo - fix this, its not working
            "Table hive_process outputTables",
            "Table loop (hive_process outputTables)",
            "Table as _loop0 loop (hive_process outputTables) withPath",
            "Table as src loop (hive_process outputTables) as dest select src.name as srcTable, dest.name as
            destTable withPath",
            */
                "Table_v1 where name=\"sales_fact\", columns",
                "Table_v1 where name=\"sales_fact\", columns as column select column.name, column.dataType, column"
                        + ".comment",
                "from DataSet", "from Process",};
    }

    private void search() throws AtlasBaseException {
        try {
            for (String dslQuery : getDSLQueries()) {
                JsonNode results = metadataServiceClient.search(dslQuery, 10, 0);
                if (results != null && results instanceof ArrayNode) {
                    System.out.println("query [" + dslQuery + "] returned [" + results.size() + "] rows");
                } else {
                    System.out.println("query [" + dslQuery + "] failed, results:" + results);
                }
            }
        } catch (Exception e) {
            throw new AtlasBaseException(AtlasErrorCode.QUICK_START, e, "one or more dsl queries failed");
        }
    }

    private void closeConnection() {
        if (metadataServiceClient != null) {
            metadataServiceClient.close();
        }
    }
}
