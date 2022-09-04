package com.alibaba.sreworks.flyadmin.server.controllers;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeployService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerService;
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
@RequestMapping("/flyadmin/appmanagerDeploy")
@Api(tags = "应用")
public class FlyadminAppmanagerDeployController extends BaseController {

    @Autowired
    FlyadminAppmanagerDeployService flyadminAppmanagerDeployService;

    @ApiOperation(value = "logs")
    @RequestMapping(value = "logs", method = RequestMethod.GET)
    public TeslaBaseResult logs(String id) throws IOException, ApiException {

        return buildSucceedResult(flyadminAppmanagerDeployService.logs(id, getUserEmployeeId()));

    }
}
