package com.alibaba.sreworks.job.master.jobschedule.dag;

import com.alibaba.sreworks.job.master.domain.DO.ElasticJobInstance;
import com.alibaba.sreworks.job.master.domain.DTO.JobInstanceStatus;
import com.alibaba.sreworks.job.master.domain.repository.ElasticJobInstanceRepository;
import com.alibaba.sreworks.job.master.services.TaskInstanceService;
import com.alibaba.sreworks.job.master.services.TaskService;
import com.alibaba.sreworks.job.master.services.WorkerService;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceDTO;
import com.alibaba.sreworks.job.taskinstance.TaskInstanceStatus;
import com.alibaba.sreworks.job.utils.BeansUtil;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.ParamType;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import com.alibaba.tesla.dag.model.domain.dagnode.DagNodeInputParam;
import com.alibaba.tesla.dag.model.domain.dagnode.ParamFromType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class DagJobTaskNode extends AbstractLocalNodeBase {

    public static String name = "dagJobTaskNode";

    public static Integer runTimeout = 86400;

    public static List<DagNodeInputParam> inputParams = Collections.singletonList(
        DagNodeInputParam.builder()
            .name("taskId")
            .alias("任务主键")
            .type(ParamType.LONG)
            .isOverWrite(false)
            .fromType(ParamFromType.CONSTANT)
            .build()
    );

    private String taskInstanceId;

    private void toRunning() {
        ElasticJobInstanceRepository jobInstanceRepository = BeansUtil.context
            .getBean(ElasticJobInstanceRepository.class);
        log.info("dagInstId: {}", dagInstId);
        ElasticJobInstance jobInstance = jobInstanceRepository.findFirstByScheduleInstanceId(dagInstId);
        jobInstance.setStatus(JobInstanceStatus.RUNNING.name());
        jobInstanceRepository.save(jobInstance);
    }

    private void toException() {
        ElasticJobInstanceRepository jobInstanceRepository = BeansUtil.context.getBean(
            ElasticJobInstanceRepository.class);
        ElasticJobInstance jobInstance = jobInstanceRepository.findFirstByScheduleInstanceId(dagInstId);
        jobInstance.setGmtEnd(System.currentTimeMillis());
        jobInstance.setStatus(JobInstanceStatus.EXCEPTION.name());
        jobInstanceRepository.save(jobInstance);
    }

    @Override
    public DagInstNodeRunRet run() throws Exception {
        log.info("start DagJobTaskNode");
        try {
            toRunning();
            TaskService taskService = BeansUtil.context.getBean(TaskService.class);
            TaskInstanceService taskInstanceService = BeansUtil.context.getBean(TaskInstanceService.class);
            ElasticJobInstanceRepository jobInstanceRepository = BeansUtil.context
                .getBean(ElasticJobInstanceRepository.class);
            ElasticJobInstance elasticJobInstance = jobInstanceRepository.findFirstByScheduleInstanceId(dagInstId);
            globalParams.put("elasticJobInstanceId", elasticJobInstance.getId());
            globalVariable.putAll(globalParams);
            globalParams.putAll(globalVariable);
            Long taskId = params.getLong("taskId");
            String taskInstanceId = taskService.start(taskId, globalParams, "dag");
            setTaskInstanceId(taskInstanceId);
            while (true) {
                ElasticTaskInstanceDTO elasticTaskInstanceDTO = taskInstanceService.get(taskInstanceId);
                TaskInstanceStatus status = TaskInstanceStatus.valueOf(elasticTaskInstanceDTO.getStatus());
                switch (status) {
                    case SUCCESS:
                        globalParams.putAll(elasticTaskInstanceDTO.getOutVarConf());
                        return DagInstNodeRunRet.builder().output(JsonUtil.map("taskInstanceId", taskInstanceId))
                            .build();
                    case EXCEPTION:
                        throw new Exception(elasticTaskInstanceDTO.getStatusDetail());
                    case TIMEOUT:
                        throw new Exception("TIMEOUT");
                    case STOPPED:
                        throw new Exception("STOPPED");
                    case CANCELLED:
                        throw new Exception("CANCELLED");
                    case INIT:
                    case RUNNING:
                    default:
                        Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            toException();
            throw e;
        }

    }

    @Override
    public void stop() {
        WorkerService workerService = BeansUtil.context.getBean(WorkerService.class);
        if (taskInstanceId != null) {

            try {
                workerService.stopInstance(taskInstanceId);
            } catch (Exception e) {
                log.error("", e);
            }

        }

    }

}
