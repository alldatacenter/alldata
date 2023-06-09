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
package org.apache.drill.exec.store.sys;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.rpc.user.UserServer.BitToUserConnection;
import org.apache.drill.exec.rpc.user.UserServer.BitToUserConnectionConfig;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.rest.profile.SimpleDurationFormat;
import org.apache.drill.exec.store.pojo.NonNullable;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.joda.time.DateTime;

/**
 * Add a system table for listing connected users on a cluster
 */
public class BitToUserConnectionIterator implements Iterator<Object> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BitToUserConnectionIterator.class);

  Iterator<ConnectionInfo> itr;
  private String queryingUsername;
  private boolean isAdmin;

  public BitToUserConnectionIterator(ExecutorFragmentContext context) {
    queryingUsername = context.getQueryUserName();
    isAdmin = hasAdminPrivileges(context);
    itr = iterateConnectionInfo(context);
  }

  private boolean hasAdminPrivileges(ExecutorFragmentContext context) {
    OptionManager options = context.getOptions();
    if (context.isUserAuthenticationEnabled() &&
        !ImpersonationUtil.hasAdminPrivileges(
          this.queryingUsername,
          ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(options),
          ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(options))) {
      return false;
    }

    //Passed checks
    return true;
  }

  private Iterator<ConnectionInfo> iterateConnectionInfo(ExecutorFragmentContext context) {
    Set<Entry<BitToUserConnection, BitToUserConnectionConfig>> activeConnections = context.getUserConnections();

    String hostname = context.getEndpoint().getAddress();
    List<ConnectionInfo> connectionInfos = new LinkedList<ConnectionInfo>();

    for (Entry<BitToUserConnection, BitToUserConnectionConfig> connection : activeConnections) {
      if ( isAdmin ||
          this.queryingUsername.equals(
              connection.getKey().getSession().getTargetUserName()) ) {
        connectionInfos.add(new ConnectionInfo(connection, hostname));
      }
    }

    return connectionInfos.iterator();
  }

  @Override
  public boolean hasNext() {
    return itr.hasNext();
  }

  @Override
  public Object next() {
    return itr.next();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  public static class ConnectionInfo {
    public String user;
    @NonNullable
    public String client;
    @NonNullable
    public String drillbit;
    @NonNullable
    public Timestamp established;
    public String duration;
    public int queries;
    public boolean isAuthenticated;
    public boolean isEncrypted;
    public boolean usingSSL;
    @NonNullable
    public String session;

    public ConnectionInfo(Entry<BitToUserConnection, BitToUserConnectionConfig> connectionConfigPair, String hostname) {
      BitToUserConnection connection = connectionConfigPair.getKey();
      BitToUserConnectionConfig config = connectionConfigPair.getValue();
      UserSession userSession = connection.getSession();
      this.user = userSession.getCredentials().getUserName();
      DateTime dateTime = config.getEstablished();
      this.established = new Timestamp(
          dateTime
          .plusMillis(TimeZone.getDefault().getOffset(dateTime.getMillis())) //Adjusting for -Duser.timezone
          .getMillis());
      this.duration = (new SimpleDurationFormat(dateTime.getMillis(), System.currentTimeMillis()))
          .verbose();
      this.client = extractIpAddr(connection.getRemoteAddress().toString());
      this.drillbit = hostname;
      this.session = userSession.getSessionId();
      this.queries = userSession.getQueryCount();
      this.isAuthenticated = config.isAuthEnabled();
      this.isEncrypted = config.isEncryptionEnabled();
      this.usingSSL = config.isSSLEnabled();
    }

    private String extractIpAddr(String clientAddrString) {
      String ipAddr = clientAddrString
          .replaceFirst("/","") //Remove any leading '/'
          .split(":")[0]; //Remove any connected port reference
      return ipAddr;
    }
  }
}
