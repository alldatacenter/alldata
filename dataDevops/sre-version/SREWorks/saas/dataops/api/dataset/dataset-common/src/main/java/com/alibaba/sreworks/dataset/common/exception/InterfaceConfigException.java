package com.alibaba.sreworks.dataset.common.exception;

/**
 * 数据接口配置异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class InterfaceConfigException extends Exception {
    public InterfaceConfigException(String message) {
        super(message);
    }

    public InterfaceConfigException(Throwable cause) {
        super(cause);
    }

    public InterfaceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
