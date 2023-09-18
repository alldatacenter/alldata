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

import datart.core.mappers.ext.RelRoleResourceMapperExt;
import datart.core.mappers.ext.RoleMapperExt;
import datart.core.mappers.ext.UserMapperExt;
import datart.security.manager.PermissionDataCache;
import org.apache.shiro.realm.Realm;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfiguration {

    @Bean
    public Realm realm(UserMapperExt userMapper,
                       RoleMapperExt roleMapper,
                       RelRoleResourceMapperExt rrrMapper,
                       PermissionDataCache permissionDataCache,
                       PasswordCredentialsMatcher passwordCredentialsMatcher) {
        return new DatartRealm(userMapper, roleMapper, rrrMapper, permissionDataCache, passwordCredentialsMatcher);
    }


}