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

package com.netease.arctic.server.utils;

import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.table.ArcticTable;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HiveLocationUtil {
  private static final Logger LOG = LoggerFactory.getLogger(HiveLocationUtil.class);

  /**
   * getRuntime table hive table/partition location
   * @param table target table
   * @return hive table/partition location
   */
  public static Set<String> getHiveLocation(ArcticTable table) {
    Set<String> hiveLocations = new HashSet<>();
    if (TableTypeUtil.isHive(table)) {
      if (table.spec().isUnpartitioned()) {
        try {
          Table hiveTable = ((SupportHive) table).getHMSClient().run(client ->
              client.getTable(table.id().getDatabase(), table.id().getTableName()));
          hiveLocations.add(hiveTable.getSd().getLocation());
        } catch (Exception e) {
          LOG.error("{} getRuntime hive table error", table.id(), e);
          throw new IllegalStateException("getRuntime hive table error", e);
        }
      } else {
        try {
          List<Partition> partitions = ((SupportHive) table).getHMSClient().run(client ->
              client.listPartitions(table.id().getDatabase(), table.id().getTableName(), Short.MAX_VALUE));
          for (Partition partition : partitions) {
            hiveLocations.add(partition.getSd().getLocation());
          }
        } catch (Exception e) {
          LOG.error("{} getRuntime hive partitions error", table.id(), e);
          throw new IllegalStateException("getRuntime hive partitions error", e);
        }
      }
    }

    return hiveLocations;
  }
}
