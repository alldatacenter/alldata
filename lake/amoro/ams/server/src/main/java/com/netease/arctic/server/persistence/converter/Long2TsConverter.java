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

package com.netease.arctic.server.persistence.converter;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.LongTypeHandler;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@MappedJdbcTypes(JdbcType.TIMESTAMP)
@MappedTypes(long.class)
public class Long2TsConverter extends LongTypeHandler {
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Long parameter, JdbcType jdbcType)
      throws SQLException {
    if (parameter == 0) {
      ps.setTimestamp(i, null);
    } else {
      ps.setTimestamp(i, new Timestamp(parameter));
    }
  }

  @Override
  public Long getNullableResult(ResultSet rs, String columnName) throws SQLException {
    Timestamp ts = rs.getTimestamp(columnName);
    if (ts != null) {
      return ts.getTime();
    }
    return null;
  }

  @Override
  public Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp ts = rs.getTimestamp(columnIndex);
    if (ts != null) {
      return ts.getTime();
    }
    return null;
  }

  @Override
  public Long getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    Timestamp ts = cs.getTimestamp(columnIndex);
    if (ts != null) {
      return ts.getTime();
    }
    return null;
  }
}
