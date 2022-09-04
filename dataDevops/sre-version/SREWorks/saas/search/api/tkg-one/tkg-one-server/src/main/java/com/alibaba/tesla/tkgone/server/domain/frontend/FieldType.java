package com.alibaba.tesla.tkgone.server.domain.frontend;

import org.apache.commons.lang3.StringUtils;

public enum FieldType {
    BOOLEAN("BOOLEAN"),
    NUMBER("NUMBER"),
    STRING("STRING"),
    OBJECT("OBJECT");

    private String type;

    FieldType(String type) {
        this.type = type;
    }

    public static boolean contains(java.lang.String value) {
        for (FieldType c : FieldType.values()) {
            if (c.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public String getType() {
        return type;
    }

    public static String toFormatString() {
        String result = StringUtils.join(FieldType.values(), ",");
        return "[" + result + "]";
    }
}