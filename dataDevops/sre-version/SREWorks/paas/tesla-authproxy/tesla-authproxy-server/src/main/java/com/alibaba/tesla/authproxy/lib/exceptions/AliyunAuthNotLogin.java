package com.alibaba.tesla.authproxy.lib.exceptions;

/**
 * 阿里云未登录异常
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AliyunAuthNotLogin extends AuthProxyException {

    public AliyunAuthNotLogin() {
    }

    public AliyunAuthNotLogin(String message) {
        super(message);
    }

}
