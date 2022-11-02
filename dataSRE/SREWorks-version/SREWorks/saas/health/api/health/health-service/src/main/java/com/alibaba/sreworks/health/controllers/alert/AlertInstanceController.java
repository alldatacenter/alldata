package com.alibaba.sreworks.health.controllers.alert;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.alert.AlertInstanceService;
import com.alibaba.sreworks.health.domain.req.alert.AlertInstanceCreateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * 告警入口
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 20:26
 */
@RestController
@RequestMapping("/alert_instance/")
@Api(tags="告警实例")
public class AlertInstanceController extends BaseController {
    @Autowired
    AlertInstanceService instanceService;

    @ApiOperation(value = "查询告警实例聚合数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "timeUnit", value = "聚合时间", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query")
    })
    @RequestMapping(value = "getAlertsTimeAgg", method = RequestMethod.GET)
    public TeslaBaseResult getAlertsTimeAgg(@RequestParam(name = "timeUnit", required = false)String timeUnit,
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
        return buildSucceedResult(instanceService.getAlertsTimeAgg(timeUnit, sTimestamp, eTimestamp, appInstanceId));
    }

    @ApiOperation(value = "查询告警实例(根据id)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实例ID", paramType = "query")
    })
    @RequestMapping(value = "getAlertById", method = RequestMethod.GET)
    public TeslaBaseResult getAlertById(@RequestParam(name = "id")Long id) {
        return buildSucceedResult(instanceService.getAlertById(id));
    }


    @ApiOperation(value = "查询告警实例列表(根据应用)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query")
    })
    @RequestMapping(value = "getAlertsByApp", method = RequestMethod.GET)
    public TeslaBaseResult getAlertsByApp(@RequestParam(name = "appId") String appId) {
        return buildSucceedResult(instanceService.getAlertsByApp(appId));
    }

    @ApiOperation(value = "查询告警实例列表(兼容全量接口)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query"),
            @ApiImplicitParam(name = "appComponentName", value = "应用组件", paramType = "query"),
            @ApiImplicitParam(name = "appComponentInstanceId", value = "应用组件实例ID", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query")
    })
    @RequestMapping(value = "getAlerts", method = RequestMethod.GET)
    public TeslaBaseResult getAlerts(@RequestParam(name = "appId", required = false) String appId,
                                     @RequestParam(name = "appInstanceId", required = false) String appInstanceId,
                                     @RequestParam(name = "appComponentName", required = false) String appComponentName,
                                     @RequestParam(name = "appComponentInstanceId", required = false) String appComponentInstanceId,
                                     @RequestParam(name = "sTimestamp", required = false) Long sTimestamp,
                                     @RequestParam(name = "eTimestamp", required = false) Long eTimestamp) {
        return buildSucceedResult(instanceService.getAlerts(appId, appInstanceId, appComponentName, appComponentInstanceId, sTimestamp, eTimestamp));
    }

    @ApiOperation(value = "根据定义创建告警实例(批量)")
    @RequestMapping(value = "pushAlerts", method = RequestMethod.POST)
    public TeslaBaseResult pushAlerts(@RequestParam(name = "defId") Integer defId,
                                             @RequestBody JSONObject[] alerts) throws Exception {
        return buildSucceedResult(instanceService.pushAlerts(defId, Arrays.asList(alerts)));
    }

    @ApiOperation(value = "创建告警实例")
    @RequestMapping(value = "createAlert", method = RequestMethod.POST)
    public TeslaBaseResult createAlert(@RequestBody AlertInstanceCreateReq req) throws Exception {
        return buildSucceedResult(instanceService.addAlert(req));
    }

    @ApiOperation(value = "删除告警实例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "定义ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "deleteAlert", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteAlert(@RequestParam(name = "id") Long id) throws Exception {
        return buildSucceedResult(instanceService.deleteAlert(id));
    }
}
