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


import datart.core.base.consts.TenantManagementMode;
import datart.core.base.exception.Exceptions;
import datart.core.common.Application;
import datart.core.entity.Organization;
import datart.core.entity.ext.RoleBaseInfo;
import datart.core.entity.ext.UserBaseInfo;
import datart.security.exception.PermissionDeniedException;
import datart.server.base.dto.InviteMemberResponse;
import datart.server.base.dto.OrganizationBaseInfo;
import datart.server.base.params.CheckNameParam;
import datart.server.base.params.OrgCreateParam;
import datart.server.base.params.OrgUpdateParam;
import datart.server.base.dto.ResponseData;
import datart.server.service.OrgService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Api
@RestController
@RequestMapping(value = "/orgs")
public class OrgController extends BaseController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }


    @ApiOperation(value = "get organization detail")
    @GetMapping("/{orgId}")
    public ResponseData<OrganizationBaseInfo> getOrganizationDetail(@PathVariable String orgId) {
        checkBlank(orgId, "orgId");
        return ResponseData.success(orgService.getOrgDetail(orgId));
    }

    @ApiOperation(value = "List user organizations")
    @GetMapping
    public ResponseData<List<OrganizationBaseInfo>> listOrganizations() {
        return ResponseData.success(orgService.listOrganizations());
    }

    @ApiOperation(value = "create organization")
    @PostMapping
    public ResponseData<Organization> createOrganization(@Validated @RequestBody OrgCreateParam createParam) {
        if (Application.getCurrMode().equals(TenantManagementMode.TEAM)) {
            Exceptions.tr(PermissionDeniedException.class, "message.provider.execute.operation.denied");
        }
        return ResponseData.success(orgService.createOrganization(createParam));
    }


    @ApiOperation(value = "update a organization")
    @PutMapping(value = "/{orgId}")
    public ResponseData<Boolean> updateOrganization(@PathVariable String orgId,
                                                    @Validated @RequestBody OrgUpdateParam orgUpdateParam) {
        return ResponseData.success(orgService.updateOrganization(orgUpdateParam));
    }


    @ApiOperation(value = "get organization members")
    @GetMapping(value = "/{orgId}/members")
    public ResponseData<List<UserBaseInfo>> listOrgMembers(@PathVariable String orgId) {
        checkBlank(orgId, "orgId");
        return ResponseData.success(orgService.listOrgMembers(orgId));
    }


    @ApiOperation(value = "get organization roles")
    @GetMapping(value = "/{orgId}/roles")
    public ResponseData<List<RoleBaseInfo>> listOrgRoles(@PathVariable String orgId) {
        checkBlank(orgId, "orgId");
        return ResponseData.success(orgService.listOrgRoles(orgId));
    }


    @ApiOperation(value = "add  members to organization")
    @PostMapping(value = "/{orgId}/invite")
    public ResponseData<InviteMemberResponse> inviteMembers(@PathVariable String orgId,
                                                            @RequestBody Set<String> emails,
                                                            @RequestParam boolean sendMail) {
        return ResponseData.success(orgService.addMembers(orgId, emails, sendMail));
    }

    @ApiOperation(value = "Confirm the invitation")
    @GetMapping(value = "/invite/confirm")
    public ResponseData<Boolean> confirmInvite(@RequestParam String token) {
        checkBlank(token, "token");
        return ResponseData.success(orgService.confirmInvite(token));
    }


    @ApiOperation(value = "Delete a organization member")
    @DeleteMapping(value = "/{orgId}/members/{memberId}")
    public ResponseData<Boolean> deleteOrgMember(@PathVariable String orgId,
                                                 @PathVariable String memberId) {
        checkBlank(orgId, "orgId");
        checkBlank(memberId, "memberId");
        return ResponseData.success(orgService.removeUser(orgId, memberId));
    }

    @ApiOperation(value = "list user roles")
    @GetMapping(value = "/{orgId}/members/{memberId}/roles")
    public ResponseData<List<RoleBaseInfo>> getMemberRoles(@PathVariable String orgId,
                                                           @PathVariable String memberId) {
        checkBlank(orgId, "orgId");
        checkBlank(memberId, "memberId");
        return ResponseData.success(orgService.listUserRoles(orgId, memberId));
    }


    @ApiOperation(value = "delete a organization")
    @DeleteMapping("/{orgId}")
    public ResponseData<Boolean> deleteOrganization(@PathVariable String orgId) {
        checkBlank(orgId, "orgId");
        return ResponseData.success(orgService.deleteOrganization(orgId));
    }

    @ApiOperation(value = "Check organization name")
    @PostMapping("/check/name")
    public ResponseData<Boolean> checkOrgName(@Validated @RequestBody CheckNameParam param) {
        Organization organization = new Organization();
        organization.setName(param.getName());
        return ResponseData.success(orgService.checkUnique(organization));
    }


}
