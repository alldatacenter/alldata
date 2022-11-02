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

package org.apache.atlas.hive.bridge;

import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.hive.hook.events.BaseHiveEvent;
import org.apache.atlas.hive.model.HiveDataTypes;
import org.apache.atlas.hook.AtlasHookException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations;
import org.apache.atlas.utils.AtlasPathExtractorUtil;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.utils.HdfsNameServiceResolver;
import org.apache.atlas.utils.AtlasConfigurationUtil;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.utils.PathExtractorContext;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.InvalidTableException;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.hive.hook.events.BaseHiveEvent.*;

/**
 * A Bridge Utility that imports metadata from the Hive Meta Store
 * and registers them in Atlas.
 */

public class HiveMetaStoreBridge {
    private static final Logger LOG = LoggerFactory.getLogger(HiveMetaStoreBridge.class);

    public static final String CONF_PREFIX                     = "atlas.hook.hive.";
    public static final String CLUSTER_NAME_KEY                = "atlas.cluster.name";
    public static final String HIVE_USERNAME                   = "atlas.hook.hive.default.username";
    public static final String HIVE_METADATA_NAMESPACE         = "atlas.metadata.namespace";
    public static final String HDFS_PATH_CONVERT_TO_LOWER_CASE = CONF_PREFIX + "hdfs_path.convert_to_lowercase";
    public static final String HOOK_AWS_S3_ATLAS_MODEL_VERSION = CONF_PREFIX + "aws_s3.atlas.model.version";
    public static final String DEFAULT_CLUSTER_NAME            = "primary";
    public static final String TEMP_TABLE_PREFIX               = "_temp-";
    public static final String ATLAS_ENDPOINT                  = "atlas.rest.address";
    public static final String SEP                             = ":".intern();
    public static final String HDFS_PATH                       = "hdfs_path";
    public static final String DEFAULT_METASTORE_CATALOG       = "hive";
    public static final String HIVE_TABLE_DB_EDGE_LABEL        = "__hive_table.db";
    public static final String HOOK_HIVE_PAGE_LIMIT            = CONF_PREFIX + "page.limit";

    static final String OPTION_OUTPUT_FILEPATH_SHORT     = "o";
    static final String OPTION_OUTPUT_FILEPATH_LONG      = "output";
    static final String OPTION_IGNORE_BULK_IMPORT_SHORT  = "i";
    static final String OPTION_IGNORE_BULK_IMPORT_LONG   = "ignoreBulkImport";
    static final String OPTION_DATABASE_SHORT            = "d";
    static final String OPTION_DATABASE_LONG             = "database";
    static final String OPTION_TABLE_SHORT               = "t";
    static final String OPTION_TABLE_LONG                = "table";
    static final String OPTION_IMPORT_DATA_FILE_SHORT    = "f";
    static final String OPTION_IMPORT_DATA_FILE_LONG     = "filename";
    static final String OPTION_FAIL_ON_ERROR             = "failOnError";
    static final String OPTION_DELETE_NON_EXISTING       = "deleteNonExisting";
    static final String OPTION_HELP_SHORT                = "h";
    static final String OPTION_HELP_LONG                 = "help";

    public static final String HOOK_AWS_S3_ATLAS_MODEL_VERSION_V2  = "v2";

    private static final int    EXIT_CODE_SUCCESS      = 0;
    private static final int    EXIT_CODE_FAILED       = 1;
    private static final int    EXIT_CODE_INVALID_ARG  = 2;

    private static final String DEFAULT_ATLAS_URL = "http://localhost:21000/";
    private static       int    pageLimit         = 10000;

    private final String        metadataNamespace;
    private final Hive          hiveClient;
    private final AtlasClientV2 atlasClientV2;
    private final boolean       convertHdfsPathToLowerCase;

    private String awsS3AtlasModelVersion = null;

    public static void main(String[] args) {
        int exitCode = EXIT_CODE_FAILED;
        AtlasClientV2 atlasClientV2 = null;
        Options acceptedCliOptions = prepareCommandLineOptions();

        try {
            CommandLine  cmd              = new BasicParser().parse(acceptedCliOptions, args);
            List<String> argsNotProcessed = cmd.getArgList();

            if (argsNotProcessed != null && argsNotProcessed.size() > 0) {
                throw new ParseException("Unrecognized arguments.");
            }

            if (cmd.hasOption(OPTION_HELP_SHORT)) {
                printUsage(acceptedCliOptions);
                exitCode = EXIT_CODE_SUCCESS;
            } else {
                Configuration atlasConf        = ApplicationProperties.get();
                String[]      atlasEndpoint    = atlasConf.getStringArray(ATLAS_ENDPOINT);

                if (atlasEndpoint == null || atlasEndpoint.length == 0) {
                    atlasEndpoint = new String[] { DEFAULT_ATLAS_URL };
                }

                if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
                    String[] basicAuthUsernamePassword = AuthenticationUtil.getBasicAuthenticationInput();

                    atlasClientV2 = new AtlasClientV2(atlasEndpoint, basicAuthUsernamePassword);
                } else {
                    UserGroupInformation ugi = UserGroupInformation.getCurrentUser();

                    atlasClientV2 = new AtlasClientV2(ugi, ugi.getShortUserName(), atlasEndpoint);
                }

                boolean createZip = cmd.hasOption(OPTION_OUTPUT_FILEPATH_LONG);

                if (createZip) {
                    HiveMetaStoreBridgeV2 hiveMetaStoreBridgeV2 = new HiveMetaStoreBridgeV2(atlasConf, new HiveConf(), atlasClientV2);

                    if (hiveMetaStoreBridgeV2.exportDataToZipAndRunAtlasImport(cmd)) {
                        exitCode = EXIT_CODE_SUCCESS;
                    }
                } else {
                    HiveMetaStoreBridge hiveMetaStoreBridge = new HiveMetaStoreBridge(atlasConf, new HiveConf(), atlasClientV2);

                    if (hiveMetaStoreBridge.importDataDirectlyToAtlas(cmd)) {
                        exitCode = EXIT_CODE_SUCCESS;
                    }
                }
            }
        } catch(ParseException e) {
            LOG.error("Invalid argument. Error: {}", e.getMessage());
            System.out.println("Invalid argument. Error: " + e.getMessage());
            exitCode = EXIT_CODE_INVALID_ARG;

            if (!(e instanceof MissingArgumentException)) {
                printUsage(acceptedCliOptions);
            }
        } catch(Exception e) {
            LOG.error("Import Failed", e);
        } finally {
            if( atlasClientV2 !=null) {
                atlasClientV2.close();
            }
        }

        System.exit(exitCode);
    }

    private static Options prepareCommandLineOptions() {
        Options acceptedCliOptions = new Options();

        return acceptedCliOptions.addOption(OPTION_OUTPUT_FILEPATH_SHORT, OPTION_OUTPUT_FILEPATH_LONG, true, "Output path or file for Zip import")
                .addOption(OPTION_IGNORE_BULK_IMPORT_SHORT, OPTION_IGNORE_BULK_IMPORT_LONG, false, "Ignore bulk Import for Zip import")
                .addOption(OPTION_DATABASE_SHORT, OPTION_DATABASE_LONG, true, "Database name")
                .addOption(OPTION_TABLE_SHORT, OPTION_TABLE_LONG, true, "Table name")
                .addOption(OPTION_IMPORT_DATA_FILE_SHORT, OPTION_IMPORT_DATA_FILE_LONG, true, "Filename")
                .addOption(OPTION_FAIL_ON_ERROR, false, "failOnError")
                .addOption(OPTION_DELETE_NON_EXISTING, false, "Delete database and table entities in Atlas if not present in Hive")
                .addOption(OPTION_HELP_SHORT, OPTION_HELP_LONG, false, "Print this help message");
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("import-hive.sh", options);
        System.out.println();
        System.out.println("Usage options:");
        System.out.println("    Usage 1: import-hive.sh [-d <database> OR --database <database>] "  );
        System.out.println("        Imports specified database and its tables ...");
        System.out.println();
        System.out.println("    Usage 2: import-hive.sh [-d <database> OR --database <database>] [-t <table> OR --table <table>]");
        System.out.println("        Imports specified table within that database ...");
        System.out.println();
        System.out.println("    Usage 3: import-hive.sh");
        System.out.println("        Imports all databases and tables...");
        System.out.println();
        System.out.println("    Usage 4: import-hive.sh -f <filename>");
        System.out.println("        Imports all databases and tables in the file...");
        System.out.println("        Format:");
        System.out.println("            database1:tbl1");
        System.out.println("            database1:tbl2");
        System.out.println("            database2:tbl2");
        System.out.println();
        System.out.println("    Usage 5: import-hive.sh [-deleteNonExisting] "  );
        System.out.println("        Deletes databases and tables which are not in Hive ...");
        System.out.println();
        System.out.println("    Usage 6: import-hive.sh -o <output Path or file> [-f <filename>] [-d <database> OR --database <database>] [-t <table> OR --table <table>]");
        System.out.println("        To create zip file with exported data and import the zip file at Atlas ...");
        System.out.println();
        System.out.println("    Usage 7: import-hive.sh -i -o <output Path or file> [-f <filename>] [-d <database> OR --database <database>] [-t <table> OR --table <table>]");
        System.out.println("        To create zip file with exported data without importing to Atlas which can be imported later ...");
        System.out.println();
    }

    /**
     * Construct a HiveMetaStoreBridge.
     * @param hiveConf {@link HiveConf} for Hive component in the cluster
     */
    public HiveMetaStoreBridge(Configuration atlasProperties, HiveConf hiveConf, AtlasClientV2 atlasClientV2) throws Exception {
        this.metadataNamespace          = getMetadataNamespace(atlasProperties);
        this.hiveClient                 = Hive.get(hiveConf);
        this.atlasClientV2              = atlasClientV2;
        this.convertHdfsPathToLowerCase = atlasProperties.getBoolean(HDFS_PATH_CONVERT_TO_LOWER_CASE, false);
        this.awsS3AtlasModelVersion     = atlasProperties.getString(HOOK_AWS_S3_ATLAS_MODEL_VERSION, HOOK_AWS_S3_ATLAS_MODEL_VERSION_V2);
        if (atlasProperties != null) {
            pageLimit = atlasProperties.getInteger(HOOK_HIVE_PAGE_LIMIT, 10000);
        }
    }

    /**
     * Construct a HiveMetaStoreBridge.
     * @param hiveConf {@link HiveConf} for Hive component in the cluster
     */
    public HiveMetaStoreBridge(Configuration atlasProperties, HiveConf hiveConf) throws Exception {
        this(atlasProperties, hiveConf, null);
    }

    HiveMetaStoreBridge(String metadataNamespace, Hive hiveClient, AtlasClientV2 atlasClientV2) {
        this(metadataNamespace, hiveClient, atlasClientV2, true);
    }

    HiveMetaStoreBridge(String metadataNamespace, Hive hiveClient, AtlasClientV2 atlasClientV2, boolean convertHdfsPathToLowerCase) {
        this.metadataNamespace          = metadataNamespace;
        this.hiveClient                 = hiveClient;
        this.atlasClientV2              = atlasClientV2;
        this.convertHdfsPathToLowerCase = convertHdfsPathToLowerCase;
    }

    public String getMetadataNamespace(Configuration config) {
        return AtlasConfigurationUtil.getRecentString(config, HIVE_METADATA_NAMESPACE, getClusterName(config));
    }

    private String getClusterName(Configuration config) {
        return config.getString(CLUSTER_NAME_KEY, DEFAULT_CLUSTER_NAME);
    }

    public String getMetadataNamespace() {
        return metadataNamespace;
    }

    public Hive getHiveClient() {
        return hiveClient;
    }

    public boolean isConvertHdfsPathToLowerCase() {
        return convertHdfsPathToLowerCase;
    }

    public boolean importDataDirectlyToAtlas(CommandLine cmd) throws Exception {
        LOG.info("Importing Hive metadata");
        boolean ret = false;

        String        databaseToImport = cmd.getOptionValue(OPTION_DATABASE_SHORT);
        String        tableToImport    = cmd.getOptionValue(OPTION_TABLE_SHORT);
        String        fileToImport     = cmd.getOptionValue(OPTION_IMPORT_DATA_FILE_SHORT);

        boolean       failOnError       = cmd.hasOption(OPTION_FAIL_ON_ERROR);
        boolean       deleteNonExisting = cmd.hasOption(OPTION_DELETE_NON_EXISTING);

        LOG.info("delete non existing flag : {} ", deleteNonExisting);

        if (deleteNonExisting) {
            deleteEntitiesForNonExistingHiveMetadata(failOnError);
            ret = true;
        } else if (StringUtils.isNotEmpty(fileToImport)) {
            File f = new File(fileToImport);

            if (f.exists() && f.canRead()) {
                BufferedReader br   = new BufferedReader(new FileReader(f));
                String         line = null;

                while((line = br.readLine()) != null) {
                    String val[] = line.split(":");

                    if (ArrayUtils.isNotEmpty(val)) {
                        databaseToImport = val[0];

                        if (val.length > 1) {
                            tableToImport = val[1];
                        } else {
                            tableToImport = "";
                        }

                        importDatabases(failOnError, databaseToImport, tableToImport);
                    }
                }
                ret = true;
            } else {
                LOG.error("Failed to read the input file: " + fileToImport);
            }
        } else {
            importDatabases(failOnError, databaseToImport, tableToImport);
            ret = true;
        }
        return ret;
    }

    @VisibleForTesting
    public void importHiveMetadata(String databaseToImport, String tableToImport, boolean failOnError) throws Exception {
        LOG.info("Importing Hive metadata");

        importDatabases(failOnError, databaseToImport, tableToImport);
    }

    private void importDatabases(boolean failOnError, String databaseToImport, String tableToImport) throws Exception {
        List<String> databaseNames = null;

        if (StringUtils.isEmpty(databaseToImport) && StringUtils.isEmpty(tableToImport)) {
            //when both database and table to import are empty, import all
            databaseNames = hiveClient.getAllDatabases();
        } else if (StringUtils.isEmpty(databaseToImport) && StringUtils.isNotEmpty(tableToImport)) {
            //when database is empty and table is not, then check table has database name in it and import that db and table
            if (isTableWithDatabaseName(tableToImport)) {
                String val[] = tableToImport.split("\\.");
                if (val.length > 1) {
                    databaseToImport = val[0];
                    tableToImport = val[1];
                }
                databaseNames = hiveClient.getDatabasesByPattern(databaseToImport);
            } else {
                databaseNames = hiveClient.getAllDatabases();
            }
        } else {
            //when database to import has some value then, import that db and all table under it.
            databaseNames = hiveClient.getDatabasesByPattern(databaseToImport);
        }

        if(!CollectionUtils.isEmpty(databaseNames)) {
            LOG.info("Found {} databases", databaseNames.size());

            for (String databaseName : databaseNames) {
                AtlasEntityWithExtInfo dbEntity = registerDatabase(databaseName);

                if (dbEntity != null) {
                    importTables(dbEntity.getEntity(), databaseName, tableToImport, failOnError);
                }
            }
        } else {
            LOG.error("No database found");
            System.exit(EXIT_CODE_FAILED);
        }
    }

    /**
     * Imports all tables for the given db
     * @param dbEntity
     * @param databaseName
     * @param failOnError
     * @throws Exception
     */
    private int importTables(AtlasEntity dbEntity, String databaseName, String tblName, final boolean failOnError) throws Exception {
        int tablesImported = 0;

        final List<String> tableNames;

        if (StringUtils.isEmpty(tblName)) {
            tableNames = hiveClient.getAllTables(databaseName);
        } else {
            tableNames = hiveClient.getTablesByPattern(databaseName, tblName);
        }

        if(!CollectionUtils.isEmpty(tableNames)) {
            LOG.info("Found {} tables to import in database {}", tableNames.size(), databaseName);

            try {
                for (String tableName : tableNames) {
                    int imported = importTable(dbEntity, databaseName, tableName, failOnError);

                    tablesImported += imported;
                }
            } finally {
                if (tablesImported == tableNames.size()) {
                    LOG.info("Successfully imported {} tables from database {}", tablesImported, databaseName);
                } else {
                    LOG.error("Imported {} of {} tables from database {}. Please check logs for errors during import", tablesImported, tableNames.size(), databaseName);
                }
            }
        } else {
            LOG.error("No tables to import in database {}", databaseName);
        }

        return tablesImported;
    }

    @VisibleForTesting
    public int importTable(AtlasEntity dbEntity, String databaseName, String tableName, final boolean failOnError) throws Exception {
        try {
            Table                  table       = hiveClient.getTable(databaseName, tableName);
            AtlasEntityWithExtInfo tableEntity = registerTable(dbEntity, table);

            if (table.getTableType() == TableType.EXTERNAL_TABLE) {
                String                 processQualifiedName = getTableProcessQualifiedName(metadataNamespace, table);
                AtlasEntityWithExtInfo processEntity        = findProcessEntity(processQualifiedName);

                if (processEntity == null) {
                    String tableLocationString = isConvertHdfsPathToLowerCase() ? lower(table.getDataLocation().toString()) : table.getDataLocation().toString();
                    Path   location            = table.getDataLocation();
                    String query               = getCreateTableString(table, tableLocationString);

                    PathExtractorContext   pathExtractorCtx  = new PathExtractorContext(getMetadataNamespace(), isConvertHdfsPathToLowerCase(), awsS3AtlasModelVersion);
                    AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(location, pathExtractorCtx);
                    AtlasEntity            pathInst          = entityWithExtInfo.getEntity();
                    AtlasEntity            tableInst         = tableEntity.getEntity();
                    AtlasEntity            processInst       = new AtlasEntity(HiveDataTypes.HIVE_PROCESS.getName());

                    long now = System.currentTimeMillis();

                    processInst.setAttribute(ATTRIBUTE_QUALIFIED_NAME, processQualifiedName);
                    processInst.setAttribute(ATTRIBUTE_NAME, query);
                    processInst.setAttribute(ATTRIBUTE_CLUSTER_NAME, metadataNamespace);
                    processInst.setRelationshipAttribute(ATTRIBUTE_INPUTS, Collections.singletonList(AtlasTypeUtil.getAtlasRelatedObjectId(pathInst, RELATIONSHIP_DATASET_PROCESS_INPUTS)));
                    processInst.setRelationshipAttribute(ATTRIBUTE_OUTPUTS, Collections.singletonList(AtlasTypeUtil.getAtlasRelatedObjectId(tableInst, RELATIONSHIP_PROCESS_DATASET_OUTPUTS)));
                    String userName = table.getOwner();
                    if (StringUtils.isEmpty(userName)) {
                        userName = ApplicationProperties.get().getString(HIVE_USERNAME, "hive");
                    }
                    processInst.setAttribute(ATTRIBUTE_USER_NAME, userName);
                    processInst.setAttribute(ATTRIBUTE_START_TIME, now);
                    processInst.setAttribute(ATTRIBUTE_END_TIME, now);
                    processInst.setAttribute(ATTRIBUTE_OPERATION_TYPE, "CREATETABLE");
                    processInst.setAttribute(ATTRIBUTE_QUERY_TEXT, query);
                    processInst.setAttribute(ATTRIBUTE_QUERY_ID, query);
                    processInst.setAttribute(ATTRIBUTE_QUERY_PLAN, "{}");
                    processInst.setAttribute(ATTRIBUTE_RECENT_QUERIES, Collections.singletonList(query));

                    AtlasEntitiesWithExtInfo createTableProcess = new AtlasEntitiesWithExtInfo();

                    createTableProcess.addEntity(processInst);

                    if (pathExtractorCtx.getKnownEntities() != null) {
                        pathExtractorCtx.getKnownEntities().values().forEach(entity -> createTableProcess.addEntity(entity));
                    } else {
                        createTableProcess.addEntity(pathInst);
                    }

                    registerInstances(createTableProcess);
                } else {
                    LOG.info("Process {} is already registered", processQualifiedName);
                }
            }

            return 1;
        } catch (Exception e) {
            LOG.error("Import failed for hive_table {}", tableName, e);

            if (failOnError) {
                throw e;
            }

            return 0;
        }
    }

    /**
     * Checks if db is already registered, else creates and registers db entity
     * @param databaseName
     * @return
     * @throws Exception
     */
    private AtlasEntityWithExtInfo registerDatabase(String databaseName) throws Exception {
        AtlasEntityWithExtInfo ret = null;
        Database               db  = hiveClient.getDatabase(databaseName);

        if (db != null) {
            ret = findDatabase(metadataNamespace, databaseName);

            if (ret == null) {
                ret = registerInstance(new AtlasEntityWithExtInfo(toDbEntity(db)));
            } else {
                LOG.info("Database {} is already registered - id={}. Updating it.", databaseName, ret.getEntity().getGuid());

                ret.setEntity(toDbEntity(db, ret.getEntity()));

                updateInstance(ret);
            }
        }

        return ret;
    }

    private AtlasEntityWithExtInfo registerTable(AtlasEntity dbEntity, Table table) throws AtlasHookException {
        try {
            AtlasEntityWithExtInfo ret;
            AtlasEntityWithExtInfo tableEntity = findTableEntity(table);

            if (tableEntity == null) {
                tableEntity = toTableEntity(dbEntity, table);

                ret = registerInstance(tableEntity);
            } else {
                LOG.info("Table {}.{} is already registered with id {}. Updating entity.", table.getDbName(), table.getTableName(), tableEntity.getEntity().getGuid());

                ret = toTableEntity(dbEntity, table, tableEntity);

                updateInstance(ret);
            }

            return ret;
        } catch (Exception e) {
            throw new AtlasHookException("HiveMetaStoreBridge.registerTable() failed.", e);
        }
    }

    /**
     * Registers an entity in atlas
     * @param entity
     * @return
     * @throws Exception
     */
    private AtlasEntityWithExtInfo registerInstance(AtlasEntityWithExtInfo entity) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("creating {} entity: {}", entity.getEntity().getTypeName(), entity);
        }

        AtlasEntityWithExtInfo  ret             = null;
        EntityMutationResponse  response        = atlasClientV2.createEntity(entity);
        List<AtlasEntityHeader> createdEntities = response.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE);

        if (CollectionUtils.isNotEmpty(createdEntities)) {
            for (AtlasEntityHeader createdEntity : createdEntities) {
                if (ret == null) {
                    ret = atlasClientV2.getEntityByGuid(createdEntity.getGuid());

                    LOG.info("Created {} entity: name={}, guid={}", ret.getEntity().getTypeName(), ret.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), ret.getEntity().getGuid());
                } else if (ret.getEntity(createdEntity.getGuid()) == null) {
                    AtlasEntityWithExtInfo newEntity = atlasClientV2.getEntityByGuid(createdEntity.getGuid());

                    ret.addReferredEntity(newEntity.getEntity());

                    if (MapUtils.isNotEmpty(newEntity.getReferredEntities())) {
                        for (Map.Entry<String, AtlasEntity> entry : newEntity.getReferredEntities().entrySet()) {
                            ret.addReferredEntity(entry.getKey(), entry.getValue());
                        }
                    }

                    LOG.info("Created {} entity: name={}, guid={}", newEntity.getEntity().getTypeName(), newEntity.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), newEntity.getEntity().getGuid());
                }
            }
        }

        clearRelationshipAttributes(ret);

        return ret;
    }

    /**
     * Registers an entity in atlas
     * @param entities
     * @return
     * @throws Exception
     */
    private AtlasEntitiesWithExtInfo registerInstances(AtlasEntitiesWithExtInfo entities) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("creating {} entities: {}", entities.getEntities().size(), entities);
        }

        AtlasEntitiesWithExtInfo ret = null;
        EntityMutationResponse   response        = atlasClientV2.createEntities(entities);
        List<AtlasEntityHeader>  createdEntities = response.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE);

        if (CollectionUtils.isNotEmpty(createdEntities)) {
            ret = new AtlasEntitiesWithExtInfo();

            for (AtlasEntityHeader createdEntity : createdEntities) {
                AtlasEntityWithExtInfo entity = atlasClientV2.getEntityByGuid(createdEntity.getGuid());

                ret.addEntity(entity.getEntity());

                if (MapUtils.isNotEmpty(entity.getReferredEntities())) {
                    for (Map.Entry<String, AtlasEntity> entry : entity.getReferredEntities().entrySet()) {
                        ret.addReferredEntity(entry.getKey(), entry.getValue());
                    }
                }

                LOG.info("Created {} entity: name={}, guid={}", entity.getEntity().getTypeName(), entity.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), entity.getEntity().getGuid());
            }
        }

        clearRelationshipAttributes(ret);

        return ret;
    }

    private void updateInstance(AtlasEntityWithExtInfo entity) throws AtlasServiceException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("updating {} entity: {}", entity.getEntity().getTypeName(), entity);
        }

        atlasClientV2.updateEntity(entity);

        LOG.info("Updated {} entity: name={}, guid={}", entity.getEntity().getTypeName(), entity.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), entity.getEntity().getGuid());
    }

    /**
     * Create a Hive Database entity
     * @param hiveDB The Hive {@link Database} object from which to map properties
     * @return new Hive Database AtlasEntity
     * @throws HiveException
     */
    private AtlasEntity toDbEntity(Database hiveDB) throws HiveException {
        return toDbEntity(hiveDB, null);
    }

    private AtlasEntity toDbEntity(Database hiveDB, AtlasEntity dbEntity) {
        if (dbEntity == null) {
            dbEntity = new AtlasEntity(HiveDataTypes.HIVE_DB.getName());
        }

        String dbName = getDatabaseName(hiveDB);

        dbEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, getDBQualifiedName(metadataNamespace, dbName));
        dbEntity.setAttribute(ATTRIBUTE_NAME, dbName);
        dbEntity.setAttribute(ATTRIBUTE_DESCRIPTION, hiveDB.getDescription());
        dbEntity.setAttribute(ATTRIBUTE_OWNER, hiveDB.getOwnerName());

        dbEntity.setAttribute(ATTRIBUTE_CLUSTER_NAME, metadataNamespace);
        dbEntity.setAttribute(ATTRIBUTE_LOCATION, HdfsNameServiceResolver.getPathWithNameServiceID(hiveDB.getLocationUri()));
        dbEntity.setAttribute(ATTRIBUTE_PARAMETERS, hiveDB.getParameters());

        if (hiveDB.getOwnerType() != null) {
            dbEntity.setAttribute(ATTRIBUTE_OWNER_TYPE, OWNER_TYPE_TO_ENUM_VALUE.get(hiveDB.getOwnerType().getValue()));
        }

        return dbEntity;
    }

    public static String getDatabaseName(Database hiveDB) {
        String dbName      = hiveDB.getName().toLowerCase();
        String catalogName = hiveDB.getCatalogName() != null ? hiveDB.getCatalogName().toLowerCase() : null;

        if (StringUtils.isNotEmpty(catalogName) && !StringUtils.equals(catalogName, DEFAULT_METASTORE_CATALOG)) {
            dbName = catalogName + SEP + dbName;
        }

        return dbName;
    }

    /**
     * Create a new table instance in Atlas
     * @param  database AtlasEntity for Hive  {@link AtlasEntity} to which this table belongs
     * @param hiveTable reference to the Hive {@link Table} from which to map properties
     * @return Newly created Hive AtlasEntity
     * @throws Exception
     */
    private AtlasEntityWithExtInfo toTableEntity(AtlasEntity database, Table hiveTable) throws AtlasHookException {
        return toTableEntity(database, hiveTable, null);
    }

    private AtlasEntityWithExtInfo toTableEntity(AtlasEntity database, final Table hiveTable, AtlasEntityWithExtInfo table) throws AtlasHookException {
        if (table == null) {
            table = new AtlasEntityWithExtInfo(new AtlasEntity(HiveDataTypes.HIVE_TABLE.getName()));
        }

        AtlasEntity tableEntity        = table.getEntity();
        String      tableQualifiedName = getTableQualifiedName(metadataNamespace, hiveTable);
        long        createTime         = BaseHiveEvent.getTableCreateTime(hiveTable);
        long        lastAccessTime     = hiveTable.getLastAccessTime() > 0 ? hiveTable.getLastAccessTime() : createTime;

        tableEntity.setRelationshipAttribute(ATTRIBUTE_DB, AtlasTypeUtil.getAtlasRelatedObjectId(database, RELATIONSHIP_HIVE_TABLE_DB));
        tableEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, tableQualifiedName);
        tableEntity.setAttribute(ATTRIBUTE_NAME, hiveTable.getTableName().toLowerCase());
        tableEntity.setAttribute(ATTRIBUTE_OWNER, hiveTable.getOwner());

        tableEntity.setAttribute(ATTRIBUTE_CREATE_TIME, createTime);
        tableEntity.setAttribute(ATTRIBUTE_LAST_ACCESS_TIME, lastAccessTime);
        tableEntity.setAttribute(ATTRIBUTE_RETENTION, hiveTable.getRetention());
        tableEntity.setAttribute(ATTRIBUTE_PARAMETERS, hiveTable.getParameters());
        tableEntity.setAttribute(ATTRIBUTE_COMMENT, hiveTable.getParameters().get(ATTRIBUTE_COMMENT));
        tableEntity.setAttribute(ATTRIBUTE_TABLE_TYPE, hiveTable.getTableType().name());
        tableEntity.setAttribute(ATTRIBUTE_TEMPORARY, hiveTable.isTemporary());

        if (hiveTable.getViewOriginalText() != null) {
            tableEntity.setAttribute(ATTRIBUTE_VIEW_ORIGINAL_TEXT, hiveTable.getViewOriginalText());
        }

        if (hiveTable.getViewExpandedText() != null) {
            tableEntity.setAttribute(ATTRIBUTE_VIEW_EXPANDED_TEXT, hiveTable.getViewExpandedText());
        }

        AtlasEntity       sdEntity = toStorageDescEntity(hiveTable.getSd(), tableQualifiedName, getStorageDescQFName(tableQualifiedName), AtlasTypeUtil.getObjectId(tableEntity));
        List<AtlasEntity> partKeys = toColumns(hiveTable.getPartitionKeys(), tableEntity, RELATIONSHIP_HIVE_TABLE_PART_KEYS);
        List<AtlasEntity> columns  = toColumns(hiveTable.getCols(), tableEntity, RELATIONSHIP_HIVE_TABLE_COLUMNS);

        tableEntity.setRelationshipAttribute(ATTRIBUTE_STORAGEDESC, AtlasTypeUtil.getAtlasRelatedObjectId(sdEntity, RELATIONSHIP_HIVE_TABLE_STORAGE_DESC));
        tableEntity.setRelationshipAttribute(ATTRIBUTE_PARTITION_KEYS, AtlasTypeUtil.getAtlasRelatedObjectIds(partKeys, RELATIONSHIP_HIVE_TABLE_PART_KEYS));
        tableEntity.setRelationshipAttribute(ATTRIBUTE_COLUMNS, AtlasTypeUtil.getAtlasRelatedObjectIds(columns, RELATIONSHIP_HIVE_TABLE_COLUMNS));

        table.addReferredEntity(database);
        table.addReferredEntity(sdEntity);

        if (partKeys != null) {
            for (AtlasEntity partKey : partKeys) {
                table.addReferredEntity(partKey);
            }
        }

        if (columns != null) {
            for (AtlasEntity column : columns) {
                table.addReferredEntity(column);
            }
        }

        table.setEntity(tableEntity);

        return table;
    }

    private AtlasEntity toStorageDescEntity(StorageDescriptor storageDesc, String tableQualifiedName, String sdQualifiedName, AtlasObjectId tableId ) throws AtlasHookException {
        AtlasEntity ret = new AtlasEntity(HiveDataTypes.HIVE_STORAGEDESC.getName());

        ret.setRelationshipAttribute(ATTRIBUTE_TABLE, AtlasTypeUtil.getAtlasRelatedObjectId(tableId, RELATIONSHIP_HIVE_TABLE_STORAGE_DESC));
        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, sdQualifiedName);
        ret.setAttribute(ATTRIBUTE_PARAMETERS, storageDesc.getParameters());
        ret.setAttribute(ATTRIBUTE_LOCATION, HdfsNameServiceResolver.getPathWithNameServiceID(storageDesc.getLocation()));
        ret.setAttribute(ATTRIBUTE_INPUT_FORMAT, storageDesc.getInputFormat());
        ret.setAttribute(ATTRIBUTE_OUTPUT_FORMAT, storageDesc.getOutputFormat());
        ret.setAttribute(ATTRIBUTE_COMPRESSED, storageDesc.isCompressed());
        ret.setAttribute(ATTRIBUTE_NUM_BUCKETS, storageDesc.getNumBuckets());
        ret.setAttribute(ATTRIBUTE_STORED_AS_SUB_DIRECTORIES, storageDesc.isStoredAsSubDirectories());

        if (storageDesc.getBucketCols().size() > 0) {
            ret.setAttribute(ATTRIBUTE_BUCKET_COLS, storageDesc.getBucketCols());
        }

        if (storageDesc.getSerdeInfo() != null) {
            SerDeInfo serdeInfo = storageDesc.getSerdeInfo();

            LOG.debug("serdeInfo = {}", serdeInfo);
            // SkewedInfo skewedInfo = storageDesc.getSkewedInfo();

            AtlasStruct serdeInfoStruct = new AtlasStruct(HiveDataTypes.HIVE_SERDE.getName());

            serdeInfoStruct.setAttribute(ATTRIBUTE_NAME, serdeInfo.getName());
            serdeInfoStruct.setAttribute(ATTRIBUTE_SERIALIZATION_LIB, serdeInfo.getSerializationLib());
            serdeInfoStruct.setAttribute(ATTRIBUTE_PARAMETERS, serdeInfo.getParameters());

            ret.setAttribute(ATTRIBUTE_SERDE_INFO, serdeInfoStruct);
        }

        if (CollectionUtils.isNotEmpty(storageDesc.getSortCols())) {
            List<AtlasStruct> sortColsStruct = new ArrayList<>();

            for (Order sortcol : storageDesc.getSortCols()) {
                String hiveOrderName = HiveDataTypes.HIVE_ORDER.getName();
                AtlasStruct colStruct = new AtlasStruct(hiveOrderName);
                colStruct.setAttribute("col", sortcol.getCol());
                colStruct.setAttribute("order", sortcol.getOrder());

                sortColsStruct.add(colStruct);
            }

            ret.setAttribute(ATTRIBUTE_SORT_COLS, sortColsStruct);
        }

        return ret;
    }

    private List<AtlasEntity> toColumns(List<FieldSchema> schemaList, AtlasEntity table, String relationshipType) throws AtlasHookException {
        List<AtlasEntity> ret = new ArrayList<>();

        int columnPosition = 0;
        for (FieldSchema fs : schemaList) {
            LOG.debug("Processing field {}", fs);

            AtlasEntity column = new AtlasEntity(HiveDataTypes.HIVE_COLUMN.getName());

            column.setRelationshipAttribute(ATTRIBUTE_TABLE, AtlasTypeUtil.getAtlasRelatedObjectId(table, relationshipType));
            column.setAttribute(ATTRIBUTE_QUALIFIED_NAME, getColumnQualifiedName((String) table.getAttribute(ATTRIBUTE_QUALIFIED_NAME), fs.getName()));
            column.setAttribute(ATTRIBUTE_NAME, fs.getName());
            column.setAttribute(ATTRIBUTE_OWNER, table.getAttribute(ATTRIBUTE_OWNER));
            column.setAttribute(ATTRIBUTE_COL_TYPE, fs.getType());
            column.setAttribute(ATTRIBUTE_COL_POSITION, columnPosition++);
            column.setAttribute(ATTRIBUTE_COMMENT, fs.getComment());

            ret.add(column);
        }
        return ret;
    }

    /**
     * Gets the atlas entity for the database
     * @param databaseName  database Name
     * @param metadataNamespace    cluster name
     * @return AtlasEntity for database if exists, else null
     * @throws Exception
     */
    private AtlasEntityWithExtInfo findDatabase(String metadataNamespace, String databaseName) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching Atlas for database {}", databaseName);
        }

        String typeName = HiveDataTypes.HIVE_DB.getName();

        return findEntity(typeName, getDBQualifiedName(metadataNamespace, databaseName), true, true);
    }

    /**
     * Gets Atlas Entity for the table
     *
     * @param hiveTable
     * @return table entity from Atlas  if exists, else null
     * @throws Exception
     */
    private AtlasEntityWithExtInfo findTableEntity(Table hiveTable)  throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching Atlas for table {}.{}", hiveTable.getDbName(), hiveTable.getTableName());
        }

        String typeName         = HiveDataTypes.HIVE_TABLE.getName();
        String tblQualifiedName = getTableQualifiedName(getMetadataNamespace(), hiveTable.getDbName(), hiveTable.getTableName());

        return findEntity(typeName, tblQualifiedName, true, true);
    }

    private AtlasEntityWithExtInfo findProcessEntity(String qualifiedName) throws Exception{
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching Atlas for process {}", qualifiedName);
        }

        String typeName = HiveDataTypes.HIVE_PROCESS.getName();

        return findEntity(typeName, qualifiedName , true , true);
    }

    private AtlasEntityWithExtInfo findEntity(final String typeName, final String qualifiedName ,  boolean minExtInfo, boolean ignoreRelationship) throws AtlasServiceException {
        AtlasEntityWithExtInfo ret = null;

        try {
            ret = atlasClientV2.getEntityByAttribute(typeName, Collections.singletonMap(ATTRIBUTE_QUALIFIED_NAME, qualifiedName), minExtInfo, ignoreRelationship);
        } catch (AtlasServiceException e) {
            if(e.getStatus() == ClientResponse.Status.NOT_FOUND) {
                return null;
            }

            throw e;
        }

        return ret;
    }

    private String getCreateTableString(Table table, String location){
        String            colString = "";
        List<FieldSchema> colList   = table.getAllCols();

        if (colList != null) {
            for (FieldSchema col : colList) {
                colString += col.getName() + " " + col.getType() + ",";
            }

            if (colList.size() > 0) {
                colString = colString.substring(0, colString.length() - 1);
                colString = "(" + colString + ")";
            }
        }

        String query = "create external table " + table.getTableName() +  colString + " location '" + location + "'";

        return query;
    }

    private String lower(String str) {
        if (StringUtils.isEmpty(str)) {
            return "";
        }

        return str.toLowerCase().trim();
    }


    /**
     * Construct the qualified name used to uniquely identify a Table instance in Atlas.
     * @param metadataNamespace Metadata namespace of the cluster to which the Hive component belongs
     * @param table hive table for which the qualified name is needed
     * @return Unique qualified name to identify the Table instance in Atlas.
     */
    private static String getTableQualifiedName(String metadataNamespace, Table table) {
        return getTableQualifiedName(metadataNamespace, table.getDbName(), table.getTableName(), table.isTemporary());
    }

    private String getHdfsPathQualifiedName(String hdfsPath) {
        return String.format("%s@%s", hdfsPath, metadataNamespace);
    }

    /**
     * Construct the qualified name used to uniquely identify a Database instance in Atlas.
     * @param metadataNamespace Name of the cluster to which the Hive component belongs
     * @param dbName Name of the Hive database
     * @return Unique qualified name to identify the Database instance in Atlas.
     */
    public static String getDBQualifiedName(String metadataNamespace, String dbName) {
        return String.format("%s@%s", dbName.toLowerCase(), metadataNamespace);
    }

    /**
     * Construct the qualified name used to uniquely identify a Table instance in Atlas.
     * @param metadataNamespace Name of the cluster to which the Hive component belongs
     * @param dbName Name of the Hive database to which the Table belongs
     * @param tableName Name of the Hive table
     * @param isTemporaryTable is this a temporary table
     * @return Unique qualified name to identify the Table instance in Atlas.
     */
    public static String getTableQualifiedName(String metadataNamespace, String dbName, String tableName, boolean isTemporaryTable) {
        String tableTempName = tableName;

        if (isTemporaryTable) {
            if (SessionState.get() != null && SessionState.get().getSessionId() != null) {
                tableTempName = tableName + TEMP_TABLE_PREFIX + SessionState.get().getSessionId();
            } else {
                tableTempName = tableName + TEMP_TABLE_PREFIX + RandomStringUtils.random(10);
            }
        }

        return String.format("%s.%s@%s", dbName.toLowerCase(), tableTempName.toLowerCase(), metadataNamespace);
    }

    public static String getTableProcessQualifiedName(String metadataNamespace, Table table) {
        String tableQualifiedName = getTableQualifiedName(metadataNamespace, table);
        long   createdTime        = getTableCreatedTime(table);

        return tableQualifiedName + SEP + createdTime;
    }


    /**
     * Construct the qualified name used to uniquely identify a Table instance in Atlas.
     * @param metadataNamespace Metadata namespace of the cluster to which the Hive component belongs
     * @param dbName Name of the Hive database to which the Table belongs
     * @param tableName Name of the Hive table
     * @return Unique qualified name to identify the Table instance in Atlas.
     */
    public static String getTableQualifiedName(String metadataNamespace, String dbName, String tableName) {
        return getTableQualifiedName(metadataNamespace, dbName, tableName, false);
    }
    public static String getStorageDescQFName(String tableQualifiedName) {
        return tableQualifiedName + "_storage";
    }

    public static String getColumnQualifiedName(final String tableQualifiedName, final String colName) {
        final String[] parts             = tableQualifiedName.split("@");
        final String   tableName         = parts[0];
        final String   metadataNamespace = parts[1];

        return String.format("%s.%s@%s", tableName, colName.toLowerCase(), metadataNamespace);
    }

    public static long getTableCreatedTime(Table table) {
        return table.getTTable().getCreateTime() * MILLIS_CONVERT_FACTOR;
    }

    private void clearRelationshipAttributes(AtlasEntitiesWithExtInfo entities) {
        if (entities != null) {
            if (entities.getEntities() != null) {
                for (AtlasEntity entity : entities.getEntities()) {
                    clearRelationshipAttributes(entity);;
                }
            }

            if (entities.getReferredEntities() != null) {
                clearRelationshipAttributes(entities.getReferredEntities().values());
            }
        }
    }

    private void clearRelationshipAttributes(AtlasEntityWithExtInfo entity) {
        if (entity != null) {
            clearRelationshipAttributes(entity.getEntity());

            if (entity.getReferredEntities() != null) {
                clearRelationshipAttributes(entity.getReferredEntities().values());
            }
        }
    }

    private void clearRelationshipAttributes(Collection<AtlasEntity> entities) {
        if (entities != null) {
            for (AtlasEntity entity : entities) {
                clearRelationshipAttributes(entity);
            }
        }
    }

    private void clearRelationshipAttributes(AtlasEntity entity) {
        if (entity != null && entity.getRelationshipAttributes() != null) {
            entity.getRelationshipAttributes().clear();
        }
    }

    private boolean isTableWithDatabaseName(String tableName) {
        boolean ret = false;
        if (tableName.contains(".")) {
            ret = true;
        }
        return ret;
    }

    private List<AtlasEntityHeader> getAllDatabaseInCluster() throws AtlasServiceException {

        List<AtlasEntityHeader> entities   = new ArrayList<>();
        final int               pageSize   = pageLimit;

        SearchParameters.FilterCriteria fc = new SearchParameters.FilterCriteria();
        fc.setAttributeName(ATTRIBUTE_CLUSTER_NAME);
        fc.setAttributeValue(metadataNamespace);
        fc.setOperator(SearchParameters.Operator.EQ);

        for (int i = 0; ; i++) {
            int offset = pageSize * i;
            LOG.info("Retrieving databases: offset={}, pageSize={}", offset, pageSize);

            AtlasSearchResult searchResult = atlasClientV2.basicSearch(HIVE_TYPE_DB, fc,null, null, true, pageSize, offset);

            List<AtlasEntityHeader> entityHeaders = searchResult == null ? null : searchResult.getEntities();
            int                     dbCount       = entityHeaders == null ? 0 : entityHeaders.size();

            LOG.info("Retrieved {} databases of {} cluster", dbCount, metadataNamespace);

            if (dbCount > 0) {
                entities.addAll(entityHeaders);
            }

            if (dbCount < pageSize) { // last page
                break;
            }
        }

        return entities;
    }

    private List<AtlasEntityHeader> getAllTablesInDb(String databaseGuid) throws AtlasServiceException {

        List<AtlasEntityHeader> entities = new ArrayList<>();
        final int               pageSize = pageLimit;

        for (int i = 0; ; i++) {
            int offset = pageSize * i;
            LOG.info("Retrieving tables: offset={}, pageSize={}", offset, pageSize);

            AtlasSearchResult searchResult = atlasClientV2.relationshipSearch(databaseGuid, HIVE_TABLE_DB_EDGE_LABEL, null, null, true, pageSize, offset);

            List<AtlasEntityHeader> entityHeaders = searchResult == null ? null : searchResult.getEntities();
            int                     tableCount    = entityHeaders == null ? 0 : entityHeaders.size();

            LOG.info("Retrieved {} tables of {} database", tableCount, databaseGuid);

            if (tableCount > 0) {
                entities.addAll(entityHeaders);
            }

            if (tableCount < pageSize) { // last page
                break;
            }
        }

        return entities;
    }

    public String getHiveDatabaseName(String qualifiedName) {

        if (StringUtils.isNotEmpty(qualifiedName)) {
            String[] split = qualifiedName.split("@");
            if (split.length > 0) {
                return split[0];
            }
        }
        return null;
    }


    public String getHiveTableName(String qualifiedName, boolean isTemporary) {

        if (StringUtils.isNotEmpty(qualifiedName)) {
            String tableName = StringUtils.substringBetween(qualifiedName, ".", "@");
            if (!isTemporary) {
                return tableName;
            } else {
                if (StringUtils.isNotEmpty(tableName)) {
                    String[] splitTemp = tableName.split(TEMP_TABLE_PREFIX);
                    if (splitTemp.length > 0) {
                        return splitTemp[0];
                    }
                }
            }
        }
        return null;
    }

    private void deleteByGuid(List<String> guidTodelete) throws AtlasServiceException {

        if (CollectionUtils.isNotEmpty(guidTodelete)) {

            for (String guid : guidTodelete) {
                EntityMutationResponse response = atlasClientV2.deleteEntityByGuid(guid);

                if (response.getDeletedEntities().size() < 1) {
                    LOG.info("Entity with guid : {} is not deleted", guid);
                } else {
                    LOG.info("Entity with guid : {} is deleted", guid);
                }
            }
        } else {
            LOG.info("No Entity to delete from Atlas");
        }
    }

    public void deleteEntitiesForNonExistingHiveMetadata(boolean failOnError) throws Exception {

        //fetch databases from Atlas
        List<AtlasEntityHeader> dbs = null;
        try {
            dbs = getAllDatabaseInCluster();
            LOG.info("Total Databases in cluster {} : {} ", metadataNamespace, dbs.size());
        } catch (AtlasServiceException e) {
            LOG.error("Failed to retrieve database entities for cluster {} from Atlas", metadataNamespace, e);
            if (failOnError) {
                throw e;
            }
        }

        if (CollectionUtils.isNotEmpty(dbs)) {
            //iterate all dbs to check if exists in hive
            for (AtlasEntityHeader db : dbs) {

                String dbGuid     = db.getGuid();
                String hiveDbName = getHiveDatabaseName((String) db.getAttribute(ATTRIBUTE_QUALIFIED_NAME));

                if (StringUtils.isEmpty(hiveDbName)) {
                    LOG.error("Failed to get database from qualifiedName: {}, guid: {} ", db.getAttribute(ATTRIBUTE_QUALIFIED_NAME), dbGuid);
                    continue;
                }

                List<AtlasEntityHeader> tables;
                try {
                    tables = getAllTablesInDb(dbGuid);
                    LOG.info("Total Tables in database {} : {} ", hiveDbName, tables.size());
                } catch (AtlasServiceException e) {
                    LOG.error("Failed to retrieve table entities for database {} from Atlas", hiveDbName, e);
                    if (failOnError) {
                        throw e;
                    }
                    continue;
                }

                List<String> guidsToDelete = new ArrayList<>();
                if (!hiveClient.databaseExists(hiveDbName)) {

                    //table guids
                    if (CollectionUtils.isNotEmpty(tables)) {
                        for (AtlasEntityHeader table : tables) {
                            guidsToDelete.add(table.getGuid());
                        }
                    }

                    //db guid
                    guidsToDelete.add(db.getGuid());
                    LOG.info("Added database {}.{} and its {} tables to delete", metadataNamespace, hiveDbName, tables.size());

                } else {
                    //iterate all table of db to check if it exists
                    if (CollectionUtils.isNotEmpty(tables)) {
                        for (AtlasEntityHeader table : tables) {
                            String hiveTableName = getHiveTableName((String) table.getAttribute(ATTRIBUTE_QUALIFIED_NAME), true);

                            if (StringUtils.isEmpty(hiveTableName)) {
                                LOG.error("Failed to get table from qualifiedName: {}, guid: {} ", table.getAttribute(ATTRIBUTE_QUALIFIED_NAME), table.getGuid());
                                continue;
                            }

                            try {
                                hiveClient.getTable(hiveDbName, hiveTableName, true);
                            } catch (InvalidTableException e) { //table doesn't exists
                                LOG.info("Added table {}.{} to delete", hiveDbName, hiveTableName);

                                guidsToDelete.add(table.getGuid());
                            } catch (HiveException e) {
                                LOG.error("Failed to get table {}.{} from Hive", hiveDbName, hiveTableName, e);

                                if (failOnError) {
                                    throw e;
                                }
                            }
                        }
                    }
                }

                //delete entities
                if (CollectionUtils.isNotEmpty(guidsToDelete)) {
                    try {
                        deleteByGuid(guidsToDelete);
                    } catch (AtlasServiceException e) {
                        LOG.error("Failed to delete Atlas entities for database {}", hiveDbName, e);

                        if (failOnError) {
                            throw e;
                        }
                    }

                }
            }

        } else {
            LOG.info("No database found in service.");
        }

    }
}
