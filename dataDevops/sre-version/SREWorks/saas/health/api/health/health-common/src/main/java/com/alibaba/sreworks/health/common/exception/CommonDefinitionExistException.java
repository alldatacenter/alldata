package com.alibaba.sreworks.health.common.exception;

/**
 * 定义存在
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class CommonDefinitionExistException extends Exception {
    public CommonDefinitionExistException(String message) {
        super(message);
    }

    public CommonDefinitionExistException(Throwable cause) {
        super(cause);
    }

    public CommonDefinitionExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
