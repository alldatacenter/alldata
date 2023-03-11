package com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl;

import com.hw.lineage.common.enums.TaskStatus;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.IntEnumTypeHandler;

/**
 * @description: TaskStatusTypeHandler
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class TaskStatusTypeHandler extends IntEnumTypeHandler<TaskStatus> {
    public TaskStatusTypeHandler() {
        super(TaskStatus.class);
    }
}
