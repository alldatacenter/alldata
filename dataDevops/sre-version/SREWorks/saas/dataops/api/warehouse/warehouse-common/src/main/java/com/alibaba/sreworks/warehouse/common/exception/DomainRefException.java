package com.alibaba.sreworks.warehouse.common.exception;

public class DomainRefException extends Exception {
    public DomainRefException(String message) {
        super(message);
    }

    public DomainRefException(Throwable cause) {
        super(cause);
    }
}
