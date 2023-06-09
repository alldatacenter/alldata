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
package org.apache.drill.exec.resourcemgr.config.selectors;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.hadoop.security.UserGroupInformation;

import java.util.List;
import java.util.Set;

/**
 * Evaluates if a query can be admitted to a ResourcePool or not by comparing query user/groups with the
 * configured users/groups policies for this selector. AclSelector can be configured using both long-form syntax or
 * short-form syntax as defined below:
 * <ul>
 *   <li>Long-Form Syntax: Allows to use identifiers to specify allowed and disallowed users/groups. For example:
 *   users: [alice:+, bob:-] means alice is allowed whereas bob is denied access to the pool</li>
 *   <li>Short-Form Syntax: Allows to specify lists of allowed users/groups only. For example: users: [alice, bob]
 *   means only alice and bob are allowed access to this pool</li>
 * </ul>
 * The selector also supports * as a wildcard for both long and short form syntax to allow/deny all users/groups.
 * Example configuration is of form:
 * <code><pre>
 * selector: {
 *   acl: {
 *     users: [alice:+, bob:-],
 *     groups: [sales, marketing]
 *   }
 * }
 * </pre></code>
 */
public class AclSelector extends AbstractResourcePoolSelector {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AclSelector.class);

  private final Set<String> allowedUsers = Sets.newHashSet();

  private final Set<String> allowedGroups = Sets.newHashSet();

  private final Set<String> deniedUsers = Sets.newHashSet();

  private final Set<String> deniedGroups = Sets.newHashSet();

  private final Config aclSelectorValue;

  private static final String ACL_VALUE_GROUPS_KEY = "groups";

  private static final String ACL_VALUE_USERS_KEY = "users";

  private static final String ACL_LONG_SYNTAX_SEPARATOR = ":";

  private static final String ACL_LONG_ALLOWED_IDENTIFIER = "+";

  private static final String ACL_LONG_DISALLOWED_IDENTIFIER = "-";

  private static final String ACL_ALLOW_ALL = "*";


  AclSelector(Config configValue) throws RMConfigException {
    super(SelectorType.ACL);
    this.aclSelectorValue = configValue;
    validateAndParseACL(aclSelectorValue);
  }

  /**
   * Determines if a given query is selected by this ACL selector of a Resource Pool or not. Following rules are
   * followed to evaluate the selection. Assumption: There is an assumption made that if a user or group is configured
   * in both +ve/-ve respective lists then it will be treated to be present in -ve list.
   *
   * Rules:
   * 1) Check if query user is present in -ve users list, If yes then query is not selected else go to 2
   * 2) Check if query user is present in +ve users list, If yes then query is selected else go to 3
   * 3) Check if * is present in -ve users list, if yes then query is not selected else go to 4
   * 4) Check if * is present in +ve users list, if yes then query is selected else go to 5
   * 5) If here that means query user or * is absent in both +ve and -ve users list so check for groups of query user
   * in step 6
   * 6) Check if any of groups of query user is present in -ve groups list, If yes then query is not selected else go
   * to 7
   * 7) Check if any of groups of query user is present in +ve groups list, If yes then query selected else go to 8
   * 8) Check if * is present in -ve groups list, If yes then query is not selected else go to 9
   * 9) Check if * is present in +ve groups list, If yes then query is selected else go to 10
   * 10) Query user and groups of it is neither present is +ve/-ve users list not +ve/-ve groups list hence the query
   * is not selected
   *
   * @param queryContext QueryContext to get information about query user
   * @return true if a query is selected by this selector, false otherwise
   */
  @Override
  public boolean isQuerySelected(QueryContext queryContext) {
    final String queryUser = queryContext.getQueryUserName();
    final UserGroupInformation queryUserUGI = ImpersonationUtil.createProxyUgi(queryUser);
    final Set<String> queryGroups = Sets.newHashSet(queryUserUGI.getGroupNames());
    return checkQueryUserGroups(queryUser, queryGroups);
  }

  @VisibleForTesting
  public boolean checkQueryUserGroups(String queryUser, Set<String> queryGroups) {
    // Check for +ve/-ve users information with query user
    if (deniedUsers.contains(queryUser)) {
      logger.debug("Query user is present in configured ACL -ve users list");
      return false;
    } else if (allowedUsers.contains(queryUser)) {
      logger.debug("Query user is present in configured ACL +ve users list");
      return true;
    } else if (isStarInDisAllowedUsersList()) {
      logger.debug("Query user is absent in configured ACL +ve/-ve users list but * is in -ve users list");
      return false;
    } else if (isStarInAllowedUsersList()) {
      logger.debug("Query user is absent in configured ACL +ve/-ve users list but * is in +ve users list");
      return true;
    }

    // Check for +ve/-ve groups information with groups of query user
    if (Sets.intersection(queryGroups, deniedGroups).size() > 0) {
      logger.debug("Groups of Query user is present in configured ACL -ve groups list");
      return false;
    } else if (Sets.intersection(queryGroups, allowedGroups).size() > 0) {
      logger.debug("Groups of Query user is present in configured ACL +ve groups list");
      return true;
    } else if (isStarInDisAllowedGroupsList()) {
      logger.debug("Groups of Query user is absent in configured ACL +ve/-ve groups list but * is in -ve groups list");
      return false;
    } else if (isStarInAllowedGroupsList()) {
      logger.debug("Groups of Query user is absent in configured ACL +ve/-ve groups list but * is in +ve groups list");
      return true;
    }

    logger.debug("Neither query user or group is present in configured ACL users/groups list");
    return false;
  }

  /**
   * Parses the acl selector config value for users and groups to populate list of allowed/denied users and groups.
   * @param aclConfig Acl config to parse
   * @throws RMConfigException in case of invalid config for either users/groups
   */
  private void validateAndParseACL(Config aclConfig) throws RMConfigException {

    // ACL config doesn't have either group or user list
    if (!aclConfig.hasPath(ACL_VALUE_GROUPS_KEY) && !aclConfig.hasPath(ACL_VALUE_USERS_KEY)) {
      throw new RMConfigException(String.format("ACL Selector config is missing both group and user list information." +
        " Please configure either of groups or users list. [Details: aclConfig: %s]", aclConfig));
    }

    if (aclConfig.hasPath(ACL_VALUE_USERS_KEY)) {
      final List<String> users = aclSelectorValue.getStringList(ACL_VALUE_USERS_KEY);
      parseACLInput(users, allowedUsers, deniedUsers);
    }

    if (aclConfig.hasPath(ACL_VALUE_GROUPS_KEY)) {
      final List<String> groups = aclSelectorValue.getStringList(ACL_VALUE_GROUPS_KEY);
      parseACLInput(groups, allowedGroups, deniedGroups);
    }

    // If no valid configuration is seen for this selector
    if (allowedGroups.size() == 0 && deniedGroups.size() == 0 &&
      deniedUsers.size() == 0 && allowedUsers.size() == 0) {
      throw new RMConfigException("No valid users or groups information is configured for this ACL selector. Either " +
        "use * or valid users/groups");
    }

    // Check if there is any intersection between allowed and disallowed users/groups
    Set<String> wrongConfig = Sets.intersection(allowedUsers, deniedUsers);
    if (wrongConfig.size() > 0) {
      logger.warn("These users are configured both in allowed and disallowed list. They will be treated as disallowed" +
        ". [Details: users: {}]", wrongConfig);
      allowedUsers.removeAll(wrongConfig);
    }

    wrongConfig = Sets.intersection(allowedGroups, deniedGroups);
    if (wrongConfig.size() > 0) {
      logger.warn("These groups are configured both in allowed and disallowed list. They will be treated as " +
        "disallowed. [Details: groups: {}]", wrongConfig);
      allowedGroups.removeAll(wrongConfig);
    }
  }

  public Set<String> getAllowedUsers() {
    return allowedUsers;
  }

  public Set<String> getAllowedGroups() {
    return allowedGroups;
  }

  public Set<String> getDeniedUsers() {
    return deniedUsers;
  }

  public Set<String> getDeniedGroups() {
    return deniedGroups;
  }

  private boolean isStarInAllowedUsersList() {
    return allowedUsers.contains(ACL_ALLOW_ALL);
  }

  private boolean isStarInAllowedGroupsList() {
    return allowedGroups.contains(ACL_ALLOW_ALL);
  }

  private boolean isStarInDisAllowedUsersList() {
    return deniedUsers.contains(ACL_ALLOW_ALL);
  }

  private boolean isStarInDisAllowedGroupsList() {
    return deniedGroups.contains(ACL_ALLOW_ALL);
  }

  private void parseACLInput(List<String> acls, Set<String> allowedIdentity, Set<String> disAllowedIdentity) {
    for (String aclValue : acls) {

      if (aclValue.isEmpty()) {
        continue;
      }
      // Check if it's long form syntax or shortForm syntax
      String[] aclValueSplits = aclValue.split(ACL_LONG_SYNTAX_SEPARATOR);
      if (aclValueSplits.length == 1) {
        // short form
        if (!allowedIdentity.add(aclValueSplits[0])) {
          logger.info("Duplicate acl identity: {} found in configured list will be ignored", aclValueSplits[0]);
        }
      } else {
        // long form
        final String identifier = aclValueSplits[1];
        if (identifier.equals(ACL_LONG_ALLOWED_IDENTIFIER)) {
          if (!allowedIdentity.add(aclValueSplits[0])) {
            logger.info("Duplicate acl identity: {} found in configured list will be ignored", aclValueSplits[0]);
          }
        } else if (identifier.equals(ACL_LONG_DISALLOWED_IDENTIFIER)) {
          if (!disAllowedIdentity.add(aclValueSplits[0])) {
            logger.info("Duplicate acl identity: {} found in configured list will be ignored", aclValueSplits[0]);
          }
        } else {
          logger.error("Invalid long form syntax encountered hence ignoring ACL string {} . Details[Allowed " +
            "identifiers are `+` and `-`. Encountered: {}]", aclValue, identifier);
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{ SelectorType: ").append(super.toString());
    sb.append(", AllowedUsers: [");
    for (String positiveUser : allowedUsers) {
      sb.append(positiveUser).append(", ");
    }
    sb.append("], AllowedGroups: [");
    for (String positiveGroup : allowedGroups) {
      sb.append(positiveGroup).append(", ");
    }
    sb.append("], DisallowedUsers: [");
    for (String negativeUser : deniedUsers) {
      sb.append(negativeUser).append(", ");
    }
    sb.append("], DisallowedGroups: [");
    for (String negativeGroup : deniedGroups) {
      sb.append(negativeGroup).append(", ");
    }
    sb.append("]}");

    return sb.toString();
  }
}
