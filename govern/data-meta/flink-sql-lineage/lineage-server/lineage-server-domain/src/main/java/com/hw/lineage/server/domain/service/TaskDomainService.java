package com.hw.lineage.server.domain.service;

import com.hw.lineage.server.domain.entity.task.Task;

/**
 * @description: TaskDomainService
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface TaskDomainService {

    void generateTaskSql(Task task);
}
