package com.alibaba.tesla.authproxy.web;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.constants.ErrorCode;
import com.alibaba.tesla.authproxy.exceptions.ClientUserArgsException;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyNoPermissionException;
import com.alibaba.tesla.authproxy.service.PermissionService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.common.base.TeslaResultFactory;
import com.alibaba.tesla.common.base.exception.ClientException;
import com.alibaba.tesla.web.controller.BaseController;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;

/**
 * Permission 检查 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RestController
@RequestMapping("permission/gateway")
@Slf4j
public class PermissionGatewayController extends BaseController {

    private static final String LOG_PRE = "[" + PermissionGatewayController.class.getSimpleName() + "] ";

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private PermissionService permissionService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TeslaBaseResult checkGatewayPermission(@RequestBody @Valid CheckGatewayPermissionParam param,
                                                  BindingResult validation)
        throws ClientException, ClientUserArgsException {
        String username = getUserName();
        String userId = getUserId();
        String appId = getAppId();
        log.info(LOG_PRE + "Received check gateway permission request. param={}, username={}, userId={}, appId={}",
            TeslaGsonUtil.toJson(param), username, userId, appId);

        if (StringUtils.isEmpty(userId)) {
            throw new ClientUserArgsException("X-Auth-UserId is required in header");
        }
        checkValidationBindingResult(validation);

        // 针对 Standalone aliyun 环境，直接放行
        if (Constants.ENVIRONMENT_STANDALONE.equals(authProperties.getEnvironment())
            && Arrays.asList(authProperties.getAdminUsers().split(",")).contains(userId)) {
            return TeslaResultFactory.buildSucceedResult();
        }

        String requestUri = param.getRequestUri().replace(":", "").replace("*", "");
        String permissionName = authProperties.getPermissionPrefix() + Constants.PERMISSION_GATEWAY + requestUri;
        try {
            permissionService.checkPermission(appId, userId, permissionName);
        } catch (AuthProxyNoPermissionException e) {
            return buildResult(ErrorCode.FORBIDDEN, e.getMessage(), ErrorCode.EMPTY_OBJ);
        }
        return buildSucceedResult(ErrorCode.EMPTY_OBJ);
    }
}

@Data
class CheckGatewayPermissionParam {

    /**
     * 请求 URI 定位
     */
    @NotEmpty(message = "requestUri is required")
    private String requestUri;
}
