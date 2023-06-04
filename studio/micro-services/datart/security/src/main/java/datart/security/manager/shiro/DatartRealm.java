/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.security.manager.shiro;

import datart.core.entity.RelRoleResource;
import datart.core.entity.Role;
import datart.core.entity.User;
import datart.core.mappers.ext.RelRoleResourceMapperExt;
import datart.core.mappers.ext.RoleMapperExt;
import datart.core.mappers.ext.UserMapperExt;
import datart.security.base.RoleType;
import datart.security.manager.PermissionDataCache;
import datart.security.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.List;

@Slf4j
public class DatartRealm extends AuthorizingRealm {

    private final UserMapperExt userMapper;

    private final RoleMapperExt roleMapper;

    private final RelRoleResourceMapperExt rrrMapper;

    private final PermissionDataCache permissionDataCache;

    private final PasswordCredentialsMatcher passwordCredentialsMatcher;


    public DatartRealm(UserMapperExt userMapper,
                       RoleMapperExt roleMapper,
                       RelRoleResourceMapperExt rrrMapper,
                       PermissionDataCache permissionDataCache, PasswordCredentialsMatcher passwordCredentialsMatcher) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.rrrMapper = rrrMapper;
        this.permissionDataCache = permissionDataCache;
        this.passwordCredentialsMatcher = passwordCredentialsMatcher;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return true;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo authorizationInfo = permissionDataCache.getAuthorizationInfo();

        if (authorizationInfo != null) {
            return authorizationInfo;
        }

        String userId = ((User) principals.getPrimaryPrincipal()).getId();

        authorizationInfo = new SimpleAuthorizationInfo();
        List<Role> userRoles = roleMapper.selectByOrgAndUser(permissionDataCache.getCurrentOrg(), userId);
        for (Role role : userRoles) {
            if (role.getType().equals(RoleType.ORG_OWNER.name())) {
                addOrgOwnerRoleAndPermission(authorizationInfo, role);
            }
        }
        List<RelRoleResource> relRoleResources = rrrMapper.listByOrgAndUser(permissionDataCache.getCurrentOrg(), userId);
        for (RelRoleResource rrr : relRoleResources) {
            authorizationInfo.addStringPermissions(ShiroSecurityManager
                    .toShiroPermissionString(rrr.getOrgId(), rrr.getRoleId(), rrr.getResourceType(), rrr.getResourceId(), rrr.getPermission()));
        }
        permissionDataCache.setAuthorizationInfo(authorizationInfo);

        return authorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        SimpleAuthenticationInfo authenticationInfo = permissionDataCache.getAuthenticationInfo();

        if (authenticationInfo != null) {
            return authenticationInfo;
        }

        String username = getUsername(token);
        User user = userMapper.selectByNameOrEmail(username);
        if (user == null)
            return null;
        authenticationInfo = new SimpleAuthenticationInfo(user, user.getPassword(), getName());
        permissionDataCache.setAuthenticationInfo(authenticationInfo);
        return authenticationInfo;
    }

    @Override
    public CredentialsMatcher getCredentialsMatcher() {
        return passwordCredentialsMatcher;
    }

    /**
     * 为用户添加隐式权限
     */
    private void addOrgOwnerRoleAndPermission(SimpleAuthorizationInfo simpleAuthorizationInfo, Role role) {
        //添加组织拥有者角色
        simpleAuthorizationInfo.addRole(ShiroSecurityManager.toShiroRoleString(role.getType(), role.getOrgId()));
        //添加组织拥有者权限
        String allPermission = ShiroSecurityManager.toShiroPermissionString(role.getOrgId(), "*", "*", "*");
        simpleAuthorizationInfo.addStringPermission(allPermission);
    }

    private String getUsername(AuthenticationToken token) {
        if (token instanceof BearerToken) {
            return JwtUtils.toJwtToken((String) token.getPrincipal()).getSubject();
        } else {
            return (String) token.getPrincipal();
        }
    }

}
