package com.alibaba.tesla.authproxy.lib.exceptions;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证错误 Exception
 * <p>
 * 用于在运行时发现用户提供的参数有误，进行二次验证报错
 */
public class PrivateValidationError extends AuthProxyException {

    private Map<String, String> errorMap;

    public PrivateValidationError() {
        errorMap = new HashMap<>(1);
    }

    public PrivateValidationError(String field, String message) {
        this();
        errorMap.put(field, message);
    }

    public void add(String field, String message) {
        errorMap.put(field, message);
    }

    public Map<String, String> getErrorMap() {
        return errorMap;
    }

}
