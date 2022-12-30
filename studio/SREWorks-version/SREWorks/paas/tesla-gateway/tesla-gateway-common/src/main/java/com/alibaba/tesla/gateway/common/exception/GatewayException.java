package com.alibaba.tesla.gateway.common.exception;

public class GatewayException extends RuntimeException {

    private int code;

    private String message;

    public GatewayException() {
        super();
    }

    public GatewayException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public GatewayException(String message) {
        super(message);
    }
}
