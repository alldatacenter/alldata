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

package org.apache.inlong.manager.web.auth.tenant;

import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.tenant.InlongTenantInfo;
import org.apache.inlong.manager.pojo.user.InlongRoleInfo;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.TenantRoleInfo;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.tenant.InlongTenantService;
import org.apache.inlong.manager.service.user.InlongRoleService;
import org.apache.inlong.manager.service.user.TenantRoleService;
import org.apache.inlong.manager.service.user.UserService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;

import java.util.HashSet;
import java.util.Set;

/**
 * Shiro AuthenticatingRealm for tenant
 */
@Slf4j
public class TenantAuthenticatingRealm extends AuthenticatingRealm {

    private TenantRoleService tenantRoleService;
    private InlongRoleService inlongRoleService;
    private UserService userService;

    private InlongTenantService tenantService;

    public TenantAuthenticatingRealm(
            TenantRoleService tenantRoleService,
            InlongRoleService inlongRoleService,
            UserService userService,
            InlongTenantService tenantService) {
        this.tenantRoleService = tenantRoleService;
        this.inlongRoleService = inlongRoleService;
        this.userService = userService;
        this.tenantService = tenantService;
    }

    @Override
    public AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        try {
            TenantToken tenantToken = (TenantToken) token;
            String username = tenantToken.getUsername();
            String tenant = tenantToken.getTenant();

            InlongTenantInfo tenantInfo = tenantService.getByName(tenant);
            if (tenantInfo == null) {
                String errMsg = String.format("tenant=[%s] not found", tenant);
                log.error(errMsg);
                throw new AuthenticationException(errMsg);
            }

            InlongRoleInfo inlongRoleInfo = inlongRoleService.getByUsername(username);
            TenantRoleInfo tenantRoleInfo = tenantRoleService.getByUsernameAndTenant(username, tenant);
            if (inlongRoleInfo == null && tenantRoleInfo == null) {
                String errMsg = String.format("user=[%s] has no privilege for tenant=[%s]", username, tenant);
                log.error(errMsg);
                throw new AuthenticationException(errMsg);
            }

            UserInfo userInfo = getUserInfo(username);
            if (inlongRoleInfo != null) {
                addRole(userInfo, inlongRoleInfo.getRoleCode());
            }

            if (tenantRoleInfo != null) {
                addRole(userInfo, tenantRoleInfo.getRoleCode());
            }

            userInfo.setTenant(tenant);
            return new SimpleAuthenticationInfo(userInfo, tenant, getName());
        } catch (Throwable t) {
            log.error("failed to do tenant authentication", t);
            throw t;
        }
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof TenantToken;
    }

    private UserInfo getUserInfo(String userName) {
        UserInfo userInfo = LoginUserUtils.getLoginUser();
        if (userInfo == null) {
            userInfo = userService.getByName(userName);
        }
        Preconditions.expectNotNull(userInfo, "User doesn't exist");
        return userInfo;
    }

    private void addRole(UserInfo userInfo, String role) {
        Set<String> roleSet = new HashSet<String>() {

            {
                add(role);
            }
        };

        if (CollectionUtils.isEmpty(userInfo.getRoles())) {
            roleSet.addAll(userInfo.getRoles());
        }
        userInfo.setRoles(roleSet);
    }
}
