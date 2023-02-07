/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.mapr.db;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.cost.PluginCost;
import org.apache.drill.exec.store.mapr.PluginConstants;
import org.apache.drill.exec.store.mapr.db.json.JsonTableGroupScan;

public class MapRDBCost implements PluginCost {

  private int JSON_AVG_COLUMN_SIZE;
  private int JSON_TABLE_BLOCK_SIZE;  // bytes per block
  private int JSON_BLOCK_SEQ_READ_COST;
  private int JSON_BLOCK_RANDOM_READ_COST;
  private int JSON_HDD_BLOCK_SEQ_READ_COST;
  private int JSON_HDD_BLOCK_RANDOM_READ_COST;
  private int JSON_SSD_BLOCK_SEQ_READ_COST;
  private int JSON_SSD_BLOCK_RANDOM_READ_COST;

  public MapRDBCost(DrillConfig config, String mediaType) {
    JSON_AVG_COLUMN_SIZE = setConfigValue(config, PluginConstants.JSON_TABLE_AVERGE_COLUMN_SIZE,
        PluginConstants.JSON_TABLE_AVERGE_COLUMN_SIZE_DEFAULT, PluginConstants.alwaysValid);
    JSON_TABLE_BLOCK_SIZE = setConfigValue(config, PluginConstants.JSON_TABLE_BLOCK_SIZE,
        PluginConstants.JSON_TABLE_BLOCK_SIZE_DEFAULT, PluginConstants.alwaysValid);
    JSON_SSD_BLOCK_SEQ_READ_COST = setConfigValue(config, PluginConstants.JSON_TABLE_SSD_BLOCK_SEQ_READ_COST,
        PluginConstants.JSON_TABLE_SSD_BLOCK_SEQ_READ_COST_DEFAULT, PluginConstants.isNonNegative);
    JSON_SSD_BLOCK_RANDOM_READ_COST = setConfigValue(config, PluginConstants.JSON_TABLE_SSD_BLOCK_RANDOM_READ_COST,
        PluginConstants.JSON_TABLE_SSD_BLOCK_RANDOM_READ_COST_DEFAULT, new greaterThanEquals(JSON_SSD_BLOCK_SEQ_READ_COST));
    JSON_HDD_BLOCK_SEQ_READ_COST = setConfigValue(config, PluginConstants.JSON_TABLE_HDD_BLOCK_SEQ_READ_COST,
            PluginConstants.JSON_TABLE_HDD_BLOCK_SEQ_READ_COST_DEFAULT, PluginConstants.isNonNegative);
    JSON_HDD_BLOCK_RANDOM_READ_COST = setConfigValue(config, PluginConstants.JSON_TABLE_HDD_BLOCK_RANDOM_READ_COST,
            PluginConstants.JSON_TABLE_HDD_BLOCK_RANDOM_READ_COST_DEFAULT, new greaterThanEquals(JSON_HDD_BLOCK_SEQ_READ_COST));
    JSON_BLOCK_SEQ_READ_COST = mediaType.equals(PluginConstants.SSD) ? JSON_SSD_BLOCK_SEQ_READ_COST :
                                    JSON_HDD_BLOCK_SEQ_READ_COST;
    JSON_BLOCK_RANDOM_READ_COST = mediaType.equals(PluginConstants.SSD) ? JSON_SSD_BLOCK_RANDOM_READ_COST :
                                    JSON_HDD_BLOCK_RANDOM_READ_COST;
  }

  private int setConfigValue(DrillConfig config, String configPath,
                             int defaultValue, CheckValid check) {
    int configValue;
    try {
      configValue = config.getInt(configPath);
      if (!check.isValid(configValue)) { configValue = defaultValue; }
    } catch (Exception ex) {
      // Use defaults, if config values not present or any other issue
      configValue = defaultValue;
    }
    return configValue;
  }

  @Override
  public int getAverageColumnSize(GroupScan scan) {
    if (scan instanceof JsonTableGroupScan) {
      return JSON_AVG_COLUMN_SIZE;
    } else {
      return PluginConstants.TABLE_AVERGE_COLUMN_SIZE_DEFAULT;
    }
  }

  @Override
  public int getBlockSize(GroupScan scan) {
    if (scan instanceof JsonTableGroupScan) {
      return JSON_TABLE_BLOCK_SIZE;
    } else {
      return PluginConstants.TABLE_BLOCK_SIZE_DEFAULT;
    }
  }

  @Override
  public int getSequentialBlockReadCost(GroupScan scan) {
    if (scan instanceof JsonTableGroupScan) {
      return JSON_BLOCK_SEQ_READ_COST;
    } else {
      return PluginConstants.TABLE_SSD_BLOCK_SEQ_READ_COST_DEFAULT;
    }
  }

  @Override
  public int getRandomBlockReadCost(GroupScan scan) {
    if (scan instanceof JsonTableGroupScan) {
      return JSON_BLOCK_RANDOM_READ_COST;
    } else {
      return PluginConstants.TABLE_SSD_BLOCK_RANDOM_READ_COST_DEFAULT;
    }
  }
}
