package com.alibaba.tdata.aisp.server.common.schedule;

import java.util.Date;

import com.alibaba.tdata.aisp.server.common.properties.TaskRemainProperties;
import com.alibaba.tdata.aisp.server.repository.AnalyseTaskRepository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @ClassName: TaskResultCleanSchdule
 * @Author: dyj
 * @DATE: 2021-12-07
 * @Description:
 **/
@Component
@Slf4j
public class TaskResultCleanSchedule {
    @Autowired
    private AnalyseTaskRepository taskRepository;
    @Autowired
    private TaskRemainProperties taskRemainProperties;

    @Scheduled(initialDelay = 10000L, fixedRate = 86400000L)
    public void cleanResult(){
        Date now = new Date();
        Date date = DateUtils.addDays(now, -taskRemainProperties.getDays());
        taskRepository.cleanResult(date);
    }
}
