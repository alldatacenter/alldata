package com.alibaba.sreworks.appdev.server.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.appdev.server.params.AppPackageStartParam;
import com.alibaba.sreworks.appdev.server.services.AppPackageService;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DO.AppPackage;
import com.alibaba.sreworks.domain.repository.AppPackageRepository;
import com.alibaba.sreworks.domain.services.SaveActionService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerPackageService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAuthproxyUserService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import io.kubernetes.client.openapi.ApiException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/appdev/appPackage")
@Api(tags = "应用构建")
public class AppPackageController extends BaseController {

    @Autowired
    FlyadminAppmanagerPackageService flyadminAppmanagerPackageService;

    @Autowired
    AppPackageService appPackageService;

    @Autowired
    AppPackageRepository appPackageRepository;

    @Autowired
    FlyadminAuthproxyUserService flyadminAuthproxyUserService;

    @Autowired
    SaveActionService saveActionService;

    @ApiOperation(value = "构建历史")
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(Long appId) throws IOException, ApiException {
        List<AppPackage> appPackageList = appPackageRepository.findAllByAppIdOrderByIdDesc(appId);
        List<JSONObject> ret = appPackageList.stream().map(AppPackage::toJsonObject).collect(Collectors.toList());
        flyadminAuthproxyUserService.patchNickName(ret, getUserEmployeeId(), "creator");
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "idSelector")
    @RequestMapping(value = "idSelector", method = RequestMethod.GET)
    public TeslaBaseResult idSelector() {
        List<AppPackage> appPackageList = appPackageRepository.findAllByOnSale(1);
        return buildSucceedResult(JsonUtil.map(
            "options", appPackageList.stream().map(appPackage -> JsonUtil.map(
                "label", appPackage.app().getName() + "#" + appPackage.getSimpleVersion(),
                "value", appPackage.getId()
            )).collect(Collectors.toList())
        ));
    }

    @ApiOperation(value = "selector")
    @RequestMapping(value = "selector", method = RequestMethod.GET)
    public TeslaBaseResult selector(Long appId) {
        List<AppPackage> appPackageList = appPackageRepository.findAllByStatusAndAppIdOrderByIdDesc("SUCCESS", appId);
        return buildSucceedResult(JsonUtil.map(
            "options", appPackageList.stream().map(appPackage -> JsonUtil.map(
                "label", appPackage.getSimpleVersion(),
                "value", appPackage.getId()
            )).collect(Collectors.toList())
        ));
    }

    @ApiOperation(value = "启动")
    @RequestMapping(value = "start", method = RequestMethod.POST)
    public TeslaBaseResult start(Long appId, @RequestBody AppPackageStartParam param) throws IOException, ApiException {
        AppPackage appPackage = appPackageService.start(appId, param.getVersion(), getUserEmployeeId());
        saveActionService.save(getUserEmployeeId(), "app", appId, "构建应用" + appPackage.getSimpleVersion());
        return buildSucceedResult(appPackage);
    }

    @ApiOperation(value = "getConfig")
    @RequestMapping(value = "getConfig", method = RequestMethod.GET)
    public TeslaBaseResult getConfig(Long appId) throws IOException {
        JSONObject postJson = flyadminAppmanagerPackageService.getConfig(appId, "");
        return buildSucceedResult(YamlUtil.toYaml(postJson));
    }

    @ApiOperation(value = "最新版本")
    @RequestMapping(value = "lastVersion", method = RequestMethod.GET)
    public TeslaBaseResult lastVersion(Long appId) throws IOException, ApiException {
        return buildSucceedResult(flyadminAppmanagerPackageService.getLastVersion(appId, getUserEmployeeId()));
    }

    @ApiOperation(value = "onSale")
    @RequestMapping(value = "onSale", method = RequestMethod.POST)
    public TeslaBaseResult onSale(Long id) {
        AppPackage appPackage = appPackageRepository.findFirstById(id);
        saveActionService.save(
            getUserEmployeeId(), "app", appPackage.getAppId(), "上架应用版本" + appPackage.getSimpleVersion());
        appPackage.setOnSale(1);
        appPackage.setGmtModified(System.currentTimeMillis() / 1000);
        appPackage.setLastModifier(getUserEmployeeId());
        appPackageRepository.saveAndFlush(appPackage);
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "offSale")
    @RequestMapping(value = "offSale", method = RequestMethod.DELETE)
    public TeslaBaseResult offSale(Long id) {
        AppPackage appPackage = appPackageRepository.findFirstById(id);
        saveActionService.save(
            getUserEmployeeId(), "app", appPackage.getAppId(), "下架应用版本" + appPackage.getSimpleVersion());
        appPackage.setOnSale(0);
        appPackage.setGmtModified(System.currentTimeMillis() / 1000);
        appPackage.setLastModifier(getUserEmployeeId());
        appPackageRepository.saveAndFlush(appPackage);
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "get")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) {
        AppPackage appPackage = appPackageRepository.findFirstById(id);
        JSONObject ret = appPackage.toJsonObject();
        ret.put("appDetail", appPackage.app().detail());
        ret.put("appComponentDetail", appPackage.appComponentList().stream().collect(Collectors.toMap(
            AppComponent::getName,
            AppComponent::detail
        )));
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "logs")
    @RequestMapping(value = "logs", method = RequestMethod.GET)
    public TeslaBaseResult logs(Long id) throws IOException, ApiException {
        AppPackage appPackage = appPackageRepository.findFirstById(id);
        return buildSucceedResult(flyadminAppmanagerPackageService.logs(appPackage, getUserEmployeeId()));
    }

}
