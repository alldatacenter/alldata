package com.alibaba.tesla.authproxy.lib.exceptions;

/**
 * 数据库方式未登录异常
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class DataBaseAuthNotLogin extends AuthProxyException {

    public DataBaseAuthNotLogin() {
    }

    public DataBaseAuthNotLogin(String message) {
        super(message);
    }

}
