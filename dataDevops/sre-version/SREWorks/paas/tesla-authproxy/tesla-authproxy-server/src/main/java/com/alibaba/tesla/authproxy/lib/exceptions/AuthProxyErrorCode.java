package com.alibaba.tesla.authproxy.lib.exceptions;

/**
 * 全局错误码
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum AuthProxyErrorCode {

    /**
     * 未知错误
     */
    UNKNOWN_ERROR(500, "UNKNOWN_ERROR"),

    /**
     * 用户配置错误
     */
    USER_CONFIG_ERROR(10000, "USER_CONFIG_ERROR"),

    /**
     * 系统尚未准备就绪
     */
    NOT_READY(10001, "NOT_READY"),

    /**
     * 无效的用户参数
     */
    INVALID_USER_ARGS(10002, "INVALID_USER_ARGS"),

    /**
     * 添加重复的项
     */
    DUPLICATE_ENTRY(10003, "DUPLICATE_ENTRY"),

    /**
     * BUC 服务错误
     */
    BUC_SERVER_ERROR(10003, "BUC_SERVER_ERROR");

    private final int code;
    private final String description;

    private AuthProxyErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code + ": " + description;
    }
}
