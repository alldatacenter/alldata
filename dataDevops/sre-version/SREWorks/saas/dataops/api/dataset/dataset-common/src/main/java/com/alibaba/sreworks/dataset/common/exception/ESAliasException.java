package com.alibaba.sreworks.dataset.common.exception;

/**
 * ES索引别名异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/11 09:56
 */
public class ESAliasException extends Exception {
    public ESAliasException(String message) {
        super(message);
    }

    public ESAliasException(Throwable cause) {
        super(cause);
    }

    public ESAliasException(String message, Throwable cause) {
        super(message, cause);
    }
}
