package com.alibaba.tesla.authproxy.exceptions;

/**
 * @author cdx
 * @date 2020/1/2 20:01
 */
public class TeslaJwtException extends Exception {

    public TeslaJwtException() {
    }

    public TeslaJwtException(String s) {
        super(s);
    }

    public TeslaJwtException(Throwable cause) {
        this.initCause(cause);
    }

    public TeslaJwtException(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }
}
