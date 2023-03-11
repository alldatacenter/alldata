package com.hw.lineage.server.application.service;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.application.command.task.CreateTaskCmd;
import com.hw.lineage.server.application.command.task.UpdateTaskCmd;
import com.hw.lineage.server.application.dto.TaskDTO;
import com.hw.lineage.server.domain.query.task.TaskCheck;
import com.hw.lineage.server.domain.query.task.TaskQuery;

/**
 * @description: TaskService
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface TaskService {

    Long createTask(CreateTaskCmd command);

    TaskDTO queryTask(Long taskId);

    Boolean checkTaskExist(TaskCheck taskCheck);

    PageInfo<TaskDTO> queryTasks(TaskQuery taskQuery);

    void deleteTask(Long taskId);

    void updateTask(UpdateTaskCmd command);

    TaskDTO parseTaskLineage(Long taskId);
}
