package com.alibaba.tesla.gateway.server.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaAuthException extends ResponseStatusException {

    private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

    @Setter
    @Getter
    private String errorType;

    public TeslaAuthException(HttpStatus status) {
        super(status);
    }

    public TeslaAuthException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public TeslaAuthException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }

    public TeslaAuthException(String errorMsg){
        super(HTTP_STATUS, errorMsg);
    }
}
