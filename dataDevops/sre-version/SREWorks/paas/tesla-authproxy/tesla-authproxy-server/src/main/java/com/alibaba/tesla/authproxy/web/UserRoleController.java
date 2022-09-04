package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.service.UserRoleService;
import com.alibaba.tesla.authproxy.service.ao.UserRoleItemAO;
import com.alibaba.tesla.authproxy.service.ao.UserRoleListConditionAO;
import com.alibaba.tesla.authproxy.service.ao.UserRoleListResultAO;
import com.alibaba.tesla.authproxy.web.input.UserRoleListParam;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
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

import javax.validation.constraints.Pattern;

/**
 * 用户角色 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@Api(tags = "用户角色 API", description = "用户角色 API")
@RequestMapping("users/{userId:[a-zA-Z0-9,._:\\-|]+}")
@Validated
@Slf4j
public class UserRoleController extends BaseController {

    @Autowired
    private UserRoleService userRoleService;

    @ApiOperation(value = "获取指定用户的角色列表")
    @GetMapping("roles")
    @ResponseBody
    public TeslaBaseResult getUserRoles(
        @ApiParam("租户 ID")
        @NotEmpty
        @RequestHeader(value = "X-Biz-Tenant", required = false, defaultValue = Constants.DEFAULT_TENANT_ID)
            String tenantId,
        @ApiParam("应用 ID")
        @NotEmpty
        @RequestHeader(value = "X-Biz-App")
            String appId,
        @ApiParam("语言")
        @Pattern(regexp = Constants.DEFAULT_LOCALE_REGEX)
        @RequestHeader(value = "X-Locale", required = false, defaultValue = "zh_CN")
            String locale,
        @ApiParam("当前用户")
        @NotEmpty
        @RequestHeader(value = "X-EmpId")
            String empId,
        @ApiParam("实际查询的用户 ID")
        @PathVariable
            String userId,
        @ModelAttribute UserRoleListParam param) {
        if (!StringUtils.isEmpty(param.getAppId())) {
            appId = param.getAppId();
        }
        UserRoleListConditionAO condition = UserRoleListConditionAO.builder()
            .locale(locale)
            .tenantId(tenantId)
            .appId(appId)
            .userId(userId)
            .build();
        UserRoleListResultAO data = userRoleService.list(condition);
        log.info("action=getUserRoles||locale={}||tenantId={}||appId={}||empId={}||userId={}||data={}",
            locale, tenantId, appId, empId, userId, TeslaGsonUtil.toJson(data));
        return TeslaResultFactory.buildSucceedResult(data);
    }

    @ApiOperation(value = "增加用户角色")
    @PostMapping(value = "roles/{roleId:[a-zA-Z0-9,._:\\-|]+}")
    @ResponseBody
    public TeslaBaseResult addUserRoleRel(
        @ApiParam("租户 ID")
        @NotEmpty
        @RequestHeader(value = "X-Biz-Tenant", required = false, defaultValue = Constants.DEFAULT_TENANT_ID)
            String tenantId,
        @ApiParam("当前用户")
        @NotEmpty
        @RequestHeader(value = "X-EmpId")
            String empId,
        @ApiParam("需要操作的用户 ID")
        @PathVariable
            String userId,
        @ApiParam("需要操作的角色 ID")
        @PathVariable
            String roleId) {
        UserRoleItemAO param = UserRoleItemAO.builder()
            .tenantId(tenantId)
            .userId(userId)
            .roleId(roleId)
            .build();
        userRoleService.create(param);
        log.info("action=createUserRole||tenantId={}||empId={}||userId={}||roleId={}", tenantId, empId, userId, roleId);
        return TeslaResultFactory.buildSucceedResult();
    }

    @ApiOperation(value = "删除用户角色")
    @DeleteMapping(value = "roles/{roleId:[a-zA-Z0-9,._:\\-|]+}")
    @ResponseBody
    public TeslaBaseResult delUserRoleRel(
        @ApiParam("租户 ID")
        @NotEmpty
        @RequestHeader(value = "X-Biz-Tenant", required = false, defaultValue = Constants.DEFAULT_TENANT_ID)
            String tenantId,
        @ApiParam("当前用户")
        @NotEmpty
        @RequestHeader(value = "X-EmpId")
            String empId,
        @ApiParam("需要操作的用户 ID")
        @PathVariable
            String userId,
        @ApiParam("需要操作的角色 ID")
        @PathVariable
            String roleId) {
        UserRoleItemAO param = UserRoleItemAO.builder()
            .tenantId(tenantId)
            .userId(userId)
            .roleId(roleId)
            .build();
        userRoleService.delete(param);
        log.info("action=deleteUserRole||tenantId={}||empId={}||userId={}||roleId={}", tenantId, empId, userId, roleId);
        return TeslaResultFactory.buildSucceedResult();
    }
}
