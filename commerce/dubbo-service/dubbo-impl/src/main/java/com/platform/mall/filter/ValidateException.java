package com.platform.mall.filter;


/**
 * @author wulinhao
 * @ClassName：CustomExceptionFilter
 * @Description：自定义异常
 */

public class ValidateException extends RuntimeException {
    public ValidateException(String msg) {
        super(msg);
    }
}

