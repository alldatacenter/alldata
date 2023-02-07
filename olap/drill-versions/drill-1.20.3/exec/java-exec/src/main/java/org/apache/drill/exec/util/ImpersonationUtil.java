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
package org.apache.drill.exec.util;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;

import org.apache.drill.shaded.guava.com.google.common.base.Splitter;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

/**
 * Utilities for impersonation purpose.
 */
public class ImpersonationUtil {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ImpersonationUtil.class);

  private static final LoadingCache<Key, UserGroupInformation> CACHE = CacheBuilder.newBuilder()
      .maximumSize(100)
      .expireAfterAccess(60, TimeUnit.MINUTES)
      .build(new CacheLoader<Key, UserGroupInformation>() {
        @Override
        public UserGroupInformation load(Key key) throws Exception {
          return UserGroupInformation.createProxyUser(key.proxyUserName, key.loginUser);
        }
      });

  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private static class Key {
    final String proxyUserName;
    final UserGroupInformation loginUser;

    public Key(String proxyUserName, UserGroupInformation loginUser) {
      super();
      this.proxyUserName = proxyUserName;
      this.loginUser = loginUser;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((loginUser == null) ? 0 : loginUser.hashCode());
      result = prime * result + ((proxyUserName == null) ? 0 : proxyUserName.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Key other = (Key) obj;
      if (loginUser == null) {
        if (other.loginUser != null) {
          return false;
        }
      } else if (!loginUser.equals(other.loginUser)) {
        return false;
      }
      if (proxyUserName == null) {
        if (other.proxyUserName != null) {
          return false;
        }
      } else if (!proxyUserName.equals(other.proxyUserName)) {
        return false;
      }
      return true;
    }
  }
  /**
   * Create and return proxy user {@link org.apache.hadoop.security.UserGroupInformation} of operator owner if operator
   * owner is valid. Otherwise create and return proxy user {@link org.apache.hadoop.security.UserGroupInformation} for
   * query user.
   *
   * @param opUserName Name of the user whom to impersonate while setting up the operator.
   * @param queryUserName Name of the user who issues the query. If <i>opUserName</i> is invalid,
   *                      then this parameter must be valid user name.
   */
  public static UserGroupInformation createProxyUgi(String opUserName, String queryUserName) {
    if (!Strings.isNullOrEmpty(opUserName)) {
      return createProxyUgi(opUserName);
    }

    if (Strings.isNullOrEmpty(queryUserName)) {
      // TODO(DRILL-2097): Tests that use SimpleRootExec have don't assign any query user name in FragmentContext.
      // Disable throwing exception to modifying the long list of test files.
      // throw new DrillRuntimeException("Invalid value for query user name");
      return getProcessUserUGI();
    }

    return createProxyUgi(queryUserName);
  }

  /**
   * Create and return proxy user {@link org.apache.hadoop.security.UserGroupInformation} for given user name.
   *
   * @param proxyUserName Proxy user name (must be valid)
   */
  public static UserGroupInformation createProxyUgi(String proxyUserName) {
    try {
      if (Strings.isNullOrEmpty(proxyUserName)) {
        throw new DrillRuntimeException("Invalid value for proxy user name");
      }

      // If the request proxy user is same as process user name, return the process UGI.
      if (proxyUserName.equals(getProcessUserName())) {
        return getProcessUserUGI();
      }

      return CACHE.get(new Key(proxyUserName, UserGroupInformation.getLoginUser()));
    } catch (IOException | ExecutionException e) {
      final String errMsg = "Failed to create proxy user UserGroupInformation object: " + e.getMessage();
      logger.error(errMsg, e);
      throw new DrillRuntimeException(errMsg, e);
    }
  }

  /**
   * If the given user name is empty, return the current process user name. This is a temporary change to avoid
   * modifying long list of tests files which have GroupScan operator with no user name property.
   * @param userName User name found in GroupScan POP definition.
   */
  public static String resolveUserName(String userName) {
    if (!Strings.isNullOrEmpty(userName)) {
      return userName;
    }
    return getProcessUserName();
  }

  /**
   * Return the name of the user who is running the Drillbit.
   *
   * @return Drillbit process user.
   */
  public static String getProcessUserName() {
    return getProcessUserUGI().getShortUserName();
  }

  /**
   * Return the list of groups to which the process user belongs.
   *
   * @return Drillbit process user group names
   */
  public static String[] getProcessUserGroupNames() {
    return getProcessUserUGI().getGroupNames();
  }

  /**
   * Return the {@link org.apache.hadoop.security.UserGroupInformation} of user who is running the Drillbit.
   *
   * @return Drillbit process user {@link org.apache.hadoop.security.UserGroupInformation}.
   */
  public static UserGroupInformation getProcessUserUGI() {
    try {
      return UserGroupInformation.getLoginUser();
    } catch (IOException e) {
      final String errMsg = "Failed to get process user UserGroupInformation object.";
      logger.error(errMsg, e);
      throw new DrillRuntimeException(errMsg, e);
    }
  }

  /**
   * Create DrillFileSystem for given <i>proxyUserName</i> and configuration.
   *
   * @param proxyUserName Name of the user whom to impersonate while accessing the FileSystem contents.
   * @param fsConf FileSystem configuration.
   */
  public static DrillFileSystem createFileSystem(String proxyUserName, Configuration fsConf) {
    return createFileSystem(createProxyUgi(proxyUserName), fsConf, null);
  }

  /** Helper method to create DrillFileSystem */
  private static DrillFileSystem createFileSystem(UserGroupInformation proxyUserUgi, final Configuration fsConf,
      final OperatorStats stats) {
    DrillFileSystem fs;
    try {
      fs = proxyUserUgi.doAs((PrivilegedExceptionAction<DrillFileSystem>) () -> {
        logger.trace("Creating DrillFileSystem for proxy user: " + UserGroupInformation.getCurrentUser());
        return new DrillFileSystem(fsConf, stats);
      });
    } catch (InterruptedException | IOException e) {
      final String errMsg = "Failed to create DrillFileSystem for proxy user: " + e.getMessage();
      logger.error(errMsg, e);
      throw new DrillRuntimeException(errMsg, e);
    }

    return fs;
  }

  /**
   * Given admin user/group list, finds whether the given username has admin privileges.
   *
   * @param userName User who is checked for administrative privileges.
   * @param adminUsers Comma separated list of admin usernames,
   * @param adminGroups Comma separated list of admin usergroups
   * @return True if the user has admin priveleges. False otherwise.
   */
  public static boolean hasAdminPrivileges(final String userName, final String adminUsers, final String adminGroups) {
    // Process user is by default an admin
    if (getProcessUserName().equals(userName)) {
      return true;
    }

    final Set<String> adminUsersSet = Sets.newHashSet(SPLITTER.split(adminUsers));
    if (adminUsersSet.contains(userName)) {
      return true;
    }

    final UserGroupInformation ugi = createProxyUgi(userName);
    final String[] userGroups = ugi.getGroupNames();
    if (userGroups == null || userGroups.length == 0) {
      return false;
    }

    final Set<String> adminUserGroupsSet = Sets.newHashSet(SPLITTER.split(adminGroups));
    for (String userGroup : userGroups) {
      if (adminUserGroupsSet.contains(userGroup)) {
        return true;
      }
    }

    return false;
  }

  // avoid instantiation
  private ImpersonationUtil() {
  }
}
