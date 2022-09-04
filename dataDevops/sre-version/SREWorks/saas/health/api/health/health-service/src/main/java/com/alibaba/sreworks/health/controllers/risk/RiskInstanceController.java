package com.alibaba.sreworks.health.controllers.risk;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.api.risk.RiskInstanceService;
import com.alibaba.sreworks.health.domain.req.risk.RiskInstanceCreateReq;
import com.alibaba.sreworks.health.domain.req.risk.RiskInstanceUpdateReq;
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
 * 风险实例入口
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 17:42
 */
@RestController
@RequestMapping(value = "/risk_instance/")
@Api(tags = "风险实例")
public class RiskInstanceController extends BaseController {

    @Autowired
    RiskInstanceService instanceService;

    @ApiOperation(value = "查询风险实例聚合数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "timeUnit", value = "聚合时间", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query")
    })
    @RequestMapping(value = "getRisksTimeAgg", method = RequestMethod.GET)
    public TeslaBaseResult getRisksTimeAgg(@RequestParam(name = "timeUnit", required = false)String timeUnit,
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
        return buildSucceedResult(instanceService.getRisksTimeAgg(timeUnit, sTimestamp, eTimestamp, appInstanceId));
    }

    @ApiOperation(value = "查询风险实例(根据id)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实例ID", paramType = "query")
    })
    @RequestMapping(value = "getRiskById", method = RequestMethod.GET)
    public TeslaBaseResult getRiskById(@RequestParam(name = "id")Long id) {
        return buildSucceedResult(instanceService.getRiskById(id));
    }

    @ApiOperation(value = "查询风险实例列表(根据应用)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query")
    })
    @RequestMapping(value = "getRisksByApp", method = RequestMethod.GET)
    public TeslaBaseResult getRisksByApp(@RequestParam(name = "appId") String appId) {
        return buildSucceedResult(instanceService.getRisksByApp(appId));
    }

    @ApiOperation(value = "查询风险实例列表(兼容全量接口)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", paramType = "query"),
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query"),
            @ApiImplicitParam(name = "appComponentName", value = "应用组件", paramType = "query"),
            @ApiImplicitParam(name = "appComponentInstanceId", value = "应用组件实例ID", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query")
    })
    @RequestMapping(value = "getRisks", method = RequestMethod.GET)
    public TeslaBaseResult getRisks(@RequestParam(name = "appId", required = false) String appId,
                                    @RequestParam(name = "appInstanceId", required = false) String appInstanceId,
                                    @RequestParam(name = "appComponentName", required = false) String appComponentName,
                                    @RequestParam(name = "appComponentInstanceId", required = false) String appComponentInstanceId,
                                    @RequestParam(name = "sTimestamp", required = false) Long sTimestamp,
                                    @RequestParam(name = "eTimestamp", required = false) Long eTimestamp) {
        return buildSucceedResult(instanceService.getRisks(appId, appInstanceId, appComponentName, appComponentInstanceId, sTimestamp, eTimestamp));
    }

    @ApiOperation(value = "根据定义创建风险实例(批量)")
    @RequestMapping(value = "pushRisks", method = RequestMethod.POST)
    public TeslaBaseResult pushRisks(@RequestParam(name = "defId") Integer defId,
                                                @RequestBody JSONObject[] risks) throws Exception {
        return buildSucceedResult(instanceService.pushRisks(defId, Arrays.asList(risks)));
    }

    @ApiOperation(value = "创建风险实例")
    @RequestMapping(value = "createRisk", method = RequestMethod.POST)
    public TeslaBaseResult createRisk(@RequestBody RiskInstanceCreateReq req) throws Exception {
        return buildSucceedResult(instanceService.addRisk(req));
    }

    @ApiOperation(value = "更新风险实例")
    @RequestMapping(value = "updateRisk", method = RequestMethod.POST)
    public TeslaBaseResult updateRisk(@RequestBody RiskInstanceUpdateReq req) throws Exception {
        return buildSucceedResult(instanceService.updateRisk(req));
    }

    @ApiOperation(value = "删除风险实例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "定义ID", paramType = "query", required = true)
    })
    @RequestMapping(value = "deleteRisk", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteRisk(@RequestParam(name = "id") Long id) throws Exception {
        return buildSucceedResult(instanceService.deleteRisk(id));
    }
}
