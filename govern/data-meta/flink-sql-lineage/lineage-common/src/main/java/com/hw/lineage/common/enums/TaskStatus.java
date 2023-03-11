package com.hw.lineage.common.enums;

import com.hw.lineage.common.enums.basic.IntEnum;

/**
 * @description: TaskStatus
 * @author: HamaWhite
 * @version: 1.0.0
 */
public enum TaskStatus implements IntEnum<TaskStatus> {

    FAILED(-1),
    SUCCESS(0),
    INIT(1),
    RUNNING(2),
    MODIFIED(3);

    private final int value;

    TaskStatus(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }
}