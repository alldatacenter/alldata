package com.alibaba.sreworks.job.master.jobschedule.dag;

import com.alibaba.sreworks.job.master.domain.DO.ElasticJobInstance;
import com.alibaba.sreworks.job.master.domain.DTO.JobInstanceStatus;
import com.alibaba.sreworks.job.master.domain.repository.ElasticJobInstanceRepository;
import com.alibaba.sreworks.job.utils.BeansUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class DagJobEndNode extends AbstractLocalNodeBase {

    public static String name = "dagJobEndNode";

    public static Integer runTimeout = 86400;

    @Override
    public DagInstNodeRunRet run() {
        log.info("start DagJobEndNode");
        ElasticJobInstanceRepository jobInstanceRepository = BeansUtil.context.getBean(
            ElasticJobInstanceRepository.class);
        ElasticJobInstance jobInstance = jobInstanceRepository.findFirstByScheduleInstanceId(dagInstId);
        jobInstance.setGmtEnd(System.currentTimeMillis());
        jobInstance.setStatus(JobInstanceStatus.SUCCESS.name());
        jobInstanceRepository.save(jobInstance);
        return new DagInstNodeRunRet();

    }

    @SneakyThrows
    @Override
    public void stop() {
        Thread.sleep(500);
    }

}
