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
import lombok.Data;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequestScope
@Data
public class RequestScopePermissionDataCache {

    private String currentOrg;

    private final Map<Permission, Boolean> permissionCache = new ConcurrentHashMap<>();

    private SimpleAuthorizationInfo authorizationInfo = null;

    private SimpleAuthenticationInfo authenticationInfo = null;

    public Boolean getCachedPermission(Permission permission) {
        return permissionCache.get(permission);
    }

    public void setPermissionCache(Permission permission, Boolean permitted) {
        permissionCache.put(permission, permitted);
    }

    public void clear() {
        authorizationInfo = null;
        authenticationInfo = null;
        currentOrg = null;
        permissionCache.clear();
    }
}
