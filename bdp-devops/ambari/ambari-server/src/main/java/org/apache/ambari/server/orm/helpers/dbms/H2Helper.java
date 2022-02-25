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

public class H2Helper extends GenericDbmsHelper {
  public H2Helper(DatabasePlatform databasePlatform) {
    super(databasePlatform);
  }

  @Override
  public boolean supportsColumnTypeChange() {
    return false; //type change is dramatically limited to varchar length increase only, almost useless
  }

  //+
  @Override
  public String getRenameColumnStatement(String tableName, String oldName, DBAccessor.DBColumnInfo columnInfo) {
    StringBuilder builder = new StringBuilder();

    builder.append("ALTER TABLE ").append(tableName).append(" ALTER COLUMN ").append(oldName);
    builder.append(" RENAME TO ").append(columnInfo.getName());

    return builder.toString();
  }

  @Override
  public StringBuilder writeColumnModifyString(StringBuilder builder, DBAccessor.DBColumnInfo columnInfo) {
    builder.append(" ALTER COLUMN ").append(columnInfo.getName())
      .append(" SET DATA TYPE ");
    writeColumnType(builder, columnInfo);

    return builder;
  }

  //+
  @Override
  public StringBuilder writeSetNullableString(StringBuilder builder,
      String tableName, DBAccessor.DBColumnInfo columnInfo, boolean nullable) {
    builder.append(" ALTER COLUMN ").append(columnInfo.getName()).append(" SET");
    String nullStatement = nullable ? " NULL" : " NOT NULL";
    builder.append(nullStatement);
    return builder;
  }

  @Override
  public String writeGetTableConstraints(String databaseName, String tableName){
    StringBuilder statement = new StringBuilder()
      .append("SELECT")
        .append(" C.CONSTRAINTNAME AS CONSTRAINT_NAME,")
        .append(" C.TYPE AS CONSTRAINT_TYPE")
      .append(" FROM SYS.SYSCONSTRAINTS AS C, SYS.SYSTABLES AS T")
      .append(" WHERE C.TABLEID = T.TABLEID AND T.TABLENAME = '").append(tableName).append("'");
    return statement.toString();
  }

  /**
   {@inheritDoc}
   */
  @Override
  public String getCopyColumnToAnotherTableStatement(String sourceTable, String sourceColumnName,
                                                     String sourceIDColumnName, String targetTable, String targetColumnName, String targetIDColumnName) {
    return String.format("UPDATE %1$s a SET %3$s = (SELECT b.%4$s FROM %2$s b WHERE b.%6$s = a.%5$s LIMIT 1)",
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
    return String.format("UPDATE %1$s a SET %3$s = (SELECT b.%4$s FROM %2$s b WHERE b.%8$s = a.%5$s AND b.%9$s = a.%6$s AND b.%10$s = a.%7$s AND b.%11$s = '%12$s'  LIMIT 1)",
        targetTable, sourceTable, targetColumnName, sourceColumnName, targetIDColumnName1, targetIDColumnName2, targetIDColumnName3,
        sourceIDColumnName1, sourceIDColumnName2, sourceIDColumnName3, sourceConditionFieldName, condition);
  }
}
