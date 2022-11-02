package com.alibaba.tdata.aisp.server.common.constant;

import java.util.Locale;

/**
 * @EnumName:ModelLevelEnum
 * @Author:dyj
 * @DATE: 2022-05-10
 * @Description:
 **/
public enum ModelLevelEnum {
    /**
     * 场景级别
     */
    SCENE("scene"),

    /**
     * 实体级别
     */
    ENTITY("entity");

    private String value;

    ModelLevelEnum(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ModelLevelEnum fromValue(String value){
        String s = value.toLowerCase(Locale.ROOT);
        switch (s){
            case "scene": return SCENE;
            case "entity": return ENTITY;
            default: throw new IllegalArgumentException("action=fromValue|| can not convert to ModelLevelEnum from value:"+ value);
        }
    }
}
