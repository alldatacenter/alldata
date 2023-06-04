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
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.validation.SaveValidation;
import org.apache.inlong.manager.common.validation.UpdateByIdValidation;
import org.apache.inlong.manager.common.validation.UpdateByKeyValidation;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodePageRequest;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.user.UserRoleCode;
import org.apache.inlong.manager.service.node.DataNodeService;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.user.LoginUserUtils;
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
 * Data node controller
 */
@RestController
@RequestMapping("/api")
@Api(tags = "Data-Node-API")
public class DataNodeController {

    @Autowired
    private DataNodeService dataNodeService;

    @PostMapping(value = "/node/save")
    @ApiOperation(value = "Save node")
    @OperationLog(operation = OperationType.CREATE)
    @RequiresRoles(value = UserRoleCode.ADMIN)
    public Response<Integer> save(@Validated(SaveValidation.class) @RequestBody DataNodeRequest request) {
        String currentUser = LoginUserUtils.getLoginUser().getName();
        return Response.success(dataNodeService.save(request, currentUser));
    }

    @GetMapping(value = "/node/get/{id}")
    @ApiOperation(value = "Get node by id")
    @ApiImplicitParam(name = "id", value = "Data node ID", dataTypeClass = Integer.class, required = true)
    public Response<DataNodeInfo> get(@PathVariable Integer id) {
        String currentUser = LoginUserUtils.getLoginUser().getName();
        return Response.success(dataNodeService.get(id, currentUser));
    }

    @PostMapping(value = "/node/list")
    @ApiOperation(value = "List data node")
    public Response<PageResult<DataNodeInfo>> list(@RequestBody DataNodePageRequest request) {
        request.setCurrentUser(LoginUserUtils.getLoginUser().getName());
        request.setIsAdminRole(LoginUserUtils.getLoginUser().getRoles().contains(UserTypeEnum.ADMIN.name()));
        return Response.success(dataNodeService.list(request));
    }

    @PostMapping(value = "/node/update")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update data node")
    public Response<Boolean> update(@Validated(UpdateByIdValidation.class) @RequestBody DataNodeRequest request) {
        String username = LoginUserUtils.getLoginUser().getName();
        return Response.success(dataNodeService.update(request, username));
    }

    @PostMapping(value = "/node/updateByKey")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update data node by key")
    public Response<UpdateResult> updateByKey(
            @Validated(UpdateByKeyValidation.class) @RequestBody DataNodeRequest request) {
        String username = LoginUserUtils.getLoginUser().getName();
        return Response.success(dataNodeService.updateByKey(request, username));
    }

    @DeleteMapping(value = "/node/delete/{id}")
    @ApiOperation(value = "Delete data node by id")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParam(name = "id", value = "Data node ID", dataTypeClass = Integer.class, required = true)
    @RequiresRoles(value = UserRoleCode.ADMIN)
    public Response<Boolean> delete(@PathVariable Integer id) {
        return Response.success(dataNodeService.delete(id, LoginUserUtils.getLoginUser().getName()));
    }

    @DeleteMapping(value = "/node/deleteByKey")
    @ApiOperation(value = "Delete data node by key")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "Data node name", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "type", value = "Data node type", dataTypeClass = String.class, required = true)
    })
    @RequiresRoles(value = UserRoleCode.ADMIN)
    public Response<Boolean> deleteByKey(@RequestParam String name, @RequestParam String type) {
        return Response.success(dataNodeService.deleteByKey(name, type,
                LoginUserUtils.getLoginUser().getName()));
    }

    @PostMapping("/node/testConnection")
    @ApiOperation(value = "Test connection for data node")
    public Response<Boolean> testConnection(@RequestBody DataNodeRequest request) {
        return Response.success(dataNodeService.testConnection(request));
    }

}
