/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.catalog.metadata.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.datavines.common.datasource.jdbc.entity.ColumnInfo;
import io.datavines.common.datasource.jdbc.entity.DatabaseInfo;
import io.datavines.common.datasource.jdbc.entity.TableColumnInfo;
import io.datavines.common.datasource.jdbc.entity.TableInfo;
import io.datavines.common.enums.EntityRelType;
import io.datavines.common.param.ConnectorResponse;
import io.datavines.common.param.GetColumnsRequestParam;
import io.datavines.common.param.GetDatabasesRequestParam;
import io.datavines.common.param.GetTablesRequestParam;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.StringUtils;
import io.datavines.connector.api.ConnectorFactory;
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.catalog.enums.SchemaChangeType;
import io.datavines.server.repository.entity.DataSource;
import io.datavines.server.repository.entity.catalog.CatalogEntityInstance;
import io.datavines.server.repository.entity.catalog.CatalogEntityRel;
import io.datavines.server.repository.entity.catalog.CatalogSchemaChange;
import io.datavines.server.repository.service.CatalogEntityInstanceService;
import io.datavines.server.repository.service.CatalogEntityRelService;
import io.datavines.server.repository.service.CatalogSchemaChangeService;
import io.datavines.server.utils.SpringApplicationContext;
import io.datavines.spi.PluginLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.datavines.core.enums.Status.CATALOG_FETCH_DATASOURCE_NULL_ERROR;

@Slf4j
public class CatalogMetaDataFetchExecutorImpl implements CatalogMetaDataFetchExecutor {

    private final MetaDataFetchRequest request;

    private final ConnectorFactory connectorFactory;

    private final CatalogEntityInstanceService instanceService;

    private final CatalogEntityRelService relService;

    private final CatalogSchemaChangeService schemaChangeService;

    private final DataSource dataSource;

    public CatalogMetaDataFetchExecutorImpl(MetaDataFetchRequest request) {
        this.request = request;

        if (request.getDataSource() == null) {
            throw new DataVinesServerException(CATALOG_FETCH_DATASOURCE_NULL_ERROR);
        }

        this.dataSource = request.getDataSource();

        this.connectorFactory = PluginLoader
                .getPluginLoader(ConnectorFactory.class)
                .getOrCreatePlugin(dataSource.getType());

        this.instanceService = SpringApplicationContext.getBean(CatalogEntityInstanceService.class);
        this.relService = SpringApplicationContext.getBean(CatalogEntityRelService.class);
        this.schemaChangeService = SpringApplicationContext.getBean(CatalogSchemaChangeService.class);
    }

    @Override
    public void execute() throws SQLException {
        switch (request.getFetchType()) {
            case DATASOURCE:
                executeFetchDataSource();
                break;
            case DATABASE:
                executeFetchDatabase(request.getDatabase());
                break;
            case TABLE:
                executeFetchTable(request.getDatabase(), request.getTable());
                break;
            default:
                break;
        }
    }

    private void executeFetchDataSource() throws SQLException {

        List<String> createDatabaseEntityList = new ArrayList<>();
        List<String> deleteDatabaseEntityList = new ArrayList<>();

        List<String> databaseListFromDataSource = new ArrayList<>();
        Map<String, DatabaseInfo> databaseInfoMap = new HashMap<>();

        GetDatabasesRequestParam param = new GetDatabasesRequestParam();
        param.setType(dataSource.getType());
        param.setDataSourceParam(dataSource.getParam());
        ConnectorResponse connectorResponse =
                connectorFactory.getConnector().getDatabases(param);

        if (connectorResponse == null || connectorResponse.getResult() == null) {
            return;
        }

        Long datasourceId = dataSource.getId();

        List<DatabaseInfo> databaseInfoList = (List<DatabaseInfo>)connectorResponse.getResult();
        if (CollectionUtils.isEmpty(databaseInfoList)) {
            return;
        }

        // 需要跟现有的数据库进行对比，找出新增、删除的数据库
        for (DatabaseInfo databaseInfo : databaseInfoList) {
            databaseListFromDataSource.add(dataSource.getId() + "@@" + databaseInfo.getName());
            databaseInfoMap.put(dataSource.getId() + "@@" + databaseInfo.getName(), databaseInfo);
        }

        //获取数据库中的表列表
        List<String> databaseListFromDb = new ArrayList<>();
        Map<String, CatalogEntityInstance> databaseListFromDbMap = new HashMap<>();
        List<CatalogEntityRel> databaseEntityRelList =
                relService.list(new QueryWrapper<CatalogEntityRel>().eq("entity1_uuid", dataSource.getUuid()));
        if (CollectionUtils.isNotEmpty(databaseEntityRelList)) {
            databaseEntityRelList.forEach(item -> {
                CatalogEntityInstance entityInstance = instanceService.getOne(
                        new QueryWrapper<CatalogEntityInstance>().eq("uuid", item.getEntity2Uuid()).eq("status","active"));

                if (entityInstance != null) {
                    databaseListFromDb.add(dataSource.getId()+"@@"+entityInstance.getFullyQualifiedName());
                    databaseListFromDbMap.put(dataSource.getId()+"@@"+entityInstance.getFullyQualifiedName(), entityInstance);
                }
            });
        }

        List<CatalogEntityInstance> databaseList = new ArrayList<>();
        boolean isFirstFetch = false;
        if (CollectionUtils.isEmpty(databaseListFromDb)) {
            createDatabaseEntityList = databaseListFromDataSource;
            isFirstFetch = true;
        } else {
            for (String t1: databaseListFromDataSource){
                if (!databaseListFromDb.contains(t1)) {
                    createDatabaseEntityList.add(t1);
                } else {
                    databaseList.add(databaseListFromDbMap.get(t1));
                }
            }

            for (String t1: databaseListFromDb) {
                if (!databaseListFromDataSource.contains(t1)) {
                    deleteDatabaseEntityList.add(t1);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(deleteDatabaseEntityList)) {
            deleteDatabaseEntityList.forEach(t -> {
                instanceService.softDeleteEntityByDataSourceAndFQN(Long.valueOf(t.split("@@")[0]), t.split("@@")[1]);
                CatalogEntityInstance databaseEntityInstance = databaseListFromDbMap.get(t);
                CatalogSchemaChange databaseDeleteChange = new CatalogSchemaChange();
                databaseDeleteChange.setParentUuid(dataSource.getUuid());
                databaseDeleteChange.setEntityUuid(databaseEntityInstance.getUuid());
                databaseDeleteChange.setChangeType(SchemaChangeType.DATABASE_DELETED);
                databaseDeleteChange.setDatabaseName(t.split("@@")[1]);
                databaseDeleteChange.setUpdateTime(LocalDateTime.now());
                databaseDeleteChange.setUpdateBy(0L);
                schemaChangeService.save(databaseDeleteChange);
                // 记录 database delete change
            });
        }

        if (CollectionUtils.isNotEmpty(createDatabaseEntityList)) {
            for (String database : createDatabaseEntityList) {
                DatabaseInfo databaseInfo = databaseInfoMap.get(database);
                if ("sys".equals(databaseInfo.getName()) || "information_schema".equals(databaseInfo.getName()) ||
                    "performance_schema".equals(databaseInfo.getName()) || "mysql".equals(databaseInfo.getName())) {
                    continue;
                }
                CatalogEntityInstance databaseEntityInstance = new CatalogEntityInstance();
                databaseEntityInstance.setType("database");
                databaseEntityInstance.setDisplayName(databaseInfo.getName());
                databaseEntityInstance.setFullyQualifiedName(databaseInfo.getName());
                databaseEntityInstance.setUuid(UUID.randomUUID().toString());
                databaseEntityInstance.setUpdateTime(LocalDateTime.now());
                databaseEntityInstance.setUpdateBy(0L);
                databaseEntityInstance.setStatus("active");
                databaseEntityInstance.setDatasourceId(datasourceId);
                String tableUUID = instanceService.create(databaseEntityInstance);
                if (!isFirstFetch) {
                    // 记录 database added change
                    CatalogSchemaChange databaseAddChange = new CatalogSchemaChange();
                    databaseAddChange.setParentUuid(dataSource.getUuid());
                    databaseAddChange.setEntityUuid(databaseEntityInstance.getUuid());
                    databaseAddChange.setChangeType(SchemaChangeType.DATABASE_ADDED);
                    databaseAddChange.setDatabaseName(databaseInfo.getName());
                    databaseAddChange.setUpdateTime(LocalDateTime.now());
                    databaseAddChange.setUpdateBy(0L);
                    schemaChangeService.save(databaseAddChange);
                }

                databaseList.add(databaseEntityInstance);

                CatalogEntityRel dataSource2databaseRel = new CatalogEntityRel();
                dataSource2databaseRel.setEntity1Uuid(dataSource.getUuid());
                dataSource2databaseRel.setEntity2Uuid(tableUUID);
                dataSource2databaseRel.setType(EntityRelType.CHILD.getDescription());
                dataSource2databaseRel.setUpdateTime(LocalDateTime.now());
                dataSource2databaseRel.setUpdateBy(0L);
                relService.save(dataSource2databaseRel);
            }
        }

        if (CollectionUtils.isEmpty(databaseList)) {
            return;
        }

        databaseList.forEach(database -> {
            executeFetchDatabase(database.getDisplayName());
        });
    }

    private void executeFetchDatabase(String database) {

        Long datasourceId = dataSource.getId();

        CatalogEntityInstance oldDatabaseInstance =
                instanceService.getByDataSourceAndFQN(datasourceId, database);
        if (oldDatabaseInstance == null) {
            oldDatabaseInstance = new CatalogEntityInstance();
            oldDatabaseInstance.setType("database");
            oldDatabaseInstance.setDisplayName(database);
            oldDatabaseInstance.setFullyQualifiedName(database);
            oldDatabaseInstance.setUuid(UUID.randomUUID().toString());
            oldDatabaseInstance.setUpdateTime(LocalDateTime.now());
            oldDatabaseInstance.setUpdateBy(0L);
            oldDatabaseInstance.setStatus("active");
            oldDatabaseInstance.setDatasourceId(datasourceId);
            instanceService.create(oldDatabaseInstance);
        }

        String databaseUUID = oldDatabaseInstance.getUuid();
        //获取数据源中的表列表
        GetTablesRequestParam getTablesRequestParam = new GetTablesRequestParam();
        getTablesRequestParam.setType(dataSource.getType());
        getTablesRequestParam.setDataSourceParam(dataSource.getParam());
        getTablesRequestParam.setDataBase(database);
        ConnectorResponse connectorResponse = null;
        List<String> tableListFromDataSource = new ArrayList<>();
        Map<String, TableInfo> tableInfoMap = new HashMap<>();
        try {
            connectorResponse = connectorFactory
                    .getConnector()
                    .getTables(getTablesRequestParam);

            List<TableInfo> tableList = (List<TableInfo>)connectorResponse.getResult();
            if (CollectionUtils.isNotEmpty(tableList)) {
                tableList.forEach(table -> {
                    tableListFromDataSource.add(dataSource.getId() + "@@" + database + "." + table.getName());
                    tableInfoMap.put(dataSource.getId() + "@@" + database + "." + table.getName(), table);
                });
            }
        } catch (SQLException sqlException) {
            log.error("fetch table error :", sqlException);
            throw new DataVinesServerException(Status.FAIL);
        }

        //获取数据库中的表列表
        List<String> tableListFromDb = new ArrayList<>();
        List<CatalogEntityRel> tableEntityRelList =
                relService.list(new QueryWrapper<CatalogEntityRel>().eq("entity1_uuid", databaseUUID));
        Map<String, CatalogEntityInstance> tableMapFromDb = new HashMap<>();
        if (CollectionUtils.isNotEmpty(tableEntityRelList)) {
            tableEntityRelList.forEach(item -> {
                CatalogEntityInstance entityInstanceDO = instanceService.getOne(
                        new QueryWrapper<CatalogEntityInstance>().eq("uuid", item.getEntity2Uuid()).eq("status","active"));

                if (entityInstanceDO != null) {
                    tableListFromDb.add(dataSource.getId()+"@@"+entityInstanceDO.getFullyQualifiedName());
                    tableMapFromDb.put(dataSource.getId()+"@@"+entityInstanceDO.getFullyQualifiedName(), entityInstanceDO);
                }
            });
        }

        List<String> createTableEntityList = new ArrayList<>();
        List<String> maybeUpdateTableEntityList = new ArrayList<>();
        List<String> deleteTableEntityList = new ArrayList<>();

        boolean isFirstFetch = false;
        if (CollectionUtils.isEmpty(tableListFromDb)) {
            createTableEntityList = tableListFromDataSource;
            isFirstFetch = true;
        } else {
            for(String t1: tableListFromDataSource){
                if (tableListFromDb.contains(t1)) {
                    maybeUpdateTableEntityList.add(t1);
                } else {
                    createTableEntityList.add(t1);
                }
            }

            for(String t1: tableListFromDb){
                if (!tableListFromDataSource.contains(t1)) {
                    deleteTableEntityList.add(t1);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(deleteTableEntityList)) {
            deleteTableEntityList.forEach(t -> {
                instanceService.softDeleteEntityByDataSourceAndFQN(Long.valueOf(t.split("@@")[0]), t.split("@@")[1]);

                CatalogEntityInstance tableEntityInstance = tableMapFromDb.get(t);
                String[] values = t.split("@@")[1].split("\\.");
                CatalogSchemaChange tableDeleteChange = new CatalogSchemaChange();
                tableDeleteChange.setParentUuid(databaseUUID);
                tableDeleteChange.setEntityUuid(tableEntityInstance.getUuid());
                tableDeleteChange.setChangeType(SchemaChangeType.TABLE_DELETED);
                tableDeleteChange.setDatabaseName(values[0]);
                tableDeleteChange.setTableName(values[1]);
                tableDeleteChange.setUpdateTime(LocalDateTime.now());
                tableDeleteChange.setUpdateBy(0L);
                schemaChangeService.save(tableDeleteChange);
                // 记录 table delete change
            });
        }

        List<CatalogEntityInstance> tableList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(maybeUpdateTableEntityList)) {
            maybeUpdateTableEntityList.forEach(t -> {
                TableInfo tableInfo = tableInfoMap.get(t);
                CatalogEntityInstance tableEntityInstance = tableMapFromDb.get(t);

                if (StringUtils.isNotEmpty(tableEntityInstance.getProperties())) {
                    TableInfo oldInfo = JSONUtils.parseObject(tableEntityInstance.getProperties(), TableInfo.class);
                    String oldComment = null;
                    if (oldInfo != null) {
                        oldComment = oldInfo.getComment();
                    }

                    String newComment = null;
                    newComment = tableInfo.getComment();

                    if (isCommentChange(oldComment, newComment)) {
                        addTableCommentChangeRecord(databaseUUID, tableEntityInstance, database, tableInfo, oldComment, newComment);
                    }

                } else {
                    if (tableInfo != null && StringUtils.isNotEmpty(tableInfo.getComment())) {
                        tableEntityInstance.setDescription(tableInfo.getComment());
                        addTableCommentChangeRecord(databaseUUID, tableEntityInstance, database, tableInfo, null, tableInfo.getComment());
                    }
                }

                tableEntityInstance.setProperties(JSONUtils.toJsonString(tableInfo));
                instanceService.updateById(tableEntityInstance);
                tableList.add(tableEntityInstance);
            });
        }

        if (CollectionUtils.isNotEmpty(createTableEntityList)) {
            for (String t : createTableEntityList) {
                TableInfo tableInfo = tableInfoMap.get(t);
                CatalogEntityInstance tableEntityInstance = new CatalogEntityInstance();
                tableEntityInstance.setType("table");
                tableEntityInstance.setDisplayName(tableInfo.getName());
                tableEntityInstance.setFullyQualifiedName(database + "." + tableInfo.getName());
                tableEntityInstance.setDescription(tableInfo.getComment());
                tableEntityInstance.setUuid(UUID.randomUUID().toString());
                tableEntityInstance.setUpdateTime(LocalDateTime.now());
                tableEntityInstance.setUpdateBy(0L);
                tableEntityInstance.setStatus("active");
                tableEntityInstance.setProperties(JSONUtils.toJsonString(tableInfo));
                tableEntityInstance.setOwner(StringUtils.isNotEmpty(tableInfo.getOwner()) ? tableInfo.getOwner():"");
                if (StringUtils.isNotEmpty(tableInfo.getCreateTime())) {
                    tableEntityInstance.setCreateTime(
                            LocalDateTime.parse(tableInfo.getCreateTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                tableEntityInstance.setDatasourceId(datasourceId);
                String tableUUID = instanceService.create(tableEntityInstance);
                if (!isFirstFetch) {
                    CatalogSchemaChange tableAddChange = new CatalogSchemaChange();
                    tableAddChange.setParentUuid(databaseUUID);
                    tableAddChange.setEntityUuid(tableEntityInstance.getUuid());
                    tableAddChange.setChangeType(SchemaChangeType.TABLE_ADDED);
                    tableAddChange.setDatabaseName(database);
                    tableAddChange.setTableName(tableInfo.getName());
                    tableAddChange.setUpdateTime(LocalDateTime.now());
                    tableAddChange.setUpdateBy(0L);
                    schemaChangeService.save(tableAddChange);
                }

                tableList.add(tableEntityInstance);

                CatalogEntityRel entityRel = new CatalogEntityRel();
                entityRel.setEntity1Uuid(oldDatabaseInstance.getUuid());
                entityRel.setEntity2Uuid(tableUUID);
                entityRel.setType(EntityRelType.CHILD.getDescription());
                entityRel.setUpdateTime(LocalDateTime.now());
                entityRel.setUpdateBy(0L);
                relService.save(entityRel);
            }
        }

        if (CollectionUtils.isEmpty(tableList)) {
            return;
        }

        tableList.forEach(table -> {
            String[] values = table.getFullyQualifiedName().split("\\.");
            executeFetchTable(values[0], values[1]);
        });

    }

    private void addTableCommentChangeRecord(String parentUUID, CatalogEntityInstance tableEntityInstance, String database, TableInfo tableInfo, String oldComment, String newComment) {
        CatalogSchemaChange tableCommentChange = new CatalogSchemaChange();
        tableCommentChange.setParentUuid(parentUUID);
        tableCommentChange.setEntityUuid(tableEntityInstance.getUuid());
        tableCommentChange.setChangeType(SchemaChangeType.TABLE_COMMENT_CHANGE);
        tableCommentChange.setDatabaseName(database);
        tableCommentChange.setTableName(tableInfo.getName());
        tableCommentChange.setChangeBefore(oldComment);
        tableCommentChange.setChangeAfter(newComment);
        tableCommentChange.setUpdateTime(LocalDateTime.now());
        tableCommentChange.setUpdateBy(0L);
        schemaChangeService.save(tableCommentChange);
    }

    private void executeFetchTable(String database, String table) {

        Long datasourceId = dataSource.getId();

        CatalogEntityInstance oldTableInstance =
                instanceService.getByDataSourceAndFQN(datasourceId, database + "." + table);
        if (oldTableInstance == null) {
            return;
        }

        String tableUUID = oldTableInstance.getUuid();
        //获取数据源中的列列表
        GetColumnsRequestParam getColumnsRequestParam = new GetColumnsRequestParam();
        getColumnsRequestParam.setType(dataSource.getType());
        getColumnsRequestParam.setDataSourceParam(dataSource.getParam());
        getColumnsRequestParam.setDataBase(database);
        getColumnsRequestParam.setTable(table);
        ConnectorResponse connectorResponse = null;
        List<String> columnListFromDataSource = new ArrayList<>();
        Map<String, ColumnInfo> tableColumnMap = new HashMap<>();
        try {
            connectorResponse = connectorFactory
                    .getConnector()
                    .getColumns(getColumnsRequestParam);
            TableColumnInfo tableColumnInfo = (TableColumnInfo)connectorResponse.getResult();
            if (tableColumnInfo != null) {
                if (CollectionUtils.isNotEmpty(tableColumnInfo.getColumns())) {
                    tableColumnInfo.getColumns().forEach(column -> {
                        columnListFromDataSource.add(datasourceId + "@@" + database + "." + table + "." + column.getName());
                        tableColumnMap.put(datasourceId + "@@" + database + "." + table + "." + column.getName(), column);
                    });
                }
            }

        } catch (SQLException sqlException) {
            log.error("get column error :", sqlException);
            throw new DataVinesServerException(Status.FAIL);
        }

        if (CollectionUtils.isEmpty(columnListFromDataSource)) {
            return;
        }

        List<String> columnListFromDb = new ArrayList<>();
        //获取数据库中的列列表
        List<CatalogEntityRel> columnEntityRelList = relService
                .list(new QueryWrapper<CatalogEntityRel>().eq("entity1_uuid", tableUUID));

        Map<String, CatalogEntityInstance> columnMapFromDb = new HashMap<>();
        if (CollectionUtils.isNotEmpty(columnEntityRelList)) {
            columnEntityRelList.forEach(item -> {
                CatalogEntityInstance entityInstance = instanceService
                        .getOne(new QueryWrapper<CatalogEntityInstance>().eq("uuid", item.getEntity2Uuid()).eq("status","active"));

                if (entityInstance != null) {
                    columnListFromDb.add(datasourceId + "@@" + entityInstance.getFullyQualifiedName());
                    columnMapFromDb.put(datasourceId + "@@" + entityInstance.getFullyQualifiedName(), entityInstance);
                }
            });
        }

        List<String> createColumnEntityList = new ArrayList<>();
        List<String> maybeUpdateColumnEntityList = new ArrayList<>();
        List<String> deleteColumnEntityList = new ArrayList<>();

        boolean isFirstFetch = false;
        if (CollectionUtils.isEmpty(columnListFromDb)) {
            createColumnEntityList = columnListFromDataSource;
            isFirstFetch = true;
        } else {
            for(String t1: columnListFromDataSource){
                if (columnListFromDb.contains(t1)) {
                    maybeUpdateColumnEntityList.add(t1);
                } else {
                    createColumnEntityList.add(t1);
                }
            }

            for(String t1: columnListFromDb){
                if (!columnListFromDataSource.contains(t1)) {
                    deleteColumnEntityList.add(t1);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(deleteColumnEntityList)) {
            deleteColumnEntityList.forEach(t -> {
                instanceService
                        .softDeleteEntityByDataSourceAndFQN(Long.valueOf(t.split("@@")[0]),t.split("@@")[1]);
                CatalogEntityInstance columnEntityInstance = columnMapFromDb.get(t);
                String[] values = t.split("@@")[1].split("\\.");
                CatalogSchemaChange columnDeleteChange = new CatalogSchemaChange();
                columnDeleteChange.setParentUuid(tableUUID);
                columnDeleteChange.setEntityUuid(columnEntityInstance.getUuid());
                columnDeleteChange.setChangeType(SchemaChangeType.COLUMN_DELETED);
                columnDeleteChange.setDatabaseName(values[0]);
                columnDeleteChange.setTableName(values[1]);
                columnDeleteChange.setColumnName(values[2]);
                columnDeleteChange.setUpdateTime(LocalDateTime.now());
                columnDeleteChange.setUpdateBy(0L);
                schemaChangeService.save(columnDeleteChange);
            });
        }

        if (CollectionUtils.isNotEmpty(maybeUpdateColumnEntityList)) {
            maybeUpdateColumnEntityList.forEach(t -> {
                ColumnInfo columnInfo = tableColumnMap.get(t);
                CatalogEntityInstance columnEntityInstance = columnMapFromDb.get(t);

                if (StringUtils.isNotEmpty(columnEntityInstance.getProperties())) {
                    ColumnInfo oldInfo = JSONUtils.parseObject(columnEntityInstance.getProperties(), ColumnInfo.class);
                    String oldComment = null;
                    if (oldInfo != null) {
                        oldComment = oldInfo.getComment();
                    }

                    String newComment = null;
                    if (columnInfo != null) {
                        newComment = columnInfo.getComment();
                    }

                    if (isCommentChange(oldComment, newComment)) {
                        addColumnCommentChangeRecord(tableUUID,columnEntityInstance.getUuid(),database,table,columnEntityInstance.getDisplayName(),oldComment, newComment);
                    }

                    String oldType = null;
                    if (oldInfo != null) {
                        oldType = oldInfo.getType();
                    }

                    String newType = null;
                    if (columnInfo != null) {
                        newType = columnInfo.getType();
                    }

                    if (isTypeChange(oldType, newType)) {
                        addColumnTypeChangeRecord(tableUUID, columnEntityInstance.getUuid(), database, table, columnEntityInstance.getDisplayName(), oldType, newType);
                    }

                } else if (columnInfo != null) {
                    if (StringUtils.isNotEmpty(columnInfo.getComment())) {
                        columnEntityInstance.setDescription(columnInfo.getComment());
                        addColumnCommentChangeRecord(tableUUID, columnEntityInstance.getUuid(),database,table,columnEntityInstance.getDisplayName(),null, columnInfo.getComment());
                    }

                    if (StringUtils.isNotEmpty(columnInfo.getType())) {
                        addColumnTypeChangeRecord(tableUUID, columnEntityInstance.getUuid(), database, table, columnEntityInstance.getDisplayName(), null, columnInfo.getType());
                    }
                }

                columnEntityInstance.setProperties(JSONUtils.toJsonString(columnInfo));
                instanceService.updateById(columnEntityInstance);
            });
        }

        if (CollectionUtils.isNotEmpty(createColumnEntityList)) {
            for (String t : createColumnEntityList) {
                ColumnInfo columnInfo = tableColumnMap.get(t);
                CatalogEntityInstance columnEntityInstance = new CatalogEntityInstance();
                columnEntityInstance.setType("column");
                columnEntityInstance.setDisplayName(columnInfo.getName());
                columnEntityInstance.setFullyQualifiedName(database + "." + oldTableInstance.getDisplayName() + "." + columnInfo.getName());
                columnEntityInstance.setDescription(columnInfo.getComment());
                columnEntityInstance.setUuid(UUID.randomUUID().toString());
                columnEntityInstance.setUpdateTime(LocalDateTime.now());
                columnEntityInstance.setUpdateBy(0L);
                columnEntityInstance.setDatasourceId(datasourceId);
                columnEntityInstance.setProperties(JSONUtils.toJsonString(columnInfo));
                columnEntityInstance.setStatus("active");
                String columnUUID = instanceService.create(columnEntityInstance);
                if (!isFirstFetch) {
                    CatalogSchemaChange columnAddChange = new CatalogSchemaChange();
                    columnAddChange.setParentUuid(tableUUID);
                    columnAddChange.setEntityUuid(columnUUID);
                    columnAddChange.setChangeType(SchemaChangeType.COLUMN_ADDED);
                    columnAddChange.setDatabaseName(database);
                    columnAddChange.setTableName(table);
                    columnAddChange.setColumnName(columnInfo.getName());;
                    columnAddChange.setUpdateTime(LocalDateTime.now());
                    columnAddChange.setUpdateBy(0L);
                    schemaChangeService.save(columnAddChange);
                }

                CatalogEntityRel entityRelDO = new CatalogEntityRel();
                entityRelDO.setEntity1Uuid(oldTableInstance.getUuid());
                entityRelDO.setEntity2Uuid(columnUUID);
                entityRelDO.setType(EntityRelType.CHILD.getDescription());
                entityRelDO.setUpdateTime(LocalDateTime.now());
                entityRelDO.setUpdateBy(0L);
                relService.save(entityRelDO);
            }
        }
    }

    private void addColumnCommentChangeRecord(String parentUUID, String uuid, String database, String table, String column, String oldComment, String newComment) {
        CatalogSchemaChange columnCommentChange = new CatalogSchemaChange();
        columnCommentChange.setParentUuid(parentUUID);
        columnCommentChange.setEntityUuid(uuid);
        columnCommentChange.setChangeType(SchemaChangeType.COLUMN_COMMENT_CHANGE);
        columnCommentChange.setDatabaseName(database);
        columnCommentChange.setTableName(table);
        columnCommentChange.setColumnName(column);
        columnCommentChange.setChangeBefore(oldComment);
        columnCommentChange.setChangeAfter(newComment);
        columnCommentChange.setUpdateTime(LocalDateTime.now());
        columnCommentChange.setUpdateBy(0L);
        schemaChangeService.save(columnCommentChange);
    }

    private void addColumnTypeChangeRecord(String parentUUID, String uuid, String database, String table, String column, String oldType, String newType) {
        CatalogSchemaChange columnTypeChange = new CatalogSchemaChange();
        columnTypeChange.setParentUuid(parentUUID);
        columnTypeChange.setEntityUuid(uuid);
        columnTypeChange.setChangeType(SchemaChangeType.COLUMN_TYPE_CHANGE);
        columnTypeChange.setDatabaseName(database);
        columnTypeChange.setTableName(table);
        columnTypeChange.setColumnName(column);
        columnTypeChange.setChangeBefore(oldType);
        columnTypeChange.setChangeAfter(newType);
        columnTypeChange.setUpdateTime(LocalDateTime.now());
        columnTypeChange.setUpdateBy(0L);
        schemaChangeService.save(columnTypeChange);
    }

    private boolean isCommentChange(String oldComment, String newComment) {
        if (StringUtils.isEmpty(oldComment) && StringUtils.isNotEmpty(newComment)) {
            return true;
        }

        if (StringUtils.isEmpty(newComment) && StringUtils.isNotEmpty(oldComment)) {
            return true;
        }

        return StringUtils.isNotEmpty(newComment) && StringUtils.isNotEmpty(oldComment) && !oldComment.equals(newComment);
    }

    private boolean isTypeChange(String oldType, String newType) {
        if (StringUtils.isEmpty(oldType) && StringUtils.isNotEmpty(newType)) {
            return true;
        }

        if (StringUtils.isEmpty(newType) && StringUtils.isNotEmpty(oldType)) {
            return true;
        }

        return StringUtils.isNotEmpty(newType) && StringUtils.isNotEmpty(oldType) && !oldType.equals(newType);
    }
}
