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

package com.bytedance.bitsail.connector.doris.partition;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class DorisPartitionHelper {

  private static final String SQL_TEMPLATE_SHOW_TABLE = "show tables from `%s` like \"%s\";";
  private static final String SQL_TEMPLATE_SHOW_TEMP_PARTITIONS = "show temporary partitions from `%s`.`%s` where PartitionName = \"%s\";";
  private static final String SQL_TEMPLATE_SHOW_DYNAMIC_PARTITION_TABLES = "show dynamic partition tables from `%s`";

  private static final String SQL_TEMPLATE_DROP_TABLE = "drop table `%s`.`%s`;";
  private static final String SQL_TEMPLATE_DROP_TEMP_PARTITIONS = "alter table `%s`.`%s` drop temporary partition %s;";

  private static final String SQL_TEMPLATE_CREATE_PARTITION = "alter table `%s`.`%s` add %s partition if not exists %s values [(%s),(%s));";

  private static final String SQL_TEMPLATE_SHOW_CREATE_TABLE = "show create table `%s`.`%s`;";
  private static final String SQL_TEMPLATE_DESC_TABLE = "desc `%s`.`%s`;";

  private static final String SQL_TEMPLATE_ENABLE_DYNAMIC_PARTITION = "alter table `%s`.`%s` set (\"dynamic_partition.enable\"=\"%s\");";

  private static final String SQL_TEMPLATE_REPLACE_TABLE = "alter table `%s`.`%s` replace with table `%s` properties ('swap' = 'false');";
  private static final String SQL_TEMPLATE_REPLACE_PARTITIONS = "alter table `%s` replace partition (%s) with temporary partition (%s);";

  private final String database;
  private final String table;
  private final Statement statement;

  public void dropTemporaryTable(String tempTableName) throws SQLException {
    String sqlShowTemporaryTable = String.format(SQL_TEMPLATE_SHOW_TABLE, database, tempTableName);
    String sqlDropTemporaryTable = String.format(SQL_TEMPLATE_DROP_TABLE, database, tempTableName);

    log.info("Query temporary table sql: {}", sqlShowTemporaryTable);
    if (statement.executeUpdate(sqlShowTemporaryTable) != 0) {
      log.info("Drop temporary table sql: {}", sqlDropTemporaryTable);
      statement.executeUpdate(sqlDropTemporaryTable);
      log.info("Succeed drop temporary table: {} ", tempTableName);
    }
  }

  public void dropTemporaryPartition(DorisPartition partition) throws SQLException {
    String tempPartitionName = partition.getTempName();
    String sqlQueryTemporaryPartition = String.format(SQL_TEMPLATE_SHOW_TEMP_PARTITIONS, database, table, tempPartitionName);
    String sqlDropTemporaryPartition = String.format(SQL_TEMPLATE_DROP_TEMP_PARTITIONS, database, table, tempPartitionName);

    log.info("Query temporary partition sql: {}", sqlQueryTemporaryPartition);
    if (statement.executeUpdate(sqlQueryTemporaryPartition) != 0) {
      log.info("Drop temporary partition sql: {}", sqlDropTemporaryPartition);
      statement.executeUpdate(sqlDropTemporaryPartition);
      log.info("Succeed drop temporary partition: {} ", tempPartitionName);
    }
  }

  public void createPartition(DorisPartition partition, boolean isTemporary) throws SQLException {
    String temporaryFlag = isTemporary ? "temporary" : "";
    String partitionName = isTemporary ? partition.getTempName() : partition.getName();
    String partitionStartRange = partition.getStartRange();
    String partitionEndRange = partition.getEndRange();
    String sqlCreatePartition = String.format(SQL_TEMPLATE_CREATE_PARTITION,
        database, table, temporaryFlag, partitionName, partitionStartRange, partitionEndRange);
    log.info("Create partition sql: {}", sqlCreatePartition);
    statement.executeUpdate(sqlCreatePartition);
    log.info("Succeed create partition: {} ", partitionName);
  }

  public void createTemporaryTable(String tempTableName, String seqColumnName) throws SQLException {
    // step1: get ddl sql by "show create table"
    String sqlShowCreateTable = String.format(SQL_TEMPLATE_SHOW_CREATE_TABLE, database, table);
    String sourceCreateTableSql = "";
    if (statement.execute(sqlShowCreateTable)) {
      try (ResultSet resultSet = statement.getResultSet()) {
        if (resultSet.next()) {
          sourceCreateTableSql = resultSet.getString("Create Table");
        }
      }
    }
    if (StringUtils.isEmpty(sourceCreateTableSql)) {
      throw new RuntimeException("failed to get source create table sql.");
    }
    String createTemporaryTableSql = sourceCreateTableSql.replace("`" + table + "`",
        String.format("`%s`.`%s`", database, tempTableName));

    // step2: add sequence column setting to ddl sql
    String seqColumnType = "";
    if (StringUtils.isNotEmpty(seqColumnName)) {
      statement.executeUpdate("set show_hidden_columns = true;");
      String querySequenceColumn = String.format(SQL_TEMPLATE_DESC_TABLE, database, table);
      try (ResultSet resultSet = statement.executeQuery(querySequenceColumn)) {
        while (resultSet.next()) {
          log.info("field: {}, type: {}", resultSet.getString("Field"), resultSet.getString("Type"));
          if (resultSet.getString("Field").equals("__DORIS_SEQUENCE_COL__")) {
            seqColumnType = resultSet.getString("Type");
          }
        }
      }
    }
    if (StringUtils.isNotEmpty(seqColumnName) && StringUtils.isEmpty(seqColumnType)) {
      log.error("sequence column {} is set, but doris table doesn't contain sequence column", seqColumnName);
    }
    if (StringUtils.isNotEmpty(seqColumnType)) {
      createTemporaryTableSql = createTemporaryTableSql.replace(");", ",\"function_column.sequence_type\" = \"" + seqColumnType + "\");");
    }

    // step3: execute the ddl statement
    log.info("create temp table sql: {}", createTemporaryTableSql);
    statement.executeUpdate(createTemporaryTableSql);
    log.info("Succeed create temporary table: {} ", tempTableName);
  }

  public boolean isDynamicPartition() throws SQLException {
    String sqlQueryDynamicTable = String.format(SQL_TEMPLATE_SHOW_DYNAMIC_PARTITION_TABLES, database);
    Set<String> stringSet = new HashSet<>();
    log.info("find all dynamic partition tables sql: {}", sqlQueryDynamicTable);
    if (statement.execute(sqlQueryDynamicTable)) {
      try (ResultSet resultSet = statement.getResultSet()) {
        while (resultSet.next()) {
          if (resultSet.getBoolean(2)) {
            stringSet.add(resultSet.getString(1));
          }
        }
      }
    } else {
      return false;
    }
    if (stringSet.contains(table)) {
      log.info("table {} is dynamic partition table", table);
      return true;
    }
    return false;
  }

  public void setDynamicPartitionEnableConf(boolean enableDynamicPartition) throws SQLException {
    String sqlSetDynamicPartition = String.format(SQL_TEMPLATE_ENABLE_DYNAMIC_PARTITION, database, table, enableDynamicPartition);
    log.info("set dynamic partition sql: {}", sqlSetDynamicPartition);
    statement.executeUpdate(sqlSetDynamicPartition);
    log.info("Succeed set dynamic partition for table {}", table);
  }

  public void replacePartition(List<DorisPartition> partitionList) throws SQLException {
    String tempPartitions = partitionList.stream().map(DorisPartition::getTempName).collect(Collectors.joining(","));
    String formalPartitions = partitionList.stream().map(DorisPartition::getName).collect(Collectors.joining(","));
    String sqlReplacePartition = String.format(SQL_TEMPLATE_REPLACE_PARTITIONS, table, formalPartitions, tempPartitions);
    log.info("replace partition sql: {}", sqlReplacePartition);
    statement.executeUpdate(sqlReplacePartition);
    log.info("Succeed replace partition: replace partition ({}) with temporary partition ({})", formalPartitions, tempPartitions);
  }

  public void replaceTable(String tempTable) throws SQLException {
    String sqlShowTemporaryTable = String.format(SQL_TEMPLATE_SHOW_TABLE, database, tempTable);
    String sqlReplaceTable = String.format(SQL_TEMPLATE_REPLACE_TABLE, database, table, tempTable);
    if (statement.executeUpdate(sqlShowTemporaryTable) != 0) {
      log.info("replace table sql: {}", sqlReplaceTable);
      statement.executeUpdate(sqlReplaceTable);
      log.info("Succeed replace table {} with temporary table {}", table, tempTable);
    } else {
      throw new RuntimeException("cannot find temporary table: " + tempTable);
    }
  }
}

