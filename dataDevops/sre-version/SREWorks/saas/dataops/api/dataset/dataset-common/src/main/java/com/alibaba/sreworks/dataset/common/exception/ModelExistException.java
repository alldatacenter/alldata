package com.alibaba.sreworks.dataset.common.exception;

/**
 * 数据模型不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class ModelExistException extends Exception {
    public ModelExistException(String message) {
        super(message);
    }

    public ModelExistException(Throwable cause) {
        super(cause);
    }

    public ModelExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
