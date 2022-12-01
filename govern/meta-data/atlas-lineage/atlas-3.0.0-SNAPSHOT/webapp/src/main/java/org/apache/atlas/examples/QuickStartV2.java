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

import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasException;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult.AtlasFullTextResult;
import org.apache.atlas.model.discovery.AtlasSearchResult.AttributeSearchResult;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations.EntityOperation;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.atlas.model.lineage.AtlasLineageInfo.LineageDirection;
import org.apache.atlas.model.lineage.AtlasLineageInfo.LineageRelation;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.PropagateTags;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;

import javax.ws.rs.core.MultivaluedMap;
import java.util.*;

import static java.util.Arrays.asList;
import static org.apache.atlas.AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME;
import static org.apache.atlas.model.typedef.AtlasBusinessMetadataDef.ATTR_OPTION_APPLICABLE_ENTITY_TYPES;
import static org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory.AGGREGATION;
import static org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory.COMPOSITION;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality.SET;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE;
import static org.apache.atlas.type.AtlasTypeUtil.createClassTypeDef;
import static org.apache.atlas.type.AtlasTypeUtil.createOptionalAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.createRelationshipEndDef;
import static org.apache.atlas.type.AtlasTypeUtil.createRelationshipTypeDef;
import static org.apache.atlas.type.AtlasTypeUtil.createRequiredAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.createTraitTypeDef;
import static org.apache.atlas.type.AtlasTypeUtil.createUniqueRequiredAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.toAtlasRelatedObjectId;
import static org.apache.atlas.type.AtlasTypeUtil.toAtlasRelatedObjectIds;

/**
 * A driver that sets up sample types and entities using v2 types and entity model for testing purposes.
 */
public class QuickStartV2 {
    public static final String ATLAS_REST_ADDRESS          = "atlas.rest.address";

    public static final String SALES_DB                    = "Sales";
    public static final String REPORTING_DB                = "Reporting";
    public static final String LOGGING_DB                  = "Logging";

    public static final String SALES_FACT_TABLE            = "sales_fact";
    public static final String PRODUCT_DIM_TABLE           = "product_dim";
    public static final String CUSTOMER_DIM_TABLE          = "customer_dim";
    public static final String TIME_DIM_TABLE              = "time_dim";
    public static final String SALES_FACT_DAILY_MV_TABLE   = "sales_fact_daily_mv";
    public static final String SALES_FACT_MONTHLY_MV_TABLE = "sales_fact_monthly_mv";
    public static final String LOG_FACT_DAILY_MV_TABLE     = "log_fact_daily_mv";
    public static final String LOG_FACT_MONTHLY_MV_TABLE   = "logging_fact_monthly_mv";

    public static final String TIME_ID_COLUMN              = "time_id";
    public static final String PRODUCT_ID_COLUMN           = "product_id";
    public static final String CUSTOMER_ID_COLUMN          = "customer_id";
    public static final String APP_ID_COLUMN               = "app_id";
    public static final String MACHINE_ID_COLUMN           = "machine_id";
    public static final String PRODUCT_NAME_COLUMN         = "product_name";
    public static final String BRAND_NAME_COLUMN           = "brand_name";
    public static final String NAME_COLUMN                 = "name";
    public static final String SALES_COLUMN                = "sales";
    public static final String LOG_COLUMN                  = "log";
    public static final String ADDRESS_COLUMN              = "address";
    public static final String DAY_OF_YEAR_COLUMN          = "dayOfYear";
    public static final String WEEKDAY_COLUMN              = "weekDay";

    public static final String DIMENSION_CLASSIFICATION    = "Dimension";
    public static final String FACT_CLASSIFICATION         = "Fact";
    public static final String PII_CLASSIFICATION          = "PII";
    public static final String METRIC_CLASSIFICATION       = "Metric";
    public static final String ETL_CLASSIFICATION          = "ETL";
    public static final String JDBC_CLASSIFICATION         = "JdbcAccess";
    public static final String LOGDATA_CLASSIFICATION      = "Log Data";

    public static final String LOAD_SALES_DAILY_PROCESS         = "loadSalesDaily";
    public static final String LOAD_SALES_DAILY_PROCESS_EXEC1   = "loadSalesDailyExec1";
    public static final String LOAD_SALES_DAILY_PROCESS_EXEC2   = "loadSalesDailyExec2";
    public static final String LOAD_SALES_MONTHLY_PROCESS       = "loadSalesMonthly";
    public static final String LOAD_SALES_MONTHLY_PROCESS_EXEC1 = "loadSalesMonthlyExec1";
    public static final String LOAD_SALES_MONTHLY_PROCESS_EXEC2 = "loadSalesMonthlyExec2";
    public static final String LOAD_LOGS_MONTHLY_PROCESS        = "loadLogsMonthly";
    public static final String LOAD_LOGS_MONTHLY_PROCESS_EXEC1  = "loadLogsMonthlyExec1";
    public static final String LOAD_LOGS_MONTHLY_PROCESS_EXEC2  = "loadLogsMonthlyExec2";

    public static final String PRODUCT_DIM_VIEW            = "product_dim_view";
    public static final String CUSTOMER_DIM_VIEW           = "customer_dim_view";

    public static final String DATABASE_TYPE               = "DB";
    public static final String COLUMN_TYPE                 = "Column";
    public static final String TABLE_TYPE                  = "Table";
    public static final String VIEW_TYPE                   = "View";
    public static final String LOAD_PROCESS_TYPE           = "LoadProcess";
    public static final String LOAD_PROCESS_EXECUTION_TYPE = "LoadProcessExecution";
    public static final String STORAGE_DESC_TYPE           = "StorageDesc";

    public static final String TABLE_DATABASE_TYPE         = "Table_DB";
    public static final String VIEW_DATABASE_TYPE          = "View_DB";
    public static final String VIEW_TABLES_TYPE            = "View_Tables";
    public static final String TABLE_COLUMNS_TYPE          = "Table_Columns";
    public static final String TABLE_STORAGE_DESC_TYPE     = "Table_StorageDesc";
    public static final String PROCESS_PROCESS_EXECUTION_DESC_TYPE = "Process_ProcessExecution";

    public static final String VERSION_1                   = "1.0";
    public static final String MANAGED_TABLE               = "Managed";
    public static final String EXTERNAL_TABLE              = "External";
    public static final String CLUSTER_SUFFIX              = "@cl1";

    public static final String[] TYPES = { DATABASE_TYPE, TABLE_TYPE, STORAGE_DESC_TYPE, COLUMN_TYPE, LOAD_PROCESS_TYPE, LOAD_PROCESS_EXECUTION_TYPE,
                                           VIEW_TYPE, JDBC_CLASSIFICATION, ETL_CLASSIFICATION, METRIC_CLASSIFICATION,
                                           PII_CLASSIFICATION, FACT_CLASSIFICATION, DIMENSION_CLASSIFICATION, LOGDATA_CLASSIFICATION,
                                           TABLE_DATABASE_TYPE, VIEW_DATABASE_TYPE, VIEW_TABLES_TYPE, TABLE_COLUMNS_TYPE, TABLE_STORAGE_DESC_TYPE };

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

        QuickStartV2 quickStartV2 = null;
        try {
            if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
                quickStartV2 = new QuickStartV2(urls, basicAuthUsernamePassword);
            } else {
                quickStartV2 = new QuickStartV2(urls);
            }

            // Shows how to create v2 types in Atlas for your meta model
            quickStartV2.createTypes();

            // Shows how to create v2 entities (instances) for the added types in Atlas
            quickStartV2.createEntities();

            // Shows some search queries using DSL based on types
            quickStartV2.search();

            // Shows some lineage information on entity
            quickStartV2.lineage();
        } finally {
            if (quickStartV2!= null) {
                quickStartV2.closeConnection();
            }
        }

    }

    static String[] getServerUrl(String[] args) throws AtlasException {
        if (args.length > 0) {
            return args[0].split(",");
        }

        Configuration configuration = ApplicationProperties.get();
        String[]      urls          = configuration.getStringArray(ATLAS_REST_ADDRESS);

        if (ArrayUtils.isEmpty(urls)) {
            System.out.println("org.apache.atlas.examples.QuickStartV2 <Atlas REST address <http/https>://<atlas-fqdn>:<atlas-port> like http://localhost:21000>");
            System.exit(-1);
        }

        return urls;
    }

    private final AtlasClientV2 atlasClientV2;

    QuickStartV2(String[] urls, String[] basicAuthUsernamePassword) {
        atlasClientV2 = new AtlasClientV2(urls,basicAuthUsernamePassword);
    }

    QuickStartV2(String[] urls) throws AtlasException {
        atlasClientV2 = new AtlasClientV2(urls);
    }


    void createTypes() throws Exception {
        AtlasTypesDef atlasTypesDef = createTypeDefinitions();

        System.out.println("\nCreating sample types: ");

        atlasClientV2.createAtlasTypeDefs(atlasTypesDef);

        verifyTypesCreated();
    }

    AtlasTypesDef createTypeDefinitions() {
        // Entity-Definitions
        AtlasEntityDef dbTypeDef      = createClassTypeDef(DATABASE_TYPE, DATABASE_TYPE, VERSION_1, Collections.singleton("DataSet"),
                                                            createOptionalAttrDef("locationUri", "string"),
                                                            createOptionalAttrDef("createTime", "long"));

        AtlasEntityDef tableTypeDef   = createClassTypeDef(TABLE_TYPE, TABLE_TYPE, VERSION_1, Collections.singleton("DataSet"),
                                                            new HashMap<String, String>() {{ put("schemaElementsAttribute", "columns"); }} ,
                                                            createOptionalAttrDef("createTime", "long"),
                                                            createOptionalAttrDef("lastAccessTime", "long"),
                                                            createOptionalAttrDef("retention", "long"),
                                                            createOptionalAttrDef("viewOriginalText", "string"),
                                                            createOptionalAttrDef("viewExpandedText", "string"),
                                                            createOptionalAttrDef("tableType", "string"),
                                                            createOptionalAttrDef("temporary", "boolean"));

        AtlasEntityDef colTypeDef     = createClassTypeDef(COLUMN_TYPE, COLUMN_TYPE, VERSION_1, Collections.singleton("DataSet"),
                                                            new HashMap<String, String>() {{ put("schemaAttributes", "[\"name\", \"description\", \"owner\", \"type\", \"comment\", \"position\"]"); }},
                                                            createOptionalAttrDef("dataType", "string"),
                                                            createOptionalAttrDef("comment", "string"));

        AtlasEntityDef sdTypeDef      = createClassTypeDef(STORAGE_DESC_TYPE, STORAGE_DESC_TYPE, VERSION_1, Collections.singleton("DataSet"),
                                                            createOptionalAttrDef("location", "string"),
                                                            createOptionalAttrDef("inputFormat", "string"),
                                                            createOptionalAttrDef("outputFormat", "string"),
                                                            createRequiredAttrDef("compressed", "boolean"));

        AtlasEntityDef processTypeDef = createClassTypeDef(LOAD_PROCESS_TYPE, LOAD_PROCESS_TYPE, VERSION_1, Collections.singleton("Process"),
                                                            createOptionalAttrDef("userName", "string"),
                                                            createOptionalAttrDef("startTime", "long"),
                                                            createOptionalAttrDef("endTime", "long"),
                                                            createRequiredAttrDef("queryText", "string"),
                                                            createRequiredAttrDef("queryPlan", "string"),
                                                            createRequiredAttrDef("queryId", "string"),
                                                            createRequiredAttrDef("queryGraph", "string"));

        AtlasEntityDef processExecutionTypeDef = createClassTypeDef(LOAD_PROCESS_EXECUTION_TYPE, LOAD_PROCESS_EXECUTION_TYPE, VERSION_1, Collections.singleton("ProcessExecution"),
                createOptionalAttrDef("userName", "string"),
                createOptionalAttrDef("startTime", "long"),
                createOptionalAttrDef("endTime", "long"),
                createRequiredAttrDef("queryText", "string"),
                createRequiredAttrDef("queryPlan", "string"),
                createRequiredAttrDef("queryId", "string"),
                createRequiredAttrDef("queryGraph", "string"));

        processExecutionTypeDef.setOption(AtlasEntityDef.OPTION_DISPLAY_TEXT_ATTRIBUTE, "queryText");

        AtlasEntityDef viewTypeDef    = createClassTypeDef(VIEW_TYPE, VIEW_TYPE, VERSION_1, Collections.singleton("DataSet"));

        // Relationship-Definitions
        AtlasRelationshipDef tableDatabaseTypeDef            = createRelationshipTypeDef(TABLE_DATABASE_TYPE, TABLE_DATABASE_TYPE, VERSION_1, AGGREGATION, PropagateTags.NONE,
                                                                                    createRelationshipEndDef(TABLE_TYPE, "db", SINGLE, false),
                                                                                    createRelationshipEndDef(DATABASE_TYPE, "tables", SET, true));

        AtlasRelationshipDef viewDatabaseTypeDef             = createRelationshipTypeDef(VIEW_DATABASE_TYPE, VIEW_DATABASE_TYPE, VERSION_1, AGGREGATION, PropagateTags.NONE,
                                                                                    createRelationshipEndDef(VIEW_TYPE, "db", SINGLE, false),
                                                                                    createRelationshipEndDef(DATABASE_TYPE, "views", SET, true));

        AtlasRelationshipDef viewTablesTypeDef               = createRelationshipTypeDef(VIEW_TABLES_TYPE, VIEW_TABLES_TYPE, VERSION_1, AGGREGATION, PropagateTags.NONE,
                                                                                    createRelationshipEndDef(VIEW_TYPE, "inputTables", SET, true),
                                                                                    createRelationshipEndDef(TABLE_TYPE, "view", SINGLE, false));

        AtlasRelationshipDef tableColumnsTypeDef             = createRelationshipTypeDef(TABLE_COLUMNS_TYPE, TABLE_COLUMNS_TYPE, VERSION_1, COMPOSITION, PropagateTags.NONE,
                                                                                    createRelationshipEndDef(TABLE_TYPE, "columns", SET, true),
                                                                                    createRelationshipEndDef(COLUMN_TYPE, "table", SINGLE, false));

        AtlasRelationshipDef tableStorageDescTypeDef         = createRelationshipTypeDef(TABLE_STORAGE_DESC_TYPE, TABLE_STORAGE_DESC_TYPE, VERSION_1, COMPOSITION, PropagateTags.NONE,
                                                                                    createRelationshipEndDef(TABLE_TYPE, "sd", SINGLE, true),
                                                                                    createRelationshipEndDef(STORAGE_DESC_TYPE, "table", SINGLE, false));
        AtlasRelationshipDef processProcessExecutionTypeDef  = createRelationshipTypeDef(PROCESS_PROCESS_EXECUTION_DESC_TYPE, PROCESS_PROCESS_EXECUTION_DESC_TYPE, VERSION_1, AGGREGATION, PropagateTags.NONE,
                                                                                    createRelationshipEndDef(LOAD_PROCESS_TYPE, "processExecutions", SET, true),
                                                                                    createRelationshipEndDef(LOAD_PROCESS_EXECUTION_TYPE, "process", SINGLE, false));


        // Classification-Definitions
        AtlasClassificationDef dimClassifDef    = createTraitTypeDef(DIMENSION_CLASSIFICATION,  "Dimension Classification", VERSION_1, Collections.emptySet());
        AtlasClassificationDef factClassifDef   = createTraitTypeDef(FACT_CLASSIFICATION, "Fact Classification", VERSION_1, Collections.emptySet());
        AtlasClassificationDef piiClassifDef    = createTraitTypeDef(PII_CLASSIFICATION, "PII Classification", VERSION_1, Collections.emptySet());
        AtlasClassificationDef metricClassifDef = createTraitTypeDef(METRIC_CLASSIFICATION, "Metric Classification", VERSION_1, Collections.emptySet());
        AtlasClassificationDef etlClassifDef    = createTraitTypeDef(ETL_CLASSIFICATION, "ETL Classification", VERSION_1, Collections.emptySet());
        AtlasClassificationDef jdbcClassifDef   = createTraitTypeDef(JDBC_CLASSIFICATION, "JdbcAccess Classification", VERSION_1, Collections.emptySet());
        AtlasClassificationDef logClassifDef    = createTraitTypeDef(LOGDATA_CLASSIFICATION, "LogData Classification", VERSION_1, Collections.emptySet());

        List<AtlasEntityDef>         entityDefs         = asList(dbTypeDef, sdTypeDef, colTypeDef, tableTypeDef, processTypeDef, processExecutionTypeDef, viewTypeDef);
        List<AtlasRelationshipDef>   relationshipDefs   = asList(tableDatabaseTypeDef, viewDatabaseTypeDef, viewTablesTypeDef, tableColumnsTypeDef, tableStorageDescTypeDef, processProcessExecutionTypeDef);
        List<AtlasClassificationDef> classificationDefs = asList(dimClassifDef, factClassifDef, piiClassifDef, metricClassifDef, etlClassifDef, jdbcClassifDef, logClassifDef);

        // BusinessMetadata definitions
        AtlasAttributeDef bmAttrDef1 = new AtlasAttributeDef("attr1", "int");
        AtlasAttributeDef bmAttrDef2 = new AtlasAttributeDef("attr2", "int");

        bmAttrDef1.setOption(ATTR_OPTION_APPLICABLE_ENTITY_TYPES, AtlasType.toJson(Collections.singleton(TABLE_TYPE)));
        bmAttrDef1.setIsOptional(true);
        bmAttrDef1.setIsUnique(false);

        bmAttrDef2.setOption(ATTR_OPTION_APPLICABLE_ENTITY_TYPES, AtlasType.toJson(Collections.singleton(TABLE_TYPE)));
        bmAttrDef2.setIsOptional(true);
        bmAttrDef2.setIsUnique(false);

        AtlasBusinessMetadataDef testBusinessMetadataDef = new AtlasBusinessMetadataDef("test_businessMetadata", "test_description", VERSION_1);

        testBusinessMetadataDef.setAttributeDefs(Arrays.asList(bmAttrDef1, bmAttrDef2));

        List<AtlasBusinessMetadataDef> businessMetadataDefs  = asList(testBusinessMetadataDef);

        return new AtlasTypesDef(Collections.emptyList(), Collections.emptyList(), classificationDefs, entityDefs, relationshipDefs, businessMetadataDefs);
    }

    void createEntities() throws Exception {
        System.out.println("\nCreating sample entities: ");

        // Database entities
        AtlasEntity salesDB     = createDatabase(SALES_DB, "sales database", "John ETL", "hdfs://host:8000/apps/warehouse/sales");
        AtlasEntity reportingDB = createDatabase(REPORTING_DB, "reporting database", "Jane BI", "hdfs://host:8000/apps/warehouse/reporting");
        AtlasEntity logDB       = createDatabase(LOGGING_DB, "logging database", "Tim ETL", "hdfs://host:8000/apps/warehouse/logging");

        // Table entities
        AtlasEntity salesFact = createTable(SALES_FACT_TABLE, "sales fact table", salesDB, "Joe", MANAGED_TABLE,
                                                        Arrays.asList(createColumn(SALES_DB, SALES_FACT_TABLE, TIME_ID_COLUMN, "int", "time id"),
                                                                      createColumn(SALES_DB, SALES_FACT_TABLE, PRODUCT_ID_COLUMN, "int", "product id"),
                                                                      createColumn(SALES_DB, SALES_FACT_TABLE, CUSTOMER_ID_COLUMN, "int", "customer id", PII_CLASSIFICATION),
                                                                      createColumn(SALES_DB, SALES_FACT_TABLE, SALES_COLUMN, "double", "product id", METRIC_CLASSIFICATION)),
                                                        FACT_CLASSIFICATION);

        AtlasEntity productDim = createTable(PRODUCT_DIM_TABLE, "product dimension table", salesDB, "John Doe", MANAGED_TABLE,
                                                        Arrays.asList(createColumn(SALES_DB, PRODUCT_DIM_TABLE, PRODUCT_ID_COLUMN, "int", "product id"),
                                                                      createColumn(SALES_DB, PRODUCT_DIM_TABLE, PRODUCT_NAME_COLUMN, "string", "product name"),
                                                                      createColumn(SALES_DB, PRODUCT_DIM_TABLE, BRAND_NAME_COLUMN, "int", "brand name")),
                                                        DIMENSION_CLASSIFICATION);

        AtlasEntity customerDim = createTable(CUSTOMER_DIM_TABLE, "customer dimension table", salesDB, "fetl", EXTERNAL_TABLE,
                                                        Arrays.asList(createColumn(SALES_DB, CUSTOMER_DIM_TABLE, CUSTOMER_ID_COLUMN, "int", "customer id", PII_CLASSIFICATION),
                                                                      createColumn(SALES_DB, CUSTOMER_DIM_TABLE, NAME_COLUMN, "string", "customer name", PII_CLASSIFICATION),
                                                                      createColumn(SALES_DB, CUSTOMER_DIM_TABLE, ADDRESS_COLUMN, "string", "customer address", PII_CLASSIFICATION)),
                                                        DIMENSION_CLASSIFICATION);

        AtlasEntity timeDim = createTable(TIME_DIM_TABLE, "time dimension table", salesDB, "John Doe", EXTERNAL_TABLE,
                                                        Arrays.asList(createColumn(SALES_DB, TIME_DIM_TABLE, TIME_ID_COLUMN, "int", "time id"),
                                                                      createColumn(SALES_DB, TIME_DIM_TABLE, DAY_OF_YEAR_COLUMN, "int", "day Of Year"),
                                                                      createColumn(SALES_DB, TIME_DIM_TABLE, WEEKDAY_COLUMN, "int", "week Day")),
                                                        DIMENSION_CLASSIFICATION);

        AtlasEntity loggingFactDaily = createTable(LOG_FACT_DAILY_MV_TABLE, "log fact daily materialized view", logDB, "Tim ETL", MANAGED_TABLE,
                                                        Arrays.asList(createColumn(LOGGING_DB, LOG_FACT_DAILY_MV_TABLE, TIME_ID_COLUMN, "int", "time id"),
                                                                      createColumn(LOGGING_DB, LOG_FACT_DAILY_MV_TABLE, APP_ID_COLUMN, "int", "app id"),
                                                                      createColumn(LOGGING_DB, LOG_FACT_DAILY_MV_TABLE, MACHINE_ID_COLUMN, "int", "machine id"),
                                                                      createColumn(LOGGING_DB, LOG_FACT_DAILY_MV_TABLE, LOG_COLUMN, "string", "log data", LOGDATA_CLASSIFICATION)),
                                                        LOGDATA_CLASSIFICATION);

        AtlasEntity loggingFactMonthly = createTable(LOG_FACT_MONTHLY_MV_TABLE, "logging fact monthly materialized view", logDB, "Tim ETL", MANAGED_TABLE,
                                                        Arrays.asList(createColumn(LOGGING_DB, LOG_FACT_MONTHLY_MV_TABLE, TIME_ID_COLUMN, "int", "time id"),
                                                                      createColumn(LOGGING_DB, LOG_FACT_MONTHLY_MV_TABLE, APP_ID_COLUMN, "int", "app id"),
                                                                      createColumn(LOGGING_DB, LOG_FACT_MONTHLY_MV_TABLE, MACHINE_ID_COLUMN, "int", "machine id"),
                                                                      createColumn(LOGGING_DB, LOG_FACT_MONTHLY_MV_TABLE, LOG_COLUMN, "string", "log data", LOGDATA_CLASSIFICATION)),
                                                        LOGDATA_CLASSIFICATION);

        AtlasEntity salesFactDaily = createTable(SALES_FACT_DAILY_MV_TABLE, "sales fact daily materialized view", reportingDB, "Joe BI", MANAGED_TABLE,
                                                        Arrays.asList(createColumn(REPORTING_DB, SALES_FACT_DAILY_MV_TABLE, TIME_ID_COLUMN, "int", "time id"),
                                                                      createColumn(REPORTING_DB, SALES_FACT_DAILY_MV_TABLE, PRODUCT_ID_COLUMN, "int", "product id"),
                                                                      createColumn(REPORTING_DB, SALES_FACT_DAILY_MV_TABLE, CUSTOMER_ID_COLUMN, "int", "customer id", PII_CLASSIFICATION),
                                                                      createColumn(REPORTING_DB, SALES_FACT_DAILY_MV_TABLE, SALES_COLUMN, "double", "product id", METRIC_CLASSIFICATION)),
                                                        METRIC_CLASSIFICATION);

        AtlasEntity salesFactMonthly = createTable(SALES_FACT_MONTHLY_MV_TABLE, "sales fact monthly materialized view", reportingDB, "Jane BI", MANAGED_TABLE,
                                                        Arrays.asList(createColumn(REPORTING_DB, SALES_FACT_MONTHLY_MV_TABLE, TIME_ID_COLUMN, "int", "time id"),
                                                                      createColumn(REPORTING_DB, SALES_FACT_MONTHLY_MV_TABLE, PRODUCT_ID_COLUMN, "int", "product id"),
                                                                      createColumn(REPORTING_DB, SALES_FACT_MONTHLY_MV_TABLE, CUSTOMER_ID_COLUMN, "int", "customer id", PII_CLASSIFICATION),
                                                                      createColumn(REPORTING_DB, SALES_FACT_MONTHLY_MV_TABLE, SALES_COLUMN, "double", "product id", METRIC_CLASSIFICATION)),
                                                        METRIC_CLASSIFICATION);

        // View entities
        createView(PRODUCT_DIM_VIEW, reportingDB, asList(productDim), DIMENSION_CLASSIFICATION, JDBC_CLASSIFICATION);
        createView(CUSTOMER_DIM_VIEW, reportingDB, asList(customerDim), DIMENSION_CLASSIFICATION, JDBC_CLASSIFICATION);

        // Process entities
        AtlasEntity loadProcess = createProcess(LOAD_SALES_DAILY_PROCESS, "hive query for daily summary", "John ETL",
                      asList(salesFact, timeDim),
                      asList(salesFactDaily),
                      "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);

        createProcessExecution(loadProcess, LOAD_SALES_DAILY_PROCESS_EXEC1, "hive query execution 1 for daily summary", "John ETL",
                "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);

        createProcessExecution(loadProcess, LOAD_SALES_DAILY_PROCESS_EXEC2, "hive query execution 2 for daily summary", "John ETL",
                "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);

        AtlasEntity loadProcess2 = createProcess(LOAD_SALES_MONTHLY_PROCESS, "hive query for monthly summary", "John ETL",
                      asList(salesFactDaily),
                      asList(salesFactMonthly),
                      "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);
        createProcessExecution(loadProcess2, LOAD_SALES_MONTHLY_PROCESS_EXEC1, "hive query execution 1 for monthly summary", "John ETL",
                "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);

        createProcessExecution(loadProcess2, LOAD_SALES_MONTHLY_PROCESS_EXEC2, "hive query execution 2 for monthly summary", "John ETL",
                "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);


        AtlasEntity loadProcess3 = createProcess(LOAD_LOGS_MONTHLY_PROCESS, "hive query for monthly summary", "Tim ETL",
                      asList(loggingFactDaily),
                      asList(loggingFactMonthly),
                      "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);
        createProcessExecution(loadProcess3, LOAD_LOGS_MONTHLY_PROCESS_EXEC1, "hive query execution 1 for monthly summary", "Tim ETL",
                "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);
        createProcessExecution(loadProcess3, LOAD_LOGS_MONTHLY_PROCESS_EXEC2, "hive query execution 1 for monthly summary", "Tim ETL",
                "create table as select ", "plan", "id", "graph", ETL_CLASSIFICATION);
    }

    private AtlasEntity createInstance(AtlasEntity entity) throws Exception {
        return createInstance(new AtlasEntityWithExtInfo(entity));
    }

    private AtlasEntity createInstance(AtlasEntityWithExtInfo entityWithExtInfo) throws Exception {
        AtlasEntity             ret      = null;
        EntityMutationResponse  response = atlasClientV2.createEntity(entityWithExtInfo);
        List<AtlasEntityHeader> entities = response.getEntitiesByOperation(EntityOperation.CREATE);

        if (CollectionUtils.isNotEmpty(entities)) {
            AtlasEntityWithExtInfo getByGuidResponse = atlasClientV2.getEntityByGuid(entities.get(0).getGuid());

            ret = getByGuidResponse.getEntity();

            System.out.println("Created entity of type [" + ret.getTypeName() + "], guid: " + ret.getGuid());
        }

        return ret;
    }

    AtlasEntity createDatabase(String name, String description, String owner, String locationUri, String... classificationNames) throws Exception {
        AtlasEntity entity = new AtlasEntity(DATABASE_TYPE);

        // set attributes
        entity.setAttribute("name", name);
        entity.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, name + CLUSTER_SUFFIX);
        entity.setAttribute("description", description);
        entity.setAttribute("owner", owner);
        entity.setAttribute("locationUri", locationUri);
        entity.setAttribute("createTime", System.currentTimeMillis());

        // set classifications
        entity.setClassifications(toAtlasClassifications(classificationNames));

        return createInstance(entity);
    }

    private List<AtlasClassification> toAtlasClassifications(String[] classificationNames) {
        List<AtlasClassification> ret             = new ArrayList<>();
        List<String>              classifications = asList(classificationNames);

        if (CollectionUtils.isNotEmpty(classifications)) {
            for (String classificationName : classifications) {
                ret.add(new AtlasClassification(classificationName));
            }
        }

        return ret;
    }

    AtlasEntity createStorageDescriptor(String location, String inputFormat, String outputFormat, boolean compressed) {
        AtlasEntity ret = new AtlasEntity(STORAGE_DESC_TYPE);

        ret.setAttribute("name", "sd:" + location);
        ret.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, "sd:" + location + CLUSTER_SUFFIX);
        ret.setAttribute("location", location);
        ret.setAttribute("inputFormat", inputFormat);
        ret.setAttribute("outputFormat", outputFormat);
        ret.setAttribute("compressed", compressed);

        return ret;
    }

    AtlasEntity createColumn(String databaseName, String tableName, String columnName, String dataType, String comment, String... classificationNames) {
        AtlasEntity ret = new AtlasEntity(COLUMN_TYPE);

        // set attributes
        ret.setAttribute("name", columnName);
        ret.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, databaseName + "." + tableName + "." + columnName + CLUSTER_SUFFIX);
        ret.setAttribute("dataType", dataType);
        ret.setAttribute("comment", comment);

        // set classifications
        ret.setClassifications(toAtlasClassifications(classificationNames));

        return ret;
    }

    AtlasEntity createTable(String name, String description, AtlasEntity database, String owner, String tableType,
                            List<AtlasEntity> columns, String... classificationNames) throws Exception {
        AtlasEntity tblEntity = new AtlasEntity(TABLE_TYPE);

        // set attributes
        tblEntity.setAttribute("name", name);
        tblEntity.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, name + CLUSTER_SUFFIX);
        tblEntity.setAttribute("description", description);
        tblEntity.setAttribute("owner", owner);
        tblEntity.setAttribute("tableType", tableType);
        tblEntity.setAttribute("createTime", System.currentTimeMillis());
        tblEntity.setAttribute("lastAccessTime", System.currentTimeMillis());
        tblEntity.setAttribute("retention", System.currentTimeMillis());

        // set relationship attributes
        AtlasEntity storageDesc = createStorageDescriptor("hdfs://host:8000/apps/warehouse/sales", "TextInputFormat", "TextOutputFormat", true);
        storageDesc.setRelationshipAttribute("table", toAtlasRelatedObjectId(tblEntity));

        tblEntity.setRelationshipAttribute("db", toAtlasRelatedObjectId(database));
        tblEntity.setRelationshipAttribute("sd", toAtlasRelatedObjectId(storageDesc));
        tblEntity.setRelationshipAttribute("columns", toAtlasRelatedObjectIds(columns));

        // set classifications
        tblEntity.setClassifications(toAtlasClassifications(classificationNames));

        AtlasEntityWithExtInfo entityWithExtInfo = new AtlasEntityWithExtInfo();

        entityWithExtInfo.setEntity(tblEntity);
        entityWithExtInfo.addReferredEntity(storageDesc);

        for (AtlasEntity column : columns) {
            column.setRelationshipAttribute("table", toAtlasRelatedObjectId(tblEntity));

            entityWithExtInfo.addReferredEntity(column);
        }

        return createInstance(entityWithExtInfo);
    }

    AtlasEntity createProcess(String name, String description, String user, List<AtlasEntity> inputs, List<AtlasEntity> outputs,
            String queryText, String queryPlan, String queryId, String queryGraph, String... classificationNames) throws Exception {

        AtlasEntity entity = new AtlasEntity(LOAD_PROCESS_TYPE);

        // set attributes
        entity.setAttribute("name", name);
        entity.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, name + CLUSTER_SUFFIX);
        entity.setAttribute("description", description);
        entity.setAttribute("user", user);
        entity.setAttribute("startTime", System.currentTimeMillis());
        entity.setAttribute("endTime", System.currentTimeMillis() + 10000);
        entity.setAttribute("queryText", queryText);
        entity.setAttribute("queryPlan", queryPlan);
        entity.setAttribute("queryId", queryId);
        entity.setAttribute("queryGraph", queryGraph);

        // set relationship attributes
        entity.setRelationshipAttribute("inputs", toAtlasRelatedObjectIds(inputs));
        entity.setRelationshipAttribute("outputs", toAtlasRelatedObjectIds(outputs));

        // set classifications
        entity.setClassifications(toAtlasClassifications(classificationNames));

        return createInstance(entity);
    }

    AtlasEntity createProcessExecution(AtlasEntity hiveProcess, String name, String description, String user,
                              String queryText, String queryPlan, String queryId, String queryGraph, String... classificationNames) throws Exception {

        AtlasEntity entity = new AtlasEntity(LOAD_PROCESS_EXECUTION_TYPE);
        Long startTime = System.currentTimeMillis();
        Long endTime = System.currentTimeMillis() + 10000;
        // set attributes
        entity.setAttribute("name", name);
        entity.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, name + CLUSTER_SUFFIX + startTime.toString() + endTime.toString());
        entity.setAttribute("description", description);
        entity.setAttribute("user", user);
        entity.setAttribute("startTime", startTime);
        entity.setAttribute("endTime", endTime);
        entity.setAttribute("queryText", queryText);
        entity.setAttribute("queryPlan", queryPlan);
        entity.setAttribute("queryId", queryId);
        entity.setAttribute("queryGraph", queryGraph);
        entity.setRelationshipAttribute("process", AtlasTypeUtil.toAtlasRelatedObjectId(hiveProcess));

        // set classifications
        entity.setClassifications(toAtlasClassifications(classificationNames));

        return createInstance(entity);
    }

    AtlasEntity createView(String name, AtlasEntity database, List<AtlasEntity> inputTables, String... classificationNames) throws Exception {
        AtlasEntity entity = new AtlasEntity(VIEW_TYPE);

        // set attributes
        entity.setAttribute("name", name);
        entity.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, name + CLUSTER_SUFFIX);

        // set relationship attributes
        entity.setRelationshipAttribute("db", toAtlasRelatedObjectId(database));
        entity.setRelationshipAttribute("inputTables", toAtlasRelatedObjectIds(inputTables));

        // set classifications
        entity.setClassifications(toAtlasClassifications(classificationNames));

        return createInstance(entity);
    }

    private void verifyTypesCreated() throws Exception {
        MultivaluedMap<String, String> searchParams = new MultivaluedMapImpl();

        for (String typeName : TYPES) {
            searchParams.clear();
            searchParams.add(SearchFilter.PARAM_NAME, typeName);

            SearchFilter  searchFilter = new SearchFilter(searchParams);
            AtlasTypesDef searchDefs   = atlasClientV2.getAllTypeDefs(searchFilter);

            assert (!searchDefs.isEmpty());

            System.out.println("Created type [" + typeName + "]");
        }
    }

    private String[] getDSLQueries() {
        return new String[]{
                "from DB",
                "DB",
                "DB where name=%22Reporting%22",
                "DB where name=%22encode_db_name%22",
                "Table where name=%2522sales_fact%2522",
                "DB where name=\"Reporting\"",
                "DB where DB.name=\"Reporting\"",
                "DB name = \"Reporting\"",
                "DB DB.name = \"Reporting\"",
                "DB where name=\"Reporting\" select name, owner",
                "DB where DB.name=\"Reporting\" select name, owner",
                "DB has name",
                "DB where DB has name",
//--TODO: Fix   "DB, Table",    // Table, db; Table db works
                "DB is JdbcAccess",
                "from Table",
                "Table",
                "Table is Dimension",
                "Column where Column isa PII",
                "View is Dimension",
                "Column select Column.name",
                "Column select name",
                "Column where Column.name=\"customer_id\"",
                "from Table select Table.name",
                "DB where (name = \"Reporting\")",
//--TODO: Fix   "DB where (name = \"Reporting\") select name as _col_0, owner as _col_1",
                "DB where DB is JdbcAccess",
                "DB where DB has name",
//--TODO: Fix   "DB Table",
                "DB as db1 Table where (db1.name = \"Reporting\")",
//--TODO: Fix   "DB where (name = \"Reporting\") select name as _col_0, (createTime + 1) as _col_1 ", // N
                DIMENSION_CLASSIFICATION,
                JDBC_CLASSIFICATION,
                ETL_CLASSIFICATION,
                METRIC_CLASSIFICATION,
                PII_CLASSIFICATION,
                "`Log Data`",
                "Table where name=\"sales_fact\", columns",
                "Table where name=\"sales_fact\", columns as column select column.name, column.dataType, column.comment",
                "from DataSet",
                "from Process" };
    }

    private void search() throws Exception {
        System.out.println("\nSample DSL Queries: ");

        for (String dslQuery : getDSLQueries()) {
            try {
                AtlasSearchResult results = atlasClientV2.dslSearchWithParams(dslQuery, 10, 0);

                if (results != null) {
                    List<AtlasEntityHeader> entitiesResult = results.getEntities();
                    List<AtlasFullTextResult> fullTextResults = results.getFullTextResult();
                    AttributeSearchResult attribResult = results.getAttributes();

                    if (CollectionUtils.isNotEmpty(entitiesResult)) {
                        System.out.println("query [" + dslQuery + "] returned [" + entitiesResult.size() + "] rows.");
                    } else if (CollectionUtils.isNotEmpty(fullTextResults)) {
                        System.out.println("query [" + dslQuery + "] returned [" + fullTextResults.size() + "] rows.");
                    } else if (attribResult != null) {
                        System.out.println("query [" + dslQuery + "] returned [" + attribResult.getValues().size() + "] rows.");
                    } else {
                        System.out.println("query [" + dslQuery + "] returned [ 0 ] rows.");
                    }
                } else {
                    System.out.println("query [" + dslQuery + "] failed, results:" + results);
                }
            } catch (Exception e) {
                System.out.println("query [" + dslQuery + "] execution failed!");
            }
        }
    }

    private void lineage() throws AtlasServiceException {
        System.out.println("\nSample Lineage Info: ");

        AtlasLineageInfo               lineageInfo   = atlasClientV2.getLineageInfo(getTableId(SALES_FACT_DAILY_MV_TABLE), LineageDirection.BOTH, 0);
        Set<LineageRelation>           relations     = lineageInfo.getRelations();
        Map<String, AtlasEntityHeader> guidEntityMap = lineageInfo.getGuidEntityMap();

        for (LineageRelation relation : relations) {
            AtlasEntityHeader fromEntity = guidEntityMap.get(relation.getFromEntityId());
            AtlasEntityHeader toEntity   = guidEntityMap.get(relation.getToEntityId());

            System.out.println(fromEntity.getDisplayText() + "(" + fromEntity.getTypeName() + ") -> " +
                               toEntity.getDisplayText()   + "(" + toEntity.getTypeName() + ")");
        }
    }

    private String getTableId(String tableName) throws AtlasServiceException {
        Map<String, String> attributes  = Collections.singletonMap(REFERENCEABLE_ATTRIBUTE_NAME, tableName + CLUSTER_SUFFIX);
        AtlasEntity         tableEntity = atlasClientV2.getEntityByAttribute(TABLE_TYPE, attributes).getEntity();

        return tableEntity.getGuid();
    }

    private void closeConnection() {
        if (atlasClientV2 != null) {
            atlasClientV2.close();
        }
    }
}