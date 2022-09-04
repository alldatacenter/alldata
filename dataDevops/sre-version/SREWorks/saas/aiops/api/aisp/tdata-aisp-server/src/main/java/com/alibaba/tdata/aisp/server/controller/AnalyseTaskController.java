package com.alibaba.tdata.aisp.server.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.constant.AispResult;
import com.alibaba.tdata.aisp.server.controller.param.AnalyzeTaskUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.TaskTrendQueryParam;
import com.alibaba.tdata.aisp.server.service.AnalyseTaskService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildSuccessResult;

/**
 * @ClassName: AnalyseTaskController
 * @Author: dyj
 * @DATE: 2022-03-03
 * @Description:
 **/
@Api(tags = "任务接口")
@Slf4j
@RestController
public class AnalyseTaskController {
    @Autowired
    private AnalyseTaskService taskService;

    @ApiOperation("更新任务")
    @PostMapping(value = "/updateTaskRecord")
    @ResponseBody
    public AispResult updateTask(@RequestBody AnalyzeTaskUpdateParam param){
        return buildSuccessResult(taskService.updateTaskRecord(param));
    }

    @ApiOperation("查询任务列表")
    @PostMapping(value = "/{sceneCode}/{detectorCode}/list")
    @ResponseBody
    public AispResult taskList(@RequestBody TaskQueryParam param,
        @PathVariable String sceneCode, @PathVariable String detectorCode){
        return buildSuccessResult(taskService.taskList(param, sceneCode, detectorCode));
    }

    @ApiOperation("查询任务趋势")
    @PostMapping(value = "/{sceneCode}/{detectorCode}/queryTaskTrend")
    @ResponseBody
    public AispResult queryTaskResult(@RequestBody TaskTrendQueryParam param, @PathVariable String sceneCode, @PathVariable String detectorCode){
        return buildSuccessResult(taskService.queryTaskTrend(param, sceneCode, detectorCode));
    }

    @ApiOperation("查询任务统计")
    @PostMapping(value = "/{sceneCode}/{detectorCode}/queryTaskReport")
    @ResponseBody
    public AispResult queryTaskReport(@RequestBody TaskTrendQueryParam param, @PathVariable String sceneCode, @PathVariable String detectorCode){
        return buildSuccessResult(taskService.queryTaskReport(param, sceneCode, detectorCode));
    }

    @ApiOperation("查询任务结果")
    @GetMapping(value = "queryTaskResult")
    @ResponseBody
    public JSONObject queryTaskResult(@RequestParam(name = "taskUUID") String taskUUID,
        @RequestParam(name = "empId", required = false) String empId){
        return taskService.queryTaskRes(taskUUID, empId);
    }
}
