package com.alibaba.sreworks.appdev.server.controllers;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.appdev.server.params.AppDeployStartParam;
import com.alibaba.sreworks.appdev.server.params.AppDeployUpdateParam;
import com.alibaba.sreworks.appdev.server.services.AppInstallService;
import com.alibaba.sreworks.appdev.server.services.AppUninstallService;
import com.alibaba.sreworks.appdev.server.services.AppUpdateService;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.domain.DO.*;
import com.alibaba.sreworks.domain.repository.AppComponentInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppComponentRepository;
import com.alibaba.sreworks.domain.repository.AppInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppPackageRepository;
import com.alibaba.sreworks.domain.repository.AppRepository;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.sreworks.domain.services.SaveActionService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerComponentService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeployService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import io.kubernetes.client.openapi.ApiException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/appdev/appDeploy")
@Api(tags = "应用部署")
public class AppDeployController extends BaseController {

    @Autowired
    FlyadminAppmanagerDeployService flyadminAppmanagerDeployService;

    @Autowired
    AppInstanceRepository appInstanceRepository;

    @Autowired
    AppComponentRepository appComponentRepository;

    @Autowired
    AppComponentInstanceRepository appComponentInstanceRepository;

    @Autowired
    AppInstallService appInstallService;

    @Autowired
    AppUninstallService appUninstallService;

    @Autowired
    AppRepository appRepository;

    @Autowired
    FlyadminAppmanagerComponentService flyadminAppmanagerComponentService;

    @Autowired
    AppUpdateService appUpdateService;

    @Autowired
    AppPackageRepository appPackageRepository;

    @Autowired
    SaveActionService saveActionService;

    @Autowired
    ClusterRepository clusterRepository;

    @ApiOperation(value = "列表")
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(Long appId, String user) throws IOException, ApiException {
        user = StringUtil.isEmpty(user) ? getUserEmployeeId() : user;
        List<JSONObject> ret = flyadminAppmanagerDeployService.list(appId, user);
        for (JSONObject jsonObject : ret) {
            String appmanagerClusterId = jsonObject.getString("clusterId");
            jsonObject.put("appmanagerClusterId", appmanagerClusterId);
            if(StringUtils.isNotBlank(appmanagerClusterId) && appmanagerClusterId.endsWith("id")) {
                Long clusterId = Long.valueOf(appmanagerClusterId.replace("id", ""));
                Cluster cluster = clusterRepository.findFirstById(clusterId);
                jsonObject.put("clusterId", cluster.getId());
                jsonObject.put("clusterName", cluster.getName());
            }

        }
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "分页列表")
    @RequestMapping(value = "listPagination", method = RequestMethod.GET)
    public TeslaBaseResult list(Long appId, String user, Long page, Long pageSize, String stageId) throws IOException, ApiException {
        user = StringUtil.isEmpty(user) ? getUserEmployeeId() : user;
        if (page == null || page == 0L) {
            page = 1L;
        }
        if (pageSize == null || pageSize == 0L) {
            pageSize = 10L;
        }
        if (StringUtils.isBlank(stageId)){
            stageId = null;
        }
        JSONObject ret;
        if(appId != null){
            ret = flyadminAppmanagerDeployService.listPagination(appId, user, page.toString(), pageSize.toString(), stageId, null, null);
        }else{
            ret = flyadminAppmanagerDeployService.listPagination(null, user, page.toString(), pageSize.toString(), stageId, "source", "app");
        }
        for (int i = 0; i < ret.getJSONArray("items").size(); i++) {
            JSONObject jsonObject = ret.getJSONArray("items").getJSONObject(i);
            String appmanId = jsonObject.getString("appId");
            String appmanagerClusterId = jsonObject.getString("clusterId");
            jsonObject.put("appmanagerClusterId", appmanagerClusterId);
            if(StringUtils.isNotBlank(appmanagerClusterId) && appmanagerClusterId.endsWith("id")) {
                Long clusterId = Long.valueOf(appmanagerClusterId.replace("id", ""));
                Cluster cluster = clusterRepository.findFirstById(clusterId);
                jsonObject.put("clusterId", cluster.getId());
                jsonObject.put("clusterName", cluster.getName());
            }
            if(appmanId.startsWith("sreworks")){
                Long itemAppId = Long.valueOf(appmanId.replace("sreworks", ""));
                App appInfo = appRepository.findFirstById(itemAppId);
                jsonObject.put("appName", appInfo.getName());
            }
        }
        return buildSucceedResult(ret);
    }


    @ApiOperation(value = "详情")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long appInstanceId) {
        AppInstance appInstance = appInstanceRepository.findFirstById(appInstanceId);
        return buildSucceedResult(appInstance);
    }

    @ApiOperation(value = "部署")
    @RequestMapping(value = "deploy", method = RequestMethod.POST)
    public TeslaBaseResult deploy(Long appPackageId, @RequestBody AppDeployStartParam param)
        throws IOException, ApiException {

        if (appPackageId != null) {
            param.setAppPackageId(appPackageId);
        }
        AppPackage appPackage = appPackageRepository.findFirstById(param.getAppPackageId());
        if (param.getTeamId() == null) {
            param.setTeamId(appPackage.app().getTeamId());
        }

        // appInstance 创建
        AppInstance appInstance = param.toAppInstance(appPackage, getUserEmployeeId());
        appInstanceRepository.saveAndFlush(appInstance);

        // appComponentInstanceList 创建
        List<AppComponent> appComponentList = appPackage.appComponentList();
        List<AppComponentInstance> appComponentInstanceList = param.toAppComponentInstanceList(
            appInstance, appComponentList, getUserEmployeeId());
        appComponentInstanceRepository.saveAll(appComponentInstanceList);
        appComponentInstanceRepository.flush();

        // 部署
        appInstallService.deploy(appInstance, appComponentInstanceList);
        appInstanceRepository.saveAndFlush(appInstance);
        saveActionService.save(getUserEmployeeId(), "appInstance", appInstance.getId(), "部署应用实例");
        return buildSucceedResult(appInstance.getId());

    }

    @ApiOperation(value = "更新")
    @RequestMapping(value = "update", method = RequestMethod.POST)
    public TeslaBaseResult update(Long appInstanceId, @RequestBody AppDeployUpdateParam param)
        throws IOException, ApiException {
        saveActionService.save(getUserEmployeeId(), "appInstance", appInstanceId, "更新应用实例");
        AppInstance appInstance = appInstanceRepository.findFirstById(appInstanceId);
        if (param.getAppPackageId() == null) {
            param.setAppPackageId(appInstance.getAppPackageId());
        }

        AppPackage appPackage = appPackageRepository.findFirstById(param.getAppPackageId());

        // appInstance 更新
        param.patchAppInstance(appInstance, getUserEmployeeId());
        appInstanceRepository.saveAndFlush(appInstance);

        // appComponentInstanceList 获取
        List<AppComponent> appComponentList = appPackage.appComponentList();
        List<AppComponentInstance> appComponentInstanceList = param.toAppComponentInstanceList(
            appInstance, appComponentList, getUserEmployeeId());

        // 更新
        appUpdateService.update(appInstance, appComponentInstanceList, getUserEmployeeId());
        appInstanceRepository.saveAndFlush(appInstance);
        return buildSucceedResult(appInstance);

    }

    @ApiOperation(value = "卸载")
    @RequestMapping(value = "uninstall", method = RequestMethod.DELETE)
    public TeslaBaseResult uninstall(Long appInstanceId)
        throws IOException, ApiException {
        saveActionService.save(getUserEmployeeId(), "appInstance", appInstanceId, "卸载应用实例");
        AppInstance appInstance = appInstanceRepository.findFirstById(appInstanceId);
        appInstanceRepository.deleteById(appInstanceId);
        appComponentInstanceRepository.deleteAllByAppInstanceId(appInstanceId);
        appUninstallService.uninstall(appInstance);
        return buildSucceedResult("OK");

    }

}
