package com.alibaba.sreworks.dataset.controllers.otel;

import com.alibaba.sreworks.dataset.common.utils.Tools;
import com.alibaba.sreworks.dataset.services.otel.LogService;
import com.alibaba.sreworks.dataset.services.otel.TraceService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 可观测数据接口
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/20 15:44
 */

@Slf4j
@RestController
@RequestMapping("/otel")
@Api(tags = "可观测数据接口")
public class OtelController extends BaseController {

    @Autowired
    TraceService traceService;

    @Autowired
    LogService logService;

    private Integer DEFAULT_PAGE_SIZE = 10;
    private  Integer DEFAULT_PAGE_NUM = 1;

    @ApiOperation(value = "查询追踪服务实例(trace)")
    @RequestMapping(value = "/getTracingService", method = RequestMethod.GET)
    public TeslaBaseResult getTracingService(@RequestParam(name = "serviceName")String serviceName,
                                            @RequestParam(name = "sTimestamp", required = false)Long sTimestamp,
                                            @RequestParam(name = "eTimestamp", required = false)Long eTimestamp) throws Exception {

        sTimestamp = sTimestamp == null ? Tools.getDeltaTimestamp(-1) : sTimestamp;
        eTimestamp = eTimestamp == null ? new Date().getTime() : eTimestamp;

        return buildSucceedResult(traceService.getTracingService(serviceName, sTimestamp, eTimestamp));
    }

    @ApiOperation(value = "查询服务追踪列表(trace)")
    @RequestMapping(value = "/getServiceTraces", method = RequestMethod.GET)
    public TeslaBaseResult getServiceTraces(@RequestParam(name = "serviceName")String serviceName,
                                            @RequestParam(name = "traceState", required = false)String traceState,
                                            @RequestParam(name = "orderType", required = false)String orderType,
                                            @RequestParam(name = "sTimestamp", required = false)Long sTimestamp,
                                            @RequestParam(name = "eTimestamp", required = false)Long eTimestamp,
                                            @RequestParam(name = "pageNum", required = false)Integer page,
                                            @RequestParam(name = "pageSize", required = false)Integer pageSize) throws Exception {

        sTimestamp = sTimestamp == null ? Tools.getDeltaTimestamp(-1) : sTimestamp;
        eTimestamp = eTimestamp == null ? new Date().getTime() : eTimestamp;

        page = page == null ? DEFAULT_PAGE_NUM : page;
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;

        return buildSucceedResult(traceService.getServiceTraces(serviceName, traceState, orderType, sTimestamp, eTimestamp, page, pageSize));
    }

    @ApiOperation(value = "根据ID查询追踪数据(trace)")
    @RequestMapping(value = "/getTraceById", method = RequestMethod.GET)
    public TeslaBaseResult getTraceById(@RequestParam(name = "traceId")String traceId) throws Exception {
        return buildSucceedResult(traceService.getTraceById(traceId));
    }

    @ApiOperation(value = "查询服务日志列表(log)")
    @RequestMapping(value = "/getServiceLogs", method = RequestMethod.GET)
    public TeslaBaseResult getServiceLogs(@RequestParam(name = "serviceName")String serviceName,
                                            @RequestParam(name = "traceId", required = false)String traceId,
                                            @RequestParam(name = "keywordsInclude", required = false)String keywordsInclude,
                                            @RequestParam(name = "keywordsExclude", required = false)String keywordsExclude,
                                            @RequestParam(name = "sTimestamp", required = false)Long sTimestamp,
                                            @RequestParam(name = "eTimestamp", required = false)Long eTimestamp,
                                            @RequestParam(name = "pageNum", required = false)Integer page,
                                            @RequestParam(name = "pageSize", required = false)Integer pageSize) throws Exception {

        sTimestamp = sTimestamp == null ? Tools.getDeltaTimestamp(-1) : sTimestamp;
        eTimestamp = eTimestamp == null ? new Date().getTime() : eTimestamp;

        page = page == null ? DEFAULT_PAGE_NUM : page;
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;

        List<String> keywordIncludeList = StringUtils.isNotBlank(keywordsInclude) ?
                Arrays.asList(keywordsInclude.split(",")) : new ArrayList<>();
        List<String> keywordsExcludeList = StringUtils.isNotBlank(keywordsExclude) ?
                Arrays.asList(keywordsExclude.split(",")) : new ArrayList<>();

        return buildSucceedResult(logService.getServiceLogs(serviceName, traceId, keywordIncludeList, keywordsExcludeList,
                sTimestamp, eTimestamp, page, pageSize));
    }
}
