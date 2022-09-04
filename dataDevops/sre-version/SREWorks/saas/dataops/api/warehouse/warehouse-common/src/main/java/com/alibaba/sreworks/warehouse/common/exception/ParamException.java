package com.alibaba.sreworks.warehouse.common.exception;

public class ParamException extends Exception {
    public ParamException(String message) {
        super(message);
    }

    public ParamException(Throwable cause) {
        super(cause);
    }
}
