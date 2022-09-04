package com.alibaba.sreworks.dataset.controllers.inter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.inter.InterfaceService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据接口
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/20 15:44
 */

@Slf4j
@RestController
@RequestMapping("/interface/{name}")
@Api(tags = "数据接口")
public class InterfaceController extends BaseController {

    @Autowired
    InterfaceService interfaceService;

    @ApiOperation(value = "查询接口(GET)")
    @GetMapping
    @ResponseBody
    public TeslaBaseResult get(@PathVariable String name, @RequestParam Map<String, Object> params) throws Exception {
        return buildSucceedResult(interfaceService.get(name, params));
    }

    @ApiOperation(value = "查询接口(POST)")
    @PostMapping
    @ResponseBody
    public TeslaBaseResult get(@PathVariable String name, @RequestParam Map<String, Object> params, @RequestBody(required = false) JSONObject body) throws Exception {
        return buildSucceedResult(interfaceService.get(name, params, body));
    }
}
