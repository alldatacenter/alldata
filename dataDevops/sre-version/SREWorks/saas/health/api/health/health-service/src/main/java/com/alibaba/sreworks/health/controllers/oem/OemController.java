package com.alibaba.sreworks.health.controllers.oem;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.services.oem.OemService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 内置应用健康数据入口
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 15:14
 */
@RestController
@RequestMapping("/oem/")
@Api(tags="内置应用健康数据")
public class OemController extends BaseController {

    @Autowired
    OemService oemService;

    @ApiOperation(value = "内置切换")
    @RequestMapping(value = "toggle", method = RequestMethod.POST)
    public TeslaBaseResult toggle(@RequestBody JSONObject data) throws Exception {
        String appId = data.getString("appId");
        Boolean enable = data.getBoolean("enable");

        if (enable) {
            oemService.open(appId);
        } else {
            oemService.close(appId);
        }
        return buildSucceedResult("success");
    }

    @ApiOperation(value = "开启")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "open", method = RequestMethod.POST)
    public TeslaBaseResult open(@RequestParam(name = "appId") String appId) throws Exception {
        oemService.open(appId);
        return buildSucceedResult("success");
    }

    @ApiOperation(value = "状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "state", method = RequestMethod.GET)
    public TeslaBaseResult state(@RequestParam(name = "appId") String appId) throws Exception {
        return buildSucceedResult(oemService.state(appId));
    }

    @ApiOperation(value = "关闭")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query", required = true),
    })
    @RequestMapping(value = "close", method = RequestMethod.DELETE)
    public TeslaBaseResult close(@RequestParam(name = "appId") String appId) throws Exception {
        oemService.close(appId);
        return buildSucceedResult("success");
    }
}
