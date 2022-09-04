package com.alibaba.tesla.gateway.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaForbiddenException extends ResponseStatusException {

    private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN;

    public TeslaForbiddenException(String reason) {
        super(HTTP_STATUS, reason);
    }
}
