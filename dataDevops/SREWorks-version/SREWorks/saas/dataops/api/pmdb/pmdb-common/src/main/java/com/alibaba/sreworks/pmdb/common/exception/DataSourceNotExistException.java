package com.alibaba.sreworks.pmdb.common.exception;

/**
 * 指标存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class DataSourceNotExistException extends Exception {
    public DataSourceNotExistException(String message) {
        super(message);
    }

    public DataSourceNotExistException(Throwable cause) {
        super(cause);
    }

    public DataSourceNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
