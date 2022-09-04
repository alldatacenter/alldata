package com.alibaba.tdata.aisp.server.controller;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.config.AispRegister;
import com.alibaba.tdata.aisp.server.common.constant.AispResult;
import com.alibaba.tdata.aisp.server.common.exception.DetectorException;
import com.alibaba.tdata.aisp.server.common.exception.PlatformInternalException;
import com.alibaba.tdata.aisp.server.common.filter.LogConstant;
import com.alibaba.tdata.aisp.server.common.utils.UuidUtil;
import com.alibaba.tdata.aisp.server.service.AnalyseExecuteService;
import com.alibaba.tdata.aisp.server.service.AnalyseTaskService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildSuccessResult;

/**
 * @ClassName: AiopsFrontController
 * @Author: dyj
 * @DATE: 2021-12-10
 * @Description:
 **/
@Api(tags = "前端定制接口")
@Slf4j
@RestController
@RequestMapping("/front/")
public class AiopsFrontController {
    @Autowired
    private AnalyseExecuteService executeService;
    @Autowired
    private AnalyseTaskService taskService;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private AispRegister monitor;

    @ApiOperation("任务详情")
    @GetMapping(value = "queryTaskDetail")
    @ResponseBody
    public JSONObject queryTaskDetail(@RequestParam(name = "taskUUID") String taskUUID,
        @RequestParam(name = "empId", required = false) String empId){
        // 适配前端组件展示结构
        JSONObject body = new JSONObject();
        JSONObject data = new JSONObject();
        JSONObject result = taskService.queryTaskRes(taskUUID, empId);
        data.put("result", result);
        body.put("data", data);
        body.put("code", 200);
        return body;
    }

    @ApiOperation("前端任务测试")
    @PostMapping(value = "/{sceneCode}/{detectorCode}/analyze")
    @ResponseBody
    public JSONObject analyze(@RequestBody JSONObject param,
        @PathVariable String sceneCode, @PathVariable String detectorCode){
        try {
            String taskUUID = genTaskUUID();
            long start = System.currentTimeMillis();
            JSONObject result = executeService.exec(taskUUID, param, sceneCode, detectorCode);
            monitor.record(sceneCode, detectorCode, System.currentTimeMillis()-start);
            return buildSuccessBody(result);
        } catch (DetectorException e){
            return buildExceptionBody(e.getHttpStatus(), e.getTaskUUID(), e.getCode(), e.getMessage());
        } catch (PlatformInternalException e){
            return buildExceptionBody(e.getHttpStatus(), e.getTaskUUID(), "500", e.getMessage());
        } catch (IllegalArgumentException | AssertionError e){
            return buildExceptionBody(400, null, "400", e.getMessage());
        }
    }

    @ApiOperation("异常检测曲线接口")
    @GetMapping(value = "queryAdLine")
    @ResponseBody
    public AispResult queryAdLine(@RequestParam(name = "taskUUID") String taskUUID){
        return buildSuccessResult(taskService.queryAdLine(taskUUID));
    }

    private JSONObject buildSuccessBody(JSONObject result){
        JSONObject data = new JSONObject();
        data.put("result", result);
        data.put("httpStatus", 200);

        JSONObject body = new JSONObject();
        body.put("code", 200);
        body.put("data", data);

        return body;
    }

    private JSONObject buildExceptionBody(int httpStatus, String taskUUID, String code, String message){
        JSONObject result = new JSONObject();
        result.put("taskUUID", taskUUID);
        result.put("code", code);
        result.put("message", message);

        JSONObject data = new JSONObject();
        data.put("result", result);
        data.put("httpStatus", httpStatus);

        JSONObject body = new JSONObject();
        body.put("code", 200);
        body.put("data", data);

        return body;
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
