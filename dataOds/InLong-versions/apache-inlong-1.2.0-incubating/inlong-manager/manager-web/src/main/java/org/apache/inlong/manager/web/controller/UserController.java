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

package org.apache.inlong.manager.web.controller;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.pojo.user.PasswordChangeRequest;
import org.apache.inlong.manager.common.pojo.user.UserDetail;
import org.apache.inlong.manager.common.pojo.user.UserDetailListVO;
import org.apache.inlong.manager.common.pojo.user.UserDetailPageRequest;
import org.apache.inlong.manager.common.pojo.user.UserInfo;
import org.apache.inlong.manager.common.pojo.user.UserRoleCode;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.inlong.manager.service.core.UserService;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User related interface
 */
@RestController
@RequestMapping("/user")
@Api(tags = "User - Auth")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/loginUser")
    @ApiOperation(value = "Get the logged-in user")
    public Response<UserDetail> currentUser() {
        return Response.success(LoginUserUtils.getLoginUserDetail());
    }

    @PostMapping("/register")
    @ApiOperation(value = "Register user")
    @RequiresRoles(value = UserRoleCode.ADMIN)
    public Response<Boolean> register(@RequestBody UserInfo userInfo) {
        userInfo.checkValid();
        return Response.success(userService.create(userInfo));
    }

    @GetMapping("/get/{id}")
    @ApiOperation(value = "Get user info")
    public Response<UserInfo> getById(@PathVariable Integer id) {
        return Response.success(userService.getById(id));
    }

    @PostMapping("/update")
    @ApiOperation(value = "Update user info")
    public Response<Integer> update(@RequestBody UserInfo userInfo) {
        String currentUser = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(userService.update(userInfo, currentUser));
    }

    @PostMapping("/updatePassword")
    @ApiOperation(value = "Update user password")
    public Response<Integer> updatePassword(@RequestBody PasswordChangeRequest request) {
        return Response.success(userService.updatePassword(request));
    }

    @GetMapping("/listAllUsers")
    @ApiOperation(value = "List all users")
    public Response<PageInfo<UserDetailListVO>> list(UserDetailPageRequest request) {
        return Response.success(userService.list(request));
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "Delete user by id")
    @RequiresRoles(value = UserRoleCode.ADMIN)
    public Response<Boolean> delete(@RequestParam("id") Integer id) {
        String currentUser = LoginUserUtils.getLoginUserDetail().getUserName();
        return Response.success(userService.delete(id, currentUser));
    }

}
