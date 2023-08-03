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
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.TenantRoleInfo;
import org.apache.inlong.manager.pojo.user.TenantRolePageRequest;
import org.apache.inlong.manager.pojo.user.TenantRoleRequest;
import org.apache.inlong.manager.pojo.user.UserRoleCode;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.user.TenantRoleService;

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
@Api(tags = "Tenant-API")
public class InlongTenantRoleController {

    @Autowired
    private TenantRoleService tenantRoleService;

    @RequestMapping(value = "/role/tenant/get/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get tenant role by ID")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    @RequiresRoles(logical = Logical.OR, value = {UserRoleCode.TENANT_ADMIN, UserRoleCode.TENANT_OPERATOR})
    public Response<TenantRoleInfo> get(@PathVariable int id) {
        return Response.success(tenantRoleService.get(id));
    }

    @RequestMapping(value = "/role/tenant/save", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Save tenant role")
    @RequiresRoles(logical = Logical.OR, value = {UserRoleCode.TENANT_ADMIN, UserRoleCode.INLONG_ADMIN})
    public Response<Integer> save(@Validated @RequestBody TenantRoleRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(tenantRoleService.save(request, operator));
    }

    @RequestMapping(value = "/role/tenant/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.CREATE)
    @ApiOperation(value = "Update tenant role")
    @RequiresRoles(logical = Logical.OR, value = {UserRoleCode.TENANT_ADMIN, UserRoleCode.INLONG_ADMIN})
    public Response<Boolean> update(@Validated @RequestBody TenantRoleRequest request) {
        String operator = LoginUserUtils.getLoginUser().getName();
        return Response.success(tenantRoleService.update(request, operator));
    }

    @RequestMapping(value = "/role/tenant/list", method = RequestMethod.POST)
    @ApiOperation(value = "List tenant roles by paginating")
    public Response<PageResult<TenantRoleInfo>> listByCondition(@RequestBody TenantRolePageRequest request) {
        return Response.success(tenantRoleService.listByCondition(request));
    }

    @RequestMapping(value = "/role/tenant/delete/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete tenant role by ID")
    @ApiImplicitParam(name = "id", dataTypeClass = Integer.class, required = true)
    @RequiresRoles(logical = Logical.OR, value = {UserRoleCode.TENANT_ADMIN, UserRoleCode.INLONG_ADMIN})
    public Response<Boolean> delete(@PathVariable int id) {
        return Response.success(tenantRoleService.delete(id));
    }
}
