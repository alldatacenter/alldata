package com.alibaba.sreworks.health.common.exception;

/**
 * 定义不存在
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class CommonDefinitionNotExistException extends Exception {
    public CommonDefinitionNotExistException(String message) {
        super(message);
    }

    public CommonDefinitionNotExistException(Throwable cause) {
        super(cause);
    }

    public CommonDefinitionNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
