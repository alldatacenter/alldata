package com.alibaba.tesla.appmanager.common.enums;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public enum HostTypeEnum {
    IP(0),
    HOSTNAME(1);
    private int value;

    HostTypeEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static HostTypeEnum valueOf(Integer value) {
        switch (value) {
            case 0:
                return IP;
            case 1:
                return HOSTNAME;
            default:
                throw new IllegalArgumentException("Invalid HostTypeEnum value:" + value);
        }
    }
}
