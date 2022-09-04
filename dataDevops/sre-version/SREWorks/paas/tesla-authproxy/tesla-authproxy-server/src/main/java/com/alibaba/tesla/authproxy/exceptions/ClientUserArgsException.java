package com.alibaba.tesla.authproxy.exceptions;

import com.alibaba.tesla.authproxy.constants.ErrorCode;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class ClientUserArgsException extends ClientException {

    public ClientUserArgsException(String message) {
        super(ErrorCode.USER_ARG_ERROR, message);
    }
}