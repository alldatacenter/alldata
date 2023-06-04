/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.controller;


import datart.core.entity.Role;
import datart.core.entity.ext.UserBaseInfo;
import datart.security.base.PermissionInfo;
import datart.security.base.ResourceType;
import datart.security.base.SubjectType;
import datart.server.base.dto.ResourcePermissions;
import datart.server.base.dto.ResponseData;
import datart.server.base.dto.SubjectPermissions;
import datart.server.base.dto.ViewPermission;
import datart.server.base.params.*;
import datart.server.service.RoleService;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = "/roles")
public class RoleController extends BaseController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }


    @ApiOperation(value = "check role name")
    @PostMapping(value = "/check/name")
    public ResponseData<Boolean> checkRoleName(@Validated @RequestBody CheckNameParam param) {
        Role role = new Role();
        role.setName(param.getName());
        role.setOrgId(param.getOrgId());
        boolean success = roleService.checkUnique(role);
        return ResponseData.success(success);
    }


    @ApiOperation(value = "create role")
    @PostMapping()
    public ResponseData<Role> createRole(@Validated @RequestBody RoleCreateParam createParam) {
        return ResponseData.success(roleService.create(createParam));
    }


    @ApiOperation(value = "update a role")
    @PutMapping(value = "/{roleId}")
    public ResponseData<Boolean> updateRole(@PathVariable String roleId,
                                            @Validated @RequestBody RoleUpdateParam roleUpdateParam) {
        checkBlank(roleId, "roleId");
        boolean success = roleService.update(roleUpdateParam);
        return ResponseData.success(success);
    }

    @ApiOperation(value = "delete a role")
    @DeleteMapping("/{roleId}")
    public ResponseData<Boolean> deleteRole(@PathVariable String roleId) {
        checkBlank(roleId, "roleId");
        boolean success = roleService.delete(roleId);
        return ResponseData.success(success);
    }

    @ApiOperation(value = "Update users for role")
    @PutMapping("/{roleId}/users")
    public ResponseData<Boolean> updateUsersForRole(@PathVariable String roleId,
                                                    @RequestBody Set<String> userIds) {
        checkBlank(roleId, "roleId");
        if (userIds == null) userIds = new HashSet<>();
        boolean success = roleService.updateUsersForRole(roleId, userIds);
        return ResponseData.success(success);
    }


    @ApiOperation(value = "Update roles for user")
    @PutMapping("/{userId}/roles")
    public ResponseData<Boolean> updateRolesForUser(@PathVariable String userId,
                                                    @RequestParam String orgId,
                                                    @RequestBody Set<String> roleIds) {
        checkBlank(userId, "userId");
        if (roleIds == null) roleIds = new HashSet<>();
        boolean success = roleService.updateRolesForUser(userId, orgId, roleIds);
        return ResponseData.success(success);
    }

    @ApiOperation(value = "Get role users")
    @GetMapping("/{roleId}/users")
    public ResponseData<List<UserBaseInfo>> listRoleUsers(@PathVariable String roleId) {
        checkBlank(roleId, "roleId");
        List<UserBaseInfo> userBaseInfos = roleService.listRoleUsers(roleId);
        return ResponseData.success(userBaseInfos);
    }


    @ApiOperation(value = "get role/user resource permissions")
    @GetMapping(value = "/permission/subject")
    public ResponseData<SubjectPermissions> getSubjectPermissions(@RequestParam String orgId,
                                                                  @RequestParam SubjectType subjectType,
                                                                  @RequestParam String subjectId) {
        return ResponseData.success(roleService.getSubjectPermissions(orgId, subjectType, subjectId));

    }

    @ApiOperation(value = "get resource permissions")
    @GetMapping(value = "/permission/resource")
    public ResponseData<ResourcePermissions> getResourcePermissions(@RequestParam String orgId,
                                                                    @RequestParam ResourceType resourceType,
                                                                    @RequestParam(required = false) String resourceId) {
        return ResponseData.success(roleService.getResourcePermission(orgId, resourceType, resourceId));

    }

    @ApiOperation(value = "grant org owner to user")
    @PostMapping(value = "/permission/grant/org_owner")
    public ResponseData<Boolean> grantOrgOwner(@RequestParam String orgId,
                                               @RequestParam String userId) {
        return ResponseData.success(roleService.grantOrgOwner(orgId, userId, true));
    }

    @ApiOperation(value = "revoke org owner from user")
    @DeleteMapping(value = "/permission/revoke/org_owner")
    public ResponseData<Boolean> revokeOrgOwner(@RequestParam String orgId,
                                                @RequestParam String userId) {
        return ResponseData.success(roleService.revokeOrgOwner(orgId, userId));
    }

    @ApiOperation(value = "Grant permission to role")
    @PostMapping(value = "/permission/grant")
    public ResponseData<List<PermissionInfo>> grantPermission(@RequestBody GrantPermissionParam grantPermissionParam) {
        return ResponseData.success(roleService.grantPermission(grantPermissionParam));
    }

    @ApiOperation(value = "Grant permission to role")
    @PostMapping(value = "/view_permission/grant")
    public ResponseData<Boolean> grantViewPermission(@RequestBody ViewPermissionParam viewPermissionParam) {
        return ResponseData.success(roleService.grantViewPermission(viewPermissionParam));
    }

    @ApiOperation(value = "list view permissions")
    @GetMapping(value = "/view_permission/view/{viewId}")
    public ResponseData<List<ViewPermission>> listViewPermission(@PathVariable String viewId) {
        return ResponseData.success(roleService.listViewPermission(viewId));
    }

    @ApiOperation(value = "list view permissions for subject")
    @GetMapping(value = "/view_permission/subject/{subjectId}")
    public ResponseData<List<ViewPermission>> listRoleViewPermission(@PathVariable String subjectId,
                                                                     @RequestParam String orgId,
                                                                     @RequestParam SubjectType subjectType) {
        return ResponseData.success(roleService.listRoleViewPermission(orgId, subjectType, subjectId));
    }

}
