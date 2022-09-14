package com.alibaba.tesla.authproxy.lib.exceptions;

/**
 * 专有云未登录异常
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAuthNotLogin extends AuthProxyException {

    public PrivateAuthNotLogin() {
    }

    public PrivateAuthNotLogin(String message) {
        super(message);
    }

}
