package com.alibaba.sreworks.warehouse.common.exception;

public class EntityFieldExistException extends Exception {
    public EntityFieldExistException(String message) {
        super(message);
    }

    public EntityFieldExistException(Throwable cause) {
        super(cause);
    }
}
