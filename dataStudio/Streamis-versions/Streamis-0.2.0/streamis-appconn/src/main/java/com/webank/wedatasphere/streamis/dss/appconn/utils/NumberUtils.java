package com.webank.wedatasphere.streamis.dss.appconn.utils;

public class NumberUtils {

    public static Integer getInt(Object original) {
        if (original instanceof Double) {
            return ((Double) original).intValue();
        }
        return (Integer) original;
    }
}
