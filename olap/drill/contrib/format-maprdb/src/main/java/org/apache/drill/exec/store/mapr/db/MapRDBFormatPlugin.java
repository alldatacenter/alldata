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

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.planner.common.DrillStatsTable.TableStatistics;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatMatcher;
import org.apache.drill.exec.store.hbase.HBaseScanSpec;
import org.apache.drill.exec.store.mapr.PluginConstants;
import org.apache.drill.exec.store.mapr.TableFormatPlugin;
import org.apache.drill.exec.store.mapr.db.binary.BinaryTableGroupScan;
import org.apache.drill.exec.store.mapr.db.json.JsonScanSpec;
import org.apache.drill.exec.store.mapr.db.json.JsonTableGroupScan;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import com.mapr.db.index.IndexDesc;
import com.mapr.fs.tables.TableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapRDBFormatPlugin extends TableFormatPlugin {
  private static final Logger logger = LoggerFactory.getLogger(MapRDBFormatPlugin.class);

  private final MapRDBFormatMatcher matcher;
  private final Configuration hbaseConf;
  private final Connection connection;
  private final MapRDBTableCache jsonTableCache;
  private final int scanRangeSizeMB;
  private final String mediaType;
  private final MapRDBCost pluginCostModel;
  private final int restrictedScanRangeSizeMB;

  public MapRDBFormatPlugin(String name, DrillbitContext context, Configuration fsConf,
      StoragePluginConfig storageConfig, MapRDBFormatPluginConfig formatConfig) throws IOException {
    super(name, context, fsConf, storageConfig, formatConfig);
    matcher = new MapRDBFormatMatcher(this);
    hbaseConf = HBaseConfiguration.create(fsConf);
    hbaseConf.set(ConnectionFactory.DEFAULT_DB, ConnectionFactory.MAPR_ENGINE2);
    connection = ConnectionFactory.createConnection(hbaseConf);
    jsonTableCache = new MapRDBTableCache(context.getConfig());
    int scanRangeSizeMBConfig = context.getConfig().getInt(PluginConstants.JSON_TABLE_SCAN_SIZE_MB);
    if (scanRangeSizeMBConfig < PluginConstants.JSON_TABLE_SCAN_SIZE_MB_MIN ||
        scanRangeSizeMBConfig > PluginConstants.JSON_TABLE_SCAN_SIZE_MB_MAX) {
      logger.warn("Invalid scan size {} for MapR-DB tables, using default", scanRangeSizeMBConfig);
      scanRangeSizeMBConfig = PluginConstants.JSON_TABLE_SCAN_SIZE_MB_DEFAULT;
    }

    int restrictedScanRangeSizeMBConfig = context.getConfig().getInt(PluginConstants.JSON_TABLE_RESTRICTED_SCAN_SIZE_MB);
    if (restrictedScanRangeSizeMBConfig < PluginConstants.JSON_TABLE_SCAN_SIZE_MB_MIN ||
        restrictedScanRangeSizeMBConfig > PluginConstants.JSON_TABLE_SCAN_SIZE_MB_MAX) {
      logger.warn("Invalid restricted scan size {} for MapR-DB tables, using default", restrictedScanRangeSizeMBConfig);
      restrictedScanRangeSizeMBConfig = PluginConstants.JSON_TABLE_RESTRICTED_SCAN_SIZE_MB_DEFAULT;
    }

    String mediaTypeConfig = context.getConfig().getString(PluginConstants.JSON_TABLE_MEDIA_TYPE);
    if (!(mediaTypeConfig.equals(PluginConstants.SSD) ||
        mediaTypeConfig.equals(PluginConstants.HDD))) {
      logger.warn("Invalid media Type {} for MapR-DB JSON tables, using default 'SSD'", mediaTypeConfig);
      mediaTypeConfig = PluginConstants.JSON_TABLE_MEDIA_TYPE_DEFAULT;
    }
    mediaType = mediaTypeConfig;
    scanRangeSizeMB = scanRangeSizeMBConfig;
    restrictedScanRangeSizeMB = restrictedScanRangeSizeMBConfig;
    pluginCostModel = new MapRDBCost(context.getConfig(), mediaType);
  }

  @Override
  public FormatMatcher getMatcher() {
    return matcher;
  }

  @Override
  public MapRDBFormatPluginConfig getConfig() {
    return (MapRDBFormatPluginConfig)(super.getConfig());
  }

  public MapRDBTableCache getJsonTableCache() {
    return jsonTableCache;
  }

  @Override
  @JsonIgnore
  public Set<StoragePluginOptimizerRule> getOptimizerRules() {
    return ImmutableSet.of(MapRDBPushFilterIntoScan.FILTER_ON_SCAN, MapRDBPushFilterIntoScan.FILTER_ON_PROJECT,
        MapRDBPushProjectIntoScan.PROJECT_ON_SCAN, MapRDBPushLimitIntoScan.LIMIT_ON_PROJECT,
        MapRDBPushLimitIntoScan.LIMIT_ON_SCAN, MapRDBPushLimitIntoScan.LIMIT_ON_RKJOIN);
  }

  public AbstractGroupScan getGroupScan(String userName, FileSelection selection,
      List<SchemaPath> columns, IndexDesc indexDesc, MetadataProviderManager metadataProviderManager) throws IOException {
    String tableName = getTableName(selection);
    TableProperties props = getMaprFS().getTableProperties(new Path(tableName));

    if (props.getAttr().getJson()) {
      JsonScanSpec scanSpec = new JsonScanSpec(tableName, indexDesc, null/*condition*/);
      return new JsonTableGroupScan(userName, getStoragePlugin(), this, scanSpec, columns, metadataProviderManager);
    } else {
      HBaseScanSpec scanSpec = new HBaseScanSpec(tableName);
      return new BinaryTableGroupScan(userName, getStoragePlugin(), this, scanSpec, columns, metadataProviderManager);
    }
  }

  @Override
  public AbstractGroupScan getGroupScan(String userName, FileSelection selection,
      List<SchemaPath> columns) throws IOException {
    return getGroupScan(userName, selection, columns, (IndexDesc) null /* indexDesc */, null /* metadataProviderManager */);
  }

  public boolean supportsStatistics() {
    return false;
  }

  @Override
  public TableStatistics readStatistics(FileSystem fs, Path statsTablePath) {
    throw new UnsupportedOperationException("unimplemented");
  }

  @Override
  public void writeStatistics(TableStatistics statistics, FileSystem fs, Path statsTablePath) throws IOException {
    throw new UnsupportedOperationException("unimplemented");
  }

  @JsonIgnore
  public Configuration getHBaseConf() {
    return hbaseConf;
  }

  @JsonIgnore
  public Connection getConnection() {
    return connection;
  }

  public int getScanRangeSizeMB() {
    return scanRangeSizeMB;
  }

  public int getRestrictedScanRangeSizeMB() {
    return restrictedScanRangeSizeMB;
  }

  public MapRDBCost getPluginCostModel() {
    return pluginCostModel;
  }

  /**
   * Allows to get a table name from FileSelection object
   *
   * @param selection File selection object
   * @return string table name
   */
  @JsonIgnore
  public String getTableName(FileSelection selection) {
    List<Path> files = selection.getFiles();
    assert (files.size() == 1);
    return files.get(0).toUri().getPath();
  }

}
