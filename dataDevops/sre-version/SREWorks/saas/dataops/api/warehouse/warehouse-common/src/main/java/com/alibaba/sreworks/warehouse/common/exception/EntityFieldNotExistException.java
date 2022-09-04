package com.alibaba.sreworks.warehouse.common.exception;

public class EntityFieldNotExistException extends Exception {
    public EntityFieldNotExistException(String message) {
        super(message);
    }

    public EntityFieldNotExistException(Throwable cause) {
        super(cause);
    }
}
