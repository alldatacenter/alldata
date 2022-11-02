package com.alibaba.sreworks.pmdb.controllers.metric;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.metric.MetricInstanceService;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricDataReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricInstanceCreateReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricInstanceUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 指标实例Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 15:50
 */
@Slf4j
@RestController
@RequestMapping("/metric_instance/")
@Api(tags = "指标实例")
public class MetricInstanceController extends BaseController {

    @Autowired
    MetricInstanceService instanceService;

    @ApiOperation(value = "查询指标实例(根据实例ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "指标实例ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getInstanceById", method = RequestMethod.GET)
    public TeslaBaseResult getInstanceById(@RequestParam(name = "id") Long id) {
        return buildSucceedResult(instanceService.getInstanceById(id));
    }

    @ApiOperation(value = "查询指标实例(根据实例身份ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "uid", value = "指标实例身份ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getInstanceByUid", method = RequestMethod.GET)
    public TeslaBaseResult getInstanceByUid(@RequestParam(name = "uid") String uid) {
        return buildSucceedResult(instanceService.getInstanceByUid(uid));
    }

    @ApiOperation(value = "查询指标实例列表(根据指标ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metricId", value = "指标ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getInstanceByMetric", method = RequestMethod.GET)
    public TeslaBaseResult getInstanceByMetric(@RequestParam(name = "metricId") Integer metricId) {
        return buildSucceedResult(instanceService.getInstanceByMetric(metricId));
    }

    @ApiOperation(value = "查询指标实例列表(根据实例标签)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "size", value = "返回条数", paramType = "query")
    })
    @RequestMapping(value = "/getInstanceByLabels", method = RequestMethod.POST)
    public TeslaBaseResult getInstanceByLabels(@RequestParam(name = "size", required = false) Integer size, @RequestBody(required = false) JSONObject labels) {
        if (size == null || size <= 0) {
            size = 100;
        }
        return buildSucceedResult(instanceService.getInstanceByLabels(labels, size));
    }

    @ApiOperation(value = "查询指标实例列表(根据指标ID和实例标签)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metricId", value = "指标ID", paramType = "query"),
    })
    @RequestMapping(value = "/getInstanceByMetricLabels", method = RequestMethod.POST)
    public TeslaBaseResult getInstanceByMetricLabels(@RequestParam(name = "metricId", required = false) Integer metricId, @RequestBody(required = false) JSONObject labels) {
        if (metricId == null) {
            return buildSucceedResult(instanceService.getInstanceByLabels(labels, 100));
        } else {
            return buildSucceedResult(instanceService.getInstanceByLabels(metricId, labels));
        }
    }

    @ApiOperation(value = "新增指标实例")
    @RequestMapping(value = "/createMetricInstance", method = RequestMethod.POST)
    public TeslaBaseResult createMetricInstance(@RequestBody MetricInstanceCreateReq req) throws Exception {
        return buildSucceedResult(instanceService.createInstance(req));
    }

    @ApiOperation(value = "更新指标实例")
    @RequestMapping(value = "/updateMetricInstance", method = RequestMethod.POST)
    public TeslaBaseResult updateMetricInstance(@RequestBody MetricInstanceUpdateReq req) throws Exception {
        return buildSucceedResult(instanceService.updateInstance(req));
    }

    @ApiOperation(value = "删除指标实例(实例ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "实例ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/deleteInstanceById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteInstanceById(@RequestParam(name = "id") Long id) {
        return buildSucceedResult(instanceService.deleteInstanceById(id));
    }

    @ApiOperation(value = "删除指标实例(指标ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metricId", value = "指标ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/deleteInstanceByMetric", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteInstanceByMetric(@RequestParam(name = "metricId") Integer metricId) {
        return buildSucceedResult(instanceService.deleteInstanceByMetric(metricId));
    }

    @ApiOperation(value = "推送指标数据(单条)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metricId", value = "指标ID", required = true, paramType = "query"),
            @ApiImplicitParam(name = "isInsertNewIns", value = "插入新实例", required = true, paramType = "query"),
            @ApiImplicitParam(name = "isPushQueue", value = "推送消息队列", required = true, paramType = "query")
    })
    @RequestMapping(value = "/pushMetricData", method = RequestMethod.POST)
    public TeslaBaseResult pushMetricData(@RequestParam(name = "metricId") Integer metricId,
                                          @RequestParam(name = "isInsertNewIns") Boolean isInsertNewIns,
                                          @RequestParam(name = "isPushQueue") Boolean isPushQueue,
                                           @RequestBody MetricDataReq data) throws Exception {
        instanceService.pushData(metricId, isInsertNewIns, isPushQueue, data);
        return buildSucceedResult("success");
    }

    @ApiOperation(value = "推送指标数据(批量)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metricId", value = "指标ID", required = true, paramType = "query"),
            @ApiImplicitParam(name = "isInsertNewIns", value = "插入新实例", required = true, paramType = "query"),
            @ApiImplicitParam(name = "isDeleteOldIns", value = "删除旧实例", required = true, paramType = "query"),
            @ApiImplicitParam(name = "isPushQueue", value = "推送消息队列", required = true, paramType = "query")
    })
    @RequestMapping(value = "/pushMetricDatas", method = RequestMethod.POST)
    public TeslaBaseResult pushMetricDatas(@RequestParam(name = "metricId") Integer metricId,
                                           @RequestParam(name = "isInsertNewIns") Boolean isInsertNewIns,
                                           @RequestParam(name = "isDeleteOldIns") Boolean isDeleteOldIns,
                                           @RequestParam(name = "isPushQueue") Boolean isPushQueue,
                                           @RequestBody MetricDataReq[] datas) throws Exception {
        instanceService.pushDatas(metricId, isInsertNewIns, isDeleteOldIns, isPushQueue, Arrays.asList(datas));
        return buildSucceedResult("success");
    }
}
