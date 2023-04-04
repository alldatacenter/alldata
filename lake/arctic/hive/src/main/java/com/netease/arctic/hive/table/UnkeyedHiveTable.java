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
import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.op.HiveOperationTransaction;
import com.netease.arctic.hive.op.HiveSchemaUpdate;
import com.netease.arctic.hive.op.OverwriteHiveFiles;
import com.netease.arctic.hive.op.ReplaceHivePartitions;
import com.netease.arctic.hive.op.RewriteHiveFiles;
import com.netease.arctic.hive.utils.HiveMetaSynchronizer;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.table.BaseTable;
import com.netease.arctic.table.BasicUnkeyedTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.ReplacePartitions;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.util.PropertyUtil;

import java.util.Map;

import static com.netease.arctic.hive.HiveTableProperties.BASE_HIVE_LOCATION_ROOT;

/**
 * Implementation of {@link com.netease.arctic.table.UnkeyedTable} with Hive table as base store.
 */
public class UnkeyedHiveTable extends BasicUnkeyedTable implements BaseTable, SupportHive {

  private final HMSClientPool hiveClient;
  private final String tableLocation;

  private boolean syncHiveChange = true;

  public UnkeyedHiveTable(
      TableIdentifier tableIdentifier,
      Table icebergTable,
      ArcticFileIO arcticFileIO,
      String tableLocation,
      AmsClient client,
      HMSClientPool hiveClient,
      Map<String, String> catalogProperties) {
    this(tableIdentifier, icebergTable, arcticFileIO, tableLocation, client, hiveClient, catalogProperties, true);
  }

  public UnkeyedHiveTable(
      TableIdentifier tableIdentifier,
      Table icebergTable,
      ArcticFileIO arcticFileIO,
      String tableLocation,
      AmsClient client,
      HMSClientPool hiveClient,
      Map<String, String> catalogProperties,
      boolean syncHiveChange) {
    super(tableIdentifier, icebergTable, arcticFileIO, client, catalogProperties);
    this.hiveClient = hiveClient;
    this.tableLocation = tableLocation;
    this.syncHiveChange = syncHiveChange;
    if (enableSyncHiveSchemaToArctic()) {
      syncHiveSchemaToArctic();
    }
    if (enableSyncHiveDataToArctic()) {
      syncHiveDataToArctic(false);
    }
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
  public Schema schema() {
    return super.schema();
  }

  @Override
  public ReplacePartitions newReplacePartitions() {
    return new ReplaceHivePartitions(super.newTransaction(),
        false, this, hiveClient, hiveClient);
  }

  @Override
  public String name() {
    return id().getTableName();
  }

  @Override
  public String hiveLocation() {
    return properties().containsKey(BASE_HIVE_LOCATION_ROOT) ?
        properties().get(BASE_HIVE_LOCATION_ROOT) :
        HiveTableUtil.hiveRootLocation(tableLocation);
  }

  @Override
  public HMSClientPool getHMSClient() {
    return hiveClient;
  }

  @Override
  public OverwriteHiveFiles newOverwrite() {
    return new OverwriteHiveFiles(super.newTransaction(), false, this, hiveClient, hiveClient);
  }

  @Override
  public RewriteHiveFiles newRewrite() {
    return new RewriteHiveFiles(super.newTransaction(), false, this, hiveClient, hiveClient);
  }

  @Override
  public Transaction newTransaction() {
    Transaction transaction = super.newTransaction();
    return new HiveOperationTransaction(this, transaction, hiveClient);
  }

  @Override
  public UpdateSchema updateSchema() {
    return new HiveSchemaUpdate(this, hiveClient, super.updateSchema());
  }

  @Override
  public boolean enableSyncHiveSchemaToArctic() {
    return syncHiveChange && PropertyUtil.propertyAsBoolean(
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
    return syncHiveChange && PropertyUtil.propertyAsBoolean(
        properties(),
        HiveTableProperties.AUTO_SYNC_HIVE_DATA_WRITE,
        HiveTableProperties.AUTO_SYNC_HIVE_DATA_WRITE_DEFAULT);
  }

  @Override
  public void syncHiveDataToArctic(boolean force) {
    HiveMetaSynchronizer.syncHiveDataToArctic(this, hiveClient, force);
  }
}
