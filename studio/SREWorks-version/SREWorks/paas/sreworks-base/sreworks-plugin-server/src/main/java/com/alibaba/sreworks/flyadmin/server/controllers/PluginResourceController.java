package com.alibaba.sreworks.flyadmin.server.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.domain.DO.TeamAccount;
import com.alibaba.sreworks.domain.repository.TeamAccountRepository;
import com.alibaba.sreworks.flyadmin.server.DTO.PluginResource;
import com.alibaba.sreworks.flyadmin.server.services.PluginResourceService;
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
@RequestMapping("/plugin/resource")
@Api(tags = "插件-资源")
public class PluginResourceController extends BaseController {

    @Autowired
    PluginResourceService pluginResourceService;

    @Autowired
    TeamAccountRepository teamAccountRepository;

    @ApiOperation(value = "typeSelector")
    @RequestMapping(value = "typeSelector", method = RequestMethod.GET)
    public TeslaBaseResult typeSelector(Long accountId) {
        TeamAccount teamAccount = teamAccountRepository.findFirstById(accountId);
        Set<String> resourceTypeSet = pluginResourceService.serviceMap.get(teamAccount.getType()).keySet();
        return buildSucceedResult(JsonUtil.map(
            "options", resourceTypeSet.stream().map(resourceType -> JsonUtil.map(
                "label", resourceType,
                "value", resourceType
            )).collect(Collectors.toList())
        ));
    }

    @ApiOperation(value = "nameSelector")
    @RequestMapping(value = "nameSelector", method = RequestMethod.GET)
    public TeslaBaseResult nameSelector(Long accountId, String resourceType) throws IOException {

        TeamAccount teamAccount = teamAccountRepository.findFirstById(accountId);
        List<PluginResource> remoteResources = pluginResourceService.list(teamAccount.getType(),
            resourceType, teamAccount.detail(), getUserEmployeeId());

        return buildSucceedResult(JsonUtil.map(
            "options", remoteResources.stream().map(remoteResource -> JsonUtil.map(
                "label", remoteResource.getAlias(),
                "value", remoteResource.getName()
            )).collect(Collectors.toList())
        ));
    }
}
