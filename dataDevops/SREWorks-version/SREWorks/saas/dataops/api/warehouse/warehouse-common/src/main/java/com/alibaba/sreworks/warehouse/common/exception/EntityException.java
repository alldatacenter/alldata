package com.alibaba.sreworks.warehouse.common.exception;

public class EntityException extends Exception {
    public EntityException(String message) {
        super(message);
    }

    public EntityException(Throwable cause) {
        super(cause);
    }
}
