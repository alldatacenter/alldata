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

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;

/**
 * Provides factory methods for initialization of {@link DrillHiveMetaStoreClient} instances.
 */
public final class DrillHiveMetaStoreClientFactory {

  private DrillHiveMetaStoreClientFactory() {
  }

  /**
   * Create a DrillHiveMetaStoreClient for cases where:
   * 1. Drill impersonation is enabled and
   * 2. either storage (in remote HiveMetaStore server) or SQL standard based authorization (in Hive storage plugin)
   * is enabled
   *
   * @param processUserMetaStoreClient MetaStoreClient of process user. Useful for generating the delegation tokens when
   *                                   SASL (KERBEROS or custom SASL implementations) is enabled.
   * @param hiveConf                   Conf including authorization configuration
   * @param userName                   User who is trying to access the Hive metadata
   * @return instance of client
   */
  public static DrillHiveMetaStoreClient createClientWithAuthz(final DrillHiveMetaStoreClient processUserMetaStoreClient,
                                                               final HiveConf hiveConf, final String userName) {
    try {
      boolean delegationTokenGenerated = false;

      final UserGroupInformation ugiForRpc; // UGI credentials to use for RPC communication with Hive MetaStore server
      if (!hiveConf.getBoolVar(HiveConf.ConfVars.HIVE_SERVER2_ENABLE_DOAS)) {
        // If the user impersonation is disabled in Hive storage plugin (not Drill impersonation), use the process
        // user UGI credentials.
        ugiForRpc = ImpersonationUtil.getProcessUserUGI();
      } else {
        ugiForRpc = ImpersonationUtil.createProxyUgi(userName);
        if (hiveConf.getBoolVar(HiveConf.ConfVars.METASTORE_USE_THRIFT_SASL)) {
          // When SASL is enabled for proxy user create a delegation token. Currently HiveMetaStoreClient can create
          // client transport for proxy users only when the authentication mechanims is DIGEST (through use of
          // delegation tokens).
          String delegationToken = processUserMetaStoreClient.getDelegationToken(userName, userName);
          try {
            setTokenStr(ugiForRpc, delegationToken, DrillHiveMetaStoreClientWithAuthorization.DRILL2HMS_TOKEN);
          } catch (IOException e) {
            throw new DrillRuntimeException("Couldn't setup delegation token in the UGI for Hive MetaStoreClient", e);
          }
          delegationTokenGenerated = true;
        }
      }

      final HiveConf hiveConfForClient;
      if (delegationTokenGenerated) {
        hiveConfForClient = new HiveConf(hiveConf);
        hiveConfForClient.set("hive.metastore.token.signature", DrillHiveMetaStoreClientWithAuthorization.DRILL2HMS_TOKEN);
      } else {
        hiveConfForClient = hiveConf;
      }

      return ugiForRpc.doAs((PrivilegedExceptionAction<DrillHiveMetaStoreClient>)
          () -> new DrillHiveMetaStoreClientWithAuthorization(hiveConfForClient, ugiForRpc, userName));
    } catch (final Exception e) {
      throw new DrillRuntimeException("Failure setting up HiveMetaStore client.", e);
    }
  }

  /**
   * Create a delegation token object for the given token string and service.
   * Add the token to given UGI
   *
   * @param ugi          user group information
   * @param tokenStr     token string
   * @param tokenService token service
   * @throws IOException if error happened during decoding token string
   */
  public static void setTokenStr(UserGroupInformation ugi, String tokenStr, String tokenService)
      throws IOException {
    Token<?> delegationToken = createToken(tokenStr, tokenService);
    ugi.addToken(delegationToken);
  }

  /**
   * Create a new token using the given string and service
   *
   * @param tokenStr     token string
   * @param tokenService token service
   * @return {@link Token} instance with decoded string
   * @throws IOException if error happened during decoding token string
   */
  private static Token<?> createToken(String tokenStr, String tokenService)
      throws IOException {
    Token<?> delegationToken = new Token<>();
    delegationToken.decodeFromUrlString(tokenStr);
    delegationToken.setService(new Text(tokenService));
    return delegationToken;
  }

  /**
   * Create a DrillMetaStoreClient that can be shared across multiple users. This is created when impersonation is
   * disabled.
   *
   * @param hiveConf hive properties set in Drill storage plugin
   * @return instance of client
   * @throws MetaException when initialization failed
   */
  public static DrillHiveMetaStoreClient createCloseableClientWithCaching(final HiveConf hiveConf)
      throws MetaException {
    return new DrillHiveMetaStoreClient(hiveConf);
  }

}
