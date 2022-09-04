package com.alibaba.sreworks.dataset.controllers.inter;

import com.alibaba.sreworks.dataset.api.inter.InterfaceConfigService;
import com.alibaba.sreworks.dataset.domain.req.inter.InterfaceConfigCreateReq;
import com.alibaba.sreworks.dataset.domain.req.inter.InterfaceConfigUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 数据接口配置信息Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/interfaceConfig_v2/")
@Api(tags = "数据接口配置信息v2")
public class InterfaceConfigControl extends BaseController {

    @Autowired
    InterfaceConfigService configService;

    @ApiOperation(value = "查询数据接口配置(根据数据接口ID)")
    @RequestMapping(value = "/getConfigById", method = RequestMethod.GET)
    public TeslaBaseResult getConfigById(@RequestParam(name = "interfaceId") Integer interfaceId) {
        return buildSucceedResult(configService.getConfigById(interfaceId));
    }

    @ApiOperation(value = "查询数据接口配置(全量)")
    @RequestMapping(value = "/getConfigs", method = RequestMethod.GET)
    public TeslaBaseResult getConfigs() {
        return buildSucceedResult(configService.getConfigs());
    }

    @ApiOperation(value = "新增数据接口")
    @RequestMapping(value = "/createConfig", method = RequestMethod.POST)
    public TeslaBaseResult createConfig(@RequestHeader(name = "x-empid", required = false) String userId,
                                                   @RequestBody InterfaceConfigCreateReq req) throws Exception {
        req.setCreator(userId);
        req.setLastModifier(userId);
        return buildSucceedResult(configService.addConfig(req));
    }

    @ApiOperation(value = "更新数据接口")
    @RequestMapping(value = "/updateConfig", method = RequestMethod.POST)
    public TeslaBaseResult updateConfig(@RequestHeader(name = "x-empid", required = false) String userId,
                                                   @RequestBody InterfaceConfigUpdateReq req) throws Exception {
        req.setCreator(userId);
        req.setLastModifier(userId);
        return buildSucceedResult(configService.updateConfig(req));
    }

    @ApiOperation(value = "删除数据接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "interfaceId", value = "接口ID", paramType = "query")
    })
    @RequestMapping(value = "/deleteConfigById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteConfigById(@RequestParam(name = "interfaceId") Integer interfaceId) throws Exception {
        return buildSucceedResult(configService.deleteConfigById(interfaceId));
    }

}
