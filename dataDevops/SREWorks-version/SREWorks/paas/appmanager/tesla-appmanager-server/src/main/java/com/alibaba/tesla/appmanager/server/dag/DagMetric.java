package com.alibaba.tesla.appmanager.server.dag;

import com.alibaba.tesla.dag.services.LocalTaskService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dag Metric 信息暴露
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class DagMetric {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private LocalTaskService localTaskService;

    @PostConstruct
    public void init() {
        ThreadPoolExecutor executor = localTaskService.getLocalTaskThreadPoolExecutor();
        meterRegistry.gauge("dag.localtask.threadpool.activeCount", executor,
                ThreadPoolExecutor::getActiveCount);
        meterRegistry.gauge("dag.localtask.threadpool.corePoolSize", executor,
                ThreadPoolExecutor::getCorePoolSize);
        meterRegistry.gauge("dag.localtask.threadpool.largestPoolSize", executor,
                ThreadPoolExecutor::getLargestPoolSize);
        meterRegistry.gauge("dag.localtask.threadpool.maximumPoolSize", executor,
                ThreadPoolExecutor::getMaximumPoolSize);
        meterRegistry.gauge("dag.localtask.threadpool.poolSize", executor,
                ThreadPoolExecutor::getPoolSize);
        meterRegistry.gauge("dag.localtask.threadpool.taskCount", executor,
                ThreadPoolExecutor::getTaskCount);
        meterRegistry.gauge("dag.localtask.threadpool.completedTaskCount", executor,
                ThreadPoolExecutor::getCompletedTaskCount);
    }
}
