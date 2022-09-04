package com.alibaba.sreworks.clustermanage.server.controllers;

import com.alibaba.sreworks.clustermanage.server.services.DeployClientService;
import com.alibaba.sreworks.domain.DO.Cluster;
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
@RequestMapping("/deployClient")
@Api(tags = "deployClient")
public class DeployClientController extends BaseController {

    @Autowired
    DeployClientService deployClientService;

    @Autowired
    ClusterRepository clusterRepository;

    @ApiOperation(value = "部署")
    @RequestMapping(value = "deploy", method = RequestMethod.POST)
    public TeslaBaseResult deploy(Long clusterId) throws Exception {
        Cluster cluster = clusterRepository.findFirstById(clusterId);
        String kubeconfig = cluster.getKubeconfig();
        deployClientService.run(kubeconfig);
        return buildSucceedResult("OK");
    }

}
