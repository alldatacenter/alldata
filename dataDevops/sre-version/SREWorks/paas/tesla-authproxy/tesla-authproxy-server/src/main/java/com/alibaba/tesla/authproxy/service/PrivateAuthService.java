package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateAuthForbidden;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateAuthNotLogin;
import com.alibaba.tesla.authproxy.model.UserDO;

/**
 * 专有云验证服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface PrivateAuthService {

    void interceptFirstLogin(UserDO userDo) throws PrivateAuthForbidden;

    UserDO checkAccountManagerPermission(UserDO userDo) throws PrivateAuthForbidden,
            PrivateAuthNotLogin, AuthProxyThirdPartyError;

}
