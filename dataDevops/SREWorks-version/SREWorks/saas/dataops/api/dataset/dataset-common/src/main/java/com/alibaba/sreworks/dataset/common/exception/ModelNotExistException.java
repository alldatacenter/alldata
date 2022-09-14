package com.alibaba.sreworks.dataset.common.exception;

/**
 * 数据模型不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class ModelNotExistException extends Exception {
    public ModelNotExistException(String message) {
        super(message);
    }

    public ModelNotExistException(Throwable cause) {
        super(cause);
    }

    public ModelNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
