package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.service.RolePermissionService;
import com.alibaba.tesla.authproxy.service.ao.PermissionRoleListConditionAO;
import com.alibaba.tesla.authproxy.service.ao.RoleListResultAO;
import com.alibaba.tesla.authproxy.web.input.PermissionGetParam;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 权限 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@Api(tags = "权限 API", description = "权限 API")
@RequestMapping("permissions/{permissionId:[a-zA-Z0-9,._:\\-]+}")
@Validated
@Slf4j
public class PermissionController extends BaseController {

    @Autowired
    private RolePermissionService rolePermissionService;

    @ApiOperation(value = "列出指定权限点的关联角色")
    @GetMapping("roles")
    @ResponseBody
    public TeslaBaseResult getRoles(
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
        @ApiParam("Permission ID")
        @PathVariable
            String permissionId,
        @ModelAttribute PermissionGetParam param) {
        if (!StringUtils.isEmpty(param.getAppId())) {
            appId = param.getAppId();
        }
        PermissionRoleListConditionAO condition = PermissionRoleListConditionAO.builder()
            .tenantId(tenantId)
            .appId(appId)
            .permissionId(permissionId)
            .build();
        RoleListResultAO data = rolePermissionService.listRoleByPermission(condition);
        return TeslaResultFactory.buildSucceedResult(data);
    }
}
