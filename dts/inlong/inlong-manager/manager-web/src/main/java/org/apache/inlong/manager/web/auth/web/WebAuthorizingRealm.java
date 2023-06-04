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

package org.apache.inlong.manager.web.auth.web;

import com.google.common.collect.Sets;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.service.user.UserService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.Date;

/**
 * Web user authorization.
 */
public class WebAuthorizingRealm extends AuthorizingRealm {

    private final UserService userService;

    public WebAuthorizingRealm(UserService userService) {
        this.userService = userService;
    }

    /**
     * Login authentication
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();
        UserInfo userInfo = userService.getByName(username);
        Preconditions.expectNotNull(userInfo, "User not exist with name=" + username);
        Preconditions.expectTrue(userInfo.getDueDate().after(new Date()), "User " + username + " was expired");
        userInfo.setRoles(Sets.newHashSet(userInfo.getAccountType() == 0
                ? UserTypeEnum.ADMIN.name()
                : UserTypeEnum.OPERATOR.name()));
        return new SimpleAuthenticationInfo(userInfo, userInfo.getPassword(), getName());
    }

    /**
     * URI access control
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        UserInfo userInfo = (UserInfo) getAvailablePrincipal(principalCollection);
        if (userInfo != null) {
            simpleAuthorizationInfo.setRoles(userInfo.getRoles());
        }
        return simpleAuthorizationInfo;
    }
}
