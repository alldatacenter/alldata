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

import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.user.InlongRoleInfo;
import org.apache.inlong.manager.pojo.user.InlongRolePageRequest;
import org.apache.inlong.manager.pojo.user.InlongRoleRequest;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.UserRoleCode;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.user.InlongRoleService;

import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Api(tags = "INLONG-USER-API")
public class InlongRoleController {

    @Autowired
    private InlongRoleService inlongRoleService;

    @RequestMapping(value = "/role/inlong/get/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get inlong role by ID")
    @RequiresRoles(logical = Logical.OR, value = {UserRoleCode.INLONG_ADMIN, UserRoleCode.INLONG_OPERATOR})
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    public Response<InlongRoleInfo> get(@PathVariable int id) {
        return Response.success(inlongRoleService.get(id));
    }

    @RequestMapping(value = "/role/inlong/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @RequiresRoles(value = UserRoleCode.INLONG_ADMIN)
    @ApiOperation(value = "Save inlong role")
    public Response<Integer> save(@Validated @RequestBody InlongRoleRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(inlongRoleService.save(request, operator));
    }

    @RequestMapping(value = "/role/inlong/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @RequiresRoles(value = UserRoleCode.INLONG_ADMIN)
    @ApiOperation(value = "Update inlong role")
    public Response<Boolean> update(@Validated @RequestBody InlongRoleRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(inlongRoleService.update(request, operator));
    }

    @RequestMapping(value = "/role/inlong/list", method = RequestMethod.POST)
    @ApiOperation(value = "List inlong roles by paginating")
    @RequiresRoles(logical = Logical.OR, value = {UserRoleCode.INLONG_ADMIN, UserRoleCode.INLONG_OPERATOR})
    public Response<PageInfo<InlongRoleInfo>> listByCondition(@RequestBody InlongRolePageRequest request) {
        return Response.success(inlongRoleService.listByCondition(request));
    }
}
