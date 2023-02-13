package com.alibaba.sreworks.warehouse.common.exception;

/**
 * 实体存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/05/08 16:41
 */
public class ModelExistException extends Exception {
    public ModelExistException(String message) {
        super(message);
    }

    public ModelExistException(Throwable cause) {
        super(cause);
    }
}
