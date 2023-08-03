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
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(List.class)
public class List2StringConverter implements TypeHandler<List> {

  private String separator = ",";

  @Override
  public void setParameter(PreparedStatement ps, int i, List parameter, JdbcType jdbcType) throws SQLException {
    if (parameter == null) {
      ps.setString(i, null);
      return;
    }

    StringBuilder builder = new StringBuilder();
    Iterator iterator = parameter.iterator();
    while (iterator.hasNext()) {
      builder.append(iterator.next());
      if (iterator.hasNext()) {
        builder.append(separator);
      }
    }
    ps.setString(i, builder.toString());

  }

  @Override
  public List getResult(ResultSet rs, String columnName) throws SQLException {
    String res = rs.getString(columnName);
    if (res == null) {
      return null;
    }

    List set = new ArrayList<>();
    if (res.length() != 0) {
      String[] fields = res.split(separator);
      for (String field : fields) {
        set.add(field);
      }
    }

    return set;
  }

  @Override
  public List getResult(ResultSet rs, int columnIndex) throws SQLException {
    String res = rs.getString(columnIndex);
    if (res == null) {
      return null;
    }

    List set = new ArrayList<>();
    if (res.length() != 0) {
      String[] fields = res.split(separator);
      for (String field : fields) {
        set.add(field);
      }
    }

    return set;
  }

  @Override
  public List getResult(CallableStatement cs, int columnIndex) throws SQLException {
    String res = cs.getString(columnIndex);
    if (res == null) {
      return null;
    }

    List set = new ArrayList<>();
    if (res.length() != 0) {
      String[] fields = res.split(separator);
      for (String field : fields) {
        set.add(field);
      }
    }

    return set;
  }
}

