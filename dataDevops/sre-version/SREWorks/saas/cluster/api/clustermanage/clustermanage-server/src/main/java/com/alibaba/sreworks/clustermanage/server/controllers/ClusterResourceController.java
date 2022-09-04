package com.alibaba.sreworks.clustermanage.server.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.clustermanage.server.params.ClusterResourceCreateParam;
import com.alibaba.sreworks.clustermanage.server.params.ClusterResourceModifyParam;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.domain.DO.ClusterResource;
import com.alibaba.sreworks.domain.repository.ClusterResourceRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAuthproxyUserService;
import com.alibaba.sreworks.flyadmin.server.services.PluginResourceService;
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
@RequestMapping("/clusterResource")
@Api(tags = "集群-资源")
public class ClusterResourceController extends BaseController {

    @Autowired
    ClusterResourceRepository clusterResourceRepository;

    @Autowired
    PluginResourceService pluginResourceService;

    @Autowired
    FlyadminAuthproxyUserService flyadminAuthproxyUserService;

    @ApiOperation(value = "创建")
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public TeslaBaseResult create(Long clusterId, @RequestBody ClusterResourceCreateParam param)
        throws IOException {
        param.setClusterId(clusterId);
        ClusterResource clusterResource = param.toClusterResource(getUserEmployeeId());
        clusterResource.setUsageDetail(JSONObject.toJSONString(pluginResourceService.getUsageDetail(
            clusterResource.getAccountId(),
            clusterResource.getType(),
            clusterResource.getInstanceName(),
            getUserEmployeeId()
        )));
        clusterResourceRepository.saveAndFlush(clusterResource);
        return buildSucceedResult(clusterResource.getId());
    }

    @ApiOperation(value = "删除")
    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(Long id) {
        clusterResourceRepository.deleteById(id);
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "修改")
    @RequestMapping(value = "modify", method = RequestMethod.POST)
    public TeslaBaseResult modify(@RequestBody ClusterResourceModifyParam param) {
        ClusterResource clusterResource = clusterResourceRepository.findFirstById(param.getId());
        param.patchClusterResource(clusterResource, getUserEmployeeId());
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "详情")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) {
        return buildSucceedResult(
            clusterResourceRepository.findFirstById(id)
        );
    }

    @ApiOperation(value = "列表")
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(Long clusterId) throws IOException, ApiException {
        List<JSONObject> ret = clusterResourceRepository.findObjectByClusterId(clusterId);
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        flyadminAuthproxyUserService.patchNickName(ret, getUserEmployeeId(), "creator");
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "selector")
    @RequestMapping(value = "selector", method = RequestMethod.GET)
    public TeslaBaseResult selector(Long clusterId) {
        List<ClusterResource> clusterResourceList = clusterResourceRepository.findAllByClusterId(clusterId);
        return buildSucceedResult(JsonUtil.map(
            "options", clusterResourceList.stream().map(clusterResource -> JsonUtil.map(
                "label", clusterResource.getName(),
                "value", clusterResource.getId()
            )).collect(Collectors.toList())
        ));
    }
}
