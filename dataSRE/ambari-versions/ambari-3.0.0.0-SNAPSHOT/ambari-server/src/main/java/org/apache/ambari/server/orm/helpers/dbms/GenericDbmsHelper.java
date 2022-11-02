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

package org.apache.ambari.server.orm.helpers.dbms;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.internal.databaseaccess.FieldTypeDefinition;
import org.eclipse.persistence.internal.databaseaccess.Platform;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.platform.database.DatabasePlatform;
import org.eclipse.persistence.tools.schemaframework.FieldDefinition;
import org.eclipse.persistence.tools.schemaframework.ForeignKeyConstraint;
import org.eclipse.persistence.tools.schemaframework.TableDefinition;
import org.eclipse.persistence.tools.schemaframework.UniqueKeyConstraint;

public class GenericDbmsHelper implements DbmsHelper {
  protected final DatabasePlatform databasePlatform;

  public GenericDbmsHelper(DatabasePlatform databasePlatform) {
    this.databasePlatform = databasePlatform;
  }

  @Override
  public boolean supportsColumnTypeChange() {
    return false;
  }

  @Override
  public String quoteObjectName(String name) {
    return "\"" + name + "\"";
  }

  @Override
  public String getRenameColumnStatement(String tableName, String oldName, DBAccessor.DBColumnInfo columnInfo) {
    StringBuilder stringBuilder = new StringBuilder();

    writeAlterTableClause(stringBuilder, tableName);
    writeColumnRenameString(stringBuilder, oldName, columnInfo);

    return stringBuilder.toString();
  }

  @Override
  public String getAlterColumnStatement(String tableName, DBAccessor.DBColumnInfo columnInfo) {
    StringBuilder stringBuilder = new StringBuilder();
    writeAlterTableClause(stringBuilder, tableName);
    writeColumnModifyString(stringBuilder, columnInfo);

    return stringBuilder.toString();
  }

  @Override
  public String getSetNullableStatement(String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable) {
    StringBuilder stringBuilder = new StringBuilder();
    writeAlterTableClause(stringBuilder, tableName);
    writeSetNullableString(stringBuilder, tableName, columnInfo, nullable);
    return stringBuilder.toString();
  }

  /**
   {@inheritDoc}
   */
  @Override
  public String getCopyColumnToAnotherTableStatement(String sourceTable, String sourceColumnName, String sourceIDColumnName, String targetTable, String targetColumnName, String targetIDColumnName) {
    throw new UnsupportedOperationException("Column copy is not supported for generic DB");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCopyColumnToAnotherTableStatement(String sourceTable, String sourceColumnName,
                                                     String sourceIDColumnName1, String sourceIDColumnName2,
                                                     String sourceIDColumnName3,
                                                     String targetTable, String targetColumnName,
                                                     String targetIDColumnName1, String targetIDColumnName2,
                                                     String targetIDColumnName3,
                                                     String sourceConditionFieldName, String condition) {
    throw new UnsupportedOperationException("Column copy is not supported for generic DB");
  }

  public StringBuilder writeAlterTableClause(StringBuilder builder, String tableName) {
    builder.append("ALTER TABLE ").append(tableName).append(" ");
    return builder;
  }

  public StringBuilder writeColumnModifyString(StringBuilder builder, DBAccessor.DBColumnInfo columnInfo) {
    throw new UnsupportedOperationException("Column type modification not supported for generic DB");
  }

  public StringBuilder writeColumnRenameString(StringBuilder builder, String oldName, DBAccessor.DBColumnInfo newColumnInfo) {
    throw new UnsupportedOperationException("Column rename not supported for generic DB");
  }

  public StringBuilder writeColumnType(StringBuilder builder, DBAccessor.DBColumnInfo columnInfo) {
    FieldTypeDefinition fieldType = columnInfo.getDbType();

    if (fieldType == null) {
      fieldType = databasePlatform.getFieldTypeDefinition(columnInfo.getType());
    }

    if (fieldType == null) {
      throw new IllegalArgumentException("Unable to convert data type");
    }

    FieldDefinition definition = convertToFieldDefinition(columnInfo);

    StringWriter writer = new StringWriter();

    try {
      databasePlatform.printFieldTypeSize(writer, definition, fieldType, false); //Ambari doesn't use identity fields
    } catch (IOException ignored) {
      // no writing to file
    }

    builder.append(writer);

    return builder;
  }

  public String writeGetTableConstraints(String databaseName, String tableName){
    throw new UnsupportedOperationException("List of table constraints is not supported for generic DB");
  }

  public StringBuilder writeAddPrimaryKeyString(StringBuilder builder, String constraintName, String... columnName){
    builder.append("ADD CONSTRAINT ").append(constraintName).append(" PRIMARY KEY (").append(StringUtils.join(columnName, ",")).append(")");
    return builder;
  }

  public StringBuilder writeSetNullableString(StringBuilder builder,
      String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable) {
    throw new UnsupportedOperationException(
        "Column nullable modification not supported for generic DB");
  }

  public StringBuilder writeDropTableColumnStatement(StringBuilder builder, String columnName){
    builder.append("DROP COLUMN ").append(columnName);
    return builder;
  }

  /**
   * @param builder String Builder passed by reference
   * @param constraintName Constraint Name used by Postgres
   * @param cascade In postgres, can perform a CASCADE delete. In the other DB flavors, this is ignored.
   * @return Return the String Builder
   */
  public  StringBuilder writeDropPrimaryKeyStatement(StringBuilder builder, String constraintName, boolean cascade){
      // constraintName required only for postgres
      return builder.append("DROP PRIMARY KEY");
  }

  @Override
  public String getDropPrimaryKeyStatement(String tableName, String constraintName, boolean cascade){
      StringBuilder builder = writeAlterTableClause(new StringBuilder(), tableName);
      return writeDropPrimaryKeyStatement(builder, constraintName, cascade).toString();
  }

  /**
   * get create table statement
   * @param tableName
   * @param columns
   * @param primaryKeyColumns
   * @return
   */
  @Override
  public String getCreateTableStatement(String tableName,
                                        List<DBAccessor.DBColumnInfo> columns,
                                        List<String> primaryKeyColumns) {
    Writer stringWriter = new StringWriter();
    writeCreateTableStatement(stringWriter, tableName, columns, primaryKeyColumns);
    return stringWriter.toString();
  }


  /**
   * Write create table statement to writer
   * TODO default Value of column not supported
   */
  public Writer writeCreateTableStatement(Writer writer, String tableName,
                                          List<DBAccessor.DBColumnInfo> columns,
                                          List<String> primaryKeyColumns) {
    //TODO validateNames(String tableName, List<DBAccessor.DBColumnInfo> columns)
    //TODO validatePKNames(List<DBAccessor.DBColumnInfo> columns, String... primaryKeyColumns)

    TableDefinition tableDefinition = new TableDefinition();
    tableDefinition.setName(tableName);
    for (DBAccessor.DBColumnInfo columnInfo : columns) {
      //TODO case-sensitive for now
      int length = columnInfo.getLength() != null ? columnInfo.getLength() : 0;

      if (primaryKeyColumns.contains(columnInfo.getName())) {
        tableDefinition.addIdentityField(columnInfo.getName(), columnInfo.getType(), length);
      } else {
        FieldDefinition fieldDefinition = convertToFieldDefinition(columnInfo);
        tableDefinition.addField(fieldDefinition);
      }
    }

    //TODO possibly move code to avoid unnecessary dependencies and allow extension
    tableDefinition.buildCreationWriter(createStubAbstractSessionFromPlatform(databasePlatform), writer);

    return writer;
  }

  public FieldDefinition convertToFieldDefinition(DBAccessor.DBColumnInfo columnInfo) {
    int length = columnInfo.getLength() != null ? columnInfo.getLength() : 0;
    FieldDefinition fieldDefinition = new FieldDefinition(columnInfo.getName(), columnInfo.getType(), length);
    fieldDefinition.setShouldAllowNull(columnInfo.isNullable());

    if (null != columnInfo.getDefaultValue() && isConstraintSupportedAfterNullability()) {
      fieldDefinition.setConstraint("DEFAULT " + escapeParameter(columnInfo.getDefaultValue()));
    }

    return fieldDefinition;
  }

  @Override
  public String getDropUniqueConstraintStatement(String tableName, String constraintName){

    UniqueKeyConstraint uniqueKeyConstraint = new UniqueKeyConstraint();
    uniqueKeyConstraint.setName(constraintName);

    Writer writer = new StringWriter();
    TableDefinition tableDefinition = new TableDefinition();
    tableDefinition.setName(tableName);
    tableDefinition.buildUniqueConstraintDeletionWriter(createStubAbstractSessionFromPlatform(databasePlatform),
                                                         uniqueKeyConstraint, writer);

    return writer.toString();
  }

  @Override
  public String getTableConstraintsStatement(String databaseName, String tablename){
    return writeGetTableConstraints(databaseName, tablename);
  }

  /**
   * get create index statement
   * @param indexName
   * @param tableName
   * @param columnNames
   * @return
   */
  @Override
  public String getCreateIndexStatement(String indexName, String tableName,
                                        String... columnNames) {
    return getCreateIndexStatement(indexName, tableName, false, columnNames);
  }

  /**
   * get create index statement
   * @param indexName The name of the index to be created
   * @param tableName The database table the index to be created on
   * @param columnNames The columns included into the index
   * @param  isUnique Specifies whether unique index is to be created.
   * @return The sql statement for creating the index
   */
  @Override
  public String getCreateIndexStatement(String indexName, String tableName, boolean isUnique,
                                        String... columnNames) {
    //TODO validateColumnNames()
    String createIndex = databasePlatform.buildCreateIndex(tableName, indexName, "", isUnique, columnNames);
    return createIndex;
  }

  /**
   * Generating update SQL statement for {@link DBAccessor#executePreparedUpdate}
   *
   * @param tableName name of the table
   * @param setColumnName column name, value of which need to be set
   * @param conditionColumnName column name for the condition
   * @return
   */
  @Override
  public String getColumnUpdateStatementWhereColumnIsNull(String tableName, String setColumnName, String conditionColumnName){
    return "UPDATE " + tableName + " SET " + setColumnName + "=? WHERE " + conditionColumnName + " IS NULL";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDropIndexStatement(String indexName, String tableName) {
    String dropIndex = databasePlatform.buildDropIndex(tableName, indexName);
    return dropIndex;
  }

  @Override
  public String getAddUniqueConstraintStatement(String tableName, String constraintName, String... columnNames){
    UniqueKeyConstraint uniqueKeyConstraint = new UniqueKeyConstraint();
    uniqueKeyConstraint.setName(constraintName);
    for (String columnName: columnNames){
      uniqueKeyConstraint.addSourceField(columnName);
    }

    TableDefinition tableDefinition = new TableDefinition();
    tableDefinition.setName(tableName);
    Writer writer = new StringWriter();
    tableDefinition.buildUniqueConstraintCreationWriter(createStubAbstractSessionFromPlatform(databasePlatform),
                                                               uniqueKeyConstraint, writer);
    return writer.toString();
  }

  @Override
  public String getAddPrimaryKeyConstraintStatement(String tableName, String constraintName, String... columnName){
    StringBuilder builder = writeAlterTableClause(new StringBuilder(), tableName);
    builder = writeAddPrimaryKeyString(builder, constraintName, columnName);
    return builder.toString();
  }

  @Override
  public String getAddForeignKeyStatement(String tableName, String constraintName,
                                          List<String> keyColumns,
                                          String referenceTableName,
                                          List<String> referenceColumns,
                                          boolean shouldCascadeOnDelete) {
    ForeignKeyConstraint foreignKeyConstraint = new ForeignKeyConstraint();
    foreignKeyConstraint.setName(constraintName);
    foreignKeyConstraint.setTargetTable(referenceTableName);
    foreignKeyConstraint.setSourceFields(keyColumns);
    foreignKeyConstraint.setTargetFields(referenceColumns);
    foreignKeyConstraint.setShouldCascadeOnDelete(shouldCascadeOnDelete);

    TableDefinition tableDefinition = new TableDefinition();
    tableDefinition.setName(tableName);

    Writer writer = new StringWriter();
    tableDefinition.buildConstraintCreationWriter(createStubAbstractSessionFromPlatform(databasePlatform),
        foreignKeyConstraint, writer);

    return writer.toString();

  }

  @Override
  public String getAddColumnStatement(String tableName, DBAccessor.DBColumnInfo columnInfo) {
    Writer writer = new StringWriter();

    TableDefinition tableDefinition = new TableDefinition();
    tableDefinition.setName(tableName);
    tableDefinition.buildAddFieldWriter(createStubAbstractSessionFromPlatform(databasePlatform),
                                               convertToFieldDefinition(columnInfo), writer);

    return writer.toString();
  }

  @Override
  public String getDropTableColumnStatement(String tableName, String columnName){
    StringBuilder builder = writeAlterTableClause(new StringBuilder(), tableName);
    return writeDropTableColumnStatement(builder, columnName).toString();
  }


  @Override
  public String getRenameColumnStatement(String tableName, String oldColumnName,
                                         String newColumnName) {

    throw new UnsupportedOperationException("Rename operation not supported.");
  }

  @Override
  public String getDropTableStatement(String tableName) {
    Writer writer = new StringWriter();

    TableDefinition tableDefinition = new TableDefinition();
    tableDefinition.setName(tableName);
    tableDefinition.buildDeletionWriter
        (createStubAbstractSessionFromPlatform(databasePlatform), writer);

    return writer.toString();
  }

  @Override
  public String getDropFKConstraintStatement(String tableName, String constraintName) {
    Writer writer = new StringWriter();

    ForeignKeyConstraint foreignKeyConstraint = new ForeignKeyConstraint();
    foreignKeyConstraint.setName(constraintName);
    foreignKeyConstraint.setTargetTable(tableName);

    TableDefinition tableDefinition = new TableDefinition();
    tableDefinition.setName(tableName);
    tableDefinition.buildConstraintDeletionWriter(
        createStubAbstractSessionFromPlatform(databasePlatform),
        foreignKeyConstraint, writer);

    return writer.toString();
  }

  @Override
  public String getDropSequenceStatement(String sequenceName) {
    StringWriter writer = new StringWriter();
    String defaultStmt = String.format("DROP sequence %s", sequenceName);

    try {
      Writer w = databasePlatform.buildSequenceObjectDeletionWriter(writer, sequenceName);
      return w != null ? w.toString() : defaultStmt;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return defaultStmt;
  }


  public AbstractSession createStubAbstractSessionFromPlatform
      (final DatabasePlatform databasePlatform) {
    return new AbstractSession() {
      @Override
      public Platform getDatasourcePlatform() {
        return databasePlatform;
      }

      @Override
      public DatabasePlatform getPlatform() {
        return databasePlatform;
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isConstraintSupportedAfterNullability() {
    return true;
  }

  /**
   * Gets an escaped version of the specified value suitable for including as a
   * parameter when building statements.
   *
   * @param value
   *          the value to escape
   * @return the escaped value
   */
  private String escapeParameter(Object value) {
    return DBAccessorImpl.escapeParameter(value, databasePlatform);
  }
}
