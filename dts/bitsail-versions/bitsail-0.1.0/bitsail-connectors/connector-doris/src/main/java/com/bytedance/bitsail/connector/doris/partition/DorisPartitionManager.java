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

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Builder
public class DorisPartitionManager {

  /**
   * basic table info
   */
  private final String database;
  private final String table;
  private final String seqColumnName;

  /**
   * When a table is built with no partition, the system will automatically generate a partition with the same name
   * as the table name. This partition is not visible to the user and cannot be modified.
   */
  private final boolean tableHasPartition;

  /**
   * when table has partition(s), first load data into temporary partition(s), and then replace the formal partition(s)
   * with the temporary partition(s)
   * partitions: the target partitions to load data
   * mutable: if to set partitions mutable after loading data
   */
  private final List<DorisPartition> partitions;

  /**
   * when table has no partition, first load data into a temporary table, and then replace the target table with the
   * temporary table
   */
  private final String tempTable;

  /**
   * used for execute jdbc statement to manipulate doris partitions
   */
  private DorisPartitionHelper partitionHelper;

  public void open(Statement statement) {
    partitionHelper = new DorisPartitionHelper(database, table, statement);
  }

  /**
   * clean existed temporary partition/table
   */
  public void cleanTemporaryPartition() throws SQLException {
    log.info("start cleaning temporary partitions in {}.{}", database, table);
    if (tableHasPartition) {
      for (DorisPartition partition : partitions) {
        partitionHelper.dropTemporaryPartition(partition);
      }
    } else {
      partitionHelper.dropTemporaryTable(tempTable);
    }
    log.info("finish cleaning temporary partitions.");
  }

  /**
   * create new temporary partition/table
   */
  public void createTemporaryPartition() throws SQLException {
    log.info("start creating temporary partition for loading.");
    if (tableHasPartition) {
      for (DorisPartition partition : partitions) {
        partitionHelper.createPartition(partition, true);
      }
    } else {
      partitionHelper.createTemporaryTable(tempTable, seqColumnName);
    }
    log.info("finish creating temporary partition for loading.");
  }

  /**
   * create formal partition if it does not exist
   */
  public void createFormalPartition() throws SQLException {
    log.info("start creating formal partition");
    if (tableHasPartition) {
      boolean needResetDynamicPartition = false;
      try {
        if (partitionHelper.isDynamicPartition()) {
          partitionHelper.setDynamicPartitionEnableConf(false);   // unable to create partition when dynamic partition enabled
          needResetDynamicPartition = true;
        }
        for (DorisPartition partition : partitions) {
          partitionHelper.createPartition(partition, false);
        }
      } catch (Exception e) {
        throw new RuntimeException("failed to create partition.", e);
      } finally {
        if (needResetDynamicPartition) {
          partitionHelper.setDynamicPartitionEnableConf(true);
        }
      }
    }
    log.info("finish creating formal partition");
  }

  /**
   * replace the partition/table with temporary partition/table
   */
  public void replacePartition() throws SQLException {
    log.info("start replacing partition");
    if (tableHasPartition) {
      partitionHelper.replacePartition(partitions);
    } else {
      partitionHelper.replaceTable(tempTable);
    }
    log.info("finish replacing partition");
  }

  /**
   * replace the partition/table with temporary partition/table
   */
  public void replacePartitionWithoutMutable() throws SQLException {
    log.info("start replacing partition");
    if (tableHasPartition) {
      partitionHelper.replacePartition(partitions);
    } else {
      partitionHelper.replaceTable(tempTable);
    }
    log.info("finish replacing partition");
  }
}
