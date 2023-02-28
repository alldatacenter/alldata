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

import io.datavines.core.constant.DataVinesConstants;
import io.datavines.core.aop.RefreshToken;
import io.datavines.server.api.dto.bo.workspace.InviteUserIntoWorkspace;
import io.datavines.server.api.dto.bo.workspace.RemoveUserOutWorkspace;
import io.datavines.server.api.dto.bo.workspace.WorkSpaceCreate;
import io.datavines.server.api.dto.bo.workspace.WorkSpaceUpdate;
import io.datavines.server.repository.service.WorkSpaceService;
import io.datavines.core.exception.DataVinesServerException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Api(value = "workspace", tags = "workspace", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping(value = DataVinesConstants.BASE_API_PATH + "/workspace", produces = MediaType.APPLICATION_JSON_VALUE)
@RefreshToken
public class WorkSpaceController {

    @Autowired
    private WorkSpaceService workSpaceService;

    @ApiOperation(value = "create workspace")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object createWorkSpace(@RequestBody WorkSpaceCreate workSpaceCreate) throws DataVinesServerException {
        return workSpaceService.insert(workSpaceCreate);
    }

    @ApiOperation(value = "update workspace")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object updateWorkSpace(@RequestBody WorkSpaceUpdate workSpaceUpdate) throws DataVinesServerException {
        return workSpaceService.update(workSpaceUpdate)>0;
    }

    @ApiOperation(value = "delete workspace")
    @DeleteMapping(value = "/{id}")
    public Object deleteWorkSpace(@PathVariable Long id)  {
        return workSpaceService.deleteById(id);
    }

    @ApiOperation(value = "list workspace by user id")
    @GetMapping(value = "list")
    public Object listByUserId()  {
        return workSpaceService.listByUserId();
    }

    @ApiOperation(value = "invite user into workspace")
    @PostMapping(value = "/inviteUser",consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object inviteUserIntoWorkspace(@RequestBody InviteUserIntoWorkspace inviteUserIntoWorkspace) throws DataVinesServerException {
        return workSpaceService.inviteUserIntoWorkspace(inviteUserIntoWorkspace);
    }

    @ApiOperation(value = "user removed workspace")
    @DeleteMapping(value = "/removeUser",consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object removeUser(@RequestBody RemoveUserOutWorkspace removeUserOutWorkspace)  {
        return workSpaceService.removeUser(removeUserOutWorkspace);
    }

    @ApiOperation(value = "list user by workspace id")
    @GetMapping(value = "/userPage")
    public Object listUserByWorkspaceId(@RequestParam("workspaceId") Long workspaceId,
                                        @RequestParam("pageNumber") Integer pageNumber,
                                        @RequestParam("pageSize") Integer pageSize)  {
        return workSpaceService.listUserByWorkspaceId(workspaceId,pageNumber,pageSize);
    }

}
