package com.alibaba.sreworks.health.controllers.failure;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.failure.FailureInstanceService;
import com.alibaba.sreworks.health.domain.req.failure.FailureInstanceCreateReq;
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
 * 故障实例入口
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 11:41
 */

@RestController
@RequestMapping("/failure_instance/")
@Api(tags="故障实例")
public class FailureInstanceController extends BaseController {

    @Autowired
    FailureInstanceService instanceService;

    @ApiOperation(value = "查询故障实例聚合数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "timeUnit", value = "聚合时间", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query")
    })
    @RequestMapping(value = "getFailuresTimeAgg", method = RequestMethod.GET)
    public TeslaBaseResult getFailuresTimeAgg(@RequestParam(name = "timeUnit", required = false)String timeUnit,
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
        return buildSucceedResult(instanceService.getFailuresTimeAgg(timeUnit, sTimestamp, eTimestamp, appInstanceId));
    }

    @ApiOperation(value = "查询故障实例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "故障实例ID", paramType = "query")
    })
    @RequestMapping(value = "getFailureById", method = RequestMethod.GET)
    public TeslaBaseResult getFailureById(@RequestParam(name = "id") Long id) {
        return buildSucceedResult(instanceService.getFailureById(id));
    }

    @ApiOperation(value = "查询故障实例(根据故障应用)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query")
    })
    @RequestMapping(value = "getFailuresByApp", method = RequestMethod.GET)
    public TeslaBaseResult getFailuresByApp(@RequestParam(name = "appId") String appId) {
        return buildSucceedResult(instanceService.getFailuresByApp(appId));
    }

    @ApiOperation(value = "查询故障实例(根据故障定义)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "defId", value = "故障定义ID", paramType = "query")
    })
    @RequestMapping(value = "getFailuresByDefinition", method = RequestMethod.GET)
    public TeslaBaseResult getFailuresByDefinition(@RequestParam(name = "defId") Integer defId) {
        return buildSucceedResult(instanceService.getFailuresByDefinition(defId));
    }

    @ApiOperation(value = "查询故障实例列表(兼容全量接口)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query"),
            @ApiImplicitParam(name = "appComponentName", value = "应用组件", paramType = "query"),
            @ApiImplicitParam(name = "appComponentInstanceId", value = "应用组件实例ID", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query")
    })
    @RequestMapping(value = "getFailures", method = RequestMethod.GET)
    public TeslaBaseResult getFailureInstances(@RequestParam(name = "appId", required = false) String appId,
                                               @RequestParam(name = "appInstanceId", required = false) String appInstanceId,
                                               @RequestParam(name = "appComponentName", required = false) String appComponentName,
                                               @RequestParam(name = "appComponentInstanceId", required = false) String appComponentInstanceId,
                                               @RequestParam(name = "sTimestamp", required = false) Long sTimestamp,
                                               @RequestParam(name = "eTimestamp", required = false) Long eTimestamp) {
        return buildSucceedResult(instanceService.getFailures(appId, appInstanceId, appComponentName, appComponentInstanceId, sTimestamp, eTimestamp));
    }

    @ApiOperation(value = "推送故障实例")
    @RequestMapping(value = "pushFailure", method = RequestMethod.POST)
    public TeslaBaseResult pushFailure(@RequestParam(name = "defId") Integer defId,
                                       @RequestBody JSONObject failure) throws Exception {
        return buildSucceedResult(instanceService.pushFailure(defId, failure));
    }

    @ApiOperation(value = "创建故障实例")
    @RequestMapping(value = "createFailure", method = RequestMethod.POST)
    public TeslaBaseResult createFailure(@RequestBody FailureInstanceCreateReq req) throws Exception {
        return buildSucceedResult(instanceService.addFailure(req));
    }

    @ApiOperation(value = "更新故障等级")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "故障实例ID", paramType = "query", required = true),
            @ApiImplicitParam(name = "level", value = "故障等级", paramType = "query", required = true)
    })
    @RequestMapping(value = "updateFailureInstanceLevel", method = RequestMethod.PUT)
    public TeslaBaseResult updateFailureInstanceLevel(@RequestParam(name = "id") Long id, @RequestParam(name = "level") String level) throws Exception {
        return buildSucceedResult(instanceService.updateFailureLevel(id, level));
    }

    @ApiOperation(value = "升级故障等级")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "故障实例ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "upgradeFailureInstanceLevel", method = RequestMethod.PUT)
    public TeslaBaseResult upgradeFailureInstanceLevel(@RequestParam(name = "id") Long id) throws Exception {
        return buildSucceedResult(instanceService.upgradeFailureLevel(id));
    }
}
