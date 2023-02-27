/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.hive.hook.events;

import org.apache.atlas.hive.hook.AtlasHiveHookContext;
import org.apache.atlas.hive.hook.HiveHook.PreprocessAction;
import org.apache.atlas.utils.PathExtractorContext;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.AtlasPathExtractorUtil;
import org.apache.atlas.utils.HdfsNameServiceResolver;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.utils.SecurityUtils;
import org.apache.hadoop.hive.ql.hooks.*;
import org.apache.hadoop.hive.ql.hooks.LineageInfo.BaseColumnInfo;
import org.apache.hadoop.hive.ql.hooks.LineageInfo.DependencyKey;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.plan.HiveOperation;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import static org.apache.atlas.hive.bridge.HiveMetaStoreBridge.getDatabaseName;
import static org.apache.atlas.hive.hook.AtlasHiveHookContext.QNAME_SEP_METADATA_NAMESPACE;
import static org.apache.atlas.hive.hook.AtlasHiveHookContext.QNAME_SEP_ENTITY_NAME;
import static org.apache.atlas.hive.hook.AtlasHiveHookContext.QNAME_SEP_PROCESS;

public abstract class BaseHiveEvent {
    private static final Logger LOG = LoggerFactory.getLogger(BaseHiveEvent.class);

    public static final String HIVE_TYPE_DB                        = "hive_db";
    public static final String HIVE_TYPE_TABLE                     = "hive_table";
    public static final String HIVE_TYPE_STORAGEDESC               = "hive_storagedesc";
    public static final String HIVE_TYPE_COLUMN                    = "hive_column";
    public static final String HIVE_TYPE_PROCESS                   = "hive_process";
    public static final String HIVE_TYPE_COLUMN_LINEAGE            = "hive_column_lineage";
    public static final String HIVE_TYPE_SERDE                     = "hive_serde";
    public static final String HIVE_TYPE_ORDER                     = "hive_order";
    public static final String HIVE_TYPE_PROCESS_EXECUTION         = "hive_process_execution";
    public static final String HIVE_DB_DDL                         = "hive_db_ddl";
    public static final String HIVE_TABLE_DDL                      = "hive_table_ddl";
    public static final String HBASE_TYPE_TABLE                    = "hbase_table";
    public static final String HBASE_TYPE_NAMESPACE                = "hbase_namespace";
    public static final String ATTRIBUTE_QUALIFIED_NAME            = "qualifiedName";
    public static final String ATTRIBUTE_NAME                      = "name";
    public static final String ATTRIBUTE_DESCRIPTION               = "description";
    public static final String ATTRIBUTE_OWNER                     = "owner";
    public static final String ATTRIBUTE_CLUSTER_NAME              = "clusterName";
    public static final String ATTRIBUTE_LOCATION                  = "location";
    public static final String ATTRIBUTE_LOCATION_PATH             = "locationPath";
    public static final String ATTRIBUTE_PARAMETERS                = "parameters";
    public static final String ATTRIBUTE_OWNER_TYPE                = "ownerType";
    public static final String ATTRIBUTE_COMMENT                   = "comment";
    public static final String ATTRIBUTE_CREATE_TIME               = "createTime";
    public static final String ATTRIBUTE_LAST_ACCESS_TIME          = "lastAccessTime";
    public static final String ATTRIBUTE_VIEW_ORIGINAL_TEXT        = "viewOriginalText";
    public static final String ATTRIBUTE_VIEW_EXPANDED_TEXT        = "viewExpandedText";
    public static final String ATTRIBUTE_TABLE_TYPE                = "tableType";
    public static final String ATTRIBUTE_TEMPORARY                 = "temporary";
    public static final String ATTRIBUTE_RETENTION                 = "retention";
    public static final String ATTRIBUTE_DB                        = "db";
    public static final String ATTRIBUTE_HIVE_DB                   = "hiveDb";
    public static final String ATTRIBUTE_STORAGEDESC               = "sd";
    public static final String ATTRIBUTE_PARTITION_KEYS            = "partitionKeys";
    public static final String ATTRIBUTE_COLUMNS                   = "columns";
    public static final String ATTRIBUTE_INPUT_FORMAT              = "inputFormat";
    public static final String ATTRIBUTE_OUTPUT_FORMAT             = "outputFormat";
    public static final String ATTRIBUTE_COMPRESSED                = "compressed";
    public static final String ATTRIBUTE_BUCKET_COLS               = "bucketCols";
    public static final String ATTRIBUTE_NUM_BUCKETS               = "numBuckets";
    public static final String ATTRIBUTE_STORED_AS_SUB_DIRECTORIES = "storedAsSubDirectories";
    public static final String ATTRIBUTE_TABLE                     = "table";
    public static final String ATTRIBUTE_SERDE_INFO                = "serdeInfo";
    public static final String ATTRIBUTE_SERIALIZATION_LIB         = "serializationLib";
    public static final String ATTRIBUTE_SORT_COLS                 = "sortCols";
    public static final String ATTRIBUTE_COL_TYPE                  = "type";
    public static final String ATTRIBUTE_COL_POSITION              = "position";
    public static final String ATTRIBUTE_PATH                      = "path";
    public static final String ATTRIBUTE_NAMESERVICE_ID            = "nameServiceId";
    public static final String ATTRIBUTE_INPUTS                    = "inputs";
    public static final String ATTRIBUTE_OUTPUTS                   = "outputs";
    public static final String ATTRIBUTE_OPERATION_TYPE            = "operationType";
    public static final String ATTRIBUTE_START_TIME                = "startTime";
    public static final String ATTRIBUTE_USER_NAME                 = "userName";
    public static final String ATTRIBUTE_QUERY_TEXT                = "queryText";
    public static final String ATTRIBUTE_PROCESS                   = "process";
    public static final String ATTRIBUTE_PROCESS_EXECUTIONS        = "processExecutions";
    public static final String ATTRIBUTE_QUERY_ID                  = "queryId";
    public static final String ATTRIBUTE_QUERY_PLAN                = "queryPlan";
    public static final String ATTRIBUTE_END_TIME                  = "endTime";
    public static final String ATTRIBUTE_RECENT_QUERIES            = "recentQueries";
    public static final String ATTRIBUTE_QUERY                     = "query";
    public static final String ATTRIBUTE_DEPENDENCY_TYPE           = "depenendencyType";
    public static final String ATTRIBUTE_EXPRESSION                = "expression";
    public static final String ATTRIBUTE_ALIASES                   = "aliases";
    public static final String ATTRIBUTE_URI                       = "uri";
    public static final String ATTRIBUTE_STORAGE_HANDLER           = "storage_handler";
    public static final String ATTRIBUTE_NAMESPACE                 = "namespace";
    public static final String ATTRIBUTE_HOSTNAME                  = "hostName";
    public static final String ATTRIBUTE_EXEC_TIME                 = "execTime";
    public static final String ATTRIBUTE_DDL_QUERIES               = "ddlQueries";
    public static final String ATTRIBUTE_SERVICE_TYPE              = "serviceType";
    public static final String ATTRIBUTE_GUID                      = "guid";
    public static final String ATTRIBUTE_UNIQUE_ATTRIBUTES         = "uniqueAttributes";
    public static final String HBASE_STORAGE_HANDLER_CLASS         = "org.apache.hadoop.hive.hbase.HBaseStorageHandler";
    public static final String HBASE_DEFAULT_NAMESPACE             = "default";
    public static final String HBASE_NAMESPACE_TABLE_DELIMITER     = ":";
    public static final String HBASE_PARAM_TABLE_NAME              = "hbase.table.name";
    public static final long   MILLIS_CONVERT_FACTOR               = 1000;
    public static final String HDFS_PATH_PREFIX                    = "hdfs://";
    public static final String EMPTY_ATTRIBUTE_VALUE = "";

    public static final String RELATIONSHIP_DATASET_PROCESS_INPUTS        = "dataset_process_inputs";
    public static final String RELATIONSHIP_PROCESS_DATASET_OUTPUTS       = "process_dataset_outputs";
    public static final String RELATIONSHIP_HIVE_PROCESS_COLUMN_LINEAGE   = "hive_process_column_lineage";
    public static final String RELATIONSHIP_HIVE_TABLE_DB                 = "hive_table_db";
    public static final String RELATIONSHIP_HIVE_TABLE_PART_KEYS          = "hive_table_partitionkeys";
    public static final String RELATIONSHIP_HIVE_TABLE_COLUMNS            = "hive_table_columns";
    public static final String RELATIONSHIP_HIVE_TABLE_STORAGE_DESC       = "hive_table_storagedesc";
    public static final String RELATIONSHIP_HIVE_PROCESS_PROCESS_EXE      = "hive_process_process_executions";
    public static final String RELATIONSHIP_HIVE_DB_DDL_QUERIES           = "hive_db_ddl_queries";
    public static final String RELATIONSHIP_HIVE_DB_LOCATION              = "hive_db_location";
    public static final String RELATIONSHIP_HIVE_TABLE_DDL_QUERIES        = "hive_table_ddl_queries";
    public static final String RELATIONSHIP_HBASE_TABLE_NAMESPACE         = "hbase_table_namespace";


    public static final Map<Integer, String> OWNER_TYPE_TO_ENUM_VALUE = new HashMap<>();

    protected final boolean skipTempTables;

    static {
        OWNER_TYPE_TO_ENUM_VALUE.put(1, "USER");
        OWNER_TYPE_TO_ENUM_VALUE.put(2, "ROLE");
        OWNER_TYPE_TO_ENUM_VALUE.put(3, "GROUP");
    }

    protected final AtlasHiveHookContext context;


    protected BaseHiveEvent(AtlasHiveHookContext context) {
        this.context        = context;
        this.skipTempTables = context.isSkipTempTables();
    }

    public AtlasHiveHookContext getContext() {
        return context;
    }

    public List<HookNotification> getNotificationMessages() throws Exception {
        return null;
    }

    public static long getTableCreateTime(Table table) {
        return table.getTTable() != null ? (table.getTTable().getCreateTime() * MILLIS_CONVERT_FACTOR) : System.currentTimeMillis();
    }

    public static String getTableOwner(Table table) {
        return table.getTTable() != null ? (table.getOwner()): "";
    }


    public static List<AtlasObjectId> getObjectIds(List<AtlasEntity> entities) {
        final List<AtlasObjectId> ret;

        if (CollectionUtils.isNotEmpty(entities)) {
            ret = new ArrayList<>(entities.size());

            for (AtlasEntity entity : entities) {
                ret.add(AtlasTypeUtil.getObjectId(entity));
            }
        } else {
            ret = Collections.emptyList();
        }

        return ret;
    }


    protected void addProcessedEntities(AtlasEntitiesWithExtInfo entitiesWithExtInfo) {
        for (AtlasEntity entity : context.getEntities()) {
            entitiesWithExtInfo.addReferredEntity(entity);
        }

        entitiesWithExtInfo.compact();

        context.addToKnownEntities(entitiesWithExtInfo.getEntities());

        if (entitiesWithExtInfo.getReferredEntities() != null) {
            context.addToKnownEntities(entitiesWithExtInfo.getReferredEntities().values());
        }
    }

    protected AtlasEntity getInputOutputEntity(Entity entity, AtlasEntityExtInfo entityExtInfo, boolean skipTempTables) throws Exception {
        AtlasEntity ret = null;

        switch(entity.getType()) {
            case TABLE:
            case PARTITION:
            case DFS_DIR:
            case LOCAL_DIR: {
                ret = toAtlasEntity(entity, entityExtInfo, skipTempTables);
            }
            break;
        }

        return ret;
    }

    protected AtlasEntity toAtlasEntity(Entity entity, AtlasEntityExtInfo entityExtInfo, boolean skipTempTables) throws Exception {
        AtlasEntity ret = null;

        switch (entity.getType()) {
            case DATABASE: {
                String dbName = getDatabaseName(entity.getDatabase());

                if (!context.getIgnoreDummyDatabaseName().contains(dbName)) {
                    Database db = getHive().getDatabase(dbName);

                    ret = toDbEntity(db);
                }
            }
            break;

            case TABLE:
            case PARTITION: {
                String  dbName    = entity.getTable().getDbName();
                String  tableName = entity.getTable().getTableName();
                boolean skipTable = StringUtils.isNotEmpty(context.getIgnoreValuesTmpTableNamePrefix()) && tableName.toLowerCase().startsWith(context.getIgnoreValuesTmpTableNamePrefix());

                if (!skipTable) {
                    skipTable = context.getIgnoreDummyTableName().contains(tableName) && context.getIgnoreDummyDatabaseName().contains(dbName);
                }

                if (!skipTable) {
                    skipTable = skipTempTables && entity.getTable().isTemporary();
                }

                if (!skipTable) {
                    Table table = getHive().getTable(dbName, tableName);

                    ret = toTableEntity(table, entityExtInfo);
                } else {
                    context.registerSkippedEntity(entity);
                }
            }
            break;

            case DFS_DIR:
            case LOCAL_DIR: {
                URI location = entity.getLocation();

                if (location != null) {
                    ret = getPathEntity(new Path(entity.getLocation()), entityExtInfo);
                }
            }
            break;

            default:
            break;
        }

        return ret;
    }

    protected AtlasEntity toDbEntity(Database db) throws Exception {
        String      dbName          = getDatabaseName(db);
        String      dbQualifiedName = getQualifiedName(db);
        boolean     isKnownDatabase = context.isKnownDatabase(dbQualifiedName);
        AtlasEntity ret             = context.getEntity(dbQualifiedName);

        if (ret == null) {
            ret = new AtlasEntity(HIVE_TYPE_DB);

            // if this DB was sent in an earlier notification, set 'guid' to null - which will:
            //  - result in this entity to be not included in 'referredEntities'
            //  - cause Atlas server to resolve the entity by its qualifiedName
            if (isKnownDatabase) {
                ret.setGuid(null);
            }

            ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, dbQualifiedName);
            ret.setAttribute(ATTRIBUTE_NAME, dbName);

            if (StringUtils.isNotEmpty(db.getDescription())) {
                ret.setAttribute(ATTRIBUTE_DESCRIPTION, db.getDescription());
            }

            ret.setAttribute(ATTRIBUTE_OWNER, db.getOwnerName());

            ret.setAttribute(ATTRIBUTE_CLUSTER_NAME, getMetadataNamespace());
            ret.setAttribute(ATTRIBUTE_LOCATION, HdfsNameServiceResolver.getPathWithNameServiceID(db.getLocationUri()));
            ret.setAttribute(ATTRIBUTE_PARAMETERS, db.getParameters());

            if (db.getOwnerType() != null) {
                ret.setAttribute(ATTRIBUTE_OWNER_TYPE, OWNER_TYPE_TO_ENUM_VALUE.get(db.getOwnerType().getValue()));
            }

            context.putEntity(dbQualifiedName, ret);
        }

        return ret;
    }

    protected AtlasEntityWithExtInfo toTableEntity(Table table) throws Exception {
        AtlasEntityWithExtInfo ret = new AtlasEntityWithExtInfo();

        AtlasEntity entity = toTableEntity(table, ret);

        if (entity != null) {
            ret.setEntity(entity);
        } else {
            ret = null;
        }

        return ret;
    }

    protected AtlasEntity toTableEntity(Table table, AtlasEntitiesWithExtInfo entities) throws Exception {
        AtlasEntity ret = toTableEntity(table, (AtlasEntityExtInfo) entities);

        if (ret != null) {
            entities.addEntity(ret);
        }

        return ret;
    }

    protected AtlasEntity toTableEntity(Table table, AtlasEntityExtInfo entityExtInfo) throws Exception {
        Database    db       = getDatabases(table.getDbName());
        AtlasEntity dbEntity = toDbEntity(db);

        if (entityExtInfo != null) {
            if (dbEntity != null) {
                entityExtInfo.addReferredEntity(dbEntity);
            }
        }

        AtlasEntity ret = toTableEntity(AtlasTypeUtil.getObjectId(dbEntity), table, entityExtInfo);

        return ret;
    }

    protected AtlasEntity toTableEntity(AtlasObjectId dbId, Table table, AtlasEntityExtInfo entityExtInfo) throws Exception {
        String  tblQualifiedName = getQualifiedName(table);
        boolean isKnownTable     = context.isKnownTable(tblQualifiedName);

        AtlasEntity ret = context.getEntity(tblQualifiedName);

        if (ret == null) {
            PreprocessAction action = context.getPreprocessActionForHiveTable(tblQualifiedName);

            if (action == PreprocessAction.IGNORE) {
                LOG.info("ignoring table {}", tblQualifiedName);
            } else {
                ret = new AtlasEntity(HIVE_TYPE_TABLE);

                // if this table was sent in an earlier notification, set 'guid' to null - which will:
                //  - result in this entity to be not included in 'referredEntities'
                //  - cause Atlas server to resolve the entity by its qualifiedName
                if (isKnownTable && !isAlterTableOperation()) {
                    ret.setGuid(null);
                }

                long createTime     = getTableCreateTime(table);
                long lastAccessTime = table.getLastAccessTime() > 0 ? (table.getLastAccessTime() * MILLIS_CONVERT_FACTOR) : createTime;

                AtlasRelatedObjectId dbRelatedObject =     new AtlasRelatedObjectId(dbId, RELATIONSHIP_HIVE_TABLE_DB);

                ret.setRelationshipAttribute(ATTRIBUTE_DB, dbRelatedObject );
                ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, tblQualifiedName);
                ret.setAttribute(ATTRIBUTE_NAME, table.getTableName().toLowerCase());
                ret.setAttribute(ATTRIBUTE_OWNER, table.getOwner());
                ret.setAttribute(ATTRIBUTE_CREATE_TIME, createTime);
                ret.setAttribute(ATTRIBUTE_LAST_ACCESS_TIME, lastAccessTime);
                ret.setAttribute(ATTRIBUTE_RETENTION, table.getRetention());
                ret.setAttribute(ATTRIBUTE_PARAMETERS, table.getParameters());
                ret.setAttribute(ATTRIBUTE_COMMENT, table.getParameters().get(ATTRIBUTE_COMMENT));
                ret.setAttribute(ATTRIBUTE_TABLE_TYPE, table.getTableType().name());
                ret.setAttribute(ATTRIBUTE_TEMPORARY, table.isTemporary());

                if (table.getViewOriginalText() != null) {
                    ret.setAttribute(ATTRIBUTE_VIEW_ORIGINAL_TEXT, table.getViewOriginalText());
                }

                if (table.getViewExpandedText() != null) {
                    ret.setAttribute(ATTRIBUTE_VIEW_EXPANDED_TEXT, table.getViewExpandedText());
                }

                boolean pruneTable = table.isTemporary() || action == PreprocessAction.PRUNE;

                if (pruneTable) {
                    LOG.info("ignoring details of table {}", tblQualifiedName);
                } else {
                    AtlasObjectId     tableId       = AtlasTypeUtil.getObjectId(ret);
                    AtlasEntity       sd            = getStorageDescEntity(tableId, table);
                    List<AtlasEntity> partitionKeys = getColumnEntities(tableId, table, table.getPartitionKeys(), RELATIONSHIP_HIVE_TABLE_PART_KEYS);
                    List<AtlasEntity> columns       = getColumnEntities(tableId, table, table.getCols(), RELATIONSHIP_HIVE_TABLE_COLUMNS);



                    if (entityExtInfo != null) {
                        entityExtInfo.addReferredEntity(sd);

                        if (partitionKeys != null) {
                            for (AtlasEntity partitionKey : partitionKeys) {
                                entityExtInfo.addReferredEntity(partitionKey);
                            }
                        }

                        if (columns != null) {
                            for (AtlasEntity column : columns) {
                                entityExtInfo.addReferredEntity(column);
                            }
                        }
                    }


                    ret.setRelationshipAttribute(ATTRIBUTE_STORAGEDESC, AtlasTypeUtil.getAtlasRelatedObjectId(sd, RELATIONSHIP_HIVE_TABLE_STORAGE_DESC));
                    ret.setRelationshipAttribute(ATTRIBUTE_PARTITION_KEYS, AtlasTypeUtil.getAtlasRelatedObjectIds(partitionKeys, RELATIONSHIP_HIVE_TABLE_PART_KEYS));
                    ret.setRelationshipAttribute(ATTRIBUTE_COLUMNS, AtlasTypeUtil.getAtlasRelatedObjectIds(columns, RELATIONSHIP_HIVE_TABLE_COLUMNS));
                }

                context.putEntity(tblQualifiedName, ret);
            }
        }

        return ret;
    }

    protected AtlasEntity getStorageDescEntity(AtlasObjectId tableId, Table table) {
        String  sdQualifiedName = getQualifiedName(table, table.getSd());
        boolean isKnownTable    = tableId.getGuid() == null;

        AtlasEntity ret = context.getEntity(sdQualifiedName);

        if (ret == null) {
            ret = new AtlasEntity(HIVE_TYPE_STORAGEDESC);

            // if sd's table was sent in an earlier notification, set 'guid' to null - which will:
            //  - result in this entity to be not included in 'referredEntities'
            //  - cause Atlas server to resolve the entity by its qualifiedName
            if (isKnownTable) {
                ret.setGuid(null);
            }

            StorageDescriptor sd = table.getSd();

            AtlasRelatedObjectId tableRelatedObject =     new AtlasRelatedObjectId(tableId, RELATIONSHIP_HIVE_TABLE_STORAGE_DESC);

            ret.setRelationshipAttribute(ATTRIBUTE_TABLE, tableRelatedObject);
            ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, sdQualifiedName);
            ret.setAttribute(ATTRIBUTE_PARAMETERS, sd.getParameters());
            ret.setAttribute(ATTRIBUTE_LOCATION, HdfsNameServiceResolver.getPathWithNameServiceID(sd.getLocation()));
            ret.setAttribute(ATTRIBUTE_INPUT_FORMAT, sd.getInputFormat());
            ret.setAttribute(ATTRIBUTE_OUTPUT_FORMAT, sd.getOutputFormat());
            ret.setAttribute(ATTRIBUTE_COMPRESSED, sd.isCompressed());
            ret.setAttribute(ATTRIBUTE_NUM_BUCKETS, sd.getNumBuckets());
            ret.setAttribute(ATTRIBUTE_STORED_AS_SUB_DIRECTORIES, sd.isStoredAsSubDirectories());

            if (sd.getBucketCols() != null && sd.getBucketCols().size() > 0) {
                ret.setAttribute(ATTRIBUTE_BUCKET_COLS, sd.getBucketCols());
            }

            if (sd.getSerdeInfo() != null) {
                AtlasStruct serdeInfo   = new AtlasStruct(HIVE_TYPE_SERDE);
                SerDeInfo   sdSerDeInfo = sd.getSerdeInfo();

                serdeInfo.setAttribute(ATTRIBUTE_NAME, sdSerDeInfo.getName());
                serdeInfo.setAttribute(ATTRIBUTE_SERIALIZATION_LIB, sdSerDeInfo.getSerializationLib());
                serdeInfo.setAttribute(ATTRIBUTE_PARAMETERS, sdSerDeInfo.getParameters());

                ret.setAttribute(ATTRIBUTE_SERDE_INFO, serdeInfo);
            }

            if (CollectionUtils.isNotEmpty(sd.getSortCols())) {
                List<AtlasStruct> sortCols = new ArrayList<>(sd.getSortCols().size());

                for (Order sdSortCol : sd.getSortCols()) {
                    AtlasStruct sortcol = new AtlasStruct(HIVE_TYPE_ORDER);

                    sortcol.setAttribute("col", sdSortCol.getCol());
                    sortcol.setAttribute("order", sdSortCol.getOrder());

                    sortCols.add(sortcol);
                }

                ret.setAttribute(ATTRIBUTE_SORT_COLS, sortCols);
            }

            context.putEntity(sdQualifiedName, ret);
        }

        return ret;
    }

    protected List<AtlasEntity> getColumnEntities(AtlasObjectId tableId, Table table, List<FieldSchema> fieldSchemas, String relationshipType) {
        List<AtlasEntity> ret            = new ArrayList<>();
        boolean           isKnownTable   = tableId.getGuid() == null;
        int               columnPosition = 0;

        if (CollectionUtils.isNotEmpty(fieldSchemas)) {
            for (FieldSchema fieldSchema : fieldSchemas) {
                String      colQualifiedName = getQualifiedName(table, fieldSchema);
                AtlasEntity column           = context.getEntity(colQualifiedName);

                if (column == null) {
                    column = new AtlasEntity(HIVE_TYPE_COLUMN);

                    // if column's table was sent in an earlier notification, set 'guid' to null - which will:
                    //  - result in this entity to be not included in 'referredEntities'
                    //  - cause Atlas server to resolve the entity by its qualifiedName
                    if (isKnownTable) {
                        column.setGuid(null);
                    }
                    AtlasRelatedObjectId relatedObjectId = new AtlasRelatedObjectId(tableId, relationshipType);
                    column.setRelationshipAttribute(ATTRIBUTE_TABLE, (relatedObjectId));
                    column.setAttribute(ATTRIBUTE_QUALIFIED_NAME, colQualifiedName);
                    column.setAttribute(ATTRIBUTE_NAME, fieldSchema.getName());
                    column.setAttribute(ATTRIBUTE_OWNER, table.getOwner());
                    column.setAttribute(ATTRIBUTE_COL_TYPE, fieldSchema.getType());
                    column.setAttribute(ATTRIBUTE_COL_POSITION, columnPosition++);
                    column.setAttribute(ATTRIBUTE_COMMENT, fieldSchema.getComment());

                    context.putEntity(colQualifiedName, column);
                }

                ret.add(column);
            }
        }

        return ret;
    }

    protected AtlasEntity getPathEntity(Path path, AtlasEntityExtInfo extInfo) {
        String               strPath                  = path.toString();
        String               metadataNamespace        = getMetadataNamespace();
        boolean              isConvertPathToLowerCase = strPath.startsWith(HDFS_PATH_PREFIX) && context.isConvertHdfsPathToLowerCase();
        PathExtractorContext pathExtractorContext     = new PathExtractorContext(metadataNamespace, context.getQNameToEntityMap(),
                                                                                 isConvertPathToLowerCase, context.getAwsS3AtlasModelVersion());

        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, pathExtractorContext);

        if (entityWithExtInfo.getReferredEntities() != null){
            for (AtlasEntity entity : entityWithExtInfo.getReferredEntities().values()) {
                extInfo.addReferredEntity(entity);
            }
        }

        return entityWithExtInfo.getEntity();
    }

    protected AtlasEntity getHiveProcessEntity(List<AtlasEntity> inputs, List<AtlasEntity> outputs) throws Exception {
        AtlasEntity ret           = new AtlasEntity(HIVE_TYPE_PROCESS);
        String      queryStr      = getQueryString();
        String      qualifiedName = getQualifiedName(inputs, outputs);

        if (queryStr != null) {
            queryStr = queryStr.toLowerCase().trim();
        }

        ret.setAttribute(ATTRIBUTE_OPERATION_TYPE, getOperationName());

        if (context.isMetastoreHook()) {
            HiveOperation operation = context.getHiveOperation();

            if (operation == HiveOperation.CREATETABLE || operation == HiveOperation.CREATETABLE_AS_SELECT) {
                AtlasEntity table      = outputs.get(0);
                long        createTime = Long.valueOf((Long)table.getAttribute(ATTRIBUTE_CREATE_TIME));
                qualifiedName          =  (String) table.getAttribute(ATTRIBUTE_QUALIFIED_NAME) + QNAME_SEP_PROCESS + createTime;

                ret.setAttribute(ATTRIBUTE_NAME, "dummyProcess:" + UUID.randomUUID());
                ret.setAttribute(ATTRIBUTE_OPERATION_TYPE, operation.getOperationName());
            }
        }

        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, qualifiedName);
        ret.setAttribute(ATTRIBUTE_NAME, qualifiedName);
        ret.setRelationshipAttribute(ATTRIBUTE_INPUTS, AtlasTypeUtil.getAtlasRelatedObjectIds(inputs, RELATIONSHIP_DATASET_PROCESS_INPUTS));
        ret.setRelationshipAttribute(ATTRIBUTE_OUTPUTS, AtlasTypeUtil.getAtlasRelatedObjectIds(outputs, RELATIONSHIP_PROCESS_DATASET_OUTPUTS));

        // We are setting an empty value to these attributes, since now we have a new entity type called hive process
        // execution which captures these values. We have to set empty values here because these attributes are
        // mandatory attributes for hive process entity type.
        ret.setAttribute(ATTRIBUTE_START_TIME, System.currentTimeMillis());
        ret.setAttribute(ATTRIBUTE_END_TIME, System.currentTimeMillis());

        if (context.isHiveProcessPopulateDeprecatedAttributes()) {
            ret.setAttribute(ATTRIBUTE_USER_NAME, getUserName());
            ret.setAttribute(ATTRIBUTE_QUERY_TEXT, StringUtils.isNotEmpty(queryStr)? queryStr : EMPTY_ATTRIBUTE_VALUE);
            ret.setAttribute(ATTRIBUTE_QUERY_ID, StringUtils.isNotEmpty(getQueryId())? getQueryId() : EMPTY_ATTRIBUTE_VALUE);
        } else {
            ret.setAttribute(ATTRIBUTE_USER_NAME, EMPTY_ATTRIBUTE_VALUE);
            ret.setAttribute(ATTRIBUTE_QUERY_TEXT, EMPTY_ATTRIBUTE_VALUE);
            ret.setAttribute(ATTRIBUTE_QUERY_ID, EMPTY_ATTRIBUTE_VALUE);
        }

        ret.setAttribute(ATTRIBUTE_QUERY_PLAN, "Not Supported");
        ret.setAttribute(ATTRIBUTE_RECENT_QUERIES, Collections.singletonList(queryStr));
        ret.setAttribute(ATTRIBUTE_CLUSTER_NAME, getMetadataNamespace());

        return ret;
    }

    protected AtlasEntity getHiveProcessExecutionEntity(AtlasEntity hiveProcess) throws Exception {
        AtlasEntity ret         = new AtlasEntity(HIVE_TYPE_PROCESS_EXECUTION);
        String      queryStr    = getQueryString();

        if (queryStr != null) {
            queryStr = queryStr.toLowerCase().trim();
        }

        Long endTime = System.currentTimeMillis();
        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, hiveProcess.getAttribute(ATTRIBUTE_QUALIFIED_NAME).toString() +
                QNAME_SEP_PROCESS + getQueryStartTime().toString() +
                QNAME_SEP_PROCESS + endTime.toString());
        ret.setAttribute(ATTRIBUTE_NAME, ret.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
        ret.setAttribute(ATTRIBUTE_START_TIME, getQueryStartTime());
        ret.setAttribute(ATTRIBUTE_END_TIME, endTime);
        ret.setAttribute(ATTRIBUTE_USER_NAME, getUserName());
        ret.setAttribute(ATTRIBUTE_QUERY_TEXT, queryStr);
        ret.setAttribute(ATTRIBUTE_QUERY_ID, getQueryId());
        ret.setAttribute(ATTRIBUTE_QUERY_PLAN, "Not Supported");
        ret.setAttribute(ATTRIBUTE_HOSTNAME, getContext().getHostName()); //
        AtlasRelatedObjectId hiveProcessRelationObjectId = AtlasTypeUtil.toAtlasRelatedObjectId(hiveProcess, RELATIONSHIP_HIVE_PROCESS_PROCESS_EXE);
        ret.setRelationshipAttribute(ATTRIBUTE_PROCESS, hiveProcessRelationObjectId);
        return ret;
    }

    protected AtlasEntity createHiveDDLEntity(AtlasEntity dbOrTable) {
        return createHiveDDLEntity(dbOrTable, false);
    }

    protected AtlasEntity createHiveDDLEntity(AtlasEntity dbOrTable, boolean excludeEntityGuid) {
        AtlasObjectId objId   = AtlasTypeUtil.getObjectId(dbOrTable);
        AtlasEntity   hiveDDL = null;

        if (excludeEntityGuid) {
            objId.setGuid(null);
        }
        AtlasRelatedObjectId objIdRelatedObject =     new AtlasRelatedObjectId(objId);

        if (StringUtils.equals(objId.getTypeName(), HIVE_TYPE_DB)) {
            hiveDDL = new AtlasEntity(HIVE_DB_DDL);
            objIdRelatedObject.setRelationshipType(RELATIONSHIP_HIVE_DB_DDL_QUERIES);
            hiveDDL.setRelationshipAttribute(ATTRIBUTE_DB, objIdRelatedObject);
        } else if (StringUtils.equals(objId.getTypeName(), HIVE_TYPE_TABLE)) {
            hiveDDL = new AtlasEntity(HIVE_TABLE_DDL);
            objIdRelatedObject.setRelationshipType(RELATIONSHIP_HIVE_TABLE_DDL_QUERIES);
            hiveDDL.setRelationshipAttribute( ATTRIBUTE_TABLE, objIdRelatedObject);
        }

        if (hiveDDL != null) {
            hiveDDL.setAttribute(ATTRIBUTE_SERVICE_TYPE, "hive");
            hiveDDL.setAttribute(ATTRIBUTE_EXEC_TIME, getQueryStartTime());
            hiveDDL.setAttribute(ATTRIBUTE_QUERY_TEXT, getQueryString());
            hiveDDL.setAttribute(ATTRIBUTE_USER_NAME, getUserName());
            hiveDDL.setAttribute(ATTRIBUTE_NAME, getQueryString());
            hiveDDL.setAttribute(ATTRIBUTE_QUALIFIED_NAME, dbOrTable.getAttribute(ATTRIBUTE_QUALIFIED_NAME).toString()
                                                           + QNAME_SEP_PROCESS + getQueryStartTime().toString());
        }

        return hiveDDL;
    }

    protected AtlasEntity createHiveLocationEntity(AtlasEntity dbEntity, AtlasEntitiesWithExtInfo extInfoEntity) {
        AtlasEntity ret          = null;
        String      locationUri  = (String)dbEntity.getAttribute(ATTRIBUTE_LOCATION);

        if (StringUtils.isNotEmpty(locationUri)) {
            Path path = null;

            try {
                path = new Path(locationUri);
            } catch (IllegalArgumentException excp) {
                LOG.warn("failed to create Path from locationUri {}", locationUri, excp);
            }

            if (path != null) {
                ret = getPathEntity(path, extInfoEntity);

                if (ret != null) {
                    AtlasRelatedObjectId dbRelatedObjectId = AtlasTypeUtil.getAtlasRelatedObjectId(dbEntity, RELATIONSHIP_HIVE_DB_LOCATION);

                    ret.setRelationshipAttribute(ATTRIBUTE_HIVE_DB, dbRelatedObjectId);
                }
            }
        }

        return ret;
    }

    protected String getMetadataNamespace() {
        return context.getMetadataNamespace();
    }

    protected Database getDatabases(String dbName) throws Exception {
        return context.isMetastoreHook() ? context.getMetastoreHandler().get_database(dbName) :
                                           context.getHive().getDatabase(dbName);
    }

    protected Hive getHive() {
        return context.getHive();
    }

    protected Set<ReadEntity> getInputs() {
        return context != null ? context.getInputs() : Collections.emptySet();
    }

    protected Set<WriteEntity> getOutputs() {
        return context != null ? context.getOutputs() : Collections.emptySet();
    }

    protected LineageInfo getLineageInfo() {
        return context != null ? context.getLineageInfo() : null;
    }

    protected String getQueryString() {
        return isHiveContextValid() ? context.getHiveContext().getQueryPlan().getQueryStr() : null;
    }

    protected String getOperationName() {
        return isHiveContextValid() ? context.getHiveContext().getOperationName() : null;
    }

    protected String getHiveUserName() {
        return isHiveContextValid() ? context.getHiveContext().getUserName() : null;
    }

    protected UserGroupInformation getUgi() {
        return isHiveContextValid() ? context.getHiveContext().getUgi() : null;
    }

    protected Long getQueryStartTime() {
        Long queryStartTime = null;

        if (isHiveContextValid() && context.getHiveContext().getQueryPlan() != null) {
            queryStartTime = context.getHiveContext().getQueryPlan().getQueryStartTime();
        }

        return queryStartTime == null ? System.currentTimeMillis() : queryStartTime;
    }

    protected String getQueryId() {
        return isHiveContextValid() ? context.getHiveContext().getQueryPlan().getQueryId() : null;
    }

    private boolean isHiveContextValid() {
        return context != null && context.getHiveContext() != null;
    }

    protected String getUserName() {
        String               ret = null;
        UserGroupInformation ugi = null;

        if (context.isMetastoreHook()) {
            try {
                ugi = SecurityUtils.getUGI();
            } catch (Exception e) {
                //do nothing
            }
        } else {
            ret = getHiveUserName();

            if (StringUtils.isEmpty(ret)) {
                ugi = getUgi();
            }
        }

        if (ugi != null) {
            ret = ugi.getShortUserName();
        }

        if (StringUtils.isEmpty(ret)) {
            try {
                ret = UserGroupInformation.getCurrentUser().getShortUserName();
            } catch (IOException e) {
                LOG.warn("Failed for UserGroupInformation.getCurrentUser() ", e);

                ret = System.getProperty("user.name");
            }
        }

        return ret;
    }

    protected String getQualifiedName(Entity entity) throws Exception {
        switch (entity.getType()) {
            case DATABASE:
                return getQualifiedName(entity.getDatabase());

            case TABLE:
            case PARTITION:
                return getQualifiedName(entity.getTable());

            case DFS_DIR:
            case LOCAL_DIR:
                return getQualifiedName(entity.getLocation());
        }

        return null;
    }

    protected String getQualifiedName(Database db) {
        return context.getQualifiedName(db);
    }

    protected String getQualifiedName(Table table) {
        return context.getQualifiedName(table);
    }

    protected String getQualifiedName(Table table, StorageDescriptor sd) {
        return getQualifiedName(table) + "_storage";
    }

    protected String getQualifiedName(Table table, FieldSchema column) {
        String tblQualifiedName = getQualifiedName(table);

        int sepPos = tblQualifiedName.lastIndexOf(QNAME_SEP_METADATA_NAMESPACE);

        if (sepPos == -1) {
            return tblQualifiedName + QNAME_SEP_ENTITY_NAME + column.getName().toLowerCase();
        } else {
            return tblQualifiedName.substring(0, sepPos) + QNAME_SEP_ENTITY_NAME + column.getName().toLowerCase() + tblQualifiedName.substring(sepPos);
        }
    }

    protected String getQualifiedName(DependencyKey column) {
        String dbName    = column.getDataContainer().getTable().getDbName();
        String tableName = column.getDataContainer().getTable().getTableName();
        String colName   = column.getFieldSchema().getName();

        return getQualifiedName(dbName, tableName, colName);
    }

    protected String getQualifiedName(BaseColumnInfo column) {
        String dbName            = column.getTabAlias().getTable().getDbName();
        String tableName         = column.getTabAlias().getTable().getTableName();
        String colName           = column.getColumn() != null ? column.getColumn().getName() : null;
        String metadataNamespace = getMetadataNamespace();

        if (colName == null) {
            return (dbName + QNAME_SEP_ENTITY_NAME + tableName + QNAME_SEP_METADATA_NAMESPACE).toLowerCase() + metadataNamespace;
        } else {
            return (dbName + QNAME_SEP_ENTITY_NAME + tableName + QNAME_SEP_ENTITY_NAME + colName + QNAME_SEP_METADATA_NAMESPACE).toLowerCase() + metadataNamespace;
        }
    }

    protected String getQualifiedName(String dbName, String tableName, String colName) {
        return (dbName + QNAME_SEP_ENTITY_NAME + tableName + QNAME_SEP_ENTITY_NAME + colName + QNAME_SEP_METADATA_NAMESPACE).toLowerCase() + getMetadataNamespace();
    }

    protected String getQualifiedName(URI location) {
        String strPath = new Path(location).toString();

        if (strPath.startsWith(HDFS_PATH_PREFIX) && context.isConvertHdfsPathToLowerCase()) {
            strPath = strPath.toLowerCase();
        }

        String nameServiceID = HdfsNameServiceResolver.getNameServiceIDForPath(strPath);
        String attrPath      = StringUtils.isEmpty(nameServiceID) ? strPath : HdfsNameServiceResolver.getPathWithNameServiceID(strPath);

        return getQualifiedName(attrPath);
    }

    protected String getQualifiedName(String path) {
        if (path.startsWith(HdfsNameServiceResolver.HDFS_SCHEME)) {
            return path + QNAME_SEP_METADATA_NAMESPACE + getMetadataNamespace();
        }

        return path.toLowerCase();
    }

    protected String getColumnQualifiedName(String tblQualifiedName, String columnName) {
        int sepPos = tblQualifiedName.lastIndexOf(QNAME_SEP_METADATA_NAMESPACE);

        if (sepPos == -1) {
            return tblQualifiedName + QNAME_SEP_ENTITY_NAME + columnName.toLowerCase();
        } else {
            return tblQualifiedName.substring(0, sepPos) + QNAME_SEP_ENTITY_NAME + columnName.toLowerCase() + tblQualifiedName.substring(sepPos);
        }

    }

    protected String getQualifiedName(List<AtlasEntity> inputs, List<AtlasEntity> outputs) throws Exception {
        HiveOperation operation = context.getHiveOperation();

        if (operation == HiveOperation.CREATETABLE ||
            operation == HiveOperation.CREATETABLE_AS_SELECT ||
            operation == HiveOperation.CREATEVIEW ||
            operation == HiveOperation.ALTERVIEW_AS ||
            operation == HiveOperation.ALTERTABLE_LOCATION) {
            List<? extends Entity> sortedEntities = new ArrayList<>(getOutputs());

            Collections.sort(sortedEntities, entityComparator);

            for (Entity entity : sortedEntities) {
                if (entity.getType() == Entity.Type.TABLE) {
                    Table table = entity.getTable();

                    table = getHive().getTable(table.getDbName(), table.getTableName());

                    long createTime = getTableCreateTime(table);

                    return getQualifiedName(table) + QNAME_SEP_PROCESS + createTime;
                }
            }
        }

        String qualifiedName = null;
        String operationName = getOperationName();

        if (operationName != null) {
            StringBuilder sb = new StringBuilder(operationName);

            boolean ignoreHDFSPaths = ignoreHDFSPathsinProcessQualifiedName();

            addToProcessQualifiedName(sb, getInputs(), ignoreHDFSPaths);
            sb.append("->");
            addToProcessQualifiedName(sb, getOutputs(), ignoreHDFSPaths);

            qualifiedName = sb.toString();
        }


        return qualifiedName;
    }

    protected AtlasEntity toReferencedHBaseTable(Table table, AtlasEntitiesWithExtInfo entities) {
        AtlasEntity    ret               = null;
        HBaseTableInfo hBaseTableInfo    = new HBaseTableInfo(table);
        String         hbaseNameSpace    = hBaseTableInfo.getHbaseNameSpace();
        String         hbaseTableName    = hBaseTableInfo.getHbaseTableName();
        String         metadataNamespace = getMetadataNamespace();

        if (hbaseTableName != null) {
            AtlasEntity nsEntity = new AtlasEntity(HBASE_TYPE_NAMESPACE);
            nsEntity.setAttribute(ATTRIBUTE_NAME, hbaseNameSpace);
            nsEntity.setAttribute(ATTRIBUTE_CLUSTER_NAME, metadataNamespace);
            nsEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, getHBaseNameSpaceQualifiedName(metadataNamespace, hbaseNameSpace));

            ret = new AtlasEntity(HBASE_TYPE_TABLE);

            ret.setAttribute(ATTRIBUTE_NAME, hbaseTableName);
            ret.setAttribute(ATTRIBUTE_URI, hbaseTableName);

            AtlasRelatedObjectId objIdRelatedObject = new AtlasRelatedObjectId(AtlasTypeUtil.getObjectId(nsEntity), RELATIONSHIP_HBASE_TABLE_NAMESPACE);

            ret.setRelationshipAttribute(ATTRIBUTE_NAMESPACE, objIdRelatedObject);
            ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, getHBaseTableQualifiedName(metadataNamespace, hbaseNameSpace, hbaseTableName));

            entities.addReferredEntity(nsEntity);
            entities.addEntity(ret);
        }

        return ret;
    }

    protected boolean isHBaseStore(Table table) {
        boolean             ret        = false;
        Map<String, String> parameters = table.getParameters();

        if (MapUtils.isNotEmpty(parameters)) {
            String storageHandler = parameters.get(ATTRIBUTE_STORAGE_HANDLER);

            ret = (storageHandler != null && storageHandler.equals(HBASE_STORAGE_HANDLER_CLASS));
        }

        return ret;
    }

    private static String getHBaseTableQualifiedName(String metadataNamespace, String nameSpace, String tableName) {
        return String.format("%s:%s@%s", nameSpace.toLowerCase(), tableName.toLowerCase(), metadataNamespace);
    }

    private static String getHBaseNameSpaceQualifiedName(String metadataNamespace, String nameSpace) {
        return String.format("%s@%s", nameSpace.toLowerCase(), metadataNamespace);
    }

    private boolean ignoreHDFSPathsinProcessQualifiedName() {
        switch (context.getHiveOperation()) {
            case LOAD:
            case IMPORT:
                return hasPartitionEntity(getOutputs());
            case EXPORT:
                return hasPartitionEntity(getInputs());
            case QUERY:
                return true;
        }

        return false;
    }

    private boolean hasPartitionEntity(Collection<? extends Entity> entities) {
        if (entities != null) {
            for (Entity entity : entities) {
                if (entity.getType() == Entity.Type.PARTITION) {
                    return true;
                }
            }
        }

        return false;
    }

    private void addToProcessQualifiedName(StringBuilder processQualifiedName, Collection<? extends Entity> entities, boolean ignoreHDFSPaths) {
        if (entities == null) {
            return;
        }

        List<? extends Entity> sortedEntities = new ArrayList<>(entities);

        Collections.sort(sortedEntities, entityComparator);

        Set<String> dataSetsProcessed = new HashSet<>();
        Map<String, Table> tableMap   = new HashMap<>();

        for (Entity entity : sortedEntities) {
            if (ignoreHDFSPaths && (Entity.Type.DFS_DIR.equals(entity.getType()) || Entity.Type.LOCAL_DIR.equals(entity.getType()))) {
                continue;
            }

            String qualifiedName = null;
            long   createTime    = 0;

            try {
                if (entity.getType() == Entity.Type.PARTITION || entity.getType() == Entity.Type.TABLE) {
                    String tableKey = entity.getTable().getDbName() + "." + entity.getTable().getTableName();
                    Table  table    = tableMap.get(tableKey);

                    if (table == null) {
                        table = getHive().getTable(entity.getTable().getDbName(), entity.getTable().getTableName());

                        tableMap.put(tableKey, table); //since there could be several partitions in a table, store it to avoid hive calls.
                    }
                    if (table != null) {
                        createTime    = getTableCreateTime(table);
                        qualifiedName = getQualifiedName(table);
                    }
                } else {
                    qualifiedName = getQualifiedName(entity);
                }
            } catch (Exception excp) {
                LOG.error("error while computing qualifiedName for process", excp);
            }

            if (qualifiedName == null || !dataSetsProcessed.add(qualifiedName)) {
                continue;
            }

            if (entity instanceof WriteEntity) { // output entity
                WriteEntity writeEntity = (WriteEntity) entity;

                if (writeEntity.getWriteType() != null && HiveOperation.QUERY.equals(context.getHiveOperation())) {
                    boolean addWriteType = false;

                    switch (((WriteEntity) entity).getWriteType()) {
                        case INSERT:
                        case INSERT_OVERWRITE:
                        case UPDATE:
                        case DELETE:
                            addWriteType = true;
                        break;

                        case PATH_WRITE:
                            addWriteType = !Entity.Type.LOCAL_DIR.equals(entity.getType());
                        break;
                    }

                    if (addWriteType) {
                        processQualifiedName.append(QNAME_SEP_PROCESS).append(writeEntity.getWriteType().name());
                    }
                }
            }

            processQualifiedName.append(QNAME_SEP_PROCESS).append(qualifiedName.toLowerCase().replaceAll("/", ""));

            if (createTime != 0) {
                processQualifiedName.append(QNAME_SEP_PROCESS).append(createTime);
            }
        }
    }

    private boolean isAlterTableOperation() {
        switch (context.getHiveOperation()) {
            case ALTERTABLE_FILEFORMAT:
            case ALTERTABLE_CLUSTER_SORT:
            case ALTERTABLE_BUCKETNUM:
            case ALTERTABLE_PROPERTIES:
            case ALTERTABLE_SERDEPROPERTIES:
            case ALTERTABLE_SERIALIZER:
            case ALTERTABLE_ADDCOLS:
            case ALTERTABLE_REPLACECOLS:
            case ALTERTABLE_PARTCOLTYPE:
            case ALTERTABLE_LOCATION:
            case ALTERTABLE_RENAME:
            case ALTERTABLE_RENAMECOL:
            case ALTERVIEW_PROPERTIES:
            case ALTERVIEW_RENAME:
            case ALTERVIEW_AS:
                return true;
        }

        return false;
    }

    static final class EntityComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity entity1, Entity entity2) {
            String name1 = entity1.getName();
            String name2 = entity2.getName();

            if (name1 == null || name2 == null) {
                name1 = entity1.getD().toString();
                name2 = entity2.getD().toString();
            }

            return name1.toLowerCase().compareTo(name2.toLowerCase());
        }
    }

    static final Comparator<Entity> entityComparator = new EntityComparator();

    static final class HBaseTableInfo {
        String hbaseNameSpace = null;
        String hbaseTableName = null;

         HBaseTableInfo(Table table) {
            Map<String, String> parameters = table.getParameters();

            if (MapUtils.isNotEmpty(parameters)) {
                hbaseNameSpace = HBASE_DEFAULT_NAMESPACE;
                hbaseTableName = parameters.get(HBASE_PARAM_TABLE_NAME);

                if (hbaseTableName != null) {
                    if (hbaseTableName.contains(HBASE_NAMESPACE_TABLE_DELIMITER)) {
                        String[] hbaseTableInfo = hbaseTableName.split(HBASE_NAMESPACE_TABLE_DELIMITER);

                        if (hbaseTableInfo.length > 1) {
                            hbaseNameSpace = hbaseTableInfo[0];
                            hbaseTableName = hbaseTableInfo[1];
                        }
                    }
                }
            }
        }

        public String getHbaseNameSpace() {
            return hbaseNameSpace;
        }

        public String getHbaseTableName() {
            return hbaseTableName;
        }
    }

    public static Table toTable(org.apache.hadoop.hive.metastore.api.Table table) {
        return new Table(table);
    }
}
