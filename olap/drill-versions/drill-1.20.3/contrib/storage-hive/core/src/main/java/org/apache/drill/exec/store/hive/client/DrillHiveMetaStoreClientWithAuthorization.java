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

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.calcite.schema.Schema;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.store.hive.HiveReadEntry;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides authorization verifications when SQL Standard Based authorization is
 * enabled. In another case (Storage Based authorization) Hive client just won't allow unauthorized
 * access for reading tables.
 */
class DrillHiveMetaStoreClientWithAuthorization extends DrillHiveMetaStoreClient {

  private static final Logger logger = LoggerFactory.getLogger(DrillHiveMetaStoreClientWithAuthorization.class);

  static final String DRILL2HMS_TOKEN = "DrillDelegationTokenForHiveMetaStoreServer";

  private final UserGroupInformation ugiForRpc;

  private final HiveAuthorizationHelper authorizer;

  DrillHiveMetaStoreClientWithAuthorization(final HiveConf hiveConf, final UserGroupInformation ugiForRpc,
                                            final String userName) throws TException {
    super(hiveConf);
    this.ugiForRpc = ugiForRpc;
    this.authorizer = new HiveAuthorizationHelper(this, hiveConf, userName);
  }

  @Override
  public List<String> getDatabases(boolean ignoreAuthzErrors) throws TException {
    try {
      authorizer.authorizeShowDatabases();
    } catch (final HiveAccessControlException e) {
      if (ignoreAuthzErrors) {
        return Collections.emptyList();
      }
      throw UserException.permissionError(e).build(logger);
    }
    return super.getDatabases(ignoreAuthzErrors);
  }

  @Override
  public HiveReadEntry getHiveReadEntry(final String dbName, final String tableName, boolean ignoreAuthzErrors) throws TException {
    try {
      authorizer.authorizeReadTable(dbName, tableName);
    } catch (final HiveAccessControlException e) {
      if (!ignoreAuthzErrors) {
        throw UserException.permissionError(e).build(logger);
      }
    }
    return super.getHiveReadEntry(dbName, tableName, ignoreAuthzErrors);
  }

  @Override
  public Map<String, Schema.TableType> getTableNamesAndTypes(String dbName, boolean ignoreAuthzErrors) throws TException {
    try {
      authorizer.authorizeShowTables(dbName);
    } catch (final HiveAccessControlException e) {
      if (ignoreAuthzErrors) {
        return Collections.emptyMap();
      }
      throw UserException.permissionError(e).build(logger);
    }
    return super.getTableNamesAndTypes(dbName, ignoreAuthzErrors);
  }

  @Override
  public void reconnect() throws MetaException {
    try {
      ugiForRpc.doAs((PrivilegedExceptionAction<Void>) () -> {
        super.reconnect();
        return null;
      });
    } catch (final InterruptedException | IOException e) {
      throw new DrillRuntimeException("Failed to reconnect to HiveMetaStore: " + e.getMessage(), e);
    }
  }

}
