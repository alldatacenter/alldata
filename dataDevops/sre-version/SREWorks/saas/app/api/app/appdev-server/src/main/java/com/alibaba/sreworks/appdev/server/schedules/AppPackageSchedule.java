package com.alibaba.sreworks.appdev.server.schedules;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DO.AppPackage;
import com.alibaba.sreworks.domain.repository.AppInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppPackageRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeployService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerPackageService;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.alibaba.sreworks.domain.utils.AppUtil.appmanagerId;

@Slf4j
@Service
public class AppPackageSchedule {

    @Autowired
    AppPackageRepository appPackageRepository;

    @Autowired
    FlyadminAppmanagerPackageService flyadminAppmanagerPackageService;

    private static final ScheduledThreadPoolExecutor SCHEDULE_POOL_EXECUTOR = new ScheduledThreadPoolExecutor(
        2, new BasicThreadFactory.Builder().namingPattern("%d").daemon(true).build()
    );

    @PostConstruct
    public void postConstruct() {

        SCHEDULE_POOL_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                refresh();
            } catch (Exception e) {
                log.error("", e);
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);

    }

    public void refresh() {
        List<AppPackage> appPackageList = appPackageRepository.findAllByStatusNotIn(Arrays.asList(
            "WAIT_FOR_OP", "FAILURE", "SUCCESS", "EXCEPTION"));
        appPackageList.parallelStream().filter(x -> !StringUtil.isEmpty(x.getStatus()))
            .forEach(appPackage -> {
                try {
                    JSONObject ret = flyadminAppmanagerPackageService.get(
                        appmanagerId(appPackage.getAppId()), appPackage.getAppPackageTaskId(), "999999999"
                    );
                    String taskStatus = ret.getString("taskStatus");
                    if (!appPackage.getStatus().equals(taskStatus)) {
                        appPackage.setStatus(taskStatus);
                        appPackage.setAppPackageId(ret.getLong("appPackageId"));
                        appPackage.setSimpleVersion(ret.getString("simplePackageVersion"));
                        appPackage.setVersion(ret.getString("packageVersion"));
                        appPackageRepository.saveAndFlush(appPackage);
                    }
                } catch (Exception e) {
                    log.error("", e);
                }
            });
    }

}
