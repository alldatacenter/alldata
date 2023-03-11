package com.hw.lineage.server.infrastructure.repository.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.hw.lineage.common.exception.LineageException;
import com.hw.lineage.common.util.PageUtils;
import com.hw.lineage.server.domain.entity.task.Task;
import com.hw.lineage.server.domain.entity.task.TaskLineage;
import com.hw.lineage.server.domain.entity.task.TaskSql;
import com.hw.lineage.server.domain.query.task.TaskQuery;
import com.hw.lineage.server.domain.repository.TaskRepository;
import com.hw.lineage.server.domain.vo.TaskId;
import com.hw.lineage.server.infrastructure.persistence.converter.DataConverter;
import com.hw.lineage.server.infrastructure.persistence.dos.TaskDO;
import com.hw.lineage.server.infrastructure.persistence.dos.TaskLineageDO;
import com.hw.lineage.server.infrastructure.persistence.dos.TaskSqlDO;
import com.hw.lineage.server.infrastructure.persistence.mapper.*;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.hw.lineage.server.infrastructure.persistence.mapper.TaskDynamicSqlSupport.task;
import static com.hw.lineage.server.infrastructure.persistence.mapper.TaskDynamicSqlSupport.taskName;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isLike;

/**
 * @description: TaskRepositoryImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Repository
public class TaskRepositoryImpl extends AbstractBasicRepository implements TaskRepository {
    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskSqlMapper taskSqlMapper;

    @Resource
    private TaskLineageMapper taskLineageMapper;

    @Resource
    private DataConverter converter;

    @Override
    public Task find(TaskId taskId) {
        TaskDO taskDO = taskMapper.selectByPrimaryKey(taskId.getValue())
                .orElseThrow(() ->
                        new LineageException(String.format("taskId [%s] is not existed", taskId.getValue()))
                );
        return converter.toTask(taskDO);
    }


    @Override
    public boolean check(String name) {
        return !taskMapper.select(completer -> completer.where(taskName, isEqualTo(name))).isEmpty();
    }

    @Override
    public Task save(Task task) {
        TaskDO taskDO = converter.fromTask(task);
        if (taskDO.getTaskId() == null) {
            taskMapper.insertSelective(taskDO);
        } else {
            taskMapper.updateByPrimaryKeySelective(taskDO);
        }
        return converter.toTask(taskDO);
    }

    @Override
    public void saveTaskSql(Task task) {
        List<TaskSql> taskSqlList = task.getTaskSqlList().stream().map(taskSql -> {
                    TaskSqlDO taskSqlDO = converter.fromTaskSql(taskSql);
                    taskSqlMapper.insertSelective(taskSqlDO);
                    return converter.toTaskSql(taskSqlDO);
                }
        ).collect(Collectors.toList());
        task.setTaskSqlList(taskSqlList);
    }


    @Override
    public void saveTaskLineage(Task task) {
        List<TaskLineage> taskLineageList = task.getTaskLineageList().stream().map(taskLineage -> {
                    TaskLineageDO taskLineageDO = converter.fromTaskLineage(taskLineage);
                    taskLineageMapper.insertSelective(taskLineageDO);
                    return converter.toTaskLineage(taskLineageDO);
                }
        ).collect(Collectors.toList());
        task.setTaskLineageList(taskLineageList);
    }

    @Override
    public void removeTaskSql(TaskId taskId) {
        taskSqlMapper.delete(completer ->
                completer.where(task.taskId, isEqualTo(taskId.getValue()))
        );
    }

    @Override
    public void removeTaskLineage(TaskId taskId) {
        taskLineageMapper.delete(completer ->
                completer.where(task.taskId, isEqualTo(taskId.getValue()))
        );
    }

    @Override
    public void remove(TaskId taskId) {
        taskMapper.deleteByPrimaryKey(taskId.getValue());
    }

    @Override
    public PageInfo<Task> findAll(TaskQuery taskQuery) {
        try (Page<TaskDO> page = PageMethod.startPage(taskQuery.getPageNum(), taskQuery.getPageSize())) {
            PageInfo<TaskDO> pageInfo = page.doSelectPageInfo(() ->
                    taskMapper.select(completer ->
                            completer.where(taskName, isLike(buildLikeValue(taskQuery.getTaskName())))
                                    .orderBy(buildSortSpecification(taskQuery))
                    )
            );
            return PageUtils.convertPage(pageInfo, converter::toTask);
        }
    }
}
