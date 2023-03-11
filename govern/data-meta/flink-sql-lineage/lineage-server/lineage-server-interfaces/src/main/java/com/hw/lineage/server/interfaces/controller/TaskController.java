package com.hw.lineage.server.interfaces.controller;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.application.command.task.CreateTaskCmd;
import com.hw.lineage.server.application.command.task.UpdateTaskCmd;
import com.hw.lineage.server.application.dto.TaskDTO;
import com.hw.lineage.server.application.service.TaskService;
import com.hw.lineage.server.domain.query.task.TaskCheck;
import com.hw.lineage.server.domain.query.task.TaskQuery;
import com.hw.lineage.server.interfaces.result.Result;
import com.hw.lineage.server.interfaces.result.ResultMessage;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * @description: TaskController
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Validated
@RestController
@Api(tags = "Tasks API")
@RequestMapping("/tasks")
public class TaskController {

    @Resource
    private TaskService taskService;

    @GetMapping("/{taskId}")
    public Result<TaskDTO> queryTask(@PathVariable("taskId") Long taskId) {
        TaskDTO taskDTO = taskService.queryTask(taskId);
        return Result.success(ResultMessage.DETAIL_SUCCESS, taskDTO);
    }

    @GetMapping("")
    public Result<PageInfo<TaskDTO>> queryTasks(TaskQuery taskQuery) {
        PageInfo<TaskDTO> pageInfo = taskService.queryTasks(taskQuery);
        return Result.success(ResultMessage.QUERY_SUCCESS, pageInfo);
    }

    @PostMapping("")
    public Result<Long> createTask(@Valid @RequestBody CreateTaskCmd command) {
        Long taskId = taskService.createTask(command);
        return Result.success(ResultMessage.CREATE_SUCCESS, taskId);
    }

    @GetMapping("/exist")
    public Result<Boolean> checkTaskExist(@Valid TaskCheck taskCheck) {
        return Result.success(ResultMessage.CHECK_SUCCESS, taskService.checkTaskExist(taskCheck));
    }

    @PutMapping("/{taskId}")
    public Result<Boolean> updateTask(@PathVariable("taskId") Long taskId,
                                      @Valid @RequestBody UpdateTaskCmd command) {
        command.setTaskId(taskId);
        taskService.updateTask(command);
        return Result.success(ResultMessage.UPDATE_SUCCESS);
    }

    @DeleteMapping("/{taskId}")
    public Result<Boolean> deleteTask(@PathVariable("taskId") Long taskId) {
        taskService.deleteTask(taskId);
        return Result.success(ResultMessage.DELETE_SUCCESS);
    }

    @PostMapping("/{taskId}/lineage")
    public Result<TaskDTO> parseTaskLineage(@PathVariable("taskId") Long taskId) {
        TaskDTO taskDTO = taskService.parseTaskLineage(taskId);
        return Result.success(ResultMessage.PARSE_LINEAGE_SUCCESS, taskDTO);
    }
}
