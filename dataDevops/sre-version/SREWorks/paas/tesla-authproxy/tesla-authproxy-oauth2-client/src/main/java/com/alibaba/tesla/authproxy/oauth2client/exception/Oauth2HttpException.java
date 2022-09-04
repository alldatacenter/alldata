package com.alibaba.tesla.authproxy.oauth2client.exception;

/**
 * @author cdx
 * @date 2020/2/25 9:59
 */
public class Oauth2HttpException extends Exception{
    public Oauth2HttpException() {
    }

    public Oauth2HttpException(String message) {
        super(message);
    }

    public Oauth2HttpException(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }
}
