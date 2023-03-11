package com.hw.lineage.common.enums;

import com.hw.lineage.common.enums.basic.StringEnum;

/**
 * @description: TableKind
 * @author: HamaWhite
 * @version: 1.0.0
 */
public enum TableKind implements StringEnum<TableKind> {

    TABLE("table"),
    VIEW("view");

    private final String value;

    TableKind(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
