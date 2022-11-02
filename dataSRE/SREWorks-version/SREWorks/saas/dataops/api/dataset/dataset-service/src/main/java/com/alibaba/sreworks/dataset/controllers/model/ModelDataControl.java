//package com.alibaba.sreworks.dataset.controllers.model;
//
//import com.alibaba.fastjson.JSONObject;
//import com.alibaba.sreworks.dataset.api.model.ModelDataService;
//import com.alibaba.tesla.common.base.TeslaBaseResult;
//import com.alibaba.tesla.web.controller.BaseController;
//import io.swagger.annotations.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Arrays;
//
///**
// * 模型数据Control
// *
// * @author: fangzong.lyj@alibaba-inc.com
// * @date: 2020/12/23 14:55
// */
//
//@Slf4j
//@RestController
//@RequestMapping("/modelData/")
//@Api(tags = "模型数据接口")
//public class ModelDataControl extends BaseController {
//
//    @Autowired
//    ModelDataService modelDataService;
//
//    @ApiOperation(value = "模型单条数据写入(需要预先定义模型)")
//    @RequestMapping(value = "/pushData", method = RequestMethod.POST)
//    public TeslaBaseResult pushModelData(@RequestParam(name = "modeId") Integer modeId,
//                                              @RequestBody @ApiParam(value = "数据") JSONObject node) throws Exception {
//        return buildSucceedResult(modelDataService.flushModelData(modeId, node));
//    }
//
//    @ApiOperation(value = "模型多条数据写入(需要预先定义模型)")
//    @RequestMapping(value = "/pushDatas", method = RequestMethod.POST)
//    public TeslaBaseResult pushModelDatas(@RequestParam(name = "modeId") Integer modeId,
//                                              @RequestBody @ApiParam(value = "数据列表") JSONObject[] nodes) throws Exception {
//        return buildSucceedResult(modelDataService.flushModelDatas(modeId, Arrays.asList(nodes)));
//    }
//}
