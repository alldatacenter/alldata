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

package com.bytedance.bitsail.connector.doris.sink.ddl;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.doris.DorisConnectionHolder;
import com.bytedance.bitsail.connector.doris.config.DorisOptions;
import com.bytedance.bitsail.connector.doris.partition.DorisPartitionManager;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DorisSchemaManagerGenerator {
  private static final String MYSQL_URL_FORMAT = "jdbc:mysql://%s/%s?useUnicode=true&characterEncoding=utf-8";
  private static final int CONNECTION_REQUIRE_TIMEOUT = 5;

  public static DorisConnectionHolder getDorisConnection(DorisConnectionHolder dorisConnectionHolder, DorisOptions dorisOptions) throws IOException {
    String user = StringUtils.isNotEmpty(dorisOptions.getUsername()) ? dorisOptions.getUsername() : "root";
    String pwd = StringUtils.isNotEmpty(dorisOptions.getPassword()) ? dorisOptions.getPassword() : "";

    try {
      if (Objects.isNull(dorisConnectionHolder.getDorisConnection()) ||
          !dorisConnectionHolder.getDorisConnection().isValid(CONNECTION_REQUIRE_TIMEOUT)) {
        Class.forName("com.mysql.jdbc.Driver");
        dorisConnectionHolder.setDorisConnection(DriverManager.getConnection(getRandomMysqlUrl(dorisOptions), user, pwd));
        dorisConnectionHolder.setStatement(dorisConnectionHolder.getDorisConnection().createStatement());
        dorisConnectionHolder.getStatement().executeUpdate("set query_timeout = 300;");
      }
    } catch (Exception e) {
      dorisConnectionHolder.closeDorisConnection();
      throw new IOException("failed to open doris connection or create statement", e);
    }
    return dorisConnectionHolder;
  }

  public static DorisPartitionManager openDorisPartitionManager(DorisConnectionHolder dorisConnectionHolder, DorisOptions dorisOptions) throws IOException {
    DorisPartitionManager dorisPartitionManager;
    dorisPartitionManager = DorisPartitionManager.builder()
        .database(dorisOptions.getDatabaseName())
        .table(dorisOptions.getTableName())
        .seqColumnName(getSeqColumnNames(dorisOptions.getColumnInfos()))
        .tableHasPartition(dorisOptions.isTableHasPartitions())
        .partitions(dorisOptions.getPartitions())
        .tempTable(dorisOptions.getTmpTableName())
        .build();
    try {
      dorisPartitionManager.open(dorisConnectionHolder.getStatement());
    } catch (Exception e) {
      throw new IOException("failed to open doris partition manager", e);
    }
    return dorisPartitionManager;
  }

  private static String getRandomMysqlUrl(DorisOptions dorisOptions) {
    if (StringUtils.isEmpty(dorisOptions.getMysqlNodes())) {
      throw new IllegalArgumentException("mysql nodes is empty");
    }
    List<String> nodes = Arrays.stream(dorisOptions.getMysqlNodes().split(",")).map(String::trim).collect(Collectors.toList());
    Collections.shuffle(nodes);
    return String.format(MYSQL_URL_FORMAT, nodes.get(0), dorisOptions.getDatabaseName());
  }

  private static String getSeqColumnNames(List<ColumnInfo> columnInfos) {
    List<String> columnNames = new ArrayList<>();
    for (ColumnInfo columnInfo : columnInfos) {
      columnNames.add(columnInfo.getName());
    }
    return String.join(",", columnNames);
  }
}
