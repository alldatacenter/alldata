package com.alibaba.tesla.appmanager.common.exception;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class AddonOperateException extends RuntimeException {
    public AddonOperateException(String message) {
        super(message);
    }

    public AddonOperateException(Throwable cause) {
        super(cause);
    }
}
