package com.alibaba.sreworks.job.master.jobtrigger.quartz;

import com.alibaba.sreworks.job.master.domain.DO.ElasticJobInstance;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJob;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobRepository;
import com.alibaba.sreworks.job.master.services.JobService;
import com.alibaba.sreworks.job.utils.BeansUtil;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.ArrayList;
import java.util.Collections;

@Data
@Slf4j
public class QuartzJob implements Job {

    @SneakyThrows
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {

        JobService jobService = BeansUtil.context.getBean(JobService.class);
        SreworksJobRepository jobRepository = BeansUtil.context.getBean(SreworksJobRepository.class);

        Long jobId = jobExecutionContext.getJobDetail().getJobDataMap().getLong("jobId");
        SreworksJob sreworksJob = jobRepository.findFirstById(jobId);
        ElasticJobInstance jobInstance = jobService.start(
            jobId, sreworksJob.varConf(), new ArrayList<>(), Collections.emptyList(), "cron");
        log.info("start job {}:{} instance {}", jobId, sreworksJob.getName(), jobInstance.getId());

    }

}
