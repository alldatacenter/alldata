package com.alibaba.tesla.server.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.server.common.constant.AispResult;
import com.alibaba.tesla.server.common.factory.AispResponseFactory;
import com.alibaba.tesla.server.controller.param.ProcessParam;
import com.alibaba.tesla.server.service.ProcessService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName: ProcessController
 * @Author: dyj
 * @DATE: 2022-02-28
 * @Description:
 **/
@Api(tags = "检测器接口")
@Slf4j
@RestController
public class ProcessController {
    @Autowired
    private ProcessService processService;
    @Autowired
    private HttpServletResponse response;

    @ApiOperation("status")
    @GetMapping(value = "status.taobao")
    @ResponseBody
    public String status(){
        return "success";
    }

    @ApiOperation("input")
    @PostMapping(value = "input")
    @ResponseBody
    public JSONObject input(){
        JSONObject input = new JSONObject();
        input.put("jobName", "change_demo");
        JSONArray reqList = new JSONArray();
        for (int i = 1; i < 11; i++) {
            JSONObject req = new JSONObject();
            req.put("test".concat(String.valueOf(i)),"test".concat(String.valueOf(i)));
            reqList.add(req);
        }
        input.put("reqList", reqList);
        input.put("step", 3);
        input.put("timeout", "120000");
        return input;
    }

    @ApiOperation("doc")
    @PostMapping(value = "doc")
    @ResponseBody
    public String doc() throws IOException {
        return processService.doc();
    }

    @ApiOperation("analyze")
    @PostMapping(value = "analyze")
    @ResponseBody
    public AispResult analyze(@RequestBody @Validated ProcessParam param){
        response.setStatus(HttpStatus.CREATED.value());
        return AispResponseFactory.buildSuccessResult(processService.process(param));
    }
}
