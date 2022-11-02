package com.alibaba.tdata.aisp.server.common.constant;

import java.util.Locale;

/**
 * @EnumName:codeTypeEnum
 * @Author:dyj
 * @DATE: 2021-11-24
 * @Description:
 **/
public enum CodeTypeEnum {
    /**
     * curl
     */
    CURL("curl"),

    /**
     * python
     */
    PYTHON("python"),

    /**
     * java
     */
    JAVA("java");

    private String value;

    CodeTypeEnum(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CodeTypeEnum fromValue(String value){
        String s = value.toLowerCase(Locale.ROOT);
        switch (s){
            case "curl": return CURL;
            case "python": return PYTHON;
            case "java": return JAVA;
            default: throw new IllegalArgumentException("action=fromValue|| can not convert to CodeTypeEnum from value:"+ value);
        }
    }
}
