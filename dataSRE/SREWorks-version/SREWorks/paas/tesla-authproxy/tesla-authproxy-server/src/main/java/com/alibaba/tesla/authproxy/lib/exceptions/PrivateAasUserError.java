package com.alibaba.tesla.authproxy.lib.exceptions;

/**
 * 专有云 AAS 用户态异常
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAasUserError extends AuthProxyException {

    public PrivateAasUserError() {
    }

    public PrivateAasUserError(String message) {
        super(message);
    }

}
