/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.user;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.user.TenantRoleInfo;
import org.apache.inlong.manager.pojo.user.TenantRolePageRequest;
import org.apache.inlong.manager.pojo.user.TenantRoleRequest;

import java.util.List;

/**
 * Tenant Role service
 */
public interface TenantRoleService {

    /**
     * List all tenant role by paginating
     */
    PageResult<TenantRoleInfo> listByCondition(TenantRolePageRequest request);

    /**
     * Save one tenant role
     */
    int save(TenantRoleRequest record, String operator);

    /**
     * Update one tenant role
     */
    boolean update(TenantRoleRequest record, String operator);

    /**
     * Get one tenant role by id
     */
    TenantRoleInfo get(int id);

    /**
     * Get one tenant role by name and tenant
     */
    TenantRoleInfo getByUsernameAndTenant(String name, String tenant);

    /**
     * List tenant by given username
     */
    List<String> listTenantByUsername(String username);

    /**
     * Delete tenant role by id
     *
     * @param id tenant role
     * @return true = delete success/ false = delete fail
     */
    Boolean delete(Integer id);

    /**
     * Add user to default tenant
     */
    Integer saveDefault(String username, String operator);
}
