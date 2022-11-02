package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.BaseController;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.vo.TeslaRoleVO;
import com.alibaba.tesla.authproxy.service.RoleService;
import com.alibaba.tesla.authproxy.service.UserRoleService;
import com.alibaba.tesla.authproxy.service.ao.*;
import com.alibaba.tesla.authproxy.web.input.RoleGetParam;
import com.alibaba.tesla.authproxy.web.input.RoleListParam;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

/**
 * 角色管理 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@Api(tags = "角色 API", description = "角色 API")
@Validated
@Slf4j
public class RoleController extends BaseController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @ApiOperation(value = "获取角色列表")
    @GetMapping("roles")
    @ResponseBody
    public TeslaBaseResult retrieveList(
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
        @RequestHeader(value = "X-Locale", required = false, defaultValue = Constants.DEFAULT_LOCALE)
            String locale,
        @ApiParam(value = "排序列", example = "gmtCreate", allowableValues = "tenantId,roleId,gmtCreate,gmtModified")
        @Pattern(regexp = "^(tenantId|roleId|gmtCreate|gmtModified)$")
        @RequestParam(name = "sortBy", required = false, defaultValue = "gmtCreate")
            String sortBy,
        @ApiParam(value = "排序规则", example = "desc", allowableValues = "asc,desc")
        @Pattern(regexp = "^(asc|desc)$")
        @RequestParam(name = "orderBy", required = false, defaultValue = "desc")
            String orderBy,
        @ApiParam("Filter: roleId")
        @RequestParam(name = "roleId", required = false, defaultValue = "")
            String roleId,
        @ApiParam("Filter: name")
        @RequestParam(name = "name", required = false, defaultValue = "")
            String name,
        @ApiParam(value = "当前页", example = "1")
        @Min(value = 1)
        @RequestParam(name = "page", required = false, defaultValue = "1")
            Integer page,
        @ApiParam(value = "每页大小", example = "20")
        @Min(value = 1)
        @Max(value = 200)
        @RequestParam(name = "pageSize", required = false, defaultValue = "20")
            Integer pageSize,
        @ModelAttribute RoleListParam param) {
        if (!StringUtils.isEmpty(param.getAppId())) {
            appId = param.getAppId();
        }
        RoleListConditionAO condition = RoleListConditionAO.builder()
            .locale(locale)
            .tenantId(tenantId)
            .appId(appId)
            .roleId(roleId)
            .name(name)
            .sortBy(sortBy)
            .orderBy(orderBy)
            .page(page)
            .pageSize(pageSize)
            .build();
        RoleListResultAO data = roleService.list(condition);
        return TeslaResultFactory.buildSucceedResult(data);
    }

    @ApiOperation(value = "获取指定角色详情")
    @GetMapping("roles/{roleId:[a-zA-Z0-9,._\\-|]+}")
    @ResponseBody
    public TeslaBaseResult detail(
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
        @RequestHeader(value = "X-Locale", required = false, defaultValue = Constants.DEFAULT_LOCALE)
            String locale,
        @ApiParam("角色 ID")
        @PathVariable
            String roleId,
        @ModelAttribute RoleGetParam param) {
        if (!StringUtils.isEmpty(param.getAppId())) {
            appId = param.getAppId();
        }
        RoleGetConditionAO condition = RoleGetConditionAO.builder()
            .tenantId(tenantId)
            .appId(appId)
            .locale(locale)
            .roleId(roleId)
            .build();
        RoleGetResultAO data = roleService.get(condition);
        return TeslaResultFactory.buildSucceedResult(data);
    }

    @ApiOperation(value = "创建角色")
    @PostMapping("roles")
    @ResponseBody
    public TeslaBaseResult create(
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
        @RequestBody
            RoleItemAO param) {
        if (StringUtils.isEmpty(param.getAppId())) {
            param.setAppId(appId);
        }
        param.setTenantId(tenantId);
        param.setEmpId(empId);
        roleService.create(param);
        log.info("action=createRole||tenantId={}||appId={}||empId={}||param={}",
            tenantId, appId, empId, TeslaGsonUtil.toJson(param));
        return TeslaResultFactory.buildSucceedResult();
    }

    @ApiOperation(value = "更新角色")
    @PutMapping("roles/{roleId:[a-zA-Z0-9,._:\\-|]+}")
    @ResponseBody
    public TeslaBaseResult update(
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
        @RequestBody
            RoleItemAO param,
        @ApiParam("角色 ID")
        @PathVariable
            String roleId) {
        if (StringUtils.isEmpty(param.getAppId())) {
            param.setAppId(appId);
        }
        param.setTenantId(tenantId);
        param.setEmpId(empId);
        param.setRoleId(roleId);
        roleService.update(param);
        log.info("action=updateRole||tenantId={}||appId={}||empId={}||roleId={}||param={}",
            tenantId, appId, empId, roleId, TeslaGsonUtil.toJson(param));
        return TeslaResultFactory.buildSucceedResult();
    }

    @ApiOperation(value = "删除角色")
    @DeleteMapping("roles/{roleId:[a-zA-Z0-9,._:\\-|]+}")
    @ResponseBody
    public TeslaBaseResult delete(
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
        roleService.delete(tenantId, roleId);
        log.info("action=deleteRole||tenantId={}||empId={}||roleId={}", tenantId, empId, roleId);
        return TeslaResultFactory.buildSucceedResult();
    }

    @ApiOperation(value = "获取角色对应的用户列表")
    @GetMapping("roles/{roleId:[a-zA-Z0-9,._:\\-|]+}/users")
    @ResponseBody
    public TeslaBaseResult users(
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
        @RequestHeader(value = "X-Locale", required = false, defaultValue = Constants.DEFAULT_LOCALE)
            String locale,
        @ApiParam("角色 ID")
        @PathVariable
            String roleId) {
        RoleUserListConditionAO condition = RoleUserListConditionAO.builder()
            .tenantId(tenantId)
            .locale(locale)
            .roleId(roleId)
            .build();
        UserListResultAO data = roleService.listRoleUsers(condition);
        return TeslaResultFactory.buildSucceedResult(data);
    }

    @ApiOperation(value = "新增角色绑定的用户")
    @PostMapping("roles/{roleId:[a-zA-Z0-9,._:\\-|]+}/users")
    @ResponseBody
    public TeslaBaseResult bindUsers(
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
        @RequestHeader(value = "X-Locale", required = false, defaultValue = Constants.DEFAULT_LOCALE)
            String locale,
        @ApiParam("角色 ID")
        @PathVariable
            String roleId,
        @RequestBody
            RoleUserAssigningConditionAO param) {
        RoleUserAssigningConditionAO condition = RoleUserAssigningConditionAO.builder()
            .tenantId(tenantId)
            .locale(locale)
            .roleId(roleId)
            .userIds(param.getUserIds())
            .build();
        roleService.assignUsers(condition);
        return TeslaResultFactory.buildSucceedResult();
    }

    @ApiOperation(value = "获取角色对应的部门列表")
    @GetMapping("roles/{roleId:[a-zA-Z0-9,._:\\-|]+}/departments")
    @ResponseBody
    public TeslaBaseResult departments(
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
        @RequestHeader(value = "X-Locale", required = false, defaultValue = Constants.DEFAULT_LOCALE)
            String locale,
        @ApiParam("角色 ID")
        @PathVariable
            String roleId) {
        RoleDepartmentListConditionAO condition = RoleDepartmentListConditionAO.builder()
            .tenantId(tenantId)
            .locale(locale)
            .roleId(roleId)
            .build();
        RoleDepartmentListResultAO data = roleService.listRoleDepartments(condition);
        return TeslaResultFactory.buildSucceedResult(data);
    }

    /**
     * 批量添加角色 (已废弃)
     */
    @PostMapping(value = "auth/role/batchAdd")
    @ResponseBody
    public TeslaResult addRole(HttpServletRequest request, @RequestBody List<TeslaRoleVO> roles) {
        if (null == roles || roles.size() == 0) {
            return TeslaResultBuilder.errorResult(null, "Empty roles");
        }
        Map<String, Object> ret = this.authPolicy.getAuthServiceManager().addRoles(this.getLoginUser(request), roles);
        return TeslaResultBuilder.successResult(ret);
    }

    /**
     * 查询某个 app 下的所有角色信息 (已废弃)
     */
    @GetMapping(value = "auth/role/all")
    @ResponseBody
    public TeslaResult getAllRoles(HttpServletRequest request, @RequestParam String appId) {
        if (null == appId || appId.length() == 0) {
            return TeslaResultBuilder.errorResult(null, "Empty appId");
        }
        List<TeslaRoleVO> appRoles = this.authPolicy.getAuthServiceManager()
            .getAllRoles(this.getLoginUser(request), appId);
        return TeslaResultBuilder.successResult(appRoles);
    }
}
