package com.alibaba.tdata.aisp.server.common.constant;

import java.util.Locale;

/**
 * @EnumName:AnalyseTaskType
 * @Author:dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
public enum AnalyseTaskTypeEnum {
    /**
     * 同步
     */
    SYNC("sync"),
    /**
     * 异步
     */
    ASYNC("async");

    private String value;

    AnalyseTaskTypeEnum(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AnalyseTaskTypeEnum fromValue(String value){
        String s = value.toLowerCase(Locale.ROOT);
        switch (s){
            case "sync": return SYNC;
            case "async": return ASYNC;
            default: throw new IllegalArgumentException("action=fromValue|| can not convert to AnalyseTaskType from value:"+ value);
        }
    }
}
