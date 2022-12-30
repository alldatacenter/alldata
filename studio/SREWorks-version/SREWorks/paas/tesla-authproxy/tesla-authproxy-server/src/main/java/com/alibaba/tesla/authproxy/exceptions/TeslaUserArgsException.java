package com.alibaba.tesla.authproxy.exceptions;

import com.alibaba.tesla.authproxy.constants.ErrorCode;

import java.util.Map;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaUserArgsException extends TeslaBaseException {

    public TeslaUserArgsException(Map<String, String> errorMessages) {
        super(ErrorCode.VALIDATION_ERROR, "Validation Error", errorMessages);
    }
}