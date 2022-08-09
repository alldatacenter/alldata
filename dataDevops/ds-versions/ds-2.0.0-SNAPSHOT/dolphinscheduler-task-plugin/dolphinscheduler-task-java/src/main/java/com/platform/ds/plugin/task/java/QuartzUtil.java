package com.platform.ds.plugin.task.java;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * @program: wlhbdp
 * @description: 对quartz任务进行操作
 * @author: wlhbdp
 * @create: 2021-06-16 17:29
 **/
public class QuartzUtil {

    private final static String ID = "id";
    private final static String UUID = "uuid";
    public final static String TRIAL = "TRIAL";
    public final static String SCHEDULING = "SCHEDULING";
    public final static String HIVE = "HIVE";
    private final static Logger logger = LoggerFactory.getLogger(QuartzUtil.class);

    /**
     * 创建quartz job
     * @author wlhbdp
     * @date 2021 06 16
     * @param
     * @return
     */
    public static void createJob(Scheduler scheduler, Class<? extends Job> jobClass,
                                 String jobName, String jobGroup,
                                 String cron, Map<String, Object> param) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroup)
                .storeDurably()
                .build();
        if (param != null && !param.isEmpty()) {
            param.forEach((key, value) -> jobDetail.getJobDataMap().put(key, value));
        }
        scheduler.scheduleJob(jobDetail, buildCronTrigger(jobName, jobGroup, cron));
        ensureSchedulerStarted(scheduler);
    }

    /**
     * 生成cron trigger
     * @author wlhbdp
     * @date 2021/6/16 17:36
     * @param
     * @return
     */
    private static Trigger buildCronTrigger(String jobName, String jobGroup, String cron) {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
        return TriggerBuilder.newTrigger()
                .withIdentity(jobName, jobGroup).withSchedule(scheduleBuilder)
                .build();
    }

    /**
     * keep scheduler to start
     * @author wlhbdp
     * @date 2021/6/16 17:36
     * @param
     * @return
     */
    private static void ensureSchedulerStarted(Scheduler scheduler) {
        try {
            if (scheduler.isShutdown()){
                scheduler.start();
            }
        } catch (SchedulerException e) {
            logger.error("重启scheduler时发生异常:{}", e.getMessage());
        }
    }

}
