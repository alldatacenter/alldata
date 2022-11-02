package com.alibaba.sreworks.pmdb.controllers.metric;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.api.metric.MetricService;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricCreateReq;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricUpdateReq;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 指标Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 15:50
 */
@Slf4j
@RestController
@RequestMapping("/metric/")
@Api(tags = "指标")
public class MetricController extends BaseController {

    @Autowired
    MetricService metricService;

    @ApiOperation(value = "查询指标列表(指标ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "指标ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getMetricById", method = RequestMethod.GET)
    public TeslaBaseResult getMetricById(@RequestParam(name = "id") Integer id) {
        return buildSucceedResult(metricService.getMetricById(id));
    }

    @ApiOperation(value = "查询指标列表(根据指标名称)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "指标名称", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getMetricsByName", method = RequestMethod.GET)
    public TeslaBaseResult getMetricsByName(@RequestParam(name = "name") String name) {
        return buildSucceedResult(metricService.getMetricsByName(name));
    }

    @ApiOperation(value = "查询指标列表(根据指标标签)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "指标名称", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getMetricsByLabels", method = RequestMethod.POST)
    public TeslaBaseResult getMetricsByLabels(@RequestParam(name = "name") String name,
                                           @RequestBody JSONObject labels) {
        return buildSucceedResult(metricService.getMetricsByLabels(name, labels));
    }

    @ApiOperation(value = "查询指标列表(兼容全量接口)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "指标名称", required = false, paramType = "query")
    })
    @RequestMapping(value = "/getMetrics", method = RequestMethod.POST)
    public TeslaBaseResult getMetrics(@RequestParam(name = "name", required = false) String name,
                                      @RequestBody(required = false) JSONObject labels) {
        return buildSucceedResult(metricService.getMetrics(name, labels));
    }

    @ApiOperation(value = "新增指标")
    @RequestMapping(value = "/createMetric", method = RequestMethod.POST)
    public TeslaBaseResult createMetric(@RequestHeader(name = "x-empid", required = false) String userId,
                                        @RequestBody MetricCreateReq req) throws Exception {
        req.setCreator(userId);
        req.setLastModifier(userId);
        return buildSucceedResult(metricService.createMetric(req));
    }

    @ApiOperation(value = "更新指标")
    @RequestMapping(value = "/modifyMetric", method = RequestMethod.POST)
    public TeslaBaseResult modifyMetric(@RequestHeader(name = "x-empid", required = false) String userId,
                                        @RequestBody MetricUpdateReq req) throws Exception {
        req.setLastModifier(userId);
        return buildSucceedResult(metricService.updateMetric(req));
    }

    @ApiOperation(value = "删除指标")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "指标ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/deleteMetricById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteMetricById(@RequestParam(name = "id") Integer id) {
        return buildSucceedResult(metricService.deleteMetricById(id));
    }
}
