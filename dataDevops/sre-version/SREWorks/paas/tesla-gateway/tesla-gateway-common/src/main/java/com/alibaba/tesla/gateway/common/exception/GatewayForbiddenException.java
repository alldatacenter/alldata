package com.alibaba.tesla.gateway.common.exception;

/**
 * 无权限异常
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class GatewayForbiddenException extends GatewayException {

    public GatewayForbiddenException() {
    }

    public GatewayForbiddenException(int code, String message) {
        super(code, message);
    }

    public GatewayForbiddenException(String message) {
        super(message);
    }
}
