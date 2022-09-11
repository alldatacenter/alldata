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

package org.apache.inlong.manager.web.auth;

import com.google.common.collect.Sets;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.pojo.user.UserDetail;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.UserEntity;
import org.apache.inlong.manager.service.core.UserService;
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
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
            throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) authenticationToken;
        String username = upToken.getUsername();
        UserEntity userEntity = userService.getByName(username);
        Preconditions.checkNotNull(userEntity, "User doesn't exist");
        Preconditions.checkTrue(userEntity.getDueDate().after(new Date()), "user has expired");
        UserDetail userDetail = new UserDetail();
        userDetail.setUserName(username);
        userDetail.setRoles(Sets.newHashSet(userEntity.getAccountType() == 0
                ? UserTypeEnum.Admin.name() : UserTypeEnum.Operator.name()));
        return new SimpleAuthenticationInfo(userDetail, userEntity.getPassword(), getName());
    }

    /**
     * URI access control
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        UserDetail userDetail = (UserDetail) getAvailablePrincipal(principalCollection);
        if (userDetail != null) {
            simpleAuthorizationInfo.setRoles(userDetail.getRoles());
        }
        return simpleAuthorizationInfo;
    }
}
