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

package com.netease.arctic.hive.op;

import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.Schema;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.util.PropertyUtil;

import java.util.Locale;

/**
 * Schema evolution API implementation for {@link KeyedTable}.
 */
public class HiveSchemaUpdate extends BaseSchemaUpdate {
  private final ArcticTable arcticTable;
  private final HMSClientPool hiveClient;
  private final HMSClientPool transactionClient;
  private final UpdateSchema updateSchema;

  public HiveSchemaUpdate(ArcticTable arcticTable, HMSClientPool hiveClient,
                          HMSClientPool transactionClient,
                          UpdateSchema updateSchema) {
    super(arcticTable, updateSchema);
    this.arcticTable = arcticTable;
    this.hiveClient = hiveClient;
    this.updateSchema = updateSchema;
    this.transactionClient = transactionClient;
  }

  @Override
  public void commit() {
    Table tbl = HiveTableUtil.loadHmsTable(hiveClient, arcticTable.id());
    if (tbl == null) {
      throw new RuntimeException(String.format("there is no such hive table named %s", arcticTable.id().toString()));
    }
    Schema newSchema = this.updateSchema.apply();
    this.updateSchema.commit();
    syncSchemaToHive(newSchema, tbl);
  }

  private void syncSchemaToHive(Schema newSchema, Table tbl) {
    tbl.setSd(HiveTableUtil.storageDescriptor(newSchema, arcticTable.spec(), tbl.getSd().getLocation(),
        FileFormat.valueOf(PropertyUtil.propertyAsString(arcticTable.properties(), TableProperties.DEFAULT_FILE_FORMAT,
            TableProperties.DEFAULT_FILE_FORMAT_DEFAULT).toUpperCase(Locale.ENGLISH))));
    HiveTableUtil.persistTable(transactionClient, tbl);
  }
}
