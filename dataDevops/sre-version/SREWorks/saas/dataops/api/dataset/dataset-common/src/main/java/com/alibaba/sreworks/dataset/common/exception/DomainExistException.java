package com.alibaba.sreworks.dataset.common.exception;

/**
 * 数据模型不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class DomainExistException extends Exception {
    public DomainExistException(String message) {
        super(message);
    }

    public DomainExistException(Throwable cause) {
        super(cause);
    }

    public DomainExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
