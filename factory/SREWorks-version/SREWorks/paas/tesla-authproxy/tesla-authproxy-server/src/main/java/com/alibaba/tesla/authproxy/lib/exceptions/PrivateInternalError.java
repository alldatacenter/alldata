package com.alibaba.tesla.authproxy.lib.exceptions;

/**
 * 专有云内部错误
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateInternalError extends AuthProxyException {

    public PrivateInternalError() {
    }

    public PrivateInternalError(String message) {
        super(message);
    }

}
