package com.alibaba.sreworks.warehouse.common.exception;

/**
 * 实体存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/05/08 16:41
 */
public class ModelException extends Exception {
    public ModelException(String message) {
        super(message);
    }

    public ModelException(Throwable cause) {
        super(cause);
    }
}
