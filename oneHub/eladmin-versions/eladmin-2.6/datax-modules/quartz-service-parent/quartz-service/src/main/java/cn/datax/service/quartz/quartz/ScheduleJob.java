package cn.datax.service.quartz.quartz;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import cn.datax.common.utils.ThrowableUtil;
import cn.datax.service.quartz.api.entity.QrtzJobEntity;
import cn.datax.service.quartz.api.entity.QrtzJobLogEntity;
import cn.datax.service.quartz.quartz.utils.ScheduleUtil;
import cn.datax.service.quartz.service.QrtzJobLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
public class ScheduleJob extends QuartzJobBean {

    @Autowired
    private QrtzJobLogService qrtzJobLogService;
	
	private static ThreadPoolExecutor executor  = new ThreadPoolExecutor(10, 20, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(16));
	
	@Override
	protected void executeInternal(JobExecutionContext context) {
        QrtzJobEntity job = (QrtzJobEntity) context.getMergedJobDataMap().get(ScheduleUtil.JOB_DATA_MAP);
        log.info("运行任务{}", job);
		QrtzJobLogEntity jobLog = new QrtzJobLogEntity();
		jobLog.setJobId(job.getId());
        jobLog.setCreateTime(LocalDateTime.now());
        long startTime = System.currentTimeMillis();
        try {
        	// 执行任务
            ScheduleRunnable task = new ScheduleRunnable(job.getBeanName(), job.getMethodName(), job.getMethodParams());
            executor.execute(task);
            long times = System.currentTimeMillis() - startTime;
            // 任务状态  0：失败 1：成功
            jobLog.setStatus("1");
            jobLog.setMsg("【" + job.getJobName() + "】任务执行结束，总共耗时：" + times + "毫秒");
		} catch (Exception e) {
            jobLog.setMsg("【" + job.getJobName() + "】任务执行失败");
            // 任务状态 0：失败 1：成功
            jobLog.setStatus("0");
            log.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
		} finally {
			qrtzJobLogService.save(jobLog);
		}
	}
}