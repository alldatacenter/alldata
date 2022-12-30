package com.alibaba.sreworks.warehouse.common.exception;

public class DomainNotExistException extends Exception {
    public DomainNotExistException(String message) {
        super(message);
    }

    public DomainNotExistException(Throwable cause) {
        super(cause);
    }
}
