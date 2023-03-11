/*
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

package com.netease.arctic.ams.server.service.impl;

import com.google.common.collect.Maps;
import com.netease.arctic.ams.api.SchemaUpdateMeta;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.api.UpdateColumn;
import com.netease.arctic.ams.server.config.ServerTableProperties;
import com.netease.arctic.ams.server.mapper.DDLRecordMapper;
import com.netease.arctic.ams.server.mapper.TableMetadataMapper;
import com.netease.arctic.ams.server.model.DDLInfo;
import com.netease.arctic.ams.server.model.TableMetadata;
import com.netease.arctic.ams.server.service.IJDBCService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.utils.CatalogUtil;
import com.netease.arctic.ams.server.utils.PropertiesUtil;
import com.netease.arctic.ams.server.utils.TableMetadataUtil;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.trace.TableTracer;
import org.apache.ibatis.session.SqlSession;
import org.apache.iceberg.HasTableOperations;
import org.apache.iceberg.HistoryEntry;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableMetadataParser;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DDLTracerService extends IJDBCService {

  private static final Logger LOG = LoggerFactory.getLogger(DDLTracerService.class);

  static final String DOT = ".";

  private static final String ALTER_TABLE = "ALTER TABLE %s";
  private static final String ADD_COLUMN = " ADD COLUMN ";
  private static final String ALTER_COLUMN = " ALTER COLUMN %s";
  private static final String MOVE_FIRST = " ALTER COLUMN %s FIRST";
  private static final String MOVE_AFTER_COLUMN = " ALTER COLUMN %s AFTER %s";
  private static final String RENAME_COLUMN = " RENAME COLUMN %s TO %s";
  private static final String DROP_COLUMNS = " DROP COLUMN %s";
  private static final String SET_PROPERTIES = " SET TBLPROPERTIES (%s)";
  private static final String UNSET_PROPERTIES = " UNSET TBLPROPERTIES (%s)";
  private static final String IS_OPTIONAL = " DROP NOT NULL";
  private static final String NOT_OPTIONAL = " SET NOT NULL";
  private static final String DOC = " COMMENT '%s'";
  private static final String TYPE = " TYPE %s";

  public void commit(TableIdentifier tableIdentifier, SchemaUpdateMeta commitMeta) {
    Long commitTime = System.currentTimeMillis();
    int schemaId = commitMeta.getSchemaId();
    ArcticCatalog catalog =
        CatalogLoader.load(ServiceContainer.getTableMetastoreHandler(), tableIdentifier.getCatalog());
    com.netease.arctic.table.TableIdentifier tmp = com.netease.arctic.table.TableIdentifier.of(
        tableIdentifier.getCatalog(),
        tableIdentifier.getDatabase(),
        tableIdentifier.getTableName());
    ArcticTable arcticTable = catalog.loadTable(tmp);
    Table table = arcticTable.isKeyedTable() ? arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
    StringBuilder schemaSql = new StringBuilder();
    for (int j = 0; j < commitMeta.getUpdateColumns().size(); j++) {
      UpdateColumn updateColumn = commitMeta.getUpdateColumns().get(j);
      String operateType = updateColumn.getOperate();
      StringBuilder sql =
          new StringBuilder(String.format(ALTER_TABLE, TableMetadataUtil.getTableAllIdentifyName(tableIdentifier)));
      switch (TableTracer.SchemaOperateType.valueOf(operateType)) {
        case ADD:
          String colName = updateColumn.getParent() == null ? updateColumn.getName() :
              updateColumn.getParent() + DOT + updateColumn.getName();
          sql.append(ADD_COLUMN).append(colName);
          if (updateColumn.getType() != null) {
            sql.append(" ").append(updateColumn.getType()).append(" ");
          }
          if (updateColumn.getDoc() != null) {
            sql.append(String.format(DOC, updateColumn.getDoc()));
          }
          break;
        case DROP:
          sql.append(String.format(DROP_COLUMNS, updateColumn.getName()));
          break;
        case RENAME:
          sql.append(String.format(RENAME_COLUMN, updateColumn.getName(), updateColumn.getNewName()));
          break;
        case ALERT:
          sql.append(String.format(ALTER_COLUMN, updateColumn.getName()));
          if (updateColumn.getType() != null) {
            sql.append(String.format(TYPE, updateColumn.getType()));
          }
          if (updateColumn.getDoc() != null) {
            sql.append(String.format(DOC, updateColumn.getDoc()));
          }
          if (updateColumn.getIsOptional() != null) {
            if (Boolean.parseBoolean(updateColumn.getIsOptional())) {
              sql.append(IS_OPTIONAL);
            } else {
              sql.append(NOT_OPTIONAL);
            }
          }
          break;
        case MOVE_BEFORE:
          Integer referencePosition = null;
          Schema schema = table.schemas().get(schemaId);
          for (int i = 0; i < schema.columns().size(); i++) {
            Types.NestedField field = schema.columns().get(i);
            if (field.name().equals(updateColumn.getName())) {
              referencePosition = i - 1;
            }
          }
          if (referencePosition != null) {
            if (referencePosition == -1) {
              sql.append(String.format(MOVE_FIRST, updateColumn.getName()));
            } else {
              String referenceName = schema.columns().get(referencePosition).name();
              sql.append(String.format(MOVE_AFTER_COLUMN, updateColumn.getName(), referenceName));
            }
          }
          break;
        case MOVE_AFTER:
          sql.append(String.format(MOVE_AFTER_COLUMN, updateColumn.getName(), updateColumn.getNewName()));
          break;
        case MOVE_FIRST:
          sql.append(String.format(MOVE_FIRST, updateColumn.getName()));
          break;
        default:
          break;
      }
      sql.append(";");
      if (sql.length() > 0) {
        if (j < commitMeta.getUpdateColumns().size() - 1) {
          sql.append("\n");
        }
        schemaSql.append(sql);
      }
    }
    DDLInfo ddlInfo =
        DDLInfo.of(tableIdentifier, schemaSql.toString(), DDLType.UPDATE_SCHEMA.name(), commitTime);
    insert(ddlInfo);
    setCurrentSchemaId(tableIdentifier, schemaId);
  }

  public void commitProperties(TableIdentifier tableIdentifier, Map<String, String> before, Map<String, String> after) {
    String tableName = TableMetadataUtil.getTableAllIdentifyName(tableIdentifier);
    Long commitTime = System.currentTimeMillis();
    StringBuilder setSql = new StringBuilder();
    StringBuilder unsetSql = new StringBuilder();
    StringBuilder unsetPro = new StringBuilder();
    int c = 0;
    for (String oldPro : before.keySet()) {
      if (TableProperties.WRITE_PROTECTED_PROPERTIES.contains(oldPro)) {
        continue;
      }
      if (!after.containsKey(oldPro)) {
        if (c > 0) {
          unsetPro.append(",").append("\n");
        }
        unsetPro.append(String.format("'%s'", oldPro));
        c++;
      }
    }
    StringBuilder setPro = new StringBuilder();
    int c1 = 0;
    for (String newPro : after.keySet()) {
      if (TableProperties.WRITE_PROTECTED_PROPERTIES.contains(newPro)) {
        continue;
      }
      if (!after.get(newPro).equals(before.get(newPro))) {
        if (c1 > 0) {
          setPro.append(",").append("\n");
        }
        setPro.append(String.format("'%s'='%s'", newPro, after.get(newPro)));
        c1++;
      }
    }
    if (setPro.length() > 0) {
      setSql.append(String.format(ALTER_TABLE, tableName)).append(String.format(SET_PROPERTIES, setPro));
    }
    if (unsetPro.length() > 0) {
      unsetSql.append(String.format(ALTER_TABLE, tableName)).append(String.format(UNSET_PROPERTIES, unsetPro));
    }
    if (setSql.length() > 0) {
      DDLInfo ddlInfo =
          DDLInfo.of(tableIdentifier, setSql.toString(), DDLType.UPDATE_PROPERTIES.name(), commitTime);
      insert(ddlInfo);
    }
    if (unsetSql.length() > 0) {
      DDLInfo ddlInfo =
          DDLInfo.of(tableIdentifier, unsetSql.toString(), DDLType.UPDATE_PROPERTIES.name(), commitTime);
      insert(ddlInfo);
    }
  }

  public List<DDLInfo> getDDL(TableIdentifier identifier) {
    ArcticCatalog ac = CatalogUtil.getArcticCatalog(identifier.getCatalog());
    if (CatalogUtil.isIcebergCatalog(identifier.getCatalog())) {
      return getNativeIcebergDDL(ac, identifier);
    }
    try (SqlSession sqlSession = getSqlSession(true)) {
      DDLRecordMapper ddlRecordMapper = getMapper(sqlSession, DDLRecordMapper.class);
      return ddlRecordMapper.getDDLInfos(identifier);
    }
  }

  public List<DDLInfo> getNativeIcebergDDL(ArcticCatalog catalog, TableIdentifier identifier) {
    List<DDLInfo> result = new ArrayList<>();
    ArcticTable arcticTable = catalog.loadTable(com.netease.arctic.table.TableIdentifier.of(identifier.getCatalog(),
        identifier.getDatabase(), identifier.getTableName()));
    Table table = arcticTable.asUnkeyedTable();
    List<HistoryEntry> snapshotLog = ((HasTableOperations) table).operations().current().snapshotLog();
    List<org.apache.iceberg.TableMetadata.MetadataLogEntry> metadataLogEntries =
        ((HasTableOperations) table).operations().current().previousFiles();
    Set<Long> time = new HashSet<>();
    snapshotLog.forEach(e -> time.add(e.timestampMillis()));
    for (int i = 1; i < metadataLogEntries.size(); i++) {
      org.apache.iceberg.TableMetadata.MetadataLogEntry e = metadataLogEntries.get(i);
      if (!time.contains(e.timestampMillis())) {
        org.apache.iceberg.TableMetadata
            oldTableMetadata = TableMetadataParser.read(table.io(), metadataLogEntries.get(i - 1).file());
        org.apache.iceberg.TableMetadata
            newTableMetadata = TableMetadataParser.read(table.io(), metadataLogEntries.get(i).file());
        genNativeIcebergDDL(arcticTable, oldTableMetadata, newTableMetadata, result);
      }
    }
    if (metadataLogEntries.size() > 0) {
      org.apache.iceberg.TableMetadata oldTableMetadata = TableMetadataParser.read(
          table.io(),
          metadataLogEntries.get(metadataLogEntries.size() - 1).file());
      org.apache.iceberg.TableMetadata newTableMetadata = ((HasTableOperations) table).operations().current();
      genNativeIcebergDDL(arcticTable, oldTableMetadata, newTableMetadata, result);
    }
    return result;
  }

  public void genNativeIcebergDDL(
      ArcticTable arcticTable, org.apache.iceberg.TableMetadata oldTableMetadata,
      org.apache.iceberg.TableMetadata newTableMetadata, List<DDLInfo> result) {
    if (oldTableMetadata.currentSchemaId() == newTableMetadata.currentSchemaId()) {
      String ddl =
          compareProperties(arcticTable.id().toString(), oldTableMetadata.properties(), newTableMetadata.properties());
      if (!ddl.isEmpty()) {
        result.add(DDLInfo.of(arcticTable.id().buildTableIdentifier(), ddl, DDLType.UPDATE_PROPERTIES.name(),
            newTableMetadata.lastUpdatedMillis()));
      }
    } else {
      String ddl = DDLTracerService.compareSchema(
          arcticTable.id().toString(),
          oldTableMetadata.schema(),
          newTableMetadata.schema());
      if (!ddl.isEmpty()) {
        result.add(DDLInfo.of(arcticTable.id().buildTableIdentifier(), ddl, DDLType.UPDATE_SCHEMA.name(),
            newTableMetadata.lastUpdatedMillis()));
      }
    }
  }

  public void dropTableData(TableIdentifier identifier) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      DDLRecordMapper ddlRecordMapper = getMapper(sqlSession, DDLRecordMapper.class);
      ddlRecordMapper.dropTableData(identifier);
    }
  }

  public Integer getCurrentSchemaId(TableIdentifier identifier) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableMetadataMapper tableMetadataMapper = getMapper(sqlSession, TableMetadataMapper.class);
      return tableMetadataMapper.getTableSchemaId(new com.netease.arctic.table.TableIdentifier(identifier));
    }
  }

  public void setCurrentSchemaId(TableIdentifier identifier, Integer schemaId) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableMetadataMapper tableMetadataMapper = getMapper(sqlSession, TableMetadataMapper.class);
      tableMetadataMapper.updateTableSchemaId(new com.netease.arctic.table.TableIdentifier(identifier), schemaId);
    }
  }

  public enum DDLType {
    UPDATE_SCHEMA,
    UPDATE_PROPERTIES
  }

  public void insert(DDLInfo ddlInfo) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      DDLRecordMapper ddlRecordMapper = getMapper(sqlSession, DDLRecordMapper.class);
      ddlRecordMapper.insert(ddlInfo);
    }
  }

  public static String compareProperties(String tableName, Map<String, String> before, Map<String, String> after) {
    StringBuilder setSql = new StringBuilder();
    StringBuilder unsetSql = new StringBuilder();
    StringBuilder unsetPro = new StringBuilder();
    int c = 0;
    for (String oldPro : before.keySet()) {
      if (TableProperties.WRITE_PROTECTED_PROPERTIES.contains(oldPro)) {
        continue;
      }
      if (!after.containsKey(oldPro)) {
        if (c > 0) {
          unsetPro.append(",").append("\n");
        }
        unsetPro.append(String.format("'%s'", oldPro));
        c++;
      }
    }
    StringBuilder setPro = new StringBuilder();
    int c1 = 0;
    for (String newPro : after.keySet()) {
      if (TableProperties.WRITE_PROTECTED_PROPERTIES.contains(newPro)) {
        continue;
      }
      if (!after.get(newPro).equals(before.get(newPro))) {
        if (c1 > 0) {
          setPro.append(",").append("\n");
        }
        setPro.append(String.format("'%s'='%s'", newPro, after.get(newPro)));
        c1++;
      }
    }
    if (setPro.length() > 0) {
      setSql.append(String.format(ALTER_TABLE, tableName)).append(String.format(SET_PROPERTIES, setPro));
    }
    if (unsetPro.length() > 0) {
      unsetSql.append(String.format(ALTER_TABLE, tableName)).append(String.format(UNSET_PROPERTIES, unsetPro));
    }
    return setSql.append(unsetSql).toString();
  }

  public static String compareSchema(String tableName, Schema before, Schema after) {
    StringBuilder rs = new StringBuilder();
    if (before == null) {
      return "";
    }

    LinkedList<String> sortedBefore =
        before.columns().stream().map(Types.NestedField::name).collect(Collectors.toCollection(LinkedList::new));
    for (int i = 0; i < before.columns().size(); i++) {
      Types.NestedField field = before.columns().get(i);
      StringBuilder sb = new StringBuilder();
      if (after.findField(field.fieldId()) == null) {
        // drop col
        sb.append(String.format(ALTER_TABLE, tableName));
        sb.append(String.format(DROP_COLUMNS, field.name()));
        sortedBefore.remove(field.name());
      }
      if (sb.length() > 0) {
        rs.append(sb).append(";");
        if (i < before.columns().size() - 1) {
          rs.append("\n");
        }
      }
    }

    int maxIndex = 0;
    for (int i = 0; i < before.columns().size(); i++) {
      int index = after.columns().indexOf(before.columns().get(i));
      maxIndex = Math.max(index, maxIndex);
    }
    for (int i = 0; i < after.columns().size(); i++) {
      Types.NestedField field = after.columns().get(i);
      StringBuilder sb = new StringBuilder();
      if (before.findField(field.fieldId()) == null) {
        // add col
        sb.append(String.format(ALTER_TABLE, tableName));
        sb.append(ADD_COLUMN);
        sb.append(field.name()).append(" ");
        sb.append(field.type().toString()).append(" ");
        if (field.doc() != null) {
          sb.append(String.format(DOC, field.doc()));
        }
        sortedBefore.add(i, field.name());
        if (i == 0) {
          sb.append(" FIRST");
          sortedBefore.removeLast();
          sortedBefore.addFirst(field.name());
        } else if (i < maxIndex) {
          sb.append(" AFTER ").append(sortedBefore.get(i - 1));
          sortedBefore.removeLast();
          sortedBefore.add(i, field.name());
        }
      } else if (!before.findField(field.fieldId()).equals(field)) {
        sb.append(String.format(ALTER_TABLE, tableName));
        //alter col
        Types.NestedField oldField = before.findField(field.fieldId());
        //rename
        if (!oldField.name().equals(field.name())) {
          sb.append(String.format(RENAME_COLUMN, oldField.name(), field.name()));
        } else {
          sb.append(String.format(ALTER_COLUMN, oldField.name()));

          if (!oldField.type().equals(field.type())) {
            if ((oldField.type() instanceof Types.MapType) && (field.type() instanceof Types.MapType)) {
              sb = new StringBuilder();
              sb.append(String.format(ALTER_TABLE, tableName)).append(compareFieldType(oldField, field));
            } else {
              sb.append(String.format(TYPE, field.type().toString()));
            }
          }

          if (!Objects.equals(oldField.doc(), field.doc())) {
            if (field.doc() != null) {
              sb.append(String.format(DOC, field.doc()));
            }
          }

          if (oldField.isOptional() != field.isOptional()) {
            if (field.isOptional()) {
              sb.append(IS_OPTIONAL);
            } else {
              sb.append(NOT_OPTIONAL);
            }
          }
        }
      } else if (i != before.columns().indexOf(field) && i != sortedBefore.indexOf(field.name())) {
        sb.append(String.format(ALTER_TABLE, tableName));
        if (i == 0) {
          sb.append(String.format(MOVE_FIRST, field.name()));
          sortedBefore.remove(field.name());
          sortedBefore.addFirst(field.name());
        } else {
          sb.append(String.format(MOVE_AFTER_COLUMN, field.name(), after.columns().get(i - 1).name()));
          sortedBefore.remove(field.name());
          sortedBefore.add(i, field.name());
        }
      }
      if (sb.length() > 0) {
        rs.append(sb).append(";");
        if (i < after.columns().size() - 1) {
          rs.append("\n");
        }
      }
    }
    return rs.toString();
  }

  private static StringBuilder compareFieldType(Types.NestedField oldField, Types.NestedField field) {
    StringBuilder sb = new StringBuilder();

    Types.StructType oldValueType = oldField.type().asMapType().valueType().asStructType();
    List<Types.NestedField> oldValueFields = oldValueType.fields();
    Types.StructType valueType = field.type().asMapType().valueType().asStructType();
    List<Types.NestedField> valueFields = valueType.fields();
    for (Types.NestedField old : oldValueFields) {
      if (valueType.field(old.fieldId()) == null) {
        //drop column
        sb.append(String.format(DROP_COLUMNS, oldField.name() + DOT + old.name()));
      }
    }
    for (Types.NestedField newF : valueFields) {
      if (oldValueType.field(newF.fieldId()) == null) {
        //add column
        sb.append(ADD_COLUMN).append(field.name()).append(DOT).append(newF.name()).append(" ").append(newF.type());
      }
    }
    return sb;
  }

  public static class DDLSyncTask {

    public void doTask() {
      LOG.info("start execute syncDDl");
      List<TableMetadata> tableMetadata = ServiceContainer.getMetaService().listTables();
      tableMetadata.forEach(meta -> {
        try {
          if (meta.getTableIdentifier() == null) {
            return;
          }
          TableIdentifier tableIdentifier = new TableIdentifier();
          tableIdentifier.catalog = meta.getTableIdentifier().getCatalog();
          tableIdentifier.database = meta.getTableIdentifier().getDatabase();
          tableIdentifier.tableName = meta.getTableIdentifier().getTableName();
          ArcticCatalog catalog =
              CatalogLoader.load(ServiceContainer.getTableMetastoreHandler(), tableIdentifier.getCatalog());
          com.netease.arctic.table.TableIdentifier tmp = com.netease.arctic.table.TableIdentifier.of(
              tableIdentifier.getCatalog(),
              tableIdentifier.getDatabase(),
              tableIdentifier.getTableName());
          ArcticTable arcticTable = catalog.loadTable(tmp);
          syncDDl(arcticTable);
          syncProperties(meta, arcticTable);
        } catch (Exception e) {
          LOG.error("table {} DDLSyncTask error", meta.getTableIdentifier().toString(), e);
        }
      });
    }

    public void syncDDl(ArcticTable arcticTable) {
      Table table = arcticTable.isKeyedTable() ? arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
      table.refresh();
      Integer amsSchemaId = ServiceContainer.getDdlTracerService().getCurrentSchemaId(arcticTable.id()
          .buildTableIdentifier());
      int tableSchemaId = table.schema().schemaId();
      if (amsSchemaId != null && amsSchemaId == tableSchemaId) {
        return;
      }
      Map<Integer, Schema> allSchemas = table.schemas();
      Schema cacheSchema = amsSchemaId == null ? null : allSchemas.get(amsSchemaId);
      Long commitTime = System.currentTimeMillis();
      StringBuilder sql = new StringBuilder();
      sql.append(compareSchema(arcticTable.id().toString(), cacheSchema, table.schema()));
      if (sql.length() > 0) {
        DDLInfo ddlInfo =
            DDLInfo.of(
                arcticTable.id().buildTableIdentifier(),
                sql.toString(),
                DDLType.UPDATE_SCHEMA.name(),
                commitTime);
        ServiceContainer.getDdlTracerService().insert(ddlInfo);
      }
      ServiceContainer.getDdlTracerService().setCurrentSchemaId(arcticTable.id().buildTableIdentifier(), tableSchemaId);
    }

    public void syncProperties(TableMetadata tableMetadata, ArcticTable arcticTable) {
      Table table = arcticTable.isKeyedTable() ? arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
      PropertiesUtil.removeHiddenProperties(tableMetadata.getProperties(), ServerTableProperties.HIDDEN_INTERNAL);
      Map<String, String> icebergProperties = Maps.newHashMap(table.properties());
      PropertiesUtil.removeHiddenProperties(icebergProperties, ServerTableProperties.HIDDEN_INTERNAL);
      ServiceContainer.getDdlTracerService()
          .commitProperties(arcticTable.id().buildTableIdentifier(), tableMetadata.getProperties(),
              icebergProperties);
      ServiceContainer.getMetaService().updateTableProperties(arcticTable.id(), icebergProperties);
    }
  }
}
