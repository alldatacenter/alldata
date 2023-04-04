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

package com.netease.arctic.ams.server.maintainer;

import com.netease.arctic.AmsClient;
import com.netease.arctic.PooledAmsClient;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.ams.api.properties.MetaTableProperties;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.ArcticFileIOs;
import com.netease.arctic.io.TableTrashManager;
import com.netease.arctic.io.TableTrashManagers;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableMetaStore;
import org.apache.thrift.TException;

import java.util.Map;

import static com.netease.arctic.utils.CatalogUtil.buildMetaStore;

public class RepairUtil {
  public static String tableRootLocation(String thriftAddress, TableIdentifier identifier) {
    AmsClient client = new PooledAmsClient(thriftAddress);
    TableMeta tableMeta = null;
    try {
      tableMeta = client.getTable(identifier.buildTableIdentifier());
    } catch (TException e) {
      throw new RuntimeException(e);
    }
    return tableMeta.locations.get(MetaTableProperties.LOCATION_KEY_TABLE);
  }

  public static String tableChangeLocation(String thriftAddress, TableIdentifier identifier) {
    AmsClient client = new PooledAmsClient(thriftAddress);
    TableMeta tableMeta = null;
    try {
      tableMeta = client.getTable(identifier.buildTableIdentifier());
    } catch (TException e) {
      throw new RuntimeException(e);
    }
    return tableMeta.locations.get(MetaTableProperties.LOCATION_KEY_CHANGE);
  }

  public static String tableBaseLocation(String thriftAddress, TableIdentifier identifier) {
    AmsClient client = new PooledAmsClient(thriftAddress);
    TableMeta tableMeta = null;
    try {
      tableMeta = client.getTable(identifier.buildTableIdentifier());
    } catch (TException e) {
      throw new RuntimeException(e);
    }
    return tableMeta.locations.get(MetaTableProperties.LOCATION_KEY_BASE);
  }

  public static ArcticFileIO arcticFileIO(String thriftAddress, String catalogName) {
    AmsClient client = new PooledAmsClient(thriftAddress);
    CatalogMeta catalogMeta = null;
    try {
      catalogMeta = client.getCatalog(catalogName);
    } catch (TException e) {
      throw new RuntimeException(e);
    }
    TableMetaStore tableMetaStore = buildMetaStore(catalogMeta);
    return ArcticFileIOs.buildHadoopFileIO(tableMetaStore);
  }

  public static TableTrashManager tableTrashManager(String thriftAddress, TableIdentifier identifier)
      throws TException {
    AmsClient client = new PooledAmsClient(thriftAddress);
    CatalogMeta catalogMeta = client.getCatalog(identifier.getCatalog());
    TableMetaStore tableMetaStore = buildMetaStore(catalogMeta);
    ArcticFileIO arcticFileIO = ArcticFileIOs.buildHadoopFileIO(tableMetaStore);
    TableMeta tableMeta = client.getTable(identifier.buildTableIdentifier());
    String tableRootLocation = tableMeta.locations.get(MetaTableProperties.LOCATION_KEY_TABLE);
    Map<String, String> mergedProperties = com.netease.arctic.utils.CatalogUtil.mergeCatalogPropertiesToTable(
        tableMeta.getProperties(), catalogMeta.getCatalogProperties());
    return TableTrashManagers.build(identifier, tableRootLocation, mergedProperties, arcticFileIO);
  }
}
