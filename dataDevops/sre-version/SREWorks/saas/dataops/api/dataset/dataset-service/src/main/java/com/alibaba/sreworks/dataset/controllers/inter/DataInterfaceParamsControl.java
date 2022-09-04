package com.alibaba.sreworks.dataset.controllers.inter;

import com.alibaba.sreworks.dataset.api.inter.DataInterfaceConfigService;
import com.alibaba.sreworks.dataset.api.inter.DataInterfaceParamsService;
import com.alibaba.sreworks.dataset.common.exception.ModelNotExistException;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceParamCreateReq;
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
 * 数据模型元信息Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/interfaceParams/")
@Api(tags = "数据接口参数信息")
public class DataInterfaceParamsControl extends BaseController {

    @Autowired
    DataInterfaceConfigService interfaceConfigService;

    @Autowired
    DataInterfaceParamsService interfaceParamsService;

    @ApiOperation(value = "根据ID查询数据接口参数")
    @RequestMapping(value = "/getInterfaceParamsById", method = RequestMethod.GET)
    public TeslaBaseResult getInterfaceParamsById(@RequestParam(name = "interfaceId") Integer interfaceId) {
        return buildSucceedResult(interfaceParamsService.getParams(interfaceId));
    }

    @ApiOperation(value = "新增数据接口参数")
    @RequestMapping(value = "/createInterfaceParam", method = RequestMethod.POST)
    public TeslaBaseResult createInterfaceParam(@RequestBody DataInterfaceParamCreateReq paramReq) throws Exception {
        if (!interfaceConfigService.existInterface(paramReq.getInterfaceId())) {
            throw new ModelNotExistException(String.format("数据接口不存在,请检查参数,模型ID:%s", paramReq.getInterfaceId()));
        }
        interfaceParamsService.addInterfaceParam(paramReq);
        return buildSucceedResult("");
    }

    @ApiOperation(value = "根据接口ID删除参数")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "interfaceId", value = "接口ID", paramType = "query")
    })
    @RequestMapping(value = "/deleteParamsByInterfaceId", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteParamsByInterfaceId(@RequestParam(name = "interfaceId") Integer interfaceId) throws Exception {
        return buildSucceedResult(interfaceParamsService.deleteInterfaceParams(interfaceId));
    }

    @ApiOperation(value = "删除参数")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "interfaceId", value = "接口ID", paramType = "query"),
            @ApiImplicitParam(name = "paramId", value = "参数ID", paramType = "query"),
    })
    @RequestMapping(value = "/deleteParam", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteParam(@RequestParam(name = "teamId") Integer interfaceId, @RequestParam(name = "appId") Long paramId) throws Exception {
        return buildSucceedResult(interfaceParamsService.deleteInterfaceParam(interfaceId, paramId));
    }
}
