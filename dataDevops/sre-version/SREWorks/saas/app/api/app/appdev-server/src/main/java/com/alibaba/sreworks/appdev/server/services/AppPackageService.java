package com.alibaba.sreworks.appdev.server.services;

import java.io.IOException;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.AppPackage;
import com.alibaba.sreworks.domain.repository.AppComponentRepository;
import com.alibaba.sreworks.domain.repository.AppPackageRepository;
import com.alibaba.sreworks.domain.repository.AppRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerPackageService;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.alibaba.sreworks.domain.utils.AppUtil.appmanagerId;

@Slf4j
@Service
public class AppPackageService {

    @Autowired
    AppRepository appRepository;

    @Autowired
    AppComponentRepository appComponentRepository;

    @Autowired
    AppPackageRepository appPackageRepository;

    @Autowired
    FlyadminAppmanagerPackageService flyadminAppmanagerPackageService;

    public AppPackage start(Long appId, String version, String user) throws IOException, ApiException {
        JSONObject startRet = flyadminAppmanagerPackageService.start(appId, version, user);
        Long appPackageTaskId = startRet.getLong("appPackageTaskId");
        AppPackage appPackage = new AppPackage(
            user,
            appRepository.findFirstById(appId),
            appComponentRepository.findAllByAppId(appId),
            appPackageTaskId,
            version
        );
        JSONObject packageJson = flyadminAppmanagerPackageService.get(appmanagerId(appId), appPackageTaskId, user);
        appPackage.setStatus(packageJson.getString("taskStatus"));
        appPackage.setVersion(packageJson.getString("packageVersion"));
        appPackageRepository.saveAndFlush(appPackage);
        return appPackage;
    }

}
