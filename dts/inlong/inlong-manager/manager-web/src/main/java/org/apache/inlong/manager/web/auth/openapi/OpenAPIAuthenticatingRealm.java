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

package org.apache.inlong.manager.web.auth.openapi;

import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.common.util.AESUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.service.user.UserService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;

import java.util.Date;

/**
 * Open api client authorization.
 */
@Slf4j
public class OpenAPIAuthenticatingRealm extends AuthenticatingRealm {

    private final UserService userService;

    public OpenAPIAuthenticatingRealm(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get open api authentication info
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
            throws AuthenticationException {
        SecretToken upToken = (SecretToken) authenticationToken;
        String username = upToken.getSecretId();
        UserInfo userInfo = userService.getByName(username);
        Preconditions.expectNotNull(userInfo, "User doesn't exist");
        Preconditions.expectTrue(userInfo.getDueDate().after(new Date()), "user has expired");
        try {
            String secretKey = new String(
                    AESUtils.decryptAsString(userInfo.getSecretKey(), userInfo.getEncryptVersion()));
            userInfo.setRoles(Sets.newHashSet(userInfo.getAccountType() == 0
                    ? UserTypeEnum.ADMIN.name()
                    : UserTypeEnum.OPERATOR.name()));
            return new SimpleAuthenticationInfo(userInfo, secretKey, getName());
        } catch (Exception e) {
            log.error("decrypt secret key fail: ", e);
            throw new AuthenticationException("internal error: " + e.getMessage());
        }
    }

    public boolean supports(AuthenticationToken token) {
        return token instanceof SecretToken;
    }

}
