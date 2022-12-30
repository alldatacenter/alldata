package com.alibaba.sreworks.job.master.jobtrigger.quartz;

import com.alibaba.sreworks.job.master.jobtrigger.AbstractJobTrigger;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quartz.*;
import org.quartz.Trigger.TriggerState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
public class QuartzJobTrigger extends AbstractJobTrigger<QuartzJobTriggerConf> {

    public String type = "cron";

    @Autowired
    private Scheduler scheduler;

    @Override
    public Class<QuartzJobTriggerConf> getConfClass() {
        return QuartzJobTriggerConf.class;
    }

    @Override
    public void create(Long id, QuartzJobTriggerConf conf) throws Exception {
        CronTrigger trigger = TriggerBuilder
            .newTrigger()
            .withIdentity(id.toString())
            .withSchedule(CronScheduleBuilder.cronSchedule(conf.getCronExpression()))
            .build();
        JobDetail jobDetail = JobBuilder
            .newJob(QuartzJob.class)
            .withIdentity(id.toString())
            .usingJobData("jobId", id)
            .build();
        scheduler.scheduleJob(jobDetail, trigger);
        if (!conf.isEnabled()) {
            scheduler.pauseJob(JobKey.jobKey(id.toString()));
        }
    }

    @Override
    public void delete(Long id) throws Exception {
        scheduler.deleteJob(JobKey.jobKey(id.toString()));
    }

    @Override
    public void modify(Long id, QuartzJobTriggerConf conf) throws Exception {
        TriggerKey triggerKey = TriggerKey.triggerKey(id.toString());
        CronTrigger trigger = (CronTrigger)scheduler.getTrigger(triggerKey);
        trigger = trigger
            .getTriggerBuilder()
            .withIdentity(triggerKey)
            .withSchedule(CronScheduleBuilder.cronSchedule(conf.getCronExpression()))
            .build();
        scheduler.rescheduleJob(triggerKey, trigger);

        if (conf.isEnabled()) {
            scheduler.resumeJob(JobKey.jobKey(id.toString()));
        } else {
            scheduler.pauseJob(JobKey.jobKey(id.toString()));
        }
    }

    @Override
    public QuartzJobTriggerConf getConf(Long id) throws Exception {
        TriggerKey triggerKey = TriggerKey.triggerKey(id.toString());
        CronTrigger trigger = (CronTrigger)scheduler.getTrigger(triggerKey);
        TriggerState state = scheduler.getTriggerState(triggerKey);
        return QuartzJobTriggerConf.builder()
            .cronExpression(trigger.getCronExpression())
            .enabled(!state.equals(TriggerState.PAUSED))
            .build();
    }

    @Override
    public Map<Long, QuartzJobTriggerConf> getConfBatch(Set<Long> ids) throws Exception {
        if (CollectionUtils.isEmpty(ids)) {
            return new HashMap<>();
        }

        return ids.stream().collect(Collectors.toMap(id -> id, id -> {
            try {
                return getConf(id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }));
    }

    @Override
    public Boolean getState(Long id) throws Exception {
        TriggerKey triggerKey = TriggerKey.triggerKey(id.toString());
        TriggerState state = scheduler.getTriggerState(triggerKey);
        return !state.equals(TriggerState.PAUSED);
    }

    @Override
    public Map<Long, Boolean> getStateBatch(Set<Long> ids) throws Exception {
        if (CollectionUtils.isEmpty(ids)) {
            return new HashMap<>();
        }

        return ids.stream().collect(Collectors.toMap(id -> id, id -> {
            try {
                return getState(id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }));
    }

    @Override
    public void toggleState(Long id, Boolean state) throws Exception {
        if (state) {
            scheduler.resumeJob(JobKey.jobKey(id.toString()));
        } else {
            scheduler.pauseJob(JobKey.jobKey(id.toString()));
        }
    }

}
