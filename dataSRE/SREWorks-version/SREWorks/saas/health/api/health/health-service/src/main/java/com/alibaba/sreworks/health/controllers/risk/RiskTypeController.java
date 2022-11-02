//package com.alibaba.sreworks.health.controllers.risk;
//
//import com.alibaba.sreworks.health.api.risk.RiskTypeService;
//import com.alibaba.sreworks.health.domain.req.risk.RiskTypeCreateReq;
//import com.alibaba.sreworks.health.domain.req.risk.RiskTypeUpdateReq;
//import com.alibaba.tesla.common.base.TeslaBaseResult;
//import com.alibaba.tesla.web.controller.BaseController;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiImplicitParam;
//import io.swagger.annotations.ApiImplicitParams;
//import io.swagger.annotations.ApiOperation;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
///**
// * 风险类型入口
// *
// * @author: fangzong.lyj@alibaba-inc.com
// * @date: 2021/10/20 11:36
// */
//@RestController
//@RequestMapping(value = "/risk_type/")
//@Api(tags = "风险类型")
//public class RiskTypeController extends BaseController {
//
//    @Autowired
//    RiskTypeService riskTypeService;
//
//    @ApiOperation(value = "查询风险类型(根据ID)")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "id", value = "类型ID", paramType = "query"),
//    })
//    @RequestMapping(value = "getRiskTypeById", method = RequestMethod.GET)
//    public TeslaBaseResult getRiskTypeById(@RequestParam(name = "id") Integer id) {
//        return buildSucceedResult(riskTypeService.getRiskTypeById(id));
//    }
//
//    @ApiOperation(value = "查询风险类型列表")
//    @RequestMapping(value = "getRiskTypes", method = RequestMethod.GET)
//    public TeslaBaseResult getRiskTypes() {
//        return buildSucceedResult(riskTypeService.getRiskTypes());
//    }
//
//    @ApiOperation(value = "创建风险类型")
//    @RequestMapping(value = "createRiskType", method = RequestMethod.POST)
//    public TeslaBaseResult createRiskType(@RequestBody RiskTypeCreateReq req) {
//        return buildSucceedResult(riskTypeService.addRiskType(req));
//    }
//
//    @ApiOperation(value = "更新风险类型")
//    @RequestMapping(value = "updateRiskType", method = RequestMethod.POST)
//    public TeslaBaseResult updateRiskType(@RequestBody RiskTypeUpdateReq req) throws Exception {
//        return buildSucceedResult(riskTypeService.updateRiskType(req));
//    }
//
//    @ApiOperation(value = "删除风险类型")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "id", value = "定义ID", paramType = "query", required = true)
//    })
//    @RequestMapping(value = "deleteRiskType", method = RequestMethod.DELETE)
//    public TeslaBaseResult deleteRiskType(@RequestParam(name = "id") Integer id) throws Exception {
//        return buildSucceedResult(riskTypeService.deleteRiskType(id));
//    }
//}
