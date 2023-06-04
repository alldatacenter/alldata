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

package org.apache.inlong.manager.web.controller.openapi;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.common.validation.SaveValidation;
import org.apache.inlong.manager.common.validation.UpdateByIdValidation;
import org.apache.inlong.manager.common.validation.UpdateValidation;
import org.apache.inlong.manager.pojo.cluster.BindTagRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;
import org.apache.inlong.manager.pojo.cluster.ClusterPageRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagPageRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagResponse;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.operationlog.OperationLog;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * Open InLong Cluster controller
 */
@RestController
@RequestMapping("/openapi")
@Api(tags = "Open-Cluster-API")
public class OpenInLongClusterController {

    @Autowired
    private InlongClusterService clusterService;

    @GetMapping(value = "/cluster/tag/get/{id}")
    @ApiOperation(value = "Get cluster tag by id")
    @ApiImplicitParam(name = "id", value = "Cluster ID", dataTypeClass = Integer.class, required = true)
    public Response<ClusterTagResponse> getTag(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "tag id cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.getTag(id, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/cluster/tag/list")
    @ApiOperation(value = "List cluster tags")
    public Response<List<ClusterTagResponse>> listTag(@RequestBody ClusterTagPageRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.listTag(request, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/cluster/tag/save")
    @ApiOperation(value = "Save cluster tag")
    @OperationLog(operation = OperationType.CREATE)
    public Response<Integer> saveTag(@Validated(SaveValidation.class) @RequestBody ClusterTagRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.saveTag(request, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/cluster/tag/update")
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update cluster tag")
    public Response<Boolean> updateTag(@Validated(UpdateValidation.class) @RequestBody ClusterTagRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.updateTag(request, LoginUserUtils.getLoginUser()));
    }

    @DeleteMapping(value = "/cluster/tag/delete/{id}")
    @ApiOperation(value = "Delete cluster tag by id")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParam(name = "id", value = "Cluster tag ID", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> deleteTag(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "tag id cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.deleteTag(id, LoginUserUtils.getLoginUser()));
    }

    @GetMapping(value = "/cluster/get/{id}")
    @ApiOperation(value = "Get cluster by id")
    @ApiImplicitParam(name = "id", value = "Cluster ID", dataTypeClass = Integer.class, required = true)
    public Response<ClusterInfo> get(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "cluster id cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.get(id, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/cluster/list")
    @ApiOperation(value = "List clusters")
    public Response<List<ClusterInfo>> list(@RequestBody ClusterPageRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.list(request, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/cluster/save")
    @ApiOperation(value = "Save cluster")
    @OperationLog(operation = OperationType.CREATE)
    public Response<Integer> save(@Validated(SaveValidation.class) @RequestBody ClusterRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.save(request, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/cluster/update")
    @ApiOperation(value = "Update cluster")
    @OperationLog(operation = OperationType.UPDATE)
    public Response<Boolean> update(@Validated(UpdateByIdValidation.class) @RequestBody ClusterRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.update(request, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/cluster/bindTag")
    @ApiOperation(value = "Bind or unbind cluster tag")
    @OperationLog(operation = OperationType.UPDATE)
    public Response<Boolean> bindTag(@Validated @RequestBody BindTagRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.bindTag(request, LoginUserUtils.getLoginUser()));
    }

    @DeleteMapping(value = "/cluster/delete/{id}")
    @ApiOperation(value = "Delete cluster by id")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParam(name = "id", value = "Cluster ID", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> delete(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "cluster id cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.delete(id, LoginUserUtils.getLoginUser()));
    }

    @GetMapping(value = "/cluster/node/get/{id}")
    @ApiOperation(value = "Get cluster node by id")
    @ApiImplicitParam(name = "id", value = "Cluster node ID", dataTypeClass = Integer.class, required = true)
    public Response<ClusterNodeResponse> getNode(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "Cluster node id cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.getNode(id, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/cluster/node/list")
    @ApiOperation(value = "List cluster nodes")
    public Response<List<ClusterNodeResponse>> listNode(@RequestBody ClusterPageRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.listNode(request, LoginUserUtils.getLoginUser()));
    }

    @GetMapping(value = "/cluster/node/listByGroupId")
    @ApiOperation(value = "List cluster nodes by groupId, clusterType and protocolType")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "inlongGroupId", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "clusterType", dataTypeClass = String.class, required = true),
            @ApiImplicitParam(name = "protocolType", dataTypeClass = String.class, required = false)
    })
    @OperationLog(operation = OperationType.GET)
    public Response<List<ClusterNodeResponse>> listByGroupId(@RequestParam String inlongGroupId,
            @RequestParam String clusterType, @RequestParam(required = false) String protocolType) {
        Preconditions.expectNotBlank(inlongGroupId, ErrorCodeEnum.INVALID_PARAMETER, "inlongGroupId cannot be blank");
        Preconditions.expectNotBlank(clusterType, ErrorCodeEnum.INVALID_PARAMETER, "clusterType cannot be blank");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.listNodeByGroupId(inlongGroupId,
                clusterType, protocolType, LoginUserUtils.getLoginUser()));
    }

    @PostMapping(value = "/cluster/node/save")
    @ApiOperation(value = "Save cluster node")
    @OperationLog(operation = OperationType.CREATE)
    public Response<Integer> saveNode(@Validated @RequestBody ClusterNodeRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.saveNode(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/cluster/node/update", method = RequestMethod.POST)
    @OperationLog(operation = OperationType.UPDATE)
    @ApiOperation(value = "Update cluster node")
    public Response<Boolean> updateNode(@Validated(UpdateValidation.class) @RequestBody ClusterNodeRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.INVALID_PARAMETER, "request cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.updateNode(request, LoginUserUtils.getLoginUser()));
    }

    @RequestMapping(value = "/cluster/node/delete/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete cluster node")
    @OperationLog(operation = OperationType.DELETE)
    @ApiImplicitParam(name = "id", value = "Cluster node ID", dataTypeClass = Integer.class, required = true)
    public Response<Boolean> deleteNode(@PathVariable Integer id) {
        Preconditions.expectNotNull(id, ErrorCodeEnum.INVALID_PARAMETER, "cluster id cannot be null");
        Preconditions.expectNotNull(LoginUserUtils.getLoginUser(), ErrorCodeEnum.LOGIN_USER_EMPTY);
        return Response.success(clusterService.deleteNode(id, LoginUserUtils.getLoginUser()));
    }

}
