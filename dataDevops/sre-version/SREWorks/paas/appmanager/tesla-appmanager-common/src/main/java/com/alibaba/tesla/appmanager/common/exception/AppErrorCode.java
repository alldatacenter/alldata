package com.alibaba.tesla.appmanager.common.exception;

/**
 * 全局错误码
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum AppErrorCode {

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
     * 无效的参数
     */
    INVALID_USER_ARGS(10002, "INVALID_USER_ARGS"),

    /**
     * 网络错误
     */
    NETWORK_ERROR(10003, "NETWORK_ERROR"),

    /**
     * IO 错误
     */
    IO_ERROR(10004, "IO_ERROR"),

    /**
     * 命令执行错误
     */
    COMMAND_ERROR(10005, "COMMAND_ERROR"),

    /**
     * 远端存储错误
     */
    STORAGE_ERROR(10006, "STORAGE_ERROR"),

    /**
     * 状态机错误
     */
    STATE_MACHINE_ERROR(10007, "STATE_MACHINE_ERROR"),

    /**
     * 部署异常错误
     */
    DEPLOY_ERROR(10008, "DEPLOY_ERROR"),

    /**
     * 乐观锁过期
     */
    LOCKER_VERSION_EXPIRED(10009, "LOCKER_VERSION_EXPIRED"),

    /**
     * 插件已存在
     */
    FORBIDDEN_ADDON_NAME_EXISTS(10010, "FORBIDDEN_ADDON_NAME_EXISTS"),

    /**
     * GIT异常
     */
    GIT_ERROR(10011, "GIT_ERROR"),

    /**
     * MetaQ 异常
     */
    MQ_ERROR(10012, "MQ_ERROR"),

    /**
     * 认证异常
     */
    AUTHORIZED_ERROR(10013, "AUTHORIZED_ERROR");

    private final int code;
    private final String description;

    private AppErrorCode(int code, String description) {
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
