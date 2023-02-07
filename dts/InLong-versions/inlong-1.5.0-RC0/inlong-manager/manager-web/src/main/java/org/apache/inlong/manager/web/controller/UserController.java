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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserRequest;
import org.apache.inlong.manager.pojo.user.UserRoleCode;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.apache.inlong.manager.service.user.UserService;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RestController
@RequestMapping("/api")
@Api(tags = "User-Auth-API")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/user/currentUser")
    @ApiOperation(value = "Get the logged-in user")
    public Response<UserInfo> currentUser() {
        return Response.success(LoginUserUtils.getLoginUser());
    }

    @PostMapping("/user/register")
    @ApiOperation(value = "Register user")
    @RequiresRoles(value = UserRoleCode.ADMIN)
    public Response<Integer> register(@Validated @RequestBody UserRequest userInfo) {
        String currentUser = LoginUserUtils.getLoginUser().getName();
        return Response.success(userService.save(userInfo, currentUser));
    }

    @GetMapping("/user/get/{id}")
    @ApiOperation(value = "Get user info by user id")
    public Response<UserInfo> getById(@PathVariable Integer id) {
        String currentUser = LoginUserUtils.getLoginUser().getName();
        return Response.success(userService.getById(id, currentUser));
    }

    @GetMapping("/user/getByName/{name}")
    @ApiOperation(value = "Get user info by username")
    public Response<UserInfo> getByName(@PathVariable String name) {
        return Response.success(userService.getByName(name));
    }

    @PostMapping("/user/listAll")
    @ApiOperation(value = "List all users")
    public Response<PageResult<UserInfo>> list(@RequestBody UserRequest request) {
        return Response.success(userService.list(request));
    }

    @PostMapping("/user/update")
    @ApiOperation(value = "Update user info")
    public Response<Integer> update(@Validated @RequestBody UserRequest userInfo) {
        String currentUser = LoginUserUtils.getLoginUser().getName();
        return Response.success(userService.update(userInfo, currentUser));
    }

    @DeleteMapping("/user/delete")
    @ApiOperation(value = "Delete user by id")
    @RequiresRoles(value = UserRoleCode.ADMIN)
    public Response<Boolean> delete(@RequestParam("id") Integer id) {
        String currentUser = LoginUserUtils.getLoginUser().getName();
        return Response.success(userService.delete(id, currentUser));
    }

}
