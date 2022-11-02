package com.alibaba.sreworks.health.controllers.event;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.event.EventInstanceService;
import com.alibaba.sreworks.health.domain.req.event.EventInstanceCreateReq;
import com.alibaba.sreworks.health.domain.req.event.EventInstanceUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 事件实例入口
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 17:42
 */
@RestController
@RequestMapping(value = "/event_instance/")
@Api(tags = "事件实例")
public class EventInstanceController extends BaseController {

    @Autowired
    EventInstanceService instanceService;

    @ApiOperation(value = "查询事件实例(根据id)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实例ID", paramType = "query")
    })
    @RequestMapping(value = "getEventById", method = RequestMethod.GET)
    public TeslaBaseResult getEventById(@RequestParam(name = "id")Long id) {
        return buildSucceedResult(instanceService.getEventById(id));
    }

    @ApiOperation(value = "查询事件实例列表(根据应用)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query")
    })
    @RequestMapping(value = "getEventsByApp", method = RequestMethod.GET)
    public TeslaBaseResult getEventsByApp(@RequestParam(name = "appId") String appId,
                                          @RequestParam(name = "sTimestamp", required = false)Long sTimestamp,
                                          @RequestParam(name = "eTimestamp", required = false)Long eTimestamp) {
        return buildSucceedResult(instanceService.getEventsByApp(appId, sTimestamp, eTimestamp));
    }

    @ApiOperation(value = "查询事件实例列表(兼容全量接口)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query"),
            @ApiImplicitParam(name = "appComponentName", value = "应用组件", paramType = "query"),
            @ApiImplicitParam(name = "appComponentInstanceId", value = "应用组件实例ID", paramType = "query")
    })
    @RequestMapping(value = "getEvents", method = RequestMethod.GET)
    public TeslaBaseResult getEvents(@RequestParam(name = "appId", required = false) String appId,
                                     @RequestParam(name = "appInstanceId", required = false) String appInstanceId,
                                     @RequestParam(name = "appComponentName", required = false) String appComponentName,
                                     @RequestParam(name = "appComponentInstanceId", required = false) String appComponentInstanceId,
                                     @RequestParam(name = "sTimestamp", required = false)Long sTimestamp,
                                     @RequestParam(name = "eTimestamp", required = false)Long eTimestamp) {
        return buildSucceedResult(instanceService.getEvents(appId, appInstanceId, appComponentName, appComponentInstanceId, sTimestamp, eTimestamp));
    }

    @ApiOperation(value = "创建事件实例")
    @RequestMapping(value = "createEvent", method = RequestMethod.POST)
    public TeslaBaseResult createEvent(@RequestBody EventInstanceCreateReq req) throws Exception {
        return buildSucceedResult(instanceService.addEvent(req));
    }

    @ApiOperation(value = "根据定义创建事件实例(批量)")
    @RequestMapping(value = "pushEvents", method = RequestMethod.POST)
    public TeslaBaseResult pushEvents(@RequestParam(name = "defId") Integer defId,
                                                @RequestBody JSONObject[] events) throws Exception {
        return buildSucceedResult(instanceService.pushEvents(defId, Arrays.asList(events)));
    }

    @ApiOperation(value = "更新事件实例")
    @RequestMapping(value = "updateEvent", method = RequestMethod.POST)
    public TeslaBaseResult updateEvent(@RequestBody EventInstanceUpdateReq req) throws Exception {
        return buildSucceedResult(instanceService.updateEvent(req));
    }

    @ApiOperation(value = "删除事件实例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "定义ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "deleteEvent", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteEvent(@RequestParam(name = "id") Long id) throws Exception {
        return buildSucceedResult(instanceService.deleteEvent(id));
    }
}
