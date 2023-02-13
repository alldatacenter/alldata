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
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.user.UserLoginRequest;
import org.apache.inlong.manager.pojo.user.UserRequest;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.apache.inlong.manager.service.user.UserService;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anno controller, such as login, register, etc.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Api(tags = "User-Anno-API")
public class AnnoController {

    @Autowired
    private UserService userService;

    @PostMapping("/anno/login")
    public Response<Boolean> login(@Validated @RequestBody UserLoginRequest loginRequest) {
        userService.login(loginRequest);
        return Response.success(true);
    }

    @PostMapping("/anno/register")
    public Response<Integer> register(@Validated @RequestBody UserRequest request) {
        String currentUser = LoginUserUtils.getLoginUser().getName();
        return Response.success(userService.save(request, currentUser));
    }

    @GetMapping("/anno/logout")
    public Response<Boolean> logout() {
        SecurityUtils.getSubject().logout();
        return Response.success(true);
    }

}
