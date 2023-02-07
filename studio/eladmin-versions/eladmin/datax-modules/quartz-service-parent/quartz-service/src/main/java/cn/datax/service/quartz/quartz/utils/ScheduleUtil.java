package cn.datax.service.quartz.quartz.utils;

import cn.datax.common.core.DataConstant;
import cn.datax.common.exception.DataException;
import cn.datax.common.utils.ThrowableUtil;
import cn.datax.service.quartz.api.entity.QrtzJobEntity;
import cn.datax.service.quartz.quartz.ScheduleJob;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.util.Date;
import java.util.List;

@Slf4j
public class ScheduleUtil {
	
	public static final String QUARTZ_TASK_KEY = "__QUARTZ_TASK_KEY__";

    public static final String JOB_DATA_MAP = "__JOB_DATA_MAP__";

	private static Scheduler scheduler;

	public static void setScheduler(Scheduler scheduler) {
		ScheduleUtil.scheduler = scheduler;
	}

	/**
     * 获取jobKey
     */
    public static JobKey getJobKey(String jobId) {
        return JobKey.jobKey(QUARTZ_TASK_KEY + jobId);
    }

	/**
	 * 获取触发器key
	 */
	public static TriggerKey getTriggerKey(String jobId)  {
		return TriggerKey.triggerKey(QUARTZ_TASK_KEY + jobId);
	}

    /**
     * 获取表达式触发器
     */
    public static CronTrigger getCronTrigger(QrtzJobEntity job) {
        try {
			TriggerKey triggerKey = getTriggerKey(job.getId());
			CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
			// 如果不存在则创建一个定时任务
			if(null == trigger){
				createScheduleJob(job);
				trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
			}
			return trigger;
		} catch (SchedulerException e) {
			throw new DataException("获取定时任务CronTrigger出现异常", e);
		}
    }
    
    /**
     * 创建定时任务
     */
	public static void createScheduleJob(QrtzJobEntity job) {
        try {
        	// 构建job信息
        	JobDetail jobDetail = JobBuilder.newJob(ScheduleJob.class).withIdentity(getJobKey(job.getId())).build();
        	// 表达式调度构建器
        	CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        	// 按新的cronExpression表达式构建一个新的trigger
        	CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(job.getId()))
					//withMisfireHandlingInstructionDoNothing
					//——不触发立即执行
					//——等待下次Cron触发频率到达时刻开始按照Cron频率依次执行
					//withMisfireHandlingInstructionIgnoreMisfires
					//——以错过的第一个频率时间立刻开始执行
					//——重做错过的所有频率周期后
					//——当下一次触发频率发生时间大于当前时间后，再按照正常的Cron频率依次执行
					//withMisfireHandlingInstructionFireAndProceed
					//——以当前时间为触发频率立刻触发一次执行
					//——然后按照Cron频率依次执行
					.withSchedule(cronScheduleBuilder.withMisfireHandlingInstructionDoNothing())
					.build();
        	// 放入参数，运行时的方法可以获取
        	jobDetail.getJobDataMap().put(JOB_DATA_MAP, job);
			// 重置启动时间
			((CronTriggerImpl)trigger).setStartTime(new Date());
        	// 交给scheduler去调度
			scheduler.scheduleJob(jobDetail, trigger);
			// 暂停任务
			if (DataConstant.EnableState.DISABLE.getKey().equals(job.getStatus())) {
				pauseJob(job.getId());
			}
		} catch (SchedulerException e) {
			throw new DataException("创建定时任务失败", e);
		}
	}
	
	/**
     * 更新定时任务
     */
    public static void updateScheduleJob(QrtzJobEntity job) {
        try {
        	TriggerKey triggerKey = getTriggerKey(job.getId());
        	CronTrigger trigger = getCronTrigger(job);
        	// 表达式调度构建器
        	CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        	// 按新的cronExpression表达式重新构建trigger
        	trigger = trigger.getTriggerBuilder().withIdentity(triggerKey)
					.withSchedule(cronScheduleBuilder.withMisfireHandlingInstructionDoNothing())
					.build();
        	// 参数
        	trigger.getJobDataMap().put(JOB_DATA_MAP, job);
			// 重置启动时间
			((CronTriggerImpl)trigger).setStartTime(new Date());
			scheduler.rescheduleJob(triggerKey, trigger);
			// 暂停任务
			if (DataConstant.EnableState.DISABLE.getKey().equals(job.getStatus())) {
				pauseJob(job.getId());
			}
		} catch (SchedulerException e) {
			throw new DataException("更新定时任务失败", e);
		}
    }
    
    /**
     * 暂停任务
     */
    public static void pauseJob(String jobId) {
        try {
			scheduler.pauseJob(getJobKey(jobId));
		} catch (SchedulerException e) {
			throw new DataException("暂停定时任务失败", e);
		}
    }

    /**
     * 恢复任务
     */
    public static void resumeJob(String jobId) {
        try {
			scheduler.resumeJob(getJobKey(jobId));
		} catch (SchedulerException e) {
			throw new DataException("恢复定时任务失败", e);
		}
    }
	
    /**
     * 立即执行任务
     */
    public static void runJob(QrtzJobEntity job) {
        try {
        	JobDataMap dataMap = new JobDataMap();
        	dataMap.put(JOB_DATA_MAP, job);
			scheduler.triggerJob(getJobKey(job.getId()), dataMap);
		} catch (SchedulerException e) {
			throw new DataException("立即执行定时任务失败", e);
		}
    }
    
    /**
     * 移除任务
     */
    public static void deleteJob(String id) {
        try {
			TriggerKey triggerKey = getTriggerKey(id);
			// 停止触发器
			scheduler.pauseTrigger(triggerKey);
			// 移除触发器
			scheduler.unscheduleJob(triggerKey);
			// 删除任务
			scheduler.deleteJob(getJobKey(id));
		} catch (SchedulerException e) {
			throw new DataException("删除定时任务失败", e);
		}
    }

	/**
	 * 项目启动时，初始化定时器
	 */
	public static void init(List<QrtzJobEntity> list) {
		if(CollUtil.isNotEmpty(list)){
			for (QrtzJobEntity job : list) {
				TriggerKey triggerKey = getTriggerKey(job.getId());
				CronTrigger trigger = null;
				try {
					trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
				} catch (SchedulerException e) {
					log.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
				}
				// 如果不存在则创建一个定时任务
				if (null == trigger) {
					ScheduleUtil.createScheduleJob(job);
				} else {
					ScheduleUtil.updateScheduleJob(job);
				}
			}
		}
	}
}