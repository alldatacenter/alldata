package com.alibaba.tdata.aisp.server.common.constant;

import java.util.Locale;

/**
 * @EnumName:AnalyseTaskStatusEnum
 * @Author:dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
public enum AnalyseTaskStatusEnum {
    /**
     * running
     */
    RUNNING("running"),

    /**
     * success
     */
    SUCCESS("success"),
    /**
     * failed
     */
    FAILED("failed");

    private String value;

    AnalyseTaskStatusEnum(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AnalyseTaskStatusEnum fromValue(String value){
        String s = value.toLowerCase(Locale.ROOT);
        switch (s){
            case "running": return RUNNING;
            case "success": return SUCCESS;
            case "failed": return FAILED;
            default: throw new IllegalArgumentException("action=fromValue|| can not convert AnalyseTaskStatus from value:"+ value);
        }
    }
}
