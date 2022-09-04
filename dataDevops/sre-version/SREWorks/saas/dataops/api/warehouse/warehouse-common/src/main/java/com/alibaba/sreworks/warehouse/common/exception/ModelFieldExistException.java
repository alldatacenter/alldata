package com.alibaba.sreworks.warehouse.common.exception;

public class ModelFieldExistException extends Exception {
    public ModelFieldExistException(String message) {
        super(message);
    }

    public ModelFieldExistException(Throwable cause) {
        super(cause);
    }
}
