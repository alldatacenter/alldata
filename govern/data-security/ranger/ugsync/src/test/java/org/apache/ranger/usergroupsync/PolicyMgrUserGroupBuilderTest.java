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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.usergroupsync;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.unixusersync.process.PolicyMgrUserGroupBuilder;

public class PolicyMgrUserGroupBuilderTest extends PolicyMgrUserGroupBuilder {
        private Set<String> allGroups;
        private Set<String> allUsers;
        private Map<String, Set<String>> groupUsers;
        private Set<String> invalidGroups;
        private Set<String> invalidUsers;

        public PolicyMgrUserGroupBuilderTest() {
                super();
        }

        @Override
        public void init() throws Throwable {
                allGroups = new HashSet<>();
                allUsers = new HashSet<>();
                groupUsers = new HashMap<>();
                invalidGroups = new HashSet<>();
                invalidUsers = new HashSet<>();
        }

        public int getTotalUsers() {
                return allUsers.size();
        }

        public int getTotalGroups() {
                //System.out.println("Groups = " + allGroups);
                return allGroups.size();
        }

        public int getTotalGroupUsers() {
                int totalGroupUsers = 0;
                for (String group : groupUsers.keySet()) {
                        totalGroupUsers += groupUsers.get(group).size();
                }
                return totalGroupUsers;
        }

        public Set<String> getAllGroups() {
                return allGroups;
        }

        public Set<String> getAllUsers() {
                return allUsers;
        }

        public int getTotalInvalidGroups() {
                return invalidGroups.size();
        }

        public int getTotalInvalidUsers() {
                return invalidUsers.size();
        }

        public int getGroupsWithNoUsers() {
                int groupsWithNoUsers = 0;
                for (String group : groupUsers.keySet()) {
                        if (CollectionUtils.isEmpty(groupUsers.get(group))) {
                                groupsWithNoUsers++;
                        }
                }
                return groupsWithNoUsers;
        }

        @Override
        public void addOrUpdateUsersGroups(Map<String, Map<String, String>> sourceGroups,
                                           Map<String, Map<String, String>> sourceUsers,
                                           Map<String, Set<String>> sourceGroupUsers,
                                           boolean computeDeletes) throws Throwable {

                for (String userdn : sourceUsers.keySet()) {
                        //System.out.println("Username: " + sourceUsers.get(userdn).get("original_name"));
                        String username = userNameTransform(sourceUsers.get(userdn).get("original_name"));
                        allUsers.add(username);
                        if (!isValidString(username)) {
                                invalidUsers.add(username);
                        }
                }
                for (String groupdn : sourceGroups.keySet()) {
                        //System.out.println("Groupname: " + sourceGroups.get(groupdn).get("original_name"));
                        String groupname = groupNameTransform(sourceGroups.get(groupdn).get("original_name"));
                        allGroups.add(groupname);
                        if (!isValidString(groupname)) {
                                invalidGroups.add(groupname);
                        }
                }
                groupUsers = sourceGroupUsers;
                //System.out.println("Username: " + user + " and associated groups: " + groups);
        }


}