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

package com.netease.arctic.hive.table;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.op.BaseSchemaUpdate;
import com.netease.arctic.hive.utils.HiveMetaSynchronizer;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.ArcticHadoopFileIO;
import com.netease.arctic.scan.ChangeTableIncrementalScan;
import com.netease.arctic.table.BaseTable;
import com.netease.arctic.table.BasicKeyedTable;
import com.netease.arctic.table.BasicUnkeyedTable;
import com.netease.arctic.table.ChangeTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.ArcticChangeTableScan;
import org.apache.iceberg.Table;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.util.PropertyUtil;

import java.util.Map;

/**
 * Implementation of {@link com.netease.arctic.table.KeyedTable} with Hive table as base store.
 */
public class KeyedHiveTable extends BasicKeyedTable implements SupportHive {

  private final HMSClientPool hiveClient;

  public KeyedHiveTable(
      TableMeta tableMeta,
      String tableLocation,
      PrimaryKeySpec primaryKeySpec,
      AmsClient client,
      HMSClientPool hiveClient,
      UnkeyedHiveTable baseTable,
      ChangeTable changeTable) {
    super(tableMeta, tableLocation, primaryKeySpec, client, baseTable, changeTable);
    this.hiveClient = hiveClient;
    if (enableSyncHiveSchemaToArctic()) {
      syncHiveSchemaToArctic();
    }
    if (enableSyncHiveDataToArctic()) {
      syncHiveDataToArctic(false);
    }
  }

  @Override
  public ArcticHadoopFileIO io() {
    return (ArcticHadoopFileIO) super.io();
  }

  @Override
  public TableFormat format() {
    return TableFormat.MIXED_HIVE;
  }

  @Override
  public void refresh() {
    super.refresh();
    if (enableSyncHiveSchemaToArctic()) {
      syncHiveSchemaToArctic();
    }
    if (enableSyncHiveDataToArctic()) {
      syncHiveDataToArctic(false);
    }
  }

  @Override
  public String hiveLocation() {
    return ((SupportHive) baseTable()).hiveLocation();
  }

  @Override
  public boolean enableSyncHiveSchemaToArctic() {
    return PropertyUtil.propertyAsBoolean(
        properties(),
        HiveTableProperties.AUTO_SYNC_HIVE_SCHEMA_CHANGE,
        HiveTableProperties.AUTO_SYNC_HIVE_SCHEMA_CHANGE_DEFAULT);
  }

  @Override
  public void syncHiveSchemaToArctic() {
    HiveMetaSynchronizer.syncHiveSchemaToArctic(this, hiveClient);
  }

  @Override
  public boolean enableSyncHiveDataToArctic() {
    return PropertyUtil.propertyAsBoolean(
        properties(),
        HiveTableProperties.AUTO_SYNC_HIVE_DATA_WRITE,
        HiveTableProperties.AUTO_SYNC_HIVE_DATA_WRITE_DEFAULT);
  }

  @Override
  public void syncHiveDataToArctic(boolean force) {
    HiveMetaSynchronizer.syncHiveDataToArctic(this, hiveClient, force);
  }

  @Override
  public HMSClientPool getHMSClient() {
    return hiveClient;
  }


  public static class HiveChangeInternalTable extends BasicUnkeyedTable implements ChangeTable {

    public HiveChangeInternalTable(
        TableIdentifier tableIdentifier, Table changeIcebergTable, ArcticFileIO arcticFileIO,
        AmsClient client, Map<String, String> catalogProperties) {
      super(tableIdentifier, changeIcebergTable, arcticFileIO, client, catalogProperties);
    }

    @Override
    public TableFormat format() {
      return TableFormat.MIXED_HIVE;
    }

    @Override
    public UpdateSchema updateSchema() {
      return new BaseSchemaUpdate(this, super.updateSchema());
    }

    @Override
    public ChangeTableIncrementalScan newScan() {
      return new ArcticChangeTableScan(operations(), this);
    }
  }

  public static class HiveBaseInternalTable extends UnkeyedHiveTable implements BaseTable {

    public HiveBaseInternalTable(
        TableIdentifier tableIdentifier, Table icebergTable,
        ArcticHadoopFileIO arcticFileIO, String tableLocation, AmsClient client,
        HMSClientPool hiveClient, Map<String, String> catalogProperties,
        boolean syncHiveChange) {
      super(tableIdentifier, icebergTable, arcticFileIO, tableLocation, client, hiveClient, catalogProperties,
          syncHiveChange);
    }

    @Override
    public TableFormat format() {
      return TableFormat.MIXED_HIVE;
    }
  }
}
