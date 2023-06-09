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

package com.netease.arctic.ams.server.mybatis;

import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.utils.TableMetadataUtil;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(TableIdentifier.class)
public class TableIdentifier2StringConverter implements TypeHandler<TableIdentifier> {
  @Override
  public void setParameter(
      PreparedStatement preparedStatement, int i, TableIdentifier tableIdentifier, JdbcType jdbcType)
      throws SQLException {
    if (tableIdentifier == null) {
      preparedStatement.setString(i, "");
    } else {
      preparedStatement.setString(i, TableMetadataUtil.getTableAllIdentifyName(tableIdentifier));
    }
  }

  @Override
  public TableIdentifier getResult(ResultSet resultSet, String s) throws SQLException {
    String rs = resultSet.getString(s);
    if (rs != null) {
      return TableMetadataUtil.getTableAllIdentify(rs);
    }
    return null;
  }

  @Override
  public TableIdentifier getResult(ResultSet resultSet, int i) throws SQLException {
    String rs = resultSet.getString(i);
    if (rs != null) {
      return TableMetadataUtil.getTableAllIdentify(rs);
    }
    return null;
  }

  @Override
  public TableIdentifier getResult(CallableStatement callableStatement, int i) throws SQLException {
    String rs = callableStatement.getString(i);
    if (rs != null) {
      return TableMetadataUtil.getTableAllIdentify(rs);
    }
    return null;
  }
}
