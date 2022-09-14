package com.alibaba.sreworks.warehouse.common.exception;

public class EntityFieldException extends Exception {
    public EntityFieldException(String message) {
        super(message);
    }

    public EntityFieldException(Throwable cause) {
        super(cause);
    }
}
