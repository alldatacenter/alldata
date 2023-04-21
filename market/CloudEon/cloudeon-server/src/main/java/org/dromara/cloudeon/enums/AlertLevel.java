package org.dromara.cloudeon.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AlertLevel {
    WARNING(1,"告警级别"),
    EXCEPTION(2,"异常级别");

    private int value;

    private String desc;

    AlertLevel(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public static AlertLevel fromDesc(String desc) {
        for (AlertLevel alertLevel : values()) {
            if (alertLevel.desc.equals(desc)) {
                return alertLevel;
            }
        }
        return null;
    }

    @JsonValue
    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }


    @Override
    public String toString() {
        return this.desc;
    }
}
