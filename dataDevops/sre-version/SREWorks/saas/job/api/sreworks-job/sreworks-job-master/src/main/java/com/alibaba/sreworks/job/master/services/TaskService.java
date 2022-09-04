package com.alibaba.sreworks.job.master.services;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJobTask;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobWorkerDTO;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobTaskRepository;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobs;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobsRepository;
import com.alibaba.sreworks.job.taskinstance.TaskInstanceStatus;
import com.alibaba.sreworks.job.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskService {

    @Autowired
    SreworksJobTaskRepository sreworksJobTaskRepository;

    @Autowired
    SreworksJobTaskRepository taskRepository;

    @Autowired
    WorkerService workerService;

    @Autowired
    ElasticTaskInstanceWithBlobsRepository elasticTaskInstanceWithBlobsRepository;

    public String start(Long taskId, JSONObject varConf, String operator) throws Exception {
        SreworksJobTask task = taskRepository.findFirstById(taskId);
        JSONObject taskVarConf = task.varConf();
        taskVarConf.putAll(varConf);
        SreworksJobWorkerDTO workerDTO = workerService.getByExecType(task.getExecType());
        ElasticTaskInstanceWithBlobs taskInstance = new ElasticTaskInstanceWithBlobs();
        taskInstance.setId(UUID.randomUUID().toString().replace("-", ""));
        taskInstance.setOperator(operator);
        taskInstance.setGmtCreate(System.currentTimeMillis());
        taskInstance.setGmtExecute(0L);
        taskInstance.setGmtStop(0L);
        taskInstance.setGmtEnd(0L);
        taskInstance.setTaskId(taskId);
        taskInstance.setJobInstanceId(varConf.getString("elasticJobInstanceId"));
        taskInstance.setAddress(workerDTO.getAddress());
        taskInstance.setName(task.getName());
        taskInstance.setAlias(task.getAlias());
        taskInstance.setExecTimeout(task.getExecTimeout());
        taskInstance.setExecType(task.getExecType());
        taskInstance.setExecContent(task.getExecContent());
        taskInstance.setExecRetryTimes(task.execRetryTimes());
        taskInstance.setExecRetryInterval(task.execRetryInterval());
        taskInstance.setExecTimes(0L);
        taskInstance.setVarConf(JSONObject.toJSONString(taskVarConf));
        taskInstance.setOutVarConf(JSONObject.toJSONString(taskVarConf));
        taskInstance.setStatus(TaskInstanceStatus.INIT.name());
        taskInstance.setStatusDetail("");
        taskInstance.setStdout("");
        taskInstance.setStderr("");
        taskInstance.setSceneType(task.getSceneType());
        taskInstance.setSceneConf(task.getSceneConf());
        elasticTaskInstanceWithBlobsRepository.save(taskInstance);
        workerService.startInstance(workerDTO.getAddress(), taskInstance.getId());
        return taskInstance.getId();
    }

    public List<SreworksJobTask> list(String name, String execType, String sceneType) {

        name = "%" + (StringUtil.isEmpty(name) ? "" : name) + "%";
        return taskRepository.findAllByNameLikeOrderByIdDesc(name).stream()
            .filter(task -> StringUtil.isEmpty(execType) || execType.contains(task.getExecType()))
            .filter(task -> StringUtil.isEmpty(sceneType) || sceneType.contains(task.getSceneType()))
            .collect(Collectors.toList());

    }

}
