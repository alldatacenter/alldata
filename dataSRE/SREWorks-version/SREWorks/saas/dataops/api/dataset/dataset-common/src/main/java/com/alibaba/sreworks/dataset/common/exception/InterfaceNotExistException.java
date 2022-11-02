package com.alibaba.sreworks.dataset.common.exception;

/**
 * 数据接口不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class InterfaceNotExistException extends Exception {
    public InterfaceNotExistException(String message) {
        super(message);
    }

    public InterfaceNotExistException(Throwable cause) {
        super(cause);
    }

    public InterfaceNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
