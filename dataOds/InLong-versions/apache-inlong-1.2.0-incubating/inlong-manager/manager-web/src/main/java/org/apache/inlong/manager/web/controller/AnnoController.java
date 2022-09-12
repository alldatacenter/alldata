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
import org.apache.inlong.manager.common.beans.Response;
import org.apache.inlong.manager.common.pojo.user.LoginUser;
import org.apache.inlong.manager.common.pojo.user.UserDetail;
import org.apache.inlong.manager.common.pojo.user.UserInfo;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.inlong.manager.service.core.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anno controller.
 */
@Slf4j
@RestController
@RequestMapping("/anno")
@Api(tags = "User - Anno (No Auth)")
public class AnnoController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    public Response<String> login(@RequestBody LoginUser loginUser) {

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(loginUser.getUsername(), loginUser.getPassword());
        subject.login(token);
        LoginUserUtils.setUserLoginInfo((UserDetail) subject.getPrincipal());

        return Response.success("success");
    }

    @PostMapping("/doRegister")
    public Response<Boolean> doRegister(@RequestBody UserInfo userInfo) {
        userInfo.checkValid();
        return Response.success(userService.create(userInfo));
    }

    @GetMapping("/logout")
    public Response<String> logout() {
        SecurityUtils.getSubject().logout();
        return Response.success("success");
    }

}
