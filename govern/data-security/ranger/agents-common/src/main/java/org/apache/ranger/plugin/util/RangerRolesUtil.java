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

package org.apache.ranger.plugin.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerRole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RangerRolesUtil {
    private final long                     roleVersion;
    private final Map<String, Set<String>> userRoleMapping = new HashMap<>();
    private final Map<String, Set<String>> groupRoleMapping = new HashMap<>();
    private final Map<String, Set<String>> roleRoleMapping  = new HashMap<>();

    private final Map<String, Set<String>> roleToUserMapping = new HashMap<>();
    private final Map<String, Set<String>> roleToGroupMapping = new HashMap<>();

    private RangerRoles                    roles            = null;
    public  enum  ROLES_FOR {USER, GROUP, ROLE}

    public RangerRolesUtil(RangerRoles roles) {
        if (roles != null) {
            this.roles  = roles;
            roleVersion = roles.getRoleVersion() != null ? roles.getRoleVersion() : -1;

            if (CollectionUtils.isNotEmpty(roles.getRangerRoles())) {
                for (RangerRole role : roles.getRangerRoles()) {
                    Set<RangerRole> containedRoles = getAllContainedRoles(roles.getRangerRoles(), role);

                    buildMap(userRoleMapping, role, containedRoles, ROLES_FOR.USER);
                    buildMap(groupRoleMapping, role, containedRoles, ROLES_FOR.GROUP);
                    buildMap(roleRoleMapping, role, containedRoles, ROLES_FOR.ROLE);

                    Set<String> roleUsers  = new HashSet<>();
                    Set<String> roleGroups = new HashSet<>();

                    addMemberNames(role.getUsers(), roleUsers);
                    addMemberNames(role.getGroups(), roleGroups);

                    for (RangerRole containedRole : containedRoles) {
                        addMemberNames(containedRole.getUsers(), roleUsers);
                        addMemberNames(containedRole.getGroups(), roleGroups);
                    }

                    roleToUserMapping.put(role.getName(), roleUsers);
                    roleToGroupMapping.put(role.getName(), roleGroups);
                }

            }
        } else {
            roleVersion = -1L;
        }
    }

    public long getRoleVersion() { return roleVersion; }

    public RangerRoles getRoles() {
        return this.roles;
    }

    public Map<String, Set<String>> getUserRoleMapping() {
        return this.userRoleMapping;
    }

    public Map<String, Set<String>> getGroupRoleMapping() {
        return this.groupRoleMapping;
    }

    public Map<String, Set<String>> getRoleRoleMapping() {
        return this.roleRoleMapping;
    }

    public Map<String, Set<String>> getRoleToUserMapping() {
        return this.roleToUserMapping;
    }

    public Map<String, Set<String>> getRoleToGroupMapping() {
        return this.roleToGroupMapping;
    }

    private Set<RangerRole> getAllContainedRoles(Set<RangerRole> roles, RangerRole role) {
        Set<RangerRole> allRoles = new HashSet<>();

        allRoles.add(role);
        addContainedRoles(allRoles, roles, role);

        return allRoles;
    }

    private void addContainedRoles(Set<RangerRole> allRoles, Set<RangerRole> roles, RangerRole role) {
        List<RangerRole.RoleMember> roleMembers = role.getRoles();

        for (RangerRole.RoleMember roleMember : roleMembers) {
            RangerRole containedRole = getContainedRole(roles, roleMember.getName());

            if (containedRole!= null && !allRoles.contains(containedRole)) {
                allRoles.add(containedRole);
                addContainedRoles(allRoles, roles, containedRole);
            }
        }
    }

    private void buildMap(Map<String, Set<String>> map, RangerRole role, Set<RangerRole> containedRoles, ROLES_FOR roleFor) {
        buildMap(map, role, role.getName(), roleFor);

        for (RangerRole containedRole : containedRoles) {
            buildMap(map, containedRole, role.getName(), roleFor);
        }
    }

    private void buildMap(Map<String, Set<String>> map, RangerRole role, String roleName, ROLES_FOR roles_for) {
        List<RangerRole.RoleMember> userOrGroupOrRole = null;
        switch(roles_for) {
            case USER:
                userOrGroupOrRole = role.getUsers();
                break;
            case GROUP:
                userOrGroupOrRole = role.getGroups();
                break;
            case ROLE:
                userOrGroupOrRole = role.getRoles();
                break;
        }
        if (CollectionUtils.isNotEmpty(userOrGroupOrRole)) {
            getRoleMap(map, roleName, userOrGroupOrRole);
        }
    }

    private void getRoleMap(Map<String, Set<String>> map, String roleName, List<RangerRole.RoleMember> userOrGroupOrRole) {
        for (RangerRole.RoleMember roleMember : userOrGroupOrRole) {
            if (StringUtils.isNotEmpty(roleMember.getName())) {
                Set<String> roleNames = map.computeIfAbsent(roleMember.getName(), k -> new HashSet<>());
                roleNames.add(roleName);
            }
        }
    }

    private RangerRole getContainedRole(Set<RangerRole> roles, String role) {
        return (roles.stream().filter(containedRole -> role.equals(containedRole.getName())).findAny().orElse(null));
    }

    private void addMemberNames(List<RangerRole.RoleMember> members, Set<String> names) {
        for (RangerRole.RoleMember member : members) {
            names.add(member.getName());
        }
    }
}


