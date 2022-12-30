package com.alibaba.sreworks.dataset.common.exception;

/**
 * 数据接口已经存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class InterfaceExistException extends Exception {
    public InterfaceExistException(String message) {
        super(message);
    }

    public InterfaceExistException(Throwable cause) {
        super(cause);
    }

    public InterfaceExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
