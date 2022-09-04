package com.alibaba.sreworks.warehouse.common.exception;

public class ModelFieldException extends Exception {
    public ModelFieldException(String message) {
        super(message);
    }

    public ModelFieldException(Throwable cause) {
        super(cause);
    }
}
