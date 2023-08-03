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

package org.apache.ranger.plugin.store;

import java.util.List;

import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.SearchFilter;

public interface RoleStore {

    void             init() throws Exception;

    RangerRole       createRole(RangerRole role, Boolean createNonExistUserGroup) throws Exception;

    RangerRole       updateRole(RangerRole role, Boolean createNonExistUserGroup) throws Exception;

    void             deleteRole(String roleName) throws Exception;

    void             deleteRole(Long roleId) throws Exception;

    RangerRole       getRole(Long id) throws Exception;

    RangerRole       getRole(String name) throws Exception;

    List<RangerRole> getRoles(SearchFilter filter) throws Exception;

    List<String>     getRoleNames(SearchFilter filter) throws Exception;

    RangerRoles getRoles(String serviceName, Long lastKnownRoleVersion) throws Exception;

    Long getRoleVersion(String serviceName);

    boolean roleExists(Long id) throws  Exception;

    boolean roleExists(String name) throws Exception;
}

