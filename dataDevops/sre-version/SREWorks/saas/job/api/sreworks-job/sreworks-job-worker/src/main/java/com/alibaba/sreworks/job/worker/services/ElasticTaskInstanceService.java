package com.alibaba.sreworks.job.worker.services;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstance;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceRepository;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobsRepository;
import com.alibaba.sreworks.job.taskinstance.TaskInstanceStatus;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.sreworks.job.utils.StringUtil;
import com.alibaba.sreworks.job.worker.taskscene.TaskSceneService;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery.Refresh;
import org.springframework.stereotype.Service;

@Data
@Slf4j
@Service
public class ElasticTaskInstanceService {

    @Autowired
    ElasticTaskInstanceWithBlobsRepository taskInstanceWithBlobsRepository;

    @Autowired
    ElasticTaskInstanceRepository taskInstanceRepository;

    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    TaskSceneService taskSceneService;

    public void appendString(String id, String key, String append) {
        if (StringUtil.isEmpty(append)) {
            return;
        }
        elasticsearchRestTemplate.update(
            UpdateQuery.builder(id)
                .withScript(String.format("ctx._source.%s = ctx._source.%s + params.append", key, key))
                .withRefresh(Refresh.True)
                .withParams(ImmutableMap.of(
                    "append", append
                ))
                .withRetryOnConflict(2)
                .build(),
            IndexCoordinates.of("sreworks-job-task-instance")
        );
    }

    public void update(String id, JSONObject jsonObject) {
        elasticsearchRestTemplate.update(
            UpdateQuery.builder(id)
                .withDocument(Document.from(jsonObject))
                .withRefresh(Refresh.True)
                .withRetryOnConflict(2)
                .build(),
            IndexCoordinates.of("sreworks-job-task-instance")
        );
    }

    public void addExecTimes(ElasticTaskInstance taskInstance) {
        Long execTimes = taskInstance.getExecTimes() + 1;
        taskInstance.setExecTimes(execTimes);
        update(taskInstance.getId(), JsonUtil.map(
            "execTimes", execTimes
        ));
    }

    public void toWaitRetry(ElasticTaskInstance taskInstance) {
        update(taskInstance.getId(), JsonUtil.map(
            "status", TaskInstanceStatus.WAIT_RETRY.name()
        ));
        taskSceneService.getTaskScene(taskInstance.getSceneType()).toWaitRetry();
    }

    public void toRunning(ElasticTaskInstance taskInstance) {
        taskInstance.setStatus(TaskInstanceStatus.RUNNING.name());
        taskInstance.setGmtExecute(System.currentTimeMillis());
        update(taskInstance.getId(), JsonUtil.map(
            "status", TaskInstanceStatus.RUNNING.name(),
            "gmtExecute", System.currentTimeMillis()
        ));
        taskSceneService.getTaskScene(taskInstance.getSceneType()).toRunning();
    }

    public void toCancelled(ElasticTaskInstance taskInstance) {
        taskInstance.setStatus(TaskInstanceStatus.CANCELLED.name());
        taskInstance.setGmtEnd(System.currentTimeMillis());
        update(taskInstance.getId(), JsonUtil.map(
            "status", TaskInstanceStatus.CANCELLED.name(),
            "gmtEnd", System.currentTimeMillis()
        ));
        taskSceneService.getTaskScene(taskInstance.getSceneType()).toCancelled();
    }

    public void toException(ElasticTaskInstance taskInstance, Exception e) {
        taskInstance.setStatus(TaskInstanceStatus.EXCEPTION.name());
        taskInstance.setGmtEnd(System.currentTimeMillis());
        update(taskInstance.getId(), JsonUtil.map(
            "status", TaskInstanceStatus.EXCEPTION.name(),
            "gmtEnd", System.currentTimeMillis(),
            "statusDetail", Throwables.getStackTraceAsString(e)
        ));
        taskSceneService.getTaskScene(taskInstance.getSceneType()).toException();
    }

    public void toTimeout(ElasticTaskInstance taskInstance) {
        taskInstance.setStatus(TaskInstanceStatus.TIMEOUT.name());
        taskInstance.setGmtEnd(System.currentTimeMillis());
        update(taskInstance.getId(), JsonUtil.map(
            "status", TaskInstanceStatus.TIMEOUT.name(),
            "gmtEnd", System.currentTimeMillis()
        ));
        taskSceneService.getTaskScene(taskInstance.getSceneType()).toTimeout();
    }

    public void toStopped(ElasticTaskInstance taskInstance) {
        taskInstance.setStatus(TaskInstanceStatus.STOPPED.name());
        taskInstance.setGmtEnd(System.currentTimeMillis());
        update(taskInstance.getId(), JsonUtil.map(
            "status", TaskInstanceStatus.STOPPED.name(),
            "gmtEnd", System.currentTimeMillis()
        ));
        taskSceneService.getTaskScene(taskInstance.getSceneType()).toStopped();
    }

    public void toSuccess(ElasticTaskInstance taskInstance, String outVarConf) throws Exception {
        taskInstance.setStatus(TaskInstanceStatus.SUCCESS.name());
        taskInstance.setGmtEnd(System.currentTimeMillis());
        update(taskInstance.getId(), JsonUtil.map(
            "status", TaskInstanceStatus.SUCCESS.name(),
            "gmtEnd", System.currentTimeMillis(),
            "outVarConf", outVarConf
        ));
        taskSceneService.getTaskScene(taskInstance.getSceneType()).toSuccess(taskInstance.getId());
    }

}
