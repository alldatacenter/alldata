package com.alibaba.tesla.authproxy.exceptions;

import com.alibaba.tesla.authproxy.constants.ErrorCode;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class ClientException extends TeslaBaseException {

    /**
     * 无具体出错分类的客户端错误
     */
    public ClientException(String message) {
        super(ErrorCode.CLIENT_ERROR, message);
    }

    public ClientException(int code, String message) {
        super(code, message);
    }

    /**
     * Slot for further extension with data info
     */
    public ClientException(int code, String message, Object data) {
        super(code, message, data);
    }
}