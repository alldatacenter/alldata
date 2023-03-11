package com.hw.lineage.common.util;

import com.hw.lineage.common.enums.SqlType;

import static com.hw.lineage.common.enums.SqlType.OTHER;

/**
 * @description: EnumUtils
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class EnumUtils {
    private EnumUtils() {
        throw new IllegalStateException("Utility class");
    }


    public static SqlType getSqlTypeByValue(String value) {
        for (SqlType sqlType : SqlType.values()) {
            if (sqlType.value().equalsIgnoreCase(value)) {
                return sqlType;
            }
        }
        return OTHER;
    }
}
