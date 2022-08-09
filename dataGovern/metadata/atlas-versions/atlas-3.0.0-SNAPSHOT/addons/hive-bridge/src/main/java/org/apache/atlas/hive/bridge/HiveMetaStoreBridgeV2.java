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

import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.hive.hook.events.BaseHiveEvent;
import org.apache.atlas.hive.model.HiveDataTypes;
import org.apache.atlas.hook.AtlasHookException;
import org.apache.atlas.utils.AtlasPathExtractorUtil;
import org.apache.atlas.utils.HdfsNameServiceResolver;
import org.apache.atlas.utils.AtlasConfigurationUtil;
import org.apache.atlas.utils.PathExtractorContext;
import org.apache.atlas.utils.LruCache;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.collections.CollectionUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
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
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.atlas.hive.hook.events.BaseHiveEvent.*;

/**
 * A Bridge Utility that imports metadata into zip file from the Hive Meta Store
 * which can be exported at Atlas
 */
public class HiveMetaStoreBridgeV2 {
    private static final Logger LOG = LoggerFactory.getLogger(HiveMetaStoreBridgeV2.class);

    private static final String OPTION_DATABASE_SHORT            = "d";
    private static final String OPTION_TABLE_SHORT               = "t";
    private static final String OPTION_IMPORT_DATA_FILE_SHORT    = "f";
    private static final String OPTION_OUTPUT_FILEPATH_SHORT     = "o";
    private static final String OPTION_IGNORE_BULK_IMPORT_SHORT  = "i";

    public static final String CONF_PREFIX                     = "atlas.hook.hive.";
    public static final String HDFS_PATH_CONVERT_TO_LOWER_CASE = CONF_PREFIX + "hdfs_path.convert_to_lowercase";
    public static final String HOOK_AWS_S3_ATLAS_MODEL_VERSION = CONF_PREFIX + "aws_s3.atlas.model.version";

    public static final String CLUSTER_NAME_KEY                = "atlas.cluster.name";
    public static final String HIVE_USERNAME                   = "atlas.hook.hive.default.username";
    public static final String HIVE_METADATA_NAMESPACE         = "atlas.metadata.namespace";
    public static final String DEFAULT_CLUSTER_NAME            = "primary";
    public static final String TEMP_TABLE_PREFIX               = "_temp-";
    public static final String SEP                             = ":".intern();
    public static final String DEFAULT_METASTORE_CATALOG       = "hive";
    public static final String HOOK_HIVE_PAGE_LIMIT            = CONF_PREFIX + "page.limit";

    private static final String HOOK_AWS_S3_ATLAS_MODEL_VERSION_V2  = "v2";
    private static final String ZIP_FILE_COMMENT_FORMAT             = "{\"entitiesCount\":%d, \"total\":%d}";
    private static final int    DEFAULT_PAGE_LIMIT                  = 10000;
    private static final String DEFAULT_ZIP_FILE_NAME               = "import-hive-output.zip";
    private static final String ZIP_ENTRY_ENTITIES                  = "entities.json";
    private static final String TYPES_DEF_JSON                      = "atlas-typesdef.json";

    private static final String JSON_ARRAY_START    = "[";
    private static final String JSON_COMMA          = ",";
    private static final String JSON_EMPTY_OBJECT   = "{}";
    private static final String JSON_ARRAY_END      = "]";

    private static       int    pageLimit = DEFAULT_PAGE_LIMIT;
    private String awsS3AtlasModelVersion = null;

    private final String        metadataNamespace;
    private final Hive          hiveClient;
    private final AtlasClientV2 atlasClientV2;
    private final boolean       convertHdfsPathToLowerCase;

    private ZipOutputStream zipOutputStream;
    private String          outZipFileName;
    private int             totalProcessedEntities = 0;

    private final Map<String, AtlasEntityWithExtInfo> entityLRUCache               = new LruCache<>(10000, 0);
    private final Map<Table, AtlasEntity>             hiveTablesAndAtlasEntity     = new HashMap<>();
    private final Map<String, AtlasEntity>            dbEntities                   = new HashMap<>();
    private final List<Map<String, String>>           databaseAndTableListToImport = new ArrayList<>();
    private final Map<String, String>                 qualifiedNameGuidMap         = new HashMap<>();

    /**
     * Construct a HiveMetaStoreBridgeV2.
     * @param hiveConf {@link HiveConf} for Hive component in the cluster
     */
    public HiveMetaStoreBridgeV2(Configuration atlasProperties, HiveConf hiveConf, AtlasClientV2 atlasClientV2) throws Exception {
        this.metadataNamespace          = getMetadataNamespace(atlasProperties);
        this.hiveClient                 = Hive.get(hiveConf);
        this.atlasClientV2              = atlasClientV2;
        this.convertHdfsPathToLowerCase = atlasProperties.getBoolean(HDFS_PATH_CONVERT_TO_LOWER_CASE, false);
        this.awsS3AtlasModelVersion     = atlasProperties.getString(HOOK_AWS_S3_ATLAS_MODEL_VERSION, HOOK_AWS_S3_ATLAS_MODEL_VERSION_V2);

        if (atlasProperties != null) {
            pageLimit = atlasProperties.getInteger(HOOK_HIVE_PAGE_LIMIT, DEFAULT_PAGE_LIMIT);
        }
    }

    public boolean exportDataToZipAndRunAtlasImport(CommandLine cmd) throws MissingArgumentException, IOException, HiveException, AtlasBaseException {
        boolean       ret               = true;
        boolean       failOnError       = cmd.hasOption("failOnError");

        String        databaseToImport = cmd.getOptionValue(OPTION_DATABASE_SHORT);
        String        tableToImport    = cmd.getOptionValue(OPTION_TABLE_SHORT);
        String        importDataFile   = cmd.getOptionValue(OPTION_IMPORT_DATA_FILE_SHORT);
        String        outputFileOrPath = cmd.getOptionValue(OPTION_OUTPUT_FILEPATH_SHORT);

        boolean       ignoreBulkImport = cmd.hasOption(OPTION_IGNORE_BULK_IMPORT_SHORT);

        validateOutputFileOrPath(outputFileOrPath);

        try {
            initializeZipStream();

            if (isValidImportDataFile(importDataFile)) {
                File f = new File(importDataFile);

                BufferedReader br = new BufferedReader(new FileReader(f));
                String line = null;

                while ((line = br.readLine()) != null) {
                    String val[] = line.split(":");

                    if (ArrayUtils.isNotEmpty(val)) {
                        databaseToImport = val[0];

                        if (val.length > 1) {
                            tableToImport = val[1];
                        } else {
                            tableToImport = "";
                        }

                        importHiveDatabases(databaseToImport, tableToImport, failOnError);
                    }
                }
            } else {
                importHiveDatabases(databaseToImport, tableToImport, failOnError);
            }

            importHiveTables(failOnError);
            importHiveColumns(failOnError);
        } finally {
            endWritingAndZipStream();
        }

        if (!ignoreBulkImport) {
            runAtlasImport();
        }

        return ret;
    }

    private void validateOutputFileOrPath(String outputFileOrPath) throws MissingArgumentException {
        if (StringUtils.isBlank(outputFileOrPath)) {
            throw new MissingArgumentException("Output Path/File can't be empty");
        }

        File fileOrDirToImport = new File(outputFileOrPath);
        if (fileOrDirToImport.exists()) {
            if (fileOrDirToImport.isDirectory()) {
                this.outZipFileName = outputFileOrPath + File.separator + DEFAULT_ZIP_FILE_NAME;
                LOG.info("The default output zip file {} will be created at {}", DEFAULT_ZIP_FILE_NAME, outputFileOrPath);
            } else {
                throw new MissingArgumentException("output file: " + outputFileOrPath + " already present");
            }
        } else if (fileOrDirToImport.getParentFile().isDirectory() && outputFileOrPath.endsWith(".zip")) {
            LOG.info("The mentioned output zip file {} will be created", outputFileOrPath);
            this.outZipFileName = outputFileOrPath;
        } else {
            throw new MissingArgumentException("Invalid File/Path");
        }
    }

    private boolean isValidImportDataFile(String importDataFile) throws MissingArgumentException {
        boolean ret = false;
        if (StringUtils.isNotBlank(importDataFile)) {
            File dataFile = new File(importDataFile);

            if (!dataFile.exists() || !dataFile.canRead()) {
                throw new MissingArgumentException("Invalid import data file");
            }
            ret = true;
        }

        return ret;
    }

    private void initializeZipStream() throws IOException, AtlasBaseException {
        this.zipOutputStream            = new ZipOutputStream(getOutputStream(this.outZipFileName));

        storeTypesDefToZip(new AtlasTypesDef());

        startWritingEntitiesToZip();
    }

    private void storeTypesDefToZip(AtlasTypesDef typesDef) throws AtlasBaseException {
        String jsonData = AtlasType.toJson(typesDef);
        saveToZip(TYPES_DEF_JSON, jsonData);
    }

    private void saveToZip(String fileName, String jsonData) throws AtlasBaseException {
        try {
            ZipEntry e = new ZipEntry(fileName);
            zipOutputStream.putNextEntry(e);
            writeBytes(jsonData);
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new AtlasBaseException(String.format("Error writing file %s.", fileName), e);
        }
    }

    private void startWritingEntitiesToZip() throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(ZIP_ENTRY_ENTITIES));
        writeBytes(JSON_ARRAY_START);
    }

    private String getDatabaseToImport(String TableWithDatabase) {
        String ret = null;
        String val[] = TableWithDatabase.split("\\.");
        if (val.length > 1) {
            ret = val[0];
        }
        return ret;
    }

    private String getTableToImport(String TableWithDatabase) {
        String ret = null;
        String val[] = TableWithDatabase.split("\\.");
        if (val.length > 1) {
            ret = val[1];
        }
        return ret;
    }

    private void importHiveDatabases(String databaseToImport, String tableWithDatabaseToImport, boolean failOnError) throws HiveException, AtlasBaseException {
        LOG.info("Importing Hive Databases");

        List<String> databaseNames = null;

        if (StringUtils.isEmpty(databaseToImport) && StringUtils.isNotEmpty(tableWithDatabaseToImport)) {
            if (isTableWithDatabaseName(tableWithDatabaseToImport)) {
                databaseToImport = getDatabaseToImport(tableWithDatabaseToImport);
                tableWithDatabaseToImport = getTableToImport(tableWithDatabaseToImport);
            }
        }

        if (StringUtils.isEmpty(databaseToImport)) {
            //when database to import is empty, import all
            databaseNames = hiveClient.getAllDatabases();
        } else {
            //when database to import has some value then, import that db and all table under it.
            databaseNames = hiveClient.getDatabasesByPattern(databaseToImport);
        }

        if (!CollectionUtils.isEmpty(databaseNames)) {
            LOG.info("Found {} databases", databaseNames.size());
            for (String databaseName : databaseNames) {
                try {
                    if (!dbEntities.containsKey(databaseName)) {
                        LOG.info("Importing Hive Database {}", databaseName);
                        AtlasEntityWithExtInfo dbEntity = writeDatabase(databaseName);
                        if (dbEntity != null) {
                            dbEntities.put(databaseName, dbEntity.getEntity());
                        }
                    }
                    databaseAndTableListToImport.add(Collections.singletonMap(databaseName, tableWithDatabaseToImport));
                } catch (IOException e) {
                    LOG.error("Import failed for hive database {}", databaseName, e);

                    if (failOnError) {
                        throw new AtlasBaseException(e.getMessage(), e);
                    }
                }
            }
        } else {
            LOG.error("No database found");
            if (failOnError) {
                throw new AtlasBaseException("No database found");
            }
        }
    }

    private void writeEntity(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) throws IOException {
        if (MapUtils.isNotEmpty(entityWithExtInfo.getReferredEntities())) {
            Iterator<Map.Entry<String, AtlasEntity>> itr = entityWithExtInfo.getReferredEntities().entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, AtlasEntity> eachEntity = itr.next();
                if (eachEntity.getValue().getTypeName().equalsIgnoreCase(HiveDataTypes.HIVE_DB.getName())) {
                    itr.remove();
                }
            }
        }

        if (!entityLRUCache.containsKey(entityWithExtInfo.getEntity().getGuid())) {
            entityLRUCache.put(entityWithExtInfo.getEntity().getGuid(), entityWithExtInfo);
            writeBytes(AtlasType.toJson(entityWithExtInfo) + JSON_COMMA);
        }
        totalProcessedEntities++;
    }

    private void endWritingAndZipStream() throws IOException {
        writeBytes(JSON_EMPTY_OBJECT);
        writeBytes(JSON_ARRAY_END);
        setStreamSize(totalProcessedEntities);
        close();
    }

    private void flush() {
        try {
            zipOutputStream.flush();
        } catch (IOException e) {
            LOG.error("Error: Flush: ", e);
        }
    }

    private void close() throws IOException {
        zipOutputStream.flush();
        zipOutputStream.closeEntry();
        zipOutputStream.close();
    }

    private void writeBytes(String payload) throws IOException {
        zipOutputStream.write(payload.getBytes());
    }

    private OutputStream getOutputStream(String fileToWrite) throws IOException {
        return FileUtils.openOutputStream(new File(fileToWrite));
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

    public boolean isConvertHdfsPathToLowerCase() {
        return convertHdfsPathToLowerCase;
    }

    /**
     * Imports Hive tables if databaseAndTableListToImport is populated
     * @param failOnError
     * @throws Exception
     */
    public void importHiveTables(boolean failOnError) throws HiveException, AtlasBaseException {
        LOG.info("Importing Hive Tables");

        int totalTablesImported = 0;

        if (CollectionUtils.isNotEmpty(databaseAndTableListToImport) && MapUtils.isNotEmpty(dbEntities)) {
            for (Map<String, String> eachEntry : databaseAndTableListToImport) {
                final List<Table> tableObjects;

                String databaseName = eachEntry.keySet().iterator().next();

                if (StringUtils.isEmpty(eachEntry.values().iterator().next())) {
                    tableObjects = hiveClient.getAllTableObjects(databaseName);

                    populateQualifiedNameGuidMap(HiveDataTypes.HIVE_DB.getName(), (String) dbEntities.get(databaseName).getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                } else {
                    List<String> tableNames = hiveClient.getTablesByPattern(databaseName, eachEntry.values().iterator().next());
                    tableObjects = new ArrayList<>();

                    for (String tableName : tableNames) {
                        Table table = hiveClient.getTable(databaseName, tableName);
                        tableObjects.add(table);
                        populateQualifiedNameGuidMap(HiveDataTypes.HIVE_TABLE.getName(), getTableQualifiedName(metadataNamespace, table));
                    }
                }

                if (!CollectionUtils.isEmpty(tableObjects)) {
                    LOG.info("Found {} tables to import in database {}", tableObjects.size(), databaseName);

                    int importedInOneRun = 0;
                    try {
                        for (Table table : tableObjects) {
                            int imported = importTable(dbEntities.get(databaseName), table, failOnError);

                            totalTablesImported += imported;
                            importedInOneRun += imported;
                        }
                    } finally {
                        if (importedInOneRun == tableObjects.size()) {
                            LOG.info("Successfully imported {} tables from database {}", importedInOneRun, databaseName);
                            LOG.info("Successfully total {} tables imported", totalTablesImported);
                        } else {
                            LOG.error("Imported {} of {} tables from database {}. Please check logs for errors during import",
                                    importedInOneRun, tableObjects.size(), databaseName);
                        }
                    }
                } else {
                    LOG.error("No tables to import in database {}", databaseName);
                    if (failOnError) {
                        throw new AtlasBaseException("No tables to import in database - " + databaseName);
                    }
                }
            }
        }

        dbEntities.clear();
    }

    private void populateQualifiedNameGuidMap(String typeName, String qualifiedName) {
        try {
            AtlasEntitiesWithExtInfo entitiesWithExtInfo = atlasClientV2.getEntitiesByAttribute(typeName, Collections.singletonList(Collections.singletonMap(ATTRIBUTE_QUALIFIED_NAME, qualifiedName)), true, false);

            if (entitiesWithExtInfo != null && entitiesWithExtInfo.getEntities() != null) {
                for (AtlasEntity entity : entitiesWithExtInfo.getEntities()) {
                    qualifiedNameGuidMap.put((String) entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), entity.getGuid());

                    for(Map.Entry<String, AtlasEntity> eachEntry : entitiesWithExtInfo.getReferredEntities().entrySet()) {
                        qualifiedNameGuidMap.put((String) eachEntry.getValue().getAttribute(ATTRIBUTE_QUALIFIED_NAME), eachEntry.getKey());
                    }

                    if (typeName.equals(HiveDataTypes.HIVE_DB.getName())) {
                        for (String eachRelatedGuid : getAllRelatedGuids(entity)) {
                            AtlasEntityWithExtInfo relatedEntity = atlasClientV2.getEntityByGuid(eachRelatedGuid, true, false);

                            qualifiedNameGuidMap.put((String) relatedEntity.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), relatedEntity.getEntity().getGuid());
                            for (Map.Entry<String, AtlasEntity> eachEntry : relatedEntity.getReferredEntities().entrySet()) {
                                qualifiedNameGuidMap.put((String) eachEntry.getValue().getAttribute(ATTRIBUTE_QUALIFIED_NAME), eachEntry.getKey());
                            }
                        }
                    }
                }
            }
        } catch (AtlasServiceException e) {
            LOG.info("Unable to load the related entities for type {} and qualified name {} from Atlas", typeName, qualifiedName, e);
        }
    }

    private Set<String> getAllRelatedGuids(AtlasEntity entity) {
        Set<String> relGuidsSet = new HashSet<>();

        for (Object o : entity.getRelationshipAttributes().values()) {
            if (o instanceof AtlasObjectId) {
                relGuidsSet.add(((AtlasObjectId) o).getGuid());
            } else if (o instanceof List) {
                for (Object id : (List) o) {
                    if (id instanceof AtlasObjectId) {
                        relGuidsSet.add(((AtlasObjectId) id).getGuid());
                    }
                    if (id instanceof Map) {
                        relGuidsSet.add((String) ((Map) id).get("guid"));
                    }
                }
            }
        }

        return relGuidsSet;
    }

    public void importHiveColumns(boolean failOnError) throws AtlasBaseException {
        LOG.info("Importing Hive Columns");

        if (MapUtils.isEmpty(hiveTablesAndAtlasEntity)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No hive table present to import columns");
            }

            return;
        }

        for (Map.Entry<Table, AtlasEntity> eachTable : hiveTablesAndAtlasEntity.entrySet()) {
            int               columnsImported = 0;
            List<AtlasEntity> columnEntities  = new ArrayList<>();

            try {
                List<AtlasEntity> partKeys = toColumns(eachTable.getKey().getPartitionKeys(), eachTable.getValue(), RELATIONSHIP_HIVE_TABLE_PART_KEYS);
                List<AtlasEntity> columns  = toColumns(eachTable.getKey().getCols(), eachTable.getValue(), RELATIONSHIP_HIVE_TABLE_COLUMNS);

                partKeys.stream().collect(Collectors.toCollection(() -> columnEntities));
                columns.stream().collect(Collectors.toCollection(() -> columnEntities));

                for (AtlasEntity eachColumnEntity : columnEntities) {
                    writeEntityToZip(new AtlasEntityWithExtInfo(eachColumnEntity));
                    columnsImported++;
                }
            } catch (IOException e) {
                LOG.error("Column Import failed for hive table {}", eachTable.getValue().getAttribute(ATTRIBUTE_QUALIFIED_NAME), e);

                if (failOnError) {
                    throw new AtlasBaseException(e.getMessage(), e);
                }
            } finally {
                if (columnsImported == columnEntities.size()) {
                    LOG.info("Successfully imported {} columns for table {}", columnsImported, eachTable.getValue().getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                } else {
                    LOG.error("Imported {} of {} columns for table {}. Please check logs for errors during import", columnsImported, columnEntities.size(), eachTable.getValue().getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                }
            }
        }

    }

    private void runAtlasImport() {
        AtlasImportRequest request = new AtlasImportRequest();
        request.setOption(AtlasImportRequest.UPDATE_TYPE_DEFINITION_KEY, "false");
        request.setOption(AtlasImportRequest.OPTION_KEY_FORMAT, AtlasImportRequest.OPTION_KEY_FORMAT_ZIP_DIRECT);

        try {
            AtlasImportResult importResult = atlasClientV2.importData(request, this.outZipFileName);

            if (importResult.getOperationStatus() == AtlasImportResult.OperationStatus.SUCCESS) {
                LOG.info("Successfully imported the zip file {} at Atlas and imported {} entities. Number of entities to be imported {}.", this.outZipFileName, importResult.getProcessedEntities().size(), totalProcessedEntities);
            } else {
                LOG.error("Failed to import or get the status of import for the zip file {} at Atlas. Number of entities to be imported {}.", this.outZipFileName, totalProcessedEntities);
            }
        } catch (Exception e) {
            LOG.error("Failed to get the status or import the zip file {} at Atlas. Number of entities to be imported {}.", this.outZipFileName, totalProcessedEntities, e);
            LOG.info("Please check Atlas for the import status of the zip file {}.", this.outZipFileName);
        }
    }

    public int importTable(AtlasEntity dbEntity, Table table, final boolean failOnError) throws AtlasBaseException {
        try {
            AtlasEntityWithExtInfo tableEntity = writeTable(dbEntity, table);

            hiveTablesAndAtlasEntity.put(table, tableEntity.getEntity());

            if (table.getTableType() == TableType.EXTERNAL_TABLE) {
                String processQualifiedName = getTableProcessQualifiedName(metadataNamespace, table);
                String tableLocationString  = isConvertHdfsPathToLowerCase() ? lower(table.getDataLocation().toString()) : table.getDataLocation().toString();
                Path   location             = table.getDataLocation();
                String query                = getCreateTableString(table, tableLocationString);

                PathExtractorContext   pathExtractorCtx  = new PathExtractorContext(getMetadataNamespace(), isConvertHdfsPathToLowerCase(), awsS3AtlasModelVersion);
                AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(location, pathExtractorCtx);
                AtlasEntity            pathInst          = entityWithExtInfo.getEntity();
                AtlasEntity            tableInst         = tableEntity.getEntity();
                AtlasEntity            processInst       = new AtlasEntity(HiveDataTypes.HIVE_PROCESS.getName());

                long now = System.currentTimeMillis();

                processInst.setGuid(getGuid(processQualifiedName));
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

                writeEntitiesToZip(createTableProcess);
            }

            return 1;
        } catch (Exception e) {
            LOG.error("Import failed for hive_table {}", table.getTableName(), e);

            if (failOnError) {
                throw new AtlasBaseException(e.getMessage(), e);
            }

            return 0;
        }
    }

    /**
     * Write db entity
     * @param databaseName
     * @return
     * @throws Exception
     */
    private AtlasEntityWithExtInfo writeDatabase(String databaseName) throws HiveException, IOException {
        AtlasEntityWithExtInfo ret = null;
        Database               db  = hiveClient.getDatabase(databaseName);

        if (db != null) {
            ret = new AtlasEntityWithExtInfo(toDbEntity(db));
            writeEntityToZip(ret);
        }

        return ret;
    }

    private AtlasEntityWithExtInfo writeTable(AtlasEntity dbEntity, Table table) throws AtlasHookException {
        try {
            AtlasEntityWithExtInfo tableEntity = toTableEntity(dbEntity, table);
            writeEntityToZip(tableEntity);

            return tableEntity;
        } catch (Exception e) {
            throw new AtlasHookException("HiveMetaStoreBridgeV2.registerTable() failed.", e);
        }
    }

    /**
     * Write an entity to Zip file
     * @param entity
     * @return
     * @throws Exception
     */
    private void writeEntityToZip(AtlasEntityWithExtInfo entity) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing {} entity: {}", entity.getEntity().getTypeName(), entity);
        }

        writeEntity(entity);
        clearRelationshipAttributes(entity.getEntity());
        flush();
    }

    /**
     * Registers an entity in atlas
     * @param entities
     * @return
     * @throws Exception
     */
    private void writeEntitiesToZip(AtlasEntitiesWithExtInfo entities) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing {} entities: {}", entities.getEntities().size(), entities);
        }

        for (AtlasEntity entity : entities.getEntities()) {
            writeEntity(new AtlasEntityWithExtInfo(entity));
        }

        flush();
        clearRelationshipAttributes(entities);
    }

    /**
     * Create a Hive Database entity
     * @param hiveDB The Hive {@link Database} object from which to map properties
     * @return new Hive Database AtlasEntity
     * @throws HiveException
     */
    private AtlasEntity toDbEntity(Database hiveDB) {
        return toDbEntity(hiveDB, null);
    }

    private AtlasEntity toDbEntity(Database hiveDB, AtlasEntity dbEntity) {
        if (dbEntity == null) {
            dbEntity = new AtlasEntity(HiveDataTypes.HIVE_DB.getName());
        }

        String dbName = getDatabaseName(hiveDB);

        String qualifiedName = getDBQualifiedName(metadataNamespace, dbName);
        dbEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, qualifiedName);

        dbEntity.setGuid(getGuid(true, qualifiedName));

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

    private String getDBGuidFromAtlas(String dBQualifiedName) {
        String guid = null;
        try {
            guid = atlasClientV2.getEntityHeaderByAttribute(HiveDataTypes.HIVE_DB.getName(), Collections.singletonMap(ATTRIBUTE_QUALIFIED_NAME, dBQualifiedName)).getGuid();
        } catch (AtlasServiceException e) {
            LOG.warn("Failed to get DB guid from Atlas with qualified name {}", dBQualifiedName, e);
        }
        return guid;
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
    private AtlasEntityWithExtInfo toTableEntity(AtlasEntity database, final Table hiveTable) throws AtlasHookException {
        AtlasEntityWithExtInfo table = new AtlasEntityWithExtInfo(new AtlasEntity(HiveDataTypes.HIVE_TABLE.getName()));

        AtlasEntity tableEntity        = table.getEntity();
        String      tableQualifiedName = getTableQualifiedName(metadataNamespace, hiveTable);
        long        createTime         = BaseHiveEvent.getTableCreateTime(hiveTable);
        long        lastAccessTime     = hiveTable.getLastAccessTime() > 0 ? hiveTable.getLastAccessTime() : createTime;

        tableEntity.setGuid(getGuid(tableQualifiedName));
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

        AtlasEntity sdEntity = toStorageDescEntity(hiveTable.getSd(), getStorageDescQFName(tableQualifiedName), AtlasTypeUtil.getObjectId(tableEntity));

        tableEntity.setRelationshipAttribute(ATTRIBUTE_STORAGEDESC, AtlasTypeUtil.getAtlasRelatedObjectId(sdEntity, RELATIONSHIP_HIVE_TABLE_STORAGE_DESC));

        table.addReferredEntity(database);
        table.addReferredEntity(sdEntity);
        table.setEntity(tableEntity);

        return table;
    }

    private AtlasEntity toStorageDescEntity(StorageDescriptor storageDesc, String sdQualifiedName, AtlasObjectId tableId) {
        AtlasEntity ret = new AtlasEntity(HiveDataTypes.HIVE_STORAGEDESC.getName());

        ret.setGuid(getGuid(sdQualifiedName));
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

            LOG.info("serdeInfo = {}", serdeInfo);
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

    private List<AtlasEntity> toColumns(List<FieldSchema> schemaList, AtlasEntity table, String relationshipType) {
        List<AtlasEntity> ret = new ArrayList<>();

        int columnPosition = 0;
        for (FieldSchema fs : schemaList) {
            LOG.debug("Processing field {}", fs);

            AtlasEntity column = new AtlasEntity(HiveDataTypes.HIVE_COLUMN.getName());

            String columnQualifiedName = getColumnQualifiedName((String) table.getAttribute(ATTRIBUTE_QUALIFIED_NAME), fs.getName());

            column.setAttribute(ATTRIBUTE_QUALIFIED_NAME, columnQualifiedName);
            column.setGuid(getGuid(columnQualifiedName));

            column.setRelationshipAttribute(ATTRIBUTE_TABLE, AtlasTypeUtil.getAtlasRelatedObjectId(table, relationshipType));

            column.setAttribute(ATTRIBUTE_NAME, fs.getName());
            column.setAttribute(ATTRIBUTE_OWNER, table.getAttribute(ATTRIBUTE_OWNER));
            column.setAttribute(ATTRIBUTE_COL_TYPE, fs.getType());
            column.setAttribute(ATTRIBUTE_COL_POSITION, columnPosition++);
            column.setAttribute(ATTRIBUTE_COMMENT, fs.getComment());

            ret.add(column);
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

    private String getGuid(String qualifiedName) {
        return getGuid(false, qualifiedName);
    }

    private String getGuid(boolean isDBType, String qualifiedName) {
        String guid = null;

        if (qualifiedNameGuidMap.containsKey(qualifiedName)) {
            guid = qualifiedNameGuidMap.get(qualifiedName);
        } else if (isDBType) {
            guid = getDBGuidFromAtlas(qualifiedName);
        }

        if (StringUtils.isBlank(guid)) {
            guid = generateGuid();
        }

        return guid;
    }

    private String generateGuid() {
        return UUID.randomUUID().toString();
    }

    public void setStreamSize(long size) {
        zipOutputStream.setComment(String.format(ZIP_FILE_COMMENT_FORMAT, size, -1));
    }
}