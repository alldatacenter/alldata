package com.alibaba.sreworks.appdev.server.schedules;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.repository.AppInstanceRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeployService;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppInstanceSchedule {

    @Autowired
    AppInstanceRepository appInstanceRepository;

    @Autowired
    FlyadminAppmanagerDeployService flyadminAppmanagerDeployService;

    private static final ScheduledThreadPoolExecutor SCHEDULE_POOL_EXECUTOR = new ScheduledThreadPoolExecutor(
        2, new BasicThreadFactory.Builder().namingPattern("%d").daemon(true).build()
    );

    @PostConstruct
    public void postConstruct() throws ApiException {

        SCHEDULE_POOL_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                refresh();
            } catch (Exception e) {
                log.error("", e);
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
    }

    public void refresh() {
        List<AppInstance> appInstanceList = appInstanceRepository.findAllByStatusNotIn(Arrays.asList(
            "WAIT_FOR_OP", "FAILURE", "SUCCESS", "EXCEPTION"));
        appInstanceList.parallelStream().filter(x -> !StringUtil.isEmpty(x.getStatus()))
            .forEach(appInstance -> {
                try {
                    JSONObject ret = flyadminAppmanagerDeployService.get(appInstance.getAppDeployId(), "999999999");
                    String deployStatus = ret.getString("deployStatus");
                    if (!appInstance.getStatus().equals(deployStatus)) {
                        appInstance.setStatus(deployStatus);
                        appInstanceRepository.saveAndFlush(appInstance);
                    }
                } catch (Exception e) {
                    log.error("", e);
                }
            });
    }

}
