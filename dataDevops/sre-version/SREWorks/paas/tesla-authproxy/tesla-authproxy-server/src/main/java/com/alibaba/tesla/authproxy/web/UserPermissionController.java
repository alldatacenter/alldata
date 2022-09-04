package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.service.UserPermissionService;
import com.alibaba.tesla.authproxy.service.ao.UserPermissionCheckConditionAO;
import com.alibaba.tesla.authproxy.service.ao.UserPermissionCheckResultAO;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.web.controller.BaseController;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户权限 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@Api(tags = "用户权限 API", description = "用户权限 API")
@RequestMapping("users/{userId:[a-zA-Z0-9,._:\\-|]+}")
@Validated
@Slf4j
public class UserPermissionController extends BaseController {

    @Autowired
    private UserPermissionService userPermissionService;

    @ApiOperation(value = "检查指定用户是否拥有指定的权限列表")
    @PostMapping("permissions")
    @ResponseBody
    public TeslaBaseResult checkPermissions(
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
        @ApiParam("实际查询的用户 ID")
        @PathVariable
            String userId,
        @ApiParam("请求参数")
        @RequestBody
            CheckPermissionParam param) {
        if (!StringUtils.isEmpty(param.getAppId())) {
            appId = param.getAppId();
        }
        UserPermissionCheckConditionAO condition = UserPermissionCheckConditionAO.builder()
            .tenantId(tenantId)
            .userId(userId)
            .appId(appId)
            .asRole(param.getAsRole())
            .defaultPermissions(param.getDefaultPermissions())
            .checkPermissions(param.getCheckPermissions())
            .build();
        UserPermissionCheckResultAO data = userPermissionService.checkPermissions(condition);
        log.info("action=checkUserPermission||tenantId={}||appId={}||empId={}||userId={}||param={}||response={}",
            tenantId, appId, empId, userId, TeslaGsonUtil.toJson(param), TeslaGsonUtil.toJson(data));
        return TeslaResultFactory.buildSucceedResult(data);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckPermissionParam {

        private String appId;
        private String asRole;
        private List<String> defaultPermissions = new ArrayList<>();
        private List<String> checkPermissions = new ArrayList<>();
    }
}
