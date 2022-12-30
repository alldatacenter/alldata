package com.alibaba.sreworks.warehouse.common.exception;

/**
 * 实体不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/05/08 16:41
 */
public class EntityNotExistException extends Exception {
    public EntityNotExistException(String message) {
        super(message);
    }

    public EntityNotExistException(Throwable cause) {
        super(cause);
    }
}
