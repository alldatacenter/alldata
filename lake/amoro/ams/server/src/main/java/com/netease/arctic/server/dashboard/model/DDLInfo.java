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

package com.netease.arctic.server.dashboard.model;

import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.Schema;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.types.Types;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DDLInfo {
  private TableIdentifier tableIdentifier;
  private String ddl;
  private String ddlType;
  private Long commitTime;

  public static DDLInfo of(
      TableIdentifier tableIdentifier,
      String ddl,
      String ddlType,
      Long commitTime) {
    DDLInfo ddlInfo = new DDLInfo();
    ddlInfo.setTableIdentifier(tableIdentifier);
    ddlInfo.setDdl(ddl);
    ddlInfo.setCommitTime(commitTime);
    ddlInfo.setDdlType(ddlType);
    return ddlInfo;
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public String getDdl() {
    return ddl;
  }

  public void setDdl(String ddl) {
    this.ddl = ddl;
  }

  public String getDdlType() {
    return ddlType;
  }

  public void setDdlType(String ddlType) {
    this.ddlType = ddlType;
  }

  public Long getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(Long commitTime) {
    this.commitTime = commitTime;
  }

  public static class Generator {
    private static final String DOT = ".";
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
    private TableMetadata oldMeta;
    private TableMetadata newMeta;
    private TableIdentifier tableIdentifier;

    public Generator oldMeta(TableMetadata oldMeta) {
      this.oldMeta = oldMeta;
      return this;
    }

    public Generator newMeta(TableMetadata newMeta) {
      this.newMeta = newMeta;
      return this;
    }

    public Generator tableIdentify(TableIdentifier tableIdentifier) {
      this.tableIdentifier = tableIdentifier;
      return this;
    }

    public List<DDLInfo> generate() {
      if (newMeta == null || tableIdentifier == null) {
        throw new RuntimeException("tableIdentifier and newMeta must not be null");
      }
      List<DDLInfo> result = new ArrayList<>();
      if (oldMeta == null) {
        return result;
      }
      genNativeIcebergDDL(tableIdentifier, oldMeta, newMeta, result);
      return result;
    }

    private void genNativeIcebergDDL(
        TableIdentifier identifier, org.apache.iceberg.TableMetadata oldTableMetadata,
        org.apache.iceberg.TableMetadata newTableMetadata, List<DDLInfo> result) {
      if (oldTableMetadata.currentSchemaId() == newTableMetadata.currentSchemaId()) {
        String ddl =
            compareProperties(identifier.toString(), oldTableMetadata.properties(), newTableMetadata.properties());
        if (!ddl.isEmpty()) {
          result.add(DDLInfo.of(identifier, ddl, DDLType.UPDATE_PROPERTIES.name(),
              newTableMetadata.lastUpdatedMillis()));
        }
      } else {
        String ddl = compareSchema(
            identifier.toString(),
            oldTableMetadata.schema(),
            newTableMetadata.schema());
        if (!ddl.isEmpty()) {
          result.add(DDLInfo.of(identifier, ddl, DDLType.UPDATE_SCHEMA.name(),
              newTableMetadata.lastUpdatedMillis()));
        }
      }
    }

    private String compareProperties(String tableName, Map<String, String> before, Map<String, String> after) {
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

    private String compareSchema(String tableName, Schema before, Schema after) {
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

    private StringBuilder compareFieldType(Types.NestedField oldField, Types.NestedField field) {
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

    public enum DDLType {
      UPDATE_SCHEMA,
      UPDATE_PROPERTIES
    }
  }
}
