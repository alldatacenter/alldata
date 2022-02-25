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

public class OracleHelper extends GenericDbmsHelper {
  public OracleHelper(DatabasePlatform databasePlatform) {
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
    builder.append(" MODIFY ").append(columnInfo.getName()).append(" ");
    writeColumnType(builder, columnInfo);
    return builder;
  }

  @Override
  public StringBuilder writeSetNullableString(StringBuilder builder,
      String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable) {
    builder.append(" MODIFY ").append(columnInfo.getName());
    String nullStatement = nullable ? " NULL" : " NOT NULL";
    builder.append(nullStatement);
    return builder;
  }

  @Override
  public String writeGetTableConstraints(String databaseName, String tableName) {
    StringBuilder statement = new StringBuilder()
                                .append("SELECT CONSTRAINT_NAME as constraint_name, CONSTRAINT_TYPE as constraint_type ")
                                .append("FROM USER_CONSTRAINTS ")
                                .append("WHERE ")
                                .append("USER_CONSTRAINTS.TABLE_NAME='").append(tableName.toUpperCase()).append("'");
    return statement.toString();
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Oracle supports the format:
   *
   * <pre>
   * ALTER TABLE foo ADD COLUMN bar varchar2(32) DEFAULT 'baz' NOT NULL
   * </pre>
   *
   * This syntax doesn't allow contraints added after the {@code NULL}
   * constraint.
   */
  @Override
  public boolean isConstraintSupportedAfterNullability() {
    return false;
  }

  /**
   {@inheritDoc}
   */
  @Override
  public String getCopyColumnToAnotherTableStatement(String sourceTable, String sourceColumnName,
         String sourceIDColumnName, String targetTable, String targetColumnName, String targetIDColumnName) {

    // sub-query should return only one value, ROWNUM is safe-guard for this
    return String.format("UPDATE %1$s a SET (a.%3$s) = (SELECT b.%4$s FROM %2$s b WHERE b.%6$s = a.%5$s and ROWNUM < 2)",
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
    return String.format("UPDATE %1$s a SET (a.%3$s) = (SELECT b.%4$s FROM %2$s b WHERE b.%8$s = a.%5$s AND b.%9$s = a.%6$s AND b.%10$s = a.%7$s AND b.%11$s = '%12$s' AND ROWNUM < 2)",
        targetTable, sourceTable, targetColumnName, sourceColumnName, targetIDColumnName1, targetIDColumnName2, targetIDColumnName3,
        sourceIDColumnName1, sourceIDColumnName2, sourceIDColumnName3, sourceConditionFieldName, condition);
  }
}
