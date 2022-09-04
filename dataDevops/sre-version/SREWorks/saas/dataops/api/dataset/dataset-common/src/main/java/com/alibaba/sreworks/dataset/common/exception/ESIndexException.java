package com.alibaba.sreworks.dataset.common.exception;

/**
 * ES索引异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/11 09:56
 */
public class ESIndexException extends Exception {
    public ESIndexException(String message) {
        super(message);
    }

    public ESIndexException(Throwable cause) {
        super(cause);
    }

    public ESIndexException(String message, Throwable cause) {
        super(message, cause);
    }
}
