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

package com.bytedance.bitsail.connector.legacy.jdbc.source;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.TypeConvertUtil.StorageEngine;
import com.bytedance.bitsail.connector.legacy.jdbc.converter.JdbcValueConverter;
import com.bytedance.bitsail.connector.legacy.jdbc.converter.PostgresValueConverter;
import com.bytedance.bitsail.connector.legacy.jdbc.split.SplitRangeInfo;
import com.bytedance.bitsail.connector.legacy.jdbc.utils.PostgresqlUtil;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@NoArgsConstructor
public class PostgresqlInputFormat extends JDBCInputFormat {

  @Override
  public String getType() {
    return "PostgreSQL";
  }

  @Override
  public String getDriverName() {
    return PostgresqlUtil.DRIVER_NAME;
  }

  @Override
  public String getFieldQuote() {
    return PostgresqlUtil.DB_QUOTE;
  }

  @Override
  public String getValueQuote() {
    return PostgresqlUtil.VALUE_QUOTE;
  }

  @Override
  public StorageEngine getStorageEngine() {
    return StorageEngine.postgresql;
  }

  @Override
  String genSqlTemplate(String splitPK, List<ColumnInfo> columns, String filter) {
    return super.genSqlTemplate(splitPK, columns, filter);
  }

  @Override
  void setStatementRange(PreparedStatement statement, SplitRangeInfo splitRangeInfo) throws SQLException {
    statement.setObject(1, splitRangeInfo.getBeginPos());
    statement.setObject(2, splitRangeInfo.getEndPos());
  }

  @Override
  protected JdbcValueConverter createJdbcValueConverter() {
    return new PostgresValueConverter();
  }
}
