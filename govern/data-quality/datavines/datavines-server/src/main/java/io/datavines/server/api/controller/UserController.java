/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.api.controller;

import io.datavines.server.api.dto.bo.user.UserLogin;
import io.datavines.server.api.dto.bo.user.UserRegister;
import io.datavines.common.exception.DataVinesException;
import io.datavines.core.constant.DataVinesConstants;
import io.datavines.core.aop.RefreshToken;
import io.datavines.server.repository.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Api(value = "user", tags = "user")
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/user")
@RefreshToken
public class UserController {

    @Autowired
    private UserService userService;

    @ApiOperation(value = "update user info")
    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object update(@RequestBody UserLogin userLogin) throws DataVinesException {
        return null;
    }

    @ApiOperation(value = "reset password")
    @PostMapping(value = "/resetPassword", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object resetPassword(@RequestBody UserRegister userRegister) throws DataVinesException {
        return null;
    }

}
