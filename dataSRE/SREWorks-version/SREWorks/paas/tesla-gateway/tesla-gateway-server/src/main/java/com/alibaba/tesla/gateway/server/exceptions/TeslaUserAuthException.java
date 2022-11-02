package com.alibaba.tesla.gateway.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaUserAuthException extends ResponseStatusException {

    private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

    public TeslaUserAuthException(HttpStatus status) {
        super(status);
    }

    public TeslaUserAuthException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public TeslaUserAuthException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }

    public TeslaUserAuthException(String errorMsg){
        super(HTTP_STATUS, errorMsg);
    }
}
