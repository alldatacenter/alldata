package com.alibaba.tesla.gateway.server.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaBlackListRejectException extends ResponseStatusException {

    private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN;

    @Setter
    @Getter
    private String errorType;

    public TeslaBlackListRejectException(HttpStatus status) {
        super(status);
    }

    public TeslaBlackListRejectException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public TeslaBlackListRejectException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }

    public TeslaBlackListRejectException(String errorMsg){
        super(HTTP_STATUS, errorMsg);
    }
}
