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

import java.util.Collections;
import java.util.List;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.HiveUtils;
import org.apache.hadoop.hive.ql.security.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAccessControlException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizer;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthorizerFactory;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext.CLIENT_TYPE;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveOperationType;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeObject;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeObject.HivePrivilegeObjectType;
import org.apache.hadoop.hive.ql.session.SessionState;

/**
 * Helper class for initializing and checking privileges according to authorization configuration set in Hive storage
 * plugin config.
 */
class HiveAuthorizationHelper {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HiveAuthorizationHelper.class);

  /**
   * Currently marks that SQLStdBasedAuthorization is enabled.
   * Then some operations on {@link DrillHiveMetaStoreClientWithAuthorization} will use
   * {@link HiveAuthorizationHelper#authorizerV2} to authorize action before
   * making request to MetaStore client.
   */
  final boolean authzEnabled;

  /**
   * Authorizer used when SQLStdBasedAuthorization is enabled.
   */
  final HiveAuthorizer authorizerV2;

  HiveAuthorizationHelper(final IMetaStoreClient mClient, final HiveConf hiveConf, final String user) {
    authzEnabled = hiveConf.getBoolVar(ConfVars.HIVE_AUTHORIZATION_ENABLED);
    if (!authzEnabled) {
      authorizerV2 = null;
      return;
    }

    try {
      final HiveConf hiveConfCopy = new HiveConf(hiveConf);
      hiveConfCopy.set("user.name", user);

      final HiveAuthenticationProvider authenticator = HiveUtils.getAuthenticator(hiveConfCopy,
          HiveConf.ConfVars.HIVE_AUTHENTICATOR_MANAGER);
      SessionState ss = new SessionState(hiveConfCopy, user);
      SessionState.start(ss);

      authenticator.setSessionState(ss);

      HiveAuthorizerFactory authorizerFactory =
          HiveUtils.getAuthorizerFactory(hiveConfCopy, HiveConf.ConfVars.HIVE_AUTHORIZATION_MANAGER);

      HiveAuthzSessionContext.Builder authzContextBuilder = new HiveAuthzSessionContext.Builder();
      authzContextBuilder.setClientType(CLIENT_TYPE.HIVESERVER2); // Drill is emulating HS2 here

      authorizerV2 = authorizerFactory.createHiveAuthorizer(
          () -> mClient,
          hiveConf, authenticator, authzContextBuilder.build());

      authorizerV2.applyAuthorizationConfigPolicy(hiveConfCopy);
    } catch (final HiveException e) {
      throw new DrillRuntimeException("Failed to initialize Hive authorization components: " + e.getMessage(), e);
    }

    logger.trace("Hive authorization enabled");
  }

  /**
   * Check authorization for "SHOW DATABASES" command. A {@link HiveAccessControlException} is thrown
   * for illegal access.
   */
  void authorizeShowDatabases() throws HiveAccessControlException {
    if (!authzEnabled) {
      return;
    }

    authorize(HiveOperationType.SHOWDATABASES, Collections.<HivePrivilegeObject> emptyList(), Collections.<HivePrivilegeObject> emptyList(), "SHOW DATABASES");
  }

  /**
   * Check authorization for "SHOW TABLES" command in given Hive db. A {@link HiveAccessControlException} is thrown
   * for illegal access.
   * @param dbName
   */
  void authorizeShowTables(final String dbName) throws HiveAccessControlException {
    if (!authzEnabled) {
      return;
    }

    final HivePrivilegeObject toRead = new HivePrivilegeObject(HivePrivilegeObjectType.DATABASE, dbName, null);

    authorize(HiveOperationType.SHOWTABLES, ImmutableList.of(toRead), Collections.<HivePrivilegeObject> emptyList(), "SHOW TABLES");
  }

  /**
   * Check authorization for "READ TABLE" for given db.table. A {@link HiveAccessControlException} is thrown
   * for illegal access.
   * @param dbName
   * @param tableName
   */
  void authorizeReadTable(final String dbName, final String tableName) throws HiveAccessControlException {
    if (!authzEnabled) {
      return;
    }

    HivePrivilegeObject toRead = new HivePrivilegeObject(HivePrivilegeObjectType.TABLE_OR_VIEW, dbName, tableName);
    authorize(HiveOperationType.QUERY, ImmutableList.of(toRead), Collections.<HivePrivilegeObject> emptyList(), "READ TABLE");
  }

  /* Helper method to check privileges */
  private void authorize(final HiveOperationType hiveOpType, final List<HivePrivilegeObject> toRead,
                         final List<HivePrivilegeObject> toWrite, final String cmd) throws HiveAccessControlException {
    try {
      HiveAuthzContext.Builder authzContextBuilder = new HiveAuthzContext.Builder();
      authzContextBuilder.setUserIpAddress("Not available");
      authzContextBuilder.setCommandString(cmd);

      authorizerV2.checkPrivileges(hiveOpType, toRead, toWrite, authzContextBuilder.build());
    } catch (final HiveAccessControlException e) {
      throw e;
    } catch (final Exception e) {
      throw new DrillRuntimeException("Failed to use the Hive authorization components: " + e.getMessage(), e);
    }
  }

}
