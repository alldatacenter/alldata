package com.alibaba.sreworks.warehouse.common.exception;

/**
 * ES索引异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/05/08 16:41
 */
public class ESIndexExistException extends Exception {
    public ESIndexExistException(String message) {
        super(message);
    }

    public ESIndexExistException(Throwable cause) {
        super(cause);
    }
}
