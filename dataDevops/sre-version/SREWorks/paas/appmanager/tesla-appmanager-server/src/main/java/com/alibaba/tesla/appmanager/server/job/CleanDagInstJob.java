package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.dag.services.DagInstClearService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 清理 DB 和 MinIO 中的历史应用包/组件包，并清理远端存储里的无用数据
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class CleanDagInstJob {

    @Autowired
    private DagInstClearService dagInstClearService;

    @Autowired
    private SystemProperties systemProperties;

    @Scheduled(cron = "${appmanager.cron-job.clean-dag-inst:-}")
    @SchedulerLock(name = "cleanDagInst")
    public void execute() {
        Integer keepSeconds = systemProperties.getFlowHistoryKeepSeconds();
        Date date = new Date(System.currentTimeMillis() - keepSeconds * 1000);
        dagInstClearService.clearDataBefore(date);
    }
}
