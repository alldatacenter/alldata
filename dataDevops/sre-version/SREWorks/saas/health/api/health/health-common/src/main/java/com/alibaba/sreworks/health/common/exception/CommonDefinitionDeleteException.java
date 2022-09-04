package com.alibaba.sreworks.health.common.exception;

/**
 * 定义删除异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class CommonDefinitionDeleteException extends Exception {
    public CommonDefinitionDeleteException(String message) {
        super(message);
    }

    public CommonDefinitionDeleteException(Throwable cause) {
        super(cause);
    }

    public CommonDefinitionDeleteException(String message, Throwable cause) {
        super(message, cause);
    }

}
