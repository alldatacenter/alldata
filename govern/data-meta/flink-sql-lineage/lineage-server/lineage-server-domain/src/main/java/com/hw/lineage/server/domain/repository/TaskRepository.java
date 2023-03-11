package com.hw.lineage.server.domain.repository;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.domain.repository.basic.Repository;
import com.hw.lineage.server.domain.entity.task.Task;
import com.hw.lineage.server.domain.query.task.TaskQuery;
import com.hw.lineage.server.domain.vo.TaskId;

/**
 * @description: TaskRepository
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface TaskRepository extends Repository<Task, TaskId> {

    PageInfo<Task> findAll(TaskQuery taskQuery);

    void saveTaskSql(Task task);

    void saveTaskLineage(Task task);

    void removeTaskSql(TaskId taskId);

    void removeTaskLineage(TaskId taskId);
}
