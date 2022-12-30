package com.alibaba.sreworks.clustermanage.server.controllers;

import com.alibaba.sreworks.clustermanage.server.services.DeployOperatorService;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

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
@RequestMapping("/deployOperator")
@Api(tags = "部署")
public class DeployOperatorController extends BaseController {

    @Autowired
    DeployOperatorService deployOperatorService;

    @Autowired
    ClusterRepository clusterRepository;

    @ApiOperation(value = "部署")
    @RequestMapping(value = "deploy", method = RequestMethod.POST)
    public TeslaBaseResult deploy(Long clusterId) throws Exception {
        deployOperatorService.deployAndCheckOperator(clusterRepository.findFirstById(clusterId));
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "进度")
    @RequestMapping(value = "progress", method = RequestMethod.POST)
    public TeslaBaseResult progress(Long clusterId) throws Exception {
        return buildSucceedResult(
            deployOperatorService.progressOperator(clusterRepository.findFirstById(clusterId))
        );
    }

    @ApiOperation(value = "检测")
    @RequestMapping(value = "check", method = RequestMethod.POST)
    public TeslaBaseResult check(Long clusterId) throws Exception {
        return buildSucceedResult(
            deployOperatorService.checkOperatorStsStatus(clusterRepository.findFirstById(clusterId))
        );
    }
}
