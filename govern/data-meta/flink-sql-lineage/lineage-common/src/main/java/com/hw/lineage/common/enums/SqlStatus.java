package com.hw.lineage.common.enums;

import com.hw.lineage.common.enums.basic.IntEnum;

/**
 * @description: SqlStatus
 * @author: HamaWhite
 * @version: 1.0.0
 */
public enum SqlStatus implements IntEnum<SqlStatus> {

    FAILED(-1),
    SUCCESS(0),
    INIT(1),
    RUNNING(2);

    private final int value;

    SqlStatus(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}