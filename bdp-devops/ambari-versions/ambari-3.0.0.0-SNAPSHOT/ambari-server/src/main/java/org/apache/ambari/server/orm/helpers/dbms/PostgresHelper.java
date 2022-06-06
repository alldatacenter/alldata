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

import org.apache.ambari.server.orm.DBAccessor;
import org.eclipse.persistence.platform.database.DatabasePlatform;

public class PostgresHelper extends GenericDbmsHelper {
  public PostgresHelper(DatabasePlatform databasePlatform) {
    super(databasePlatform);
  }

  @Override
  public boolean supportsColumnTypeChange() {
    return true;
  }

  @Override
  public StringBuilder writeColumnRenameString(StringBuilder builder, String oldName, DBAccessor.DBColumnInfo newColumnInfo) {
    builder.append(" RENAME COLUMN ").append(oldName).append(" TO ").append(newColumnInfo.getName());
    return builder;
  }

  @Override
  public StringBuilder writeColumnModifyString(StringBuilder builder, DBAccessor.DBColumnInfo columnInfo) {
    builder.append(" ALTER COLUMN ").append(columnInfo.getName()).append(" TYPE ");
    writeColumnType(builder, columnInfo);
    return builder;
  }

  /**
   {@inheritDoc}
   */
  @Override
  public String getCopyColumnToAnotherTableStatement(String sourceTable, String sourceColumnName,
         String sourceIDColumnName, String targetTable, String targetColumnName, String targetIDColumnName) {

    return String.format("UPDATE %1$s AS a SET %3$s = b.%4$s FROM %2$s AS b WHERE a.%5$s = b.%6$s",
      targetTable, sourceTable, targetColumnName, sourceColumnName, targetIDColumnName, sourceIDColumnName);
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
    return String.format("UPDATE %1$s AS a SET %3$s = b.%4$s FROM %2$s AS b WHERE a.%5$s = b.%8$s AND a.%6$s = b.%9$s AND a.%7$s = b.%10$s AND b.%11$s = '%12$s'",
        targetTable, sourceTable, targetColumnName, sourceColumnName, targetIDColumnName1, targetIDColumnName2, targetIDColumnName3,
        sourceIDColumnName1, sourceIDColumnName2, sourceIDColumnName3, sourceConditionFieldName, condition);
  }

  @Override
  public StringBuilder writeSetNullableString(StringBuilder builder,
      String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable) {
    builder.append(" ALTER COLUMN ").append(columnInfo.getName());
    String nullStatement = nullable ? " DROP NOT NULL" : " SET NOT NULL";
    builder.append(nullStatement);
    return builder;
  }

  @Override
  public String writeGetTableConstraints(String databaseName, String tableName) {
    // pg_class: http://www.postgresql.org/docs/9.4/static/catalog-pg-class.html
    // pg_constraint: http://www.postgresql.org/docs/9.4/static/catalog-pg-constraint.html
    // pg_namespace: http://www.postgresql.org/docs/9.4/static/catalog-pg-namespace.html
    StringBuilder statement = new StringBuilder()
      .append("SELECT ")
        .append("c.conname as CONSTRAINT_NAME,")
        .append("c.contype as CONSTRAINT_TYPE ")
      .append("FROM pg_catalog.pg_constraint as c ")
      .append("JOIN pg_catalog.pg_namespace as namespace ")
        .append("on namespace.oid = c.connamespace ")
      .append("JOIN pg_catalog.pg_class as class ")
        .append("on class.oid = c.conrelid ")
      .append("where (namespace.nspname='").append(databaseName).append("' or namespace.nspname='public')")
      .append("and class.relname='").append(tableName).append("'");

    return statement.toString();
  }

  @Override
  public  StringBuilder writeDropPrimaryKeyStatement(StringBuilder builder, String constraintName, boolean cascade){
      return builder.append("DROP CONSTRAINT ").append(constraintName).append(cascade ? " CASCADE" : "");
  }
}
