package com.alibaba.sreworks.warehouse.common.exception;

public class ModelFieldNotExistException extends Exception {
    public ModelFieldNotExistException(String message) {
        super(message);
    }

    public ModelFieldNotExistException(Throwable cause) {
        super(cause);
    }
}
