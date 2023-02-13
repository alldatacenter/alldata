package com.alibaba.sreworks.job.master.jobschedule.dag;

import com.alibaba.tesla.dag.services.DagInstClearService;
import com.alibaba.tesla.dag.services.TaskFlowDispatchService;
import lombok.extern.slf4j.Slf4j;
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
public class DagInstFixedRateSchedule {

    @Autowired
    private DagInstClearService dagInstClearService;

    @Autowired
    private TaskFlowDispatchService taskFlowDispatchService;

    @Scheduled(fixedRate = 600000)
    public void execute() {
        Date date = new Date(System.currentTimeMillis() - 7 * 86400 * 1000);
        dagInstClearService.clearDataBefore(date);
    }

    @Scheduled(fixedRate = 5000)
    private void taskFlowDispatchJob() {
        try {
            int timeoutCount = taskFlowDispatchService.dispatch(180, 15);
            log.info("action=taskFlowDispatchJob|execute|exit|timeoutCount={}", timeoutCount);
        } catch (Exception e) {
            log.error("action=taskFlowDispatchJob|execute|ERROR|err={}", e.getMessage(), e);
        }
    }
}
