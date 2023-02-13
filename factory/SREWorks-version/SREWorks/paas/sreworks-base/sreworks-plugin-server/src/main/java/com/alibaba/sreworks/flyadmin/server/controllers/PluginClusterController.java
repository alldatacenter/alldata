package com.alibaba.sreworks.flyadmin.server.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.sreworks.flyadmin.server.DTO.PluginCluster;
import com.alibaba.sreworks.flyadmin.server.services.PluginClusterService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

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
@RequestMapping("/plugin/cluster")
@Api(tags = "插件-集群")
public class PluginClusterController extends BaseController {

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    PluginClusterService pluginClusterService;

    @ApiOperation(value = "列表")
    @RequestMapping(value = "list", method = RequestMethod.POST)
    public TeslaBaseResult list(Long accountId) throws Exception {
        return buildSucceedResult(pluginClusterService.list(
            accountId, getUserEmployeeId()
        ));
    }

    @ApiOperation(value = "获取集群kubeconfig")
    @RequestMapping(value = "getKubeConfig", method = RequestMethod.POST)
    public TeslaBaseResult getKubeConfig(@RequestBody JSONObject param) throws Exception {
        return buildSucceedResult(pluginClusterService.getKubeConfig(
            param.getLong("accountId"),
            param.getString("clusterName"),
            getUserEmployeeId()
        ));
    }

    @ApiOperation(value = "nameSelector")
    @RequestMapping(value = "nameSelector", method = RequestMethod.GET)
    public TeslaBaseResult nameSelector(Long accountId) throws IOException {
        List<PluginCluster> remoteClusters = pluginClusterService.list(accountId, getUserEmployeeId());
        return buildSucceedResult(JsonUtil.map(
            "options", remoteClusters.stream().map(remoteCluster -> JsonUtil.map(
                "label", remoteCluster.getAlias(),
                "value", remoteCluster.getName()
            )).collect(Collectors.toList())
        ));
    }

}
