package com.alibaba.tdata.aisp.server.controller;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.config.AispRegister;
import com.alibaba.tdata.aisp.server.common.constant.AispResult;
import com.alibaba.tdata.aisp.server.common.filter.LogConstant;
import com.alibaba.tdata.aisp.server.common.utils.UuidUtil;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceFeedbackParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyzeTaskUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.CodeParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskTrendQueryParam;
import com.alibaba.tdata.aisp.server.service.AnalyseExecuteService;
import com.alibaba.tdata.aisp.server.service.AnalyseTaskService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildSuccessResult;

/**
 * @ClassName: AnalyseExecuteController
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Api(tags = "预测执行接口")
@Slf4j
@RestController
public class AnalyseExecuteController {
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private AnalyseExecuteService executeService;
    @Autowired
    private AispRegister monitor;

    @PostMapping(value = "/{sceneCode}/{detectorCode}/analyze")
    @ResponseBody
    public JSONObject analyze(@RequestBody JSONObject param,
        @PathVariable String sceneCode, @PathVariable String detectorCode){
        String taskUUID = genTaskUUID();
        long start = System.currentTimeMillis();
        JSONObject exec = executeService.exec(taskUUID, param, sceneCode, detectorCode);
        monitor.record(sceneCode, detectorCode, System.currentTimeMillis()-start);
        return exec;
    }

    @ApiOperation("导出代码")
    @PostMapping(value = "/{sceneCode}/{detectorCode}/code")
    @ResponseBody
    public AispResult code(@RequestBody CodeParam param, @PathVariable String sceneCode,
        @PathVariable String detectorCode) throws IOException, NoSuchAlgorithmException {
        return buildSuccessResult(executeService.code(param));
    }

    @ApiOperation("服务文档接口")
    @GetMapping(value = "/{sceneCode}/{detectorCode}/doc")
    @ResponseBody
    public AispResult getDoc(@PathVariable String detectorCode){
        return buildSuccessResult(executeService.getDoc(detectorCode));
    }

    @ApiOperation("获取默认参数")
    @GetMapping(value = "/{sceneCode}/{detectorCode}/input")
    @ResponseBody
    public AispResult getInput(@PathVariable String detectorCode){
        return buildSuccessResult(executeService.getInput(detectorCode));
    }

    @ApiOperation("反馈接口")
    @PostMapping(value = "/{sceneCode}/{detectorCode}/feedback")
    @ResponseBody
    public AispResult feedback(@PathVariable String sceneCode,
        @PathVariable String detectorCode, @RequestBody @Validated AnalyseInstanceFeedbackParam param){
        JSONObject result = new JSONObject();
        result.put("result", executeService.feedback(sceneCode, detectorCode, param));
        result.put("taskUUID", genTaskUUID());
        return buildSuccessResult(result);
    }

    private String genTaskUUID() {
        if (StringUtils.isEmpty(getTraceId())){
            return UuidUtil.genUuid();
        } else {
            return getTraceId();
        }
    }

    public String getTraceId() {
        String traceId = httpServletRequest.getHeader(LogConstant.X_TRACE_ID);
        if (traceId == null) {
            traceId = "";
        }
        return traceId;
    }
}
