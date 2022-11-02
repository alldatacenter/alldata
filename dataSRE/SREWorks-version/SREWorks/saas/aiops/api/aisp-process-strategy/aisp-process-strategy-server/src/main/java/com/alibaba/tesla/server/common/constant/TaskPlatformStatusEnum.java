package com.alibaba.tesla.server.common.constant;

import java.util.Locale;

/**
 * @EnumName:TaskPlatformStatusEnum
 * @Author:dyj
 * @DATE: 2022-03-01
 * @Description:
 **/
public enum TaskPlatformStatusEnum {
    /**
     * 初始化
     */
    INIT("init"),

    /**
     * 运行
     */
    RUNNING("running"),

    /**
     * 成功
     */
    SUCCESS("success"),

    /**
     * 异常
     */
    EXCEPTION("exception");

    private String value;

    TaskPlatformStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TaskPlatformStatusEnum fromValue(String value){
        String s = value.toLowerCase(Locale.ROOT);
        switch (s){
            case "init": return INIT;
            case "running": return RUNNING;
            case "success": return SUCCESS;
            case "exception": return EXCEPTION;
            default: throw new IllegalArgumentException("action=fromValue|| Can not identify TaskPlatformStatusEnum type:"+ value);
        }
    }
}
