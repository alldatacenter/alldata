package com.alibaba.sreworks.flyadmin.server.controllers;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import io.kubernetes.client.openapi.ApiException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/flyadmin/appmanager")
@Api(tags = "应用")
public class FlyadminAppmanagerController extends BaseController {

    @Autowired
    FlyadminAppmanagerService flyadminAppmanagerService;

    @ApiOperation(value = "listApp")
    @RequestMapping(value = "listApp", method = RequestMethod.GET)
    public TeslaBaseResult listApp() throws IOException, ApiException {
        List<JSONObject> ret = flyadminAppmanagerService.listMarketApp(getUserEmployeeId());
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "listResourceAddon")
    @RequestMapping(value = "listResourceAddon", method = RequestMethod.GET)
    public TeslaBaseResult listResourceAddon() throws IOException, ApiException {
        List<JSONObject> ret = flyadminAppmanagerService.listResourceAddon(getUserEmployeeId());
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "listTrait")
    @RequestMapping(value = "listTrait", method = RequestMethod.GET)
    public TeslaBaseResult listTrait() throws IOException, ApiException {
        List<JSONObject> ret = flyadminAppmanagerService.listTrait(getUserEmployeeId());
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "k8sMicroservice")
    @RequestMapping(value = "k8sMicroservice", method = RequestMethod.GET)
    public TeslaBaseResult k8sMicroservice(String appId, String componentTypeList, @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp) throws IOException {
        return buildSucceedResult(
            flyadminAppmanagerService.k8sMicroservice(headerBizApp, getUserEmployeeId(), appId, componentTypeList)
        );
    }

    @ApiOperation(value = "helm")
    @RequestMapping(value = "helm", method = RequestMethod.GET)
    public TeslaBaseResult helm(String appId, @RequestHeader(value = "X-Biz-App", required = false) String headerBizApp) throws IOException {
        return buildSucceedResult(flyadminAppmanagerService.helm(headerBizApp, getUserEmployeeId(), appId));
    }
}
