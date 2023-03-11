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

package com.netease.arctic.ams.server.service;

import com.google.common.collect.Maps;
import com.netease.arctic.ams.server.model.AMSColumnInfo;
import com.netease.arctic.ams.server.model.AMSPartitionField;
import com.netease.arctic.ams.server.model.ServerTableMeta;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class MetaService {
  private static final Logger LOG = LoggerFactory.getLogger(MetaService.class);

  private static final DateFormat WATERMARK_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /**
   * getServerTableMeta.
   *
   * @param ac
   * @param ti
   * @return
   */
  public static ServerTableMeta getServerTableMeta(ArcticCatalog ac, TableIdentifier ti) {
    if (ac == null) {
      throw new RuntimeException("Catalog can't be null!");
    }
    ArcticTable at = ac.loadTable(ti);
    ServerTableMeta serverTableMeta = new ServerTableMeta();
    serverTableMeta.setTableIdentifier(ti);
    fillTableProperties(serverTableMeta, at.properties());
    serverTableMeta.setBaseLocation(at.location());
    serverTableMeta.setPartitionColumnList(at
            .spec()
            .fields()
            .stream()
            .map(item -> AMSPartitionField.buildFromPartitionSpec(at.spec().schema(), item))
            .collect(Collectors.toList()));
    serverTableMeta.setSchema(at
            .schema()
            .columns()
            .stream()
            .map(AMSColumnInfo::buildFromNestedField)
            .collect(Collectors.toList()));

    serverTableMeta.setFilter(null);
    LOG.info("is keyedTable: {}", at instanceof KeyedTable);
    if (at.isKeyedTable()) {
      KeyedTable kt = at.asKeyedTable();
      if (kt.primaryKeySpec() != null) {
        serverTableMeta.setPkList(kt
                .primaryKeySpec()
                .fields()
                .stream()
                .map(item -> AMSColumnInfo.buildFromPartitionSpec(at.spec().schema(), item))
                .collect(Collectors.toList()));
      }
    }
    if (serverTableMeta.getPkList() == null) {
      serverTableMeta.setPkList(new ArrayList<>());
    }
    return serverTableMeta;
  }

  private static void fillTableProperties(ServerTableMeta serverTableMeta,
                                            Map<String, String> tableProperties) {
    Map<String, String> properties = Maps.newHashMap(tableProperties);
    serverTableMeta.setTableWatermark(properties.remove(TableProperties.WATERMARK_TABLE));
    serverTableMeta.setBaseWatermark(properties.remove(TableProperties.WATERMARK_BASE_STORE));
    serverTableMeta.setCreateTime(PropertyUtil.propertyAsLong(properties, TableProperties.TABLE_CREATE_TIME,
            TableProperties.TABLE_CREATE_TIME_DEFAULT));
    properties.remove(TableProperties.TABLE_CREATE_TIME);

    TableProperties.READ_PROTECTED_PROPERTIES.forEach(properties::remove);
    serverTableMeta.setProperties(properties);
  }
}
