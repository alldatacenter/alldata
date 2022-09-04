package com.alibaba.sreworks.appcenter.server.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.appcenter.server.services.AppComponentInstanceService;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DTO.Port;
import com.alibaba.sreworks.domain.repository.AppComponentInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppInstanceRepository;
import com.alibaba.sreworks.flyadmin.server.services.MicroServiceService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import io.kubernetes.client.openapi.ApiException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/appcenter/appComponentInstance")
@Api(tags = "应用组件实例")
public class AppComponentInstanceController extends BaseController {

    @Autowired
    MicroServiceService microServiceService;

    @Autowired
    AppInstanceRepository appInstanceRepository;

    @Autowired
    AppComponentInstanceRepository appComponentInstanceRepository;

    @Autowired
    AppComponentInstanceService appComponentInstanceService;

    @ApiOperation(value = "list")
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(Long appInstanceId) throws IOException, ApiException {
        AppInstance appInstance = appInstanceRepository.findFirstById(appInstanceId);
        List<JSONObject> ret = new ArrayList<>();
        for (AppComponentInstance appComponentInstance :
            appComponentInstanceRepository.findAllByAppInstanceId(appInstanceId)) {
            JSONObject jsonObject = appComponentInstance.toJsonObject();
            try {
                jsonObject.put("microservice", microServiceService.get(appInstance, appComponentInstance));
                jsonObject.put("service", microServiceService.getService(appInstance, appComponentInstance));
            } catch (Exception ignored) {
            }
            ret.add(jsonObject);
        }
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "get")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) throws IOException {
        AppComponentInstance appComponentInstance = appComponentInstanceRepository
            .findFirstById(id);
        AppInstance appInstance = appInstanceRepository.findFirstById(appComponentInstance.getAppInstanceId());
        JSONObject ret = appComponentInstance.toJsonObject();
        try {
            ret.put("microservice", microServiceService.get(appInstance, appComponentInstance));
            ret.put("detailObject", appComponentInstance.detail());
        } catch (Exception ignored) {
        }
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "getComponentDetail")
    @RequestMapping(value = "getComponentDetail", method = RequestMethod.GET)
    public TeslaBaseResult getComponentDetail(Long id) {
        List<AppComponentInstance> appComponentInstanceList = appComponentInstanceRepository.findAllByAppInstanceId(id);
        return buildSucceedResult(appComponentInstanceList.stream().collect(Collectors.toMap(
            AppComponentInstance::getName,
            AppComponentInstance::detail
        )));
    }

    @ApiOperation(value = "stop")
    @RequestMapping(value = "stop", method = RequestMethod.DELETE)
    public TeslaBaseResult stop(Long id) throws IOException {
        appComponentInstanceService.stop(id);
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "start")
    @RequestMapping(value = "start", method = RequestMethod.POST)
    public TeslaBaseResult start(Long id) throws IOException {
        appComponentInstanceService.start(id);
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "restart")
    @RequestMapping(value = "restart", method = RequestMethod.POST)
    public TeslaBaseResult restart(Long id) throws IOException, ApiException {
        appComponentInstanceService.restart(id);
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "createIngress")
    @RequestMapping(value = "createIngress", method = RequestMethod.POST)
    public TeslaBaseResult createIngress(Long id, Long portValue) throws IOException, ApiException {
        AppComponentInstance appComponentInstance = appComponentInstanceRepository.findFirstById(id);
        Port port = appComponentInstance.detail().ports().stream()
            .filter(x -> x.getValue().equals(portValue)).collect(Collectors.toList()).get(0);
        appComponentInstanceService.createIngress(id, port, getUserEmployeeId());
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "listPort")
    @RequestMapping(value = "listPort", method = RequestMethod.GET)
    public TeslaBaseResult listPort(Long id) {
        AppComponentInstance appComponentInstance = appComponentInstanceRepository.findFirstById(id);
        AppInstance appInstance = appInstanceRepository.findFirstById(appComponentInstance.getAppInstanceId());
        return buildSucceedResult(appComponentInstance.detail().ports().stream().map(port -> JsonUtil.map(
            "name", port.getName(),
            "value", port.getValue(),
            "service", appComponentInstance.microserviceName(appInstance),
            "ingress", appComponentInstanceService.getIngressHost(appInstance, appComponentInstance, port)
        )).collect(Collectors.toList()));
    }

    @ApiOperation(value = "metricOn")
    @RequestMapping(value = "metricOn", method = RequestMethod.POST)
    public TeslaBaseResult metricOn(Long id) throws IOException, ApiException {
        AppComponentInstance appComponentInstance = appComponentInstanceRepository.findFirstById(id);
        AppInstance appInstance = appInstanceRepository.findFirstById(appComponentInstance.getAppInstanceId());
        microServiceService.metricOn(appInstance, appComponentInstance);
        appComponentInstanceRepository.saveAndFlush(appComponentInstance.setMetricOn(true, getUserEmployeeId()));
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "metricOff")
    @RequestMapping(value = "metricOff", method = RequestMethod.DELETE)
    public TeslaBaseResult metricOff(Long id) throws IOException, ApiException {
        AppComponentInstance appComponentInstance = appComponentInstanceRepository.findFirstById(id);
        AppInstance appInstance = appInstanceRepository.findFirstById(appComponentInstance.getAppInstanceId());
        microServiceService.metricOff(appInstance, appComponentInstance);
        appComponentInstanceRepository.saveAndFlush(appComponentInstance.setMetricOn(false, getUserEmployeeId()));
        return buildSucceedResult("OK");
    }

}
