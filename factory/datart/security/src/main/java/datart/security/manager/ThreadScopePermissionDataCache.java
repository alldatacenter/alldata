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

package datart.security.manager;

import datart.security.base.Permission;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class ThreadScopePermissionDataCache {

    private static ThreadLocal<Map<String, Object>> permissionData;

    private final static String CURRENT_ORG = "currentOrg";

    private final static String PERMISSION_CACHE = "permissionCache";

    private final static String AUTHORIZATION = "authorization";

    private final static String AUTHENTICATION = "authentication";

    @PostConstruct
    public void initData() {
        permissionData = new InheritableThreadLocal<>();
        permissionData.set(new HashMap<>());
    }

    public String getCurrentOrg() {
        return (String) permissionData.get().get(CURRENT_ORG);
    }

    public void setCurrentOrg(String currentOrg) {
        permissionData.get().put(CURRENT_ORG, currentOrg);
    }

    public Boolean getCachedPermission(Permission permission) {
        Map<Permission, Boolean> map = (Map<Permission, Boolean>) permissionData.get().get(PERMISSION_CACHE);
        if (map == null) {
            return null;
        }
        return map.get(permission);
    }

    public void setPermissionCache(Permission permission, Boolean permitted) {
        Map<Permission, Boolean> map = (Map<Permission, Boolean>) permissionData.get().get(PERMISSION_CACHE);
        if (map == null) {
            map = new HashMap<>();
            permissionData.get().put(PERMISSION_CACHE, map);
        }
        map.put(permission, permitted);
    }

    public SimpleAuthorizationInfo getAuthorizationInfo() {
        return (SimpleAuthorizationInfo) permissionData.get().get(AUTHORIZATION);
    }

    public void setAuthorizationInfo(SimpleAuthorizationInfo authorizationInfo) {
        permissionData.get().put(AUTHORIZATION, authorizationInfo);
    }

    public SimpleAuthenticationInfo getAuthenticationInfo() {
        return (SimpleAuthenticationInfo) permissionData.get().get(AUTHENTICATION);
    }

    public void setAuthenticationInfo(SimpleAuthenticationInfo authenticationInfo) {
        permissionData.get().put(AUTHENTICATION, authenticationInfo);
    }

    public void clear() {
        permissionData.get().clear();
    }

}
