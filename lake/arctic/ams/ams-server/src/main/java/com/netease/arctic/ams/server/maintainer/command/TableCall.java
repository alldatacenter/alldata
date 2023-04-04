/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import com.netease.arctic.ams.api.ArcticTableMetastore;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogManager;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;

public class TableCall implements CallCommand {

  private ArcticTableMetastore.Iface client;
  private CatalogManager catalogManager;
  private String tablePath;
  private TableOperation tableOperation;

  public TableCall(
      ArcticTableMetastore.Iface client,
      CatalogManager catalogManager,
      String tablePath,
      TableOperation tableOperation) {
    this.client = client;
    this.catalogManager = catalogManager;
    this.tablePath = tablePath;
    this.tableOperation = tableOperation;
  }

  @Override
  public String call(Context context) throws Exception {
    TableIdentifier identifier = fullTableName(context, tablePath);
    switch (tableOperation) {
      case REFRESH: {
        client.refreshTable(identifier.buildTableIdentifier());
        return ok();
      }
      case SYNC_HIVE_METADATA: {
        ArcticCatalog arcticCatalog = catalogManager.getArcticCatalog(identifier.getCatalog());
        ArcticTable arcticTable = arcticCatalog.loadTable(identifier);
        if (TableTypeUtil.isHive(arcticTable)) {
          ((SupportHive)arcticTable).syncHiveSchemaToArctic();
        }
        return ok();
      }
      case SYNC_HIVE_DATA: {
        ArcticCatalog arcticCatalog = catalogManager.getArcticCatalog(identifier.getCatalog());
        ArcticTable arcticTable = arcticCatalog.loadTable(identifier);
        if (TableTypeUtil.isHive(arcticTable)) {
          ((SupportHive)arcticTable).syncHiveDataToArctic(false);
        }
        return ok();
      }
      case SYNC_HIVE_DATA_FORCE : {
        ArcticCatalog arcticCatalog = catalogManager.getArcticCatalog(identifier.getCatalog());
        ArcticTable arcticTable = arcticCatalog.loadTable(identifier);
        if (TableTypeUtil.isHive(arcticTable)) {
          ((SupportHive)arcticTable).syncHiveDataToArctic(true);
        }
        return ok();
      }
      case DROP_METADATA: {
        ArcticCatalog arcticCatalog = catalogManager.getArcticCatalog(identifier.getCatalog());
        arcticCatalog.dropTable(identifier, false);
        return ok();
      }
      default: {
        throw new IllegalCommandException(String.format("Unsupported %s", tableOperation));
      }
    }

  }

  public enum TableOperation {
    REFRESH,
    SYNC_HIVE_METADATA,
    SYNC_HIVE_DATA,
    SYNC_HIVE_DATA_FORCE,
    DROP_METADATA
  }
}
