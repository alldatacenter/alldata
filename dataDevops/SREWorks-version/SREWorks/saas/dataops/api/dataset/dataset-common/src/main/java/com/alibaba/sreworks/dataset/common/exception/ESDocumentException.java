package com.alibaba.sreworks.dataset.common.exception;

/**
 * ES索引异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/11 09:56
 */
public class ESDocumentException extends Exception {
    public ESDocumentException(String message) {
        super(message);
    }

    public ESDocumentException(Throwable cause) {
        super(cause);
    }

    public ESDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
