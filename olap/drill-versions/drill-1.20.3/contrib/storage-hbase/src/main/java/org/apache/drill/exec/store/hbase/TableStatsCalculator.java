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
package org.apache.drill.exec.store.hbase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.drill.common.config.DrillConfig;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.ClusterStatus;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.RegionLoad;
import org.apache.hadoop.hbase.ServerLoad;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.RegionLocator;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Computes size of each region for given table.
 */
public class TableStatsCalculator {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TableStatsCalculator.class);

  public static final long DEFAULT_ROW_COUNT = 1024L * 1024L; // 1 million rows

  private static final String DRILL_EXEC_HBASE_SCAN_SAMPLE_ROWS_COUNT = "drill.exec.hbase.scan.samplerows.count";

  private static final int DEFAULT_SAMPLE_SIZE = 100;

  // Maps each region to its size in bytes.
  private Map<byte[], Long> sizeMap = null;

  private int avgRowSizeInBytes = 1;

  private int colsPerRow = 1;

  private long estimatedRowCount = DEFAULT_ROW_COUNT;

  /**
   * Computes size of each region for table.
   *
   * @param connection connection to Hbase client
   * @param hbaseScanSpec scan specification
   * @param config drill configuration
   * @param storageConfig Hbase storage configuration
   */
  public TableStatsCalculator(Connection connection, HBaseScanSpec hbaseScanSpec, DrillConfig config, HBaseStoragePluginConfig storageConfig) throws IOException {
    TableName tableName = TableName.valueOf(hbaseScanSpec.getTableName());
    try (Admin admin = connection.getAdmin();
         Table table = connection.getTable(tableName);
         RegionLocator locator = connection.getRegionLocator(tableName)) {
      int rowsToSample = rowsToSample(config);
      if (rowsToSample > 0) {
        Scan scan = new Scan(hbaseScanSpec.getStartRow(), hbaseScanSpec.getStopRow());
        scan.setCaching(rowsToSample < DEFAULT_SAMPLE_SIZE ? rowsToSample : DEFAULT_SAMPLE_SIZE);
        scan.setMaxVersions(1);
        ResultScanner scanner = table.getScanner(scan);
        long rowSizeSum = 0;
        int numColumnsSum = 0, rowCount = 0;
        for (; rowCount < rowsToSample; ++rowCount) {
          Result row = scanner.next();
          if (row == null) {
            break;
          }
          numColumnsSum += row.size();
          Cell[] cells = row.rawCells();
          if (cells != null) {
            for (Cell cell : cells) {
              rowSizeSum += CellUtil.estimatedSerializedSizeOf(cell);
            }
          }
        }
        if (rowCount > 0) {
          avgRowSizeInBytes = (int) (rowSizeSum / rowCount);
          colsPerRow = numColumnsSum / rowCount;
          // if during sampling we receive less rows than expected, then we can use this number instead of default
          estimatedRowCount = rowCount == rowsToSample ? estimatedRowCount : rowCount;
        }

        scanner.close();
      }

      if (!enabled(storageConfig)) {
        logger.debug("Region size calculation is disabled.");
        return;
      }

      logger.debug("Calculating region sizes for table '{}'.", tableName.getNameAsString());

      // get regions for table
      List<HRegionLocation> tableRegionInfos = locator.getAllRegionLocations();
      Set<byte[]> tableRegions = new TreeSet<>(Bytes.BYTES_COMPARATOR);
      for (HRegionLocation regionInfo : tableRegionInfos) {
        tableRegions.add(regionInfo.getRegionInfo().getRegionName());
      }

      ClusterStatus clusterStatus = null;
      try {
        clusterStatus = admin.getClusterStatus();
      } catch (Exception e) {
        logger.debug(e.getMessage(), e);
      } finally {
        if (clusterStatus == null) {
          return;
        }
      }

      sizeMap = new TreeMap<>(Bytes.BYTES_COMPARATOR);

      Collection<ServerName> servers = clusterStatus.getServers();
      // iterate all cluster regions, filter regions from our table and compute their size
      for (ServerName serverName : servers) {
        ServerLoad serverLoad = clusterStatus.getLoad(serverName);

        for (RegionLoad regionLoad : serverLoad.getRegionsLoad().values()) {
          byte[] regionId = regionLoad.getName();

          if (tableRegions.contains(regionId)) {
            long regionSizeMB = regionLoad.getMemStoreSizeMB() + regionLoad.getStorefileSizeMB();
            sizeMap.put(regionId, (regionSizeMB > 0 ? regionSizeMB : 1) * estimatedRowCount);
            logger.debug("Region {} has size {} MB.", regionLoad.getNameAsString(), regionSizeMB);
          }
        }
      }
      logger.debug("Region sizes calculated.");
    }

  }

  private boolean enabled(HBaseStoragePluginConfig config) {
    return config.isSizeCalculatorEnabled();
  }

  private int rowsToSample(DrillConfig config) {
    return config.hasPath(DRILL_EXEC_HBASE_SCAN_SAMPLE_ROWS_COUNT) ?
      config.getInt(DRILL_EXEC_HBASE_SCAN_SAMPLE_ROWS_COUNT) : DEFAULT_SAMPLE_SIZE;
  }

  /**
   * Returns size of given region in bytes. Returns 0 if region was not found.
   */
  public long getRegionSizeInBytes(byte[] regionId) {
    if (sizeMap == null) {
      return (long) avgRowSizeInBytes * estimatedRowCount;
    } else {
      Long size = sizeMap.get(regionId);
      if (size == null) {
        logger.debug("Unknown region: {}.", Arrays.toString(regionId));
        return 0;
      } else {
        return size;
      }
    }
  }

  public int getAvgRowSizeInBytes() {
    return avgRowSizeInBytes;
  }

  public int getColsPerRow() {
    return colsPerRow;
  }

  public boolean usedDefaultRowCount() {
    return estimatedRowCount == DEFAULT_ROW_COUNT;
  }

}
