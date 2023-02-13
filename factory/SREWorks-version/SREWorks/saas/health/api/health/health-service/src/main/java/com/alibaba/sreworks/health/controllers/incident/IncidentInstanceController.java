package com.alibaba.sreworks.health.controllers.incident;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.incident.IncidentInstanceService;
import com.alibaba.sreworks.health.domain.req.incident.DefAppComponentInstanceReq;
import com.alibaba.sreworks.health.domain.req.incident.IncidentInstanceCreateReq;
import com.alibaba.sreworks.health.domain.req.incident.IncidentInstanceHealingReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * 异常实例入口
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 17:42
 */
@RestController
@RequestMapping(value = "/incident_instance/")
@Api(tags = "异常实例")
public class IncidentInstanceController extends BaseController {

    @Autowired
    IncidentInstanceService instanceService;

    @ApiOperation(value = "查询异常实例聚合数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "timeUnit", value = "聚合时间", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query")
    })
    @RequestMapping(value = "getIncidentsTimeAgg", method = RequestMethod.GET)
    public TeslaBaseResult getIncidentsTimeAgg(@RequestParam(name = "timeUnit", required = false)String timeUnit,
                                           @RequestParam(name = "sTimestamp", required = false)Long sTimestamp,
                                           @RequestParam(name = "eTimestamp", required = false)Long eTimestamp,
                                               @RequestParam(name = "appInstanceId", required = false)String appInstanceId) {
        if (StringUtils.isEmpty(timeUnit)) {
            timeUnit = "d";
        }
        Date now = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        if (sTimestamp == null) {
            calendar.add(Calendar.MONTH, -1);
            calendar.add(Calendar.DATE, -1);
            sTimestamp = calendar.getTimeInMillis();
        }
        if (eTimestamp == null) {
            eTimestamp = now.getTime();
        }
        return buildSucceedResult(instanceService.getIncidentsTimeAgg(timeUnit, sTimestamp, eTimestamp, appInstanceId));
    }

    @ApiOperation(value = "查询异常实例(根据id)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实例ID", paramType = "query")
    })
    @RequestMapping(value = "getIncidentById", method = RequestMethod.GET)
    public TeslaBaseResult getIncidentById(@RequestParam(name = "id")Long id) {
        return buildSucceedResult(instanceService.getIncidentById(id));
    }

    @ApiOperation(value = "查询异常实例列表(根据trace)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "traceId", value = "任务链ID", paramType = "query")
    })
    @RequestMapping(value = "getIncidentsByTrace", method = RequestMethod.GET)
    public TeslaBaseResult getIncidentsByTrace(@RequestParam(name = "traceId")String traceId) {
        return buildSucceedResult(instanceService.getIncidentsByTrace(traceId));
    }

    @ApiOperation(value = "查询异常实例列表(根据应用)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query")
    })
    @RequestMapping(value = "getIncidentsByApp", method = RequestMethod.GET)
    public TeslaBaseResult getIncidentsByApp(@RequestParam(name = "appId") String appId) {
        return buildSucceedResult(instanceService.getIncidentsByApp(appId));
    }

    @ApiOperation(value = "查询异常实例列表(兼容全量接口)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query"),
            @ApiImplicitParam(name = "appComponentName", value = "应用组件", paramType = "query"),
            @ApiImplicitParam(name = "appComponentInstanceId", value = "应用组件实例ID", paramType = "query"),
            @ApiImplicitParam(name = "incidentTypeId", value = "异常类型ID", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query")
    })
    @RequestMapping(value = "getIncidents", method = RequestMethod.GET)
    public TeslaBaseResult getIncidents(@RequestParam(name = "appId", required = false) String appId,
                                        @RequestParam(name = "appInstanceId", required = false) String appInstanceId,
                                        @RequestParam(name = "appComponentName", required = false) String appComponentName,
                                        @RequestParam(name = "appComponentInstanceId", required = false) String appComponentInstanceId,
                                        @RequestParam(name = "incidentTypeId", required = false) Integer incidentTypeId,
                                        @RequestParam(name = "sTimestamp", required = false) Long sTimestamp,
                                        @RequestParam(name = "eTimestamp", required = false) Long eTimestamp) {
        return buildSucceedResult(instanceService.getIncidents(appId, appInstanceId, appComponentName, appComponentInstanceId, incidentTypeId, sTimestamp, eTimestamp));
    }

    @ApiOperation(value = "查询异常实例列表(按照trace分组,兼容全量接口)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query"),
            @ApiImplicitParam(name = "appComponentName", value = "应用组件", paramType = "query"),
            @ApiImplicitParam(name = "appComponentInstanceId", value = "应用组件实例ID", paramType = "query"),
            @ApiImplicitParam(name = "incidentTypeId", value = "异常类型ID", paramType = "query"),
            @ApiImplicitParam(name = "traceId", value = "追踪ID", paramType = "traceId"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query")
    })
    @RequestMapping(value = "getIncidentsGroupByTrace", method = RequestMethod.GET)
    public TeslaBaseResult getIncidentsGroupByTrace(@RequestParam(name = "appId", required = false) String appId,
                                        @RequestParam(name = "appInstanceId", required = false) String appInstanceId,
                                        @RequestParam(name = "appComponentName", required = false) String appComponentName,
                                        @RequestParam(name = "appComponentInstanceId", required = false) String appComponentInstanceId,
                                        @RequestParam(name = "incidentTypeId", required = false) Integer incidentTypeId,
                                         @RequestParam(name = "traceId", required = false) String traceId,
                                        @RequestParam(name = "sTimestamp", required = false) Long sTimestamp,
                                        @RequestParam(name = "eTimestamp", required = false) Long eTimestamp) {
        return buildSucceedResult(instanceService.getIncidentsGroupByTrace(appId, appInstanceId, appComponentName, appComponentInstanceId, incidentTypeId, traceId, sTimestamp, eTimestamp));
    }

    @ApiOperation(value = "推送异常实例(实例未恢复时不创建新实例)")
    @RequestMapping(value = "pushIncident", method = RequestMethod.POST)
    public TeslaBaseResult pushIncident(@RequestParam(name = "defId") Integer defId,
                                        @RequestBody JSONObject incident) throws Exception {
        return buildSucceedResult(instanceService.pushIncident(defId, incident));
    }

    @ApiOperation(value = "创建异常实例")
    @RequestMapping(value = "createIncident", method = RequestMethod.POST)
    public TeslaBaseResult createIncident(@RequestBody IncidentInstanceCreateReq req) throws Exception {
        return buildSucceedResult(instanceService.addIncident(req));
    }

    @ApiOperation(value = "恢复异常实例")
    @RequestMapping(value = "recoveryIncidentById", method = RequestMethod.POST)
    public TeslaBaseResult recoveryIncidentById(@RequestBody JSONObject id) {
        return buildSucceedResult(instanceService.recoveryIncident(id.getLong("id")));
    }

    @ApiOperation(value = "恢复异常实例")
    @RequestMapping(value = "recoveryIncident", method = RequestMethod.POST)
    public TeslaBaseResult recoveryIncident(@RequestBody DefAppComponentInstanceReq req) throws Exception {
        return buildSucceedResult(instanceService.recoveryIncident(req.getDefId(), req.getAppInstanceId(), req.getAppComponentInstanceId()));
    }

    @ApiOperation(value = "更新异常实例自愈信息")
    @RequestMapping(value = "updateIncidentHealingById", method = RequestMethod.POST)
    public TeslaBaseResult updateIncidentHealingById(@RequestParam(name = "id") Long id,
                                                     @RequestBody IncidentInstanceHealingReq req) throws Exception {
        return buildSucceedResult(instanceService.updateIncidentSelfHealing(id, req));
    }

    @ApiOperation(value = "更新异常实例自愈信息")
    @RequestMapping(value = "updateIncidentSelfHealingByTrace", method = RequestMethod.POST)
    public TeslaBaseResult updateIncidentSelfHealingByTrace(@RequestParam(name = "traceId") String traceId,
                                                 @RequestBody IncidentInstanceHealingReq req) throws Exception {
        return buildSucceedResult(instanceService.updateIncidentSelfHealing(traceId, req));
    }

    @ApiOperation(value = "更新异常实例自愈信息")
    @RequestMapping(value = "updateIncidentSelfHealing", method = RequestMethod.POST)
    public TeslaBaseResult updateIncidentHealing(@RequestParam(name = "defId") Integer defId,
                                                 @RequestParam(name = "appInstanceId") String appInstanceId,
                                                 @RequestParam(name = "appComponentInstanceId", required = false) String appComponentInstanceId,
                                                 @RequestBody IncidentInstanceHealingReq req) throws Exception {
        return buildSucceedResult(instanceService.updateIncidentSelfHealing(defId, appInstanceId, appComponentInstanceId, req));
    }

    @ApiOperation(value = "删除异常实例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "定义ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "deleteIncident", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteIncident(@RequestParam(name = "id") Long id) throws Exception {
        return buildSucceedResult(instanceService.deleteIncident(id));
    }

    @ApiOperation(value = "删除异常实例(trace)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "traceId", value = "链路ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "deleteIncidentsByTrace", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteIncidentsByTrace(@RequestParam(name = "traceId") String traceId) throws Exception {
        return buildSucceedResult(instanceService.deleteIncidentsByTrace(traceId));
    }
}
