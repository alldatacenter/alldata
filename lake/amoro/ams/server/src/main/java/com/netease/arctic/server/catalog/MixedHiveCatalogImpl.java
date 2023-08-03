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

package com.netease.arctic.server.catalog;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.hive.CachedHiveClientPool;
import com.netease.arctic.hive.HMSClient;
import com.netease.arctic.hive.catalog.MixedHiveTables;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.thrift.TException;

import java.util.List;

public class MixedHiveCatalogImpl extends MixedCatalogImpl {

  private volatile CachedHiveClientPool hiveClientPool;

  protected MixedHiveCatalogImpl(CatalogMeta catalogMeta) {
    super(catalogMeta, new MixedHiveTables(catalogMeta));
    hiveClientPool = ((MixedHiveTables) tables()).getHiveClientPool();
  }

  @Override
  public void updateMetadata(CatalogMeta metadata) {
    super.updateMetadata(metadata);
    hiveClientPool = ((MixedHiveTables) tables()).getHiveClientPool();
  }

  @Override
  public void createDatabase(String databaseName) {
    // do not handle database operations
  }

  @Override
  public void dropDatabase(String databaseName) {
    // do not handle database operations
  }

  @Override
  protected void decreaseDatabaseTableCount(String databaseName) {
    // do not handle database operations
  }

  @Override
  protected void increaseDatabaseTableCount(String databaseName) {
    // do not handle database operations
  }

  @Override
  public boolean exist(String database) {
    try {
      return hiveClientPool.run(client -> {
        try {
          client.getDatabase(database);
          return true;
        } catch (NoSuchObjectException exception) {
          return false;
        }
      });
    } catch (TException | InterruptedException e) {
      throw new RuntimeException("Failed to get databases", e);
    }
  }

  @Override
  public List<String> listDatabases() {
    try {
      return hiveClientPool.run(HMSClient::getAllDatabases);
    } catch (TException | InterruptedException e) {
      throw new RuntimeException("Failed to list databases", e);
    }
  }

  public CachedHiveClientPool getHiveClient() {
    return hiveClientPool;
  }
}
