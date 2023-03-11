package com.hw.lineage.common.enums;

import com.hw.lineage.common.enums.basic.StringEnum;

/**
 * @description: SqlType
 * @author: HamaWhite
 * @version: 1.0.0
 */
public enum SqlType implements StringEnum<SqlType> {
    SELECT("select"),
    INSERT("insert"),
    CREATE("create"),
    DROP("drop"),
    OTHER("other");

    private final String value;

    SqlType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
