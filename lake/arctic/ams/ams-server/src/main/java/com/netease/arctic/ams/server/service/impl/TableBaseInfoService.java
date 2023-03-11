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

package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.server.model.TableBasicInfo;
import com.netease.arctic.ams.server.model.TableStatistics;
import com.netease.arctic.ams.server.service.ITableInfoService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.utils.TableStatCollector;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class TableBaseInfoService implements ITableInfoService {
  public static final Logger LOG = LoggerFactory.getLogger(TableBaseInfoService.class);

  private final AmsClient client;

  public TableBaseInfoService() {
    this.client = ServiceContainer.getTableMetastoreHandler();
  }

  @Override
  public TableBasicInfo getTableBasicInfo(TableIdentifier tableIdentifier) {
    try {
      TableBasicInfo tableBasicInfo = new TableBasicInfo();
      tableBasicInfo.setTableIdentifier(tableIdentifier);
      TableStatistics changeInfo = null;
      TableStatistics baseInfo;

      ArcticCatalog catalog = CatalogLoader.load(client, tableIdentifier.getCatalog());
      ArcticTable table = catalog.loadTable(tableIdentifier);
      if (table.isUnkeyedTable()) {
        UnkeyedTable unkeyedTable = table.asUnkeyedTable();
        baseInfo = new TableStatistics();
        TableStatCollector.fillTableStatistics(baseInfo, unkeyedTable, table, "TABLE");
      } else if (table.isKeyedTable()) {
        KeyedTable keyedTable = table.asKeyedTable();
        if (!PrimaryKeySpec.noPrimaryKey().equals(keyedTable.primaryKeySpec())) {
          changeInfo = TableStatCollector.collectChangeTableInfo(keyedTable);
        }
        baseInfo = TableStatCollector.collectBaseTableInfo(keyedTable);
      } else {
        throw new IllegalStateException("unknown type of table");
      }

      tableBasicInfo.setChangeStatistics(changeInfo);
      tableBasicInfo.setBaseStatistics(baseInfo);
      tableBasicInfo.setTableStatistics(TableStatCollector.union(changeInfo, baseInfo));

      long createTime
              = PropertyUtil.propertyAsLong(table.properties(), TableProperties.TABLE_CREATE_TIME,
              TableProperties.TABLE_CREATE_TIME_DEFAULT);
      if (createTime != TableProperties.TABLE_CREATE_TIME_DEFAULT) {
        if (tableBasicInfo.getTableStatistics() != null) {
          if (tableBasicInfo.getTableStatistics().getSummary() == null) {
            tableBasicInfo.getTableStatistics().setSummary(new HashMap<>());
          } else {
            LOG.warn("{} summary is null", tableIdentifier);
          }
          tableBasicInfo.getTableStatistics().getSummary()
                  .put("createTime", String.valueOf(createTime));
        } else {
          LOG.warn("{} table statistics is null {}", tableIdentifier, tableBasicInfo);
        }
      }
      return tableBasicInfo;
    } catch (Throwable t) {
      LOG.error("{} failed to build table basic info", tableIdentifier, t);
      throw t;
    }
  }

}
