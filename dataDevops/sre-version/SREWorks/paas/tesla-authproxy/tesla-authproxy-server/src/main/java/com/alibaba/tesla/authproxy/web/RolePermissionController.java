package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.service.RolePermissionService;
import com.alibaba.tesla.authproxy.service.ao.RolePermissionItemAO;
import com.alibaba.tesla.authproxy.service.ao.RolePermissionListConditionAO;
import com.alibaba.tesla.authproxy.service.ao.RolePermissionListResultAO;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 角色权限 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@Api(tags = "角色权限 API", description = "角色权限 API")
@RequestMapping("roles/{roleId:[a-zA-Z0-9,._:\\-|]+}/permissions")
@Validated
@Slf4j
public class RolePermissionController extends BaseController {

    @Autowired
    private RolePermissionService rolePermissionService;

    @ApiOperation(value = "获取指定角色的权限列表")
    @GetMapping
    @ResponseBody
    public TeslaBaseResult getRolePermissions(
        @ApiParam("租户 ID")
        @NotEmpty
        @RequestHeader(value = "X-Biz-Tenant", required = false, defaultValue = Constants.DEFAULT_TENANT_ID)
            String tenantId,
        @ApiParam("应用 ID")
        @NotEmpty
        @RequestHeader(value = "X-Biz-App")
            String appId,
        @ApiParam("当前用户")
        @NotEmpty
        @RequestHeader(value = "X-EmpId")
            String empId,
        @ApiParam("角色 ID")
        @PathVariable
            String roleId) {
        RolePermissionListConditionAO condition = RolePermissionListConditionAO.builder()
            .tenantId(tenantId)
            .roleId(roleId)
            .build();
        RolePermissionListResultAO data = rolePermissionService.list(condition);
        log.info("action=getRolePermissions||tenantId={}||empId={}||roleId={}||data={}",
            tenantId, empId, roleId, TeslaGsonUtil.toJson(data));
        return TeslaResultFactory.buildSucceedResult(data);
    }

    @ApiOperation(value = "为指定角色增加权限")
    @PostMapping(value = "{permissionId:[a-zA-Z0-9,.*_:\\-|]+}")
    @ResponseBody
    public TeslaBaseResult addRolePermission(
        @ApiParam("租户 ID")
        @NotEmpty
        @RequestHeader(value = "X-Biz-Tenant", required = false, defaultValue = Constants.DEFAULT_TENANT_ID)
            String tenantId,
        @ApiParam("当前用户")
        @NotEmpty
        @RequestHeader(value = "X-EmpId")
            String empId,
        @ApiParam("角色 ID")
        @PathVariable
            String roleId,
        @ApiParam("权限 ID")
        @PathVariable
            String permissionId,
        @RequestBody
        RolePermissionParam param) {
        rolePermissionService.create(RolePermissionItemAO.builder()
            .tenantId(tenantId)
            .roleId(roleId)
            .resourcePath(permissionId)
            .serviceCode(param.getServiceCode())
            .build());
        log.info("action=addRolePermission||tenantId={}||empId={}||roleId={}||permissionId={}||param={}",
            tenantId, empId, roleId, permissionId, TeslaGsonUtil.toJson(param));
        return TeslaResultFactory.buildSucceedResult();
    }

    @ApiOperation(value = "为指定角色删除权限")
    @DeleteMapping(value = "{permissionId:[a-zA-Z0-9,.*_:\\-|]+}")
    @ResponseBody
    public TeslaBaseResult delRolePermission(
        @ApiParam("租户 ID")
        @NotEmpty
        @RequestHeader(value = "X-Biz-Tenant", required = false, defaultValue = Constants.DEFAULT_TENANT_ID)
            String tenantId,
        @ApiParam("当前用户")
        @NotEmpty
        @RequestHeader(value = "X-EmpId")
            String empId,
        @ApiParam("角色 ID")
        @PathVariable
            String roleId,
        @ApiParam("权限 ID")
        @PathVariable
            String permissionId) {
        rolePermissionService.delete(tenantId, roleId, permissionId);
        log.info("action=deleteRolePermission||tenantId={}||empId={}||roleId={}||permissionId={}",
            tenantId, empId, roleId, permissionId);
        return TeslaResultFactory.buildSucceedResult();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePermissionParam {

        private String serviceCode;
    }
}
