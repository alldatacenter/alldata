package com.alibaba.sreworks.flyadmin.server.controllers;

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.sreworks.common.DTO.NameAlias;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.flyadmin.server.services.PluginAccountService;
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
@RequestMapping("/plugin/account")
@Api(tags = "插件-账号")
public class PluginAccountController extends BaseController {

    @Autowired
    PluginAccountService pluginAccountService;

    @ApiOperation(value = "typeSelector")
    @RequestMapping(value = "typeSelector", method = RequestMethod.GET)
    public TeslaBaseResult typeSelector() {
        return buildSucceedResult(JsonUtil.map(
            "options", pluginAccountService.serviceMap.keySet().stream().map(accountType -> JsonUtil.map(
                "label", accountType,
                "value", accountType
            )).collect(Collectors.toList())
        ));
    }

    @ApiOperation(value = "keys")
    @RequestMapping(value = "keys", method = RequestMethod.GET)
    public TeslaBaseResult keys(String type) throws Exception {
        return buildSucceedResult(
            pluginAccountService.keys(type, getUserEmployeeId())
        );
    }

    @ApiOperation(value = "demoDetail")
    @RequestMapping(value = "demoDetail", method = RequestMethod.GET)
    public TeslaBaseResult demoDetail(String type) throws Exception {
        List<NameAlias> nameAliasList = pluginAccountService.keys(type, getUserEmployeeId());
        return buildSucceedResult(
            nameAliasList.stream().collect(Collectors.toMap(NameAlias::getName, nameAlias -> ""))
        );
    }

}
