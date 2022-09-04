package com.alibaba.sreworks.pmdb.controllers.metric;

import com.alibaba.sreworks.pmdb.api.metric.MetricAnomalyDetectionConfigService;
import com.alibaba.sreworks.pmdb.domain.req.metric.MetricAnomalyDetectionCreateReq;
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
 * 指标异常检测配置Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 15:50
 */
@Slf4j
@RestController
@RequestMapping("/metric_ad_config/")
@Api(tags = "指标异常检测配置")
public class MetricAnomalyDetectionConfigController extends BaseController {

    @Autowired
    MetricAnomalyDetectionConfigService configService;

    @ApiOperation(value = "查询指标异常检测配置(根据配置ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "配置ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getConfigById", method = RequestMethod.GET)
    public TeslaBaseResult getConfigById(@RequestParam(name = "id") Integer id) {
        return buildSucceedResult(configService.getConfigById(id));
    }

    @ApiOperation(value = "查询指标异常检测配置(根据指标ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metricId", value = "指标ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getConfigByMetric", method = RequestMethod.GET)
    public TeslaBaseResult getConfigByMetric(@RequestParam(name = "metricId") Integer metricId) {
        return buildSucceedResult(configService.getConfigByMetric(metricId));
    }

    @ApiOperation(value = "查询指标异常检测配置(根据指标ID和规则ID)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metricId", value = "指标ID", required = true, paramType = "query"),
            @ApiImplicitParam(name = "ruleId", value = "规则ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/getConfigByMetricRule", method = RequestMethod.GET)
    public TeslaBaseResult getConfigByMetricRule(@RequestParam(name = "metricId") Integer metricId, @RequestParam(name = "ruleId") Integer ruleId) {
        return buildSucceedResult(configService.getConfigByMetricRule(metricId, ruleId));
    }

    @ApiOperation(value = "新增指标检测配置")
    @RequestMapping(value = "/createConfig", method = RequestMethod.POST)
    public TeslaBaseResult createConfig(@RequestBody MetricAnomalyDetectionCreateReq req) throws Exception {
        return buildSucceedResult(configService.createConfig(req));
    }

    @ApiOperation(value = "删除指标检测配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "配置ID", required = true, paramType = "query")
    })
    @RequestMapping(value = "/deleteConfigById", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteConfigById(@RequestParam(name = "id") Integer id) {
        return buildSucceedResult(configService.deleteConfigById(id));
    }
}
