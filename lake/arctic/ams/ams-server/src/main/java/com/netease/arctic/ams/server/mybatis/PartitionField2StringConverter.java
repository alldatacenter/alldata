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

import com.netease.arctic.ams.api.PartitionFieldData;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PartitionField2StringConverter implements TypeHandler<List<PartitionFieldData>> {
  @Override
  public void setParameter(
      PreparedStatement ps, int i, List<PartitionFieldData> partitionFieldData, JdbcType jdbcType)
      throws SQLException {
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < partitionFieldData.size(); j++) {
      if (j > 0) {
        sb.append("/");
      }
      sb.append(partitionFieldData.get(j).getName()).append("=")
          .append(partitionFieldData.get(j).getValue());
    }
    ps.setString(i, sb.toString());
  }

  @Override
  public List<PartitionFieldData> getResult(ResultSet resultSet, String s) throws SQLException {
    String res = resultSet.getString(s);
    List rs = new ArrayList();
    String[] partitions = res.split("/");
    for (String p: partitions) {
      PartitionFieldData partitionFieldData = new PartitionFieldData();
      partitionFieldData.name = p.split("=")[0];
      partitionFieldData.name = p.split("=")[1];
    }
    return rs;
  }

  @Override
  public List<PartitionFieldData> getResult(ResultSet resultSet, int i) throws SQLException {
    String res = resultSet.getString(i);
    List rs = new ArrayList();
    String[] partitions = res.split("/");
    for (String p: partitions) {
      PartitionFieldData partitionFieldData = new PartitionFieldData();
      partitionFieldData.name = p.split("=")[0];
      partitionFieldData.name = p.split("=")[1];
    }
    return rs;
  }

  @Override
  public List<PartitionFieldData> getResult(CallableStatement callableStatement, int i) throws SQLException {
    String res = callableStatement.getString(i);
    List rs = new ArrayList();
    String[] partitions = res.split("/");
    for (String p: partitions) {
      PartitionFieldData partitionFieldData = new PartitionFieldData();
      partitionFieldData.name = p.split("=")[0];
      partitionFieldData.name = p.split("=")[1];
    }
    return rs;
  }
}
