package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyNoPermissionException;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import com.alibaba.tesla.authproxy.service.AuthServiceManager;
import com.alibaba.tesla.authproxy.service.PermissionService;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private static final String LOG_PRE = "[" + PermissionServiceImpl.class.getSimpleName() + "] ";

    @Autowired
    private AuthPolicy authPolicy;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private TeslaUserService teslaUserService;

    /**
     * 检查权限
     *
     * @param appId          APP ID
     * @param username       用户定位符 (对内: loginname, 对外: aliyunPk)
     * @param permissionPath 权限路径
     * @throws AuthProxyNoPermissionException 当无权限时抛出
     */
    @Override
    public void checkPermission(String appId, String username, String permissionPath)
        throws AuthProxyNoPermissionException {
        String environment = authProperties.getEnvironment();
        UserDO user;
        switch (environment) {
            case Constants.ENVIRONMENT_INTERNAL:
                user = teslaUserService.getUserByLoginName(username);
                break;
            default:
                user = teslaUserService.getUserByAliyunPk(username);
                break;
        }
        if (user == null) {
            throw new AuthProxyNoPermissionException(String.format("Cannot find username %s in %s environment",
                username, environment));
        }

        AuthServiceManager authService = authPolicy.getAuthServiceManager();
        boolean result = authService.checkPermission(user, permissionPath, appId);
        if (!result) {
            throw new AuthProxyNoPermissionException("no permission");
        }
    }
}
