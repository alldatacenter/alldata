package com.alibaba.sreworks.health.controllers.incident;

import com.alibaba.sreworks.health.api.incident.IncidentTypeService;
import com.alibaba.sreworks.health.domain.req.incident.IncidentTypeCreateReq;
import com.alibaba.sreworks.health.domain.req.incident.IncidentTypeUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 异常类型入口
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/20 11:36
 */
@RestController
@RequestMapping(value = "/incident_type/")
@Api(tags = "异常类型")
public class IncidentTypeController extends BaseController {

    @Autowired
    IncidentTypeService incidentTypeService;

    @ApiOperation(value = "查询异常类型(根据ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "类型ID", paramType = "query"),
    })
    @RequestMapping(value = "getIncidentTypeById", method = RequestMethod.GET)
    public TeslaBaseResult getIncidentTypeById(@RequestParam(name = "id") Integer id) {
        return buildSucceedResult(incidentTypeService.getIncidentTypeById(id));
    }

    @ApiOperation(value = "查询异常类型列表")
    @RequestMapping(value = "getIncidentTypes", method = RequestMethod.GET)
    public TeslaBaseResult getIncidentTypes() {
        return buildSucceedResult(incidentTypeService.getIncidentTypes());
    }

    @ApiOperation(value = "创建异常类型")
    @RequestMapping(value = "createIncidentType", method = RequestMethod.POST)
    public TeslaBaseResult createIncidentType(@RequestHeader(name = "x-empid", required = false) String userId,
                                              @RequestBody IncidentTypeCreateReq req) throws Exception {
        req.setCreator(userId);
        req.setLastModifier(userId);
        return buildSucceedResult(incidentTypeService.addIncidentType(req));
    }

    @ApiOperation(value = "更新异常类型")
    @RequestMapping(value = "updateIncidentType", method = RequestMethod.POST)
    public TeslaBaseResult updateIncidentType(@RequestHeader(name = "x-empid", required = false) String userId,
                                              @RequestBody IncidentTypeUpdateReq req) throws Exception {
        req.setCreator(userId);
        req.setLastModifier(userId);
        return buildSucceedResult(incidentTypeService.updateIncidentType(req));
    }

    @ApiOperation(value = "删除异常类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "定义ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "deleteIncidentType", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteIncidentType(@RequestParam(name = "id") Integer id) throws Exception {
        return buildSucceedResult(incidentTypeService.deleteIncidentType(id));
    }
}
