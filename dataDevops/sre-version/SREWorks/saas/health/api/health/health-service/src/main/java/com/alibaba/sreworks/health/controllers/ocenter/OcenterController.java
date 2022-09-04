package com.alibaba.sreworks.health.controllers.ocenter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.services.ocenter.OcenterService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * 运营数据统计接口
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/02/15 16:32
 */
@RestController
@RequestMapping("/ocenter/")
@Api(tags="运营数据")
public class OcenterController extends BaseController {
    @Autowired
    OcenterService ocenterService;

    @ApiOperation(value = "应用健康分")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query")
    })
    @RequestMapping(value = "getAppHealthScore", method = RequestMethod.GET)
    public TeslaBaseResult getAppHealthScore(@RequestParam(name = "appInstanceId", required = false) String appInstanceId) {
        return buildSucceedResult(ocenterService.getHealthScore(appInstanceId));
    }

    @ApiOperation(value = "实例质量数据(按照应用)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query")
    })
    @RequestMapping(value = "getInstanceQuality", method = RequestMethod.GET)
    public TeslaBaseResult getInstanceQuality(@RequestParam(name = "appInstanceId") String appInstanceId,
                                          @RequestParam(name = "sTimestamp", required = false) Long sTimestamp,
                                          @RequestParam(name = "eTimestamp", required = false) Long eTimestamp) {
        Date now = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        if (sTimestamp == null) {
            calendar.add(Calendar.DATE, -7);
            sTimestamp = calendar.getTimeInMillis();
        }
        if (eTimestamp == null) {
            eTimestamp = now.getTime();
        }
        JSONObject instanceCnt = ocenterService.getInstanceCntStat(appInstanceId, sTimestamp, eTimestamp);
        JSONObject sla = ocenterService.getSla(appInstanceId, sTimestamp, eTimestamp);

        JSONObject quality = new JSONObject();
        quality.putAll(instanceCnt);
        quality.put("sla", sla.getDouble(appInstanceId));
        return buildSucceedResult(quality);
    }

    @ApiOperation(value = "实例统计数据(按照应用)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query")
    })
    @RequestMapping(value = "getInstanceCnt", method = RequestMethod.GET)
    public TeslaBaseResult getInstanceCnt(@RequestParam(name = "appInstanceId", required = false) String appInstanceId,
                                          @RequestParam(name = "sTimestamp", required = false) Long sTimestamp,
                                          @RequestParam(name = "eTimestamp", required = false) Long eTimestamp) {
        Date now = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        if (sTimestamp == null) {
            calendar.add(Calendar.DATE, -7);
            sTimestamp = calendar.getTimeInMillis();
        }
        if (eTimestamp == null) {
            eTimestamp = now.getTime();
        }
        return buildSucceedResult(ocenterService.getInstanceCntStat(appInstanceId, sTimestamp, eTimestamp));
    }

    @ApiOperation(value = "应用服务可用率")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appInstanceId", value = "应用实例ID", paramType = "query"),
            @ApiImplicitParam(name = "sTimestamp", value = "开始时间戳", paramType = "query"),
            @ApiImplicitParam(name = "eTimestamp", value = "结束时间戳", paramType = "query")
    })
    @RequestMapping(value = "getAppSla", method = RequestMethod.GET)
    public TeslaBaseResult getAppSla(@RequestParam(name = "appInstanceId", required = false) String appInstanceId,
                                          @RequestParam(name = "sTimestamp", required = false) Long sTimestamp,
                                          @RequestParam(name = "eTimestamp", required = false) Long eTimestamp) {
        Date now = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(now);
        if (sTimestamp == null) {
            calendar.add(Calendar.DATE, -7);
            sTimestamp = calendar.getTimeInMillis();
        }
        if (eTimestamp == null) {
            eTimestamp = now.getTime();
        }
        return buildSucceedResult(ocenterService.getSla(appInstanceId, sTimestamp, eTimestamp));
    }
}
