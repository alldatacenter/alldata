package com.alibaba.sreworks.warehouse.common.exception;

public class DomainException extends Exception {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(Throwable cause) {
        super(cause);
    }
}
