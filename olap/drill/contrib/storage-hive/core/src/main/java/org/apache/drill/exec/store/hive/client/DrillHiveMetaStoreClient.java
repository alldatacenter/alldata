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
package org.apache.drill.exec.store.hive.client;

import java.util.List;
import java.util.Map;

import org.apache.calcite.schema.Schema.TableType;
import org.apache.drill.exec.store.hive.HiveReadEntry;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.thrift.TException;

/**
 * Extension of HiveMetaStoreClient with addition of cache and methods useful
 * for Drill schema. Note, that access to parent class is synchronized either
 * on cache loading level or in overridden methods, and the synchronization
 * should not be neglected in child classes.
 */
public class DrillHiveMetaStoreClient extends HiveMetaStoreClient {

  /**
   * Unified API for work with HiveMetaStoreClient
   * client through local caches.
   */
  private final HiveMetadataCache hiveMetadataCache;

  /**
   * Package visibility performs two roles here:
   * 1) ensure that child classes in same package;
   * 2) ensure that instances published to other packages
   *    by {@link DrillHiveMetaStoreClientFactory}.
   *
   * @param hiveConf hive conf from storage plugin
   * @throws MetaException when initialization failed
   */
  DrillHiveMetaStoreClient(final HiveConf hiveConf) throws MetaException {
    super(hiveConf);
    hiveMetadataCache = new HiveMetadataCache(this, hiveConf);
  }

  /**
   * Lists all Hive database names.
   *
   * @param ignoreAuthzErrors whether authorization errors should be ignored
   * @return list of Hive databases
   * @throws TException when client fails
   */
  public List<String> getDatabases(boolean ignoreAuthzErrors) throws TException {
    return hiveMetadataCache.getDbNames();
  }

  /**
   * Returns table metadata for concrete table
   *
   * @param dbName    name of database
   * @param tableName name of table
   * @return {@link HiveReadEntry} containing table meta like columns, partitions etc.
   * @throws TException when client fails
   */
  public HiveReadEntry getHiveReadEntry(final String dbName, final String tableName, boolean ignoreAuthzErrors) throws TException {
    return hiveMetadataCache.getHiveReadEntry(dbName, tableName);
  }

  /**
   * Returns collection of view and table names along with their types.
   *
   * @param dbName            name of database
   * @param ignoreAuthzErrors hint for handling authorization errors
   * @return map where keys are db object names values are types (VIEW or TABLE)
   * @throws TException in case when if loader thrown ExecutionException
   */
  public Map<String, TableType> getTableNamesAndTypes(final String dbName, boolean ignoreAuthzErrors) throws TException {
    return hiveMetadataCache.getTableNamesAndTypes(dbName);
  }

  /**
   * Overridden to enforce synchronization.
   *
   * @param owner                        the intended owner for the token
   * @param renewerKerberosPrincipalName kerberos user
   * @return the string of the token
   * @throws TException when client fails
   */
  @Override
  public synchronized String getDelegationToken(String owner, String renewerKerberosPrincipalName) throws TException {
    return super.getDelegationToken(owner, renewerKerberosPrincipalName);
  }

}
