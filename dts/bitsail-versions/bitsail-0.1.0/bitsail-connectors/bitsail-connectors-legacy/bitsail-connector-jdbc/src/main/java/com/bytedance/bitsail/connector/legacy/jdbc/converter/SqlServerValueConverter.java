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

package com.bytedance.bitsail.connector.legacy.jdbc.converter;

import com.microsoft.sqlserver.jdbc.SQLServerResultSet;
import com.microsoft.sqlserver.jdbc.SQLServerResultSetMetaData;
import microsoft.sql.Types;
import org.apache.commons.dbcp.DelegatingResultSet;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Created 2021/4/2
 */
public class SqlServerValueConverter extends JdbcValueConverter {

  private static SQLServerResultSet unwrap(ResultSet rs) {
    SQLServerResultSet sqlserverResultSet = null;
    if (rs instanceof SQLServerResultSet) {
      sqlserverResultSet = (SQLServerResultSet) rs;
    } else if (rs instanceof DelegatingResultSet) {
      sqlserverResultSet = unwrap(((DelegatingResultSet) rs).getInnermostDelegate());
    }

    return sqlserverResultSet;
  }

  @Override
  protected Object extract(ResultSet rs,
                           ResultSetMetaData metaData,
                           int columnIndex,
                           int columnType,
                           String columnTypeName,
                           String columnName,
                           String encoding) throws Exception {
    SQLServerResultSet sqlserverResultSet = unwrap(rs);
    if (Objects.isNull(sqlserverResultSet)) {
      return super.extract(rs, metaData, columnIndex, columnType, columnTypeName, columnName, encoding);
    }
    SQLServerResultSetMetaData sqlServerResultSetMetaData = (SQLServerResultSetMetaData) metaData;
    int sqlserverColumnType = sqlServerResultSetMetaData.getColumnType(columnIndex);
    switch (sqlserverColumnType) {
      case Types.DATETIME:
      case Types.DATETIMEOFFSET:
      case Types.SMALLDATETIME:
        return extractTimestampValue(sqlserverResultSet, columnIndex);
      case Types.SQL_VARIANT:
        return extractStringValue(sqlserverResultSet, columnIndex, encoding);
      case Types.MONEY:
        return extractMoneyValue(sqlserverResultSet, columnIndex);
      default:
        return super.extract(rs, metaData, columnIndex, columnType, columnTypeName, columnName, encoding);
    }
  }

  @Override
  protected Object convert(Object value, int columnType, String columnName, String columnTypeName) throws Exception {
    switch (columnType) {
      case Types.DATETIME:
      case Types.DATETIMEOFFSET:
      case Types.SMALLDATETIME:
      case Types.SQL_VARIANT:
      case Types.MONEY:
        return value;
      default:
        return super.convert(value, columnType, columnName, columnTypeName);
    }
  }

  private BigDecimal extractMoneyValue(SQLServerResultSet rs, int columnIndex) throws SQLException {
    return rs.getMoney(columnIndex);
  }
}
