package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.tesla.appmanager.autoconfig.ThreadPoolProperties;
import com.alibaba.tesla.appmanager.server.service.rtappinstance.RtAppInstanceService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 实时应用实例状态更新 Job
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class RtAppInstanceStatusUpdateJob {

    @Autowired
    private RtAppInstanceService rtAppInstanceService;

    @Autowired
    private ThreadPoolProperties threadPoolProperties;

    private ThreadPoolExecutor threadPoolExecutor;

    private final Object threadPoolExecutorLock = new Object();

    @PostConstruct
    public void init() {
        synchronized (threadPoolExecutorLock) {
            threadPoolExecutor = new ThreadPoolExecutor(
                    threadPoolProperties.getRtAppInstanceStatusUpdateCoreSize(),
                    threadPoolProperties.getRtAppInstanceStatusUpdateMaxSize(),
                    threadPoolProperties.getRtAppInstanceStatusUpdateKeepAlive(), TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(threadPoolProperties.getRtAppInstanceStatusUpdateQueueCapacity()),
                    r -> new Thread(r, "app-instance-status-update-" + r.hashCode()),
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }
    }

    @Scheduled(cron = "${appmanager.cron-job.rt-app-instance-status-update:0/10 * * * * *}")
    @SchedulerLock(name = "rtAppInstanceStatusUpdateJob")
    public void run() throws InterruptedException {
        synchronized (threadPoolExecutorLock) {
            if (threadPoolExecutor == null) {
                return;
            }
        }
        Set<String> updateSet = rtAppInstanceService.getStatusUpdateSet();
        List<Future<UpdateTaskResult>> futures = new ArrayList<>();
        for (String appInstanceId : updateSet) {
            UpdateTaskResult result = new UpdateTaskResult();
            result.setAppInstanceId(appInstanceId);
            Future<UpdateTaskResult> future;
            try {
                future = threadPoolExecutor.submit(new UpdateTask(result), result);
            } catch (RejectedExecutionException e) {
                log.warn("cannot submit app instance status update task to thread pool, rejected|appInstanceId={}",
                        appInstanceId);
                continue;
            }
            futures.add(future);
        }

        // 等待本轮全部结束
        while (true) {
            int notReadyCount = 0;
            for (Future<UpdateTaskResult> future : futures) {
                if (future.isCancelled()) {
                    continue;
                }
                if (!future.isDone()) {
                    notReadyCount++;
                }
            }
            if (notReadyCount > 0) {
                log.info("current count for not ready app instance status update task is {}|size={}",
                        notReadyCount, futures.size());
                Thread.sleep(1000);
                continue;
            }
            for (Future<UpdateTaskResult> future : futures) {
                try {
                    UpdateTaskResult fr = future.get();
                    if (!fr.isSuccess()) {
                        rtAppInstanceService.asyncTriggerStatusUpdate(fr.getAppInstanceId());
                        log.warn("cannot update app instance status|appInstanceId={}|exception={}",
                                fr.getAppInstanceId(), fr.getMessage());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("failed to refresh component instance status|exception={}", ExceptionUtils.getStackTrace(e));
                }
            }
            return;
        }
    }

    /**
     * 更新状态任务
     */
    class UpdateTask implements Runnable {

        private final UpdateTaskResult result;

        UpdateTask(UpdateTaskResult result) {
            this.result = result;
        }

        @Override
        public void run() {
            try {
                rtAppInstanceService.removeStatusUpdateSet(result.getAppInstanceId());
                rtAppInstanceService.syncTriggerStatusUpdate(result.getAppInstanceId());
                result.setSuccess(true);
                result.setMessage("");
            } catch (Exception e) {
                result.setSuccess(false);
                result.setMessage(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    /**
     * 用于主线程和子线程交互结果的对象
     */
    @Data
    static class UpdateTaskResult {

        private String appInstanceId;

        private boolean success;

        private String message;
    }
}
