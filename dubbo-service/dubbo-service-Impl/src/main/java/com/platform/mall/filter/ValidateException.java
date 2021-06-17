package com.platform.mall.filter;


/**
 * @author wlhbdp
 * @ClassName：CustomExceptionFilter
 * @Description：自定义异常
 */

public class ValidateException extends RuntimeException {
    public ValidateException(String msg) {
        super(msg);
    }
}

