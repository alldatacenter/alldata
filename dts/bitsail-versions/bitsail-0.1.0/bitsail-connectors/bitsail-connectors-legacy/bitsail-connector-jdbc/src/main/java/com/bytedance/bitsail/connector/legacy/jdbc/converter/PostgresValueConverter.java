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

import org.apache.commons.dbcp.DelegatingResultSet;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.core.Oid;
import org.postgresql.jdbc.PgResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;

/**
 * Created 2021/4/2
 */
public class PostgresValueConverter extends JdbcValueConverter {
  private static final Logger LOG = LoggerFactory.getLogger(PostgresValueConverter.class);

  private static PgResultSet unwrap(ResultSet rs) {
    PgResultSet pgResultSet = null;
    if (rs instanceof PgResultSet) {
      pgResultSet = (PgResultSet) rs;
    } else if (rs instanceof DelegatingResultSet) {
      pgResultSet = unwrap(((DelegatingResultSet) rs).getInnermostDelegate());
    }
    return pgResultSet;
  }

  @Override
  protected Object extract(ResultSet rs,
                           ResultSetMetaData metaData,
                           int columnIndex,
                           int columnType,
                           String columnTypeName,
                           String columnName,
                           String encoding) throws Exception {
    PgResultSet pgResultSet = unwrap(rs);
    if (Objects.isNull(pgResultSet)) {
      return super.extract(rs, metaData, columnIndex, columnType, columnTypeName, columnName, encoding);
    }
    String pgColumnTypeName = metaData.getColumnTypeName(columnIndex);
    int pgColumnType;
    try {
      pgColumnType = Oid.valueOf(pgColumnTypeName);
    } catch (Exception e) {
      LOG.debug("Column type name = {} is invalid.", columnTypeName);
      return super.extract(rs, metaData, columnIndex, columnType, columnTypeName, columnName, encoding);
    }

    switch (pgColumnType) {
      case Oid.MONEY:
        return extractMoneyValue(pgResultSet, columnIndex);
      case Oid.INTERVAL:
        return extractIntervalValue(pgResultSet, columnIndex);
      case Oid.BOOL:
        return extractBooleanValue(pgResultSet, columnIndex);
      default:
        return super.extract(rs, metaData, columnIndex, columnType, columnTypeName, columnName, encoding);
    }
  }

  @Override
  protected Object convert(Object value, int columnType, String columnName, String columnTypeName) throws Exception {
    int pgColumnType;
    try {
      pgColumnType = Oid.valueOf(columnTypeName);
    } catch (Exception e) {
      LOG.debug("Column type name = {} is invalid.", columnTypeName);
      return super.convert(value, columnType, columnName, columnTypeName);
    }
    switch (pgColumnType) {
      case Oid.BOOL:
        return convertBooleanValue(value, columnName, columnTypeName);
      default:
        return super.convert(value, columnType, columnName, columnTypeName);
    }
  }

  private Double extractMoneyValue(PgResultSet pgResultSet, int columnIndex) throws SQLException, ParseException {
    String stringValue = pgResultSet.getFixedString(columnIndex);
    if (StringUtils.isEmpty(stringValue)) {
      return null;
    }
    Number number = NumberFormat.getNumberInstance().parse(stringValue);
    return number.doubleValue();
  }

  private String extractIntervalValue(PgResultSet pgResultSet, int columnIndex) throws SQLException, ParseException {
    return pgResultSet.getString(columnIndex);
  }

  private Boolean extractBooleanValue(PgResultSet pgResultSet, int columnIndex) throws SQLException, ParseException {
    return pgResultSet.getBoolean(columnIndex);
  }
}
