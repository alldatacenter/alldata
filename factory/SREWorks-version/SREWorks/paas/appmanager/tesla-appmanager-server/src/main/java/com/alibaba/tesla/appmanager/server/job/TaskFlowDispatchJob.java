package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.tesla.dag.services.TaskFlowDispatchService;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Configuration
@EnableScheduling
@Slf4j(topic = "job")
public class TaskFlowDispatchJob {
    @Autowired
    private TaskFlowDispatchService taskFlowDispatchService;

    @Scheduled(fixedRate = 5000)
    public void taskFlowDispatchJob() {
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            log.info("action=taskFlowDispatchJob|execute|enter");
            int timeoutCount = taskFlowDispatchService.dispatch(180, 15);
            log.info("action=taskFlowDispatchJob|execute|exit|timeoutCount={}, cost={}", timeoutCount,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
        } catch (Throwable e) {
            log.error("action=taskFlowDispatchJob|execute|ERROR|err={}", e.getMessage(), e);
        }
    }
}
