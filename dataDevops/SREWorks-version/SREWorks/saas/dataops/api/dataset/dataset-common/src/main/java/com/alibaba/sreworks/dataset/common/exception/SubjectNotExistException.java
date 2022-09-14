package com.alibaba.sreworks.dataset.common.exception;

/**
 * 主题不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class SubjectNotExistException extends Exception {
    public SubjectNotExistException(String message) {
        super(message);
    }

    public SubjectNotExistException(Throwable cause) {
        super(cause);
    }

    public SubjectNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
