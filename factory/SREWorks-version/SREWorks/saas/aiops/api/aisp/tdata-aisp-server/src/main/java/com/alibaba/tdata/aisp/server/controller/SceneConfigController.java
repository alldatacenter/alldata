package com.alibaba.tdata.aisp.server.controller;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.tdata.aisp.server.common.constant.AispResult;
import com.alibaba.tdata.aisp.server.controller.param.SceneCleanModelParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.SceneUpsertModelParam;
import com.alibaba.tdata.aisp.server.service.SceneService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildSuccessResult;

/**
 * @ClassName: SceneConfigController
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Api(tags = "场景配置接口")
@Slf4j
@RestController
@RequestMapping("/scene/")
public class SceneConfigController {
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private SceneService sceneService;

    @ApiOperation("新建场景")
    @PostMapping(value = "create")
    @ResponseBody
    public AispResult create(@RequestBody @Validated SceneCreateParam param){
        return buildSuccessResult(sceneService.create(param));
    }

    @ApiOperation("查询场景")
    @PostMapping(value = "list")
    @ResponseBody
    public AispResult list(@RequestBody @Validated SceneQueryParam param){
        return buildSuccessResult(sceneService.list(param, getUserEmployeeId()));
    }

    @ApiOperation("更新场景")
    @PostMapping(value = "update")
    @ResponseBody
    public AispResult list(@RequestBody @Validated SceneUpdateParam param){
        return buildSuccessResult(sceneService.update(param));
    }

    @ApiOperation("更新场景modelParam")
    @PostMapping(value = "upsert")
    @ResponseBody
    public AispResult upsertModel(@RequestBody @Validated SceneUpsertModelParam param){
        return buildSuccessResult(sceneService.upsertModel(param));
    }

    @ApiOperation("删除场景modelParam")
    @PostMapping(value = "cleanModel")
    @ResponseBody
    public AispResult cleanModel(@RequestBody @Validated SceneCleanModelParam param){
        return buildSuccessResult(sceneService.cleanModel(param));
    }

    @ApiOperation("删除场景")
    @DeleteMapping(value = "delete")
    @ResponseBody
    public AispResult delete(@RequestParam(name = "sceneCode") String sceneCode){
        return buildSuccessResult(sceneService.delete(sceneCode));
    }

    public String getUserEmployeeId() {
        String employeeId = httpServletRequest.getHeader("x-empid");
        employeeId = employeeId == null ? "" : employeeId;
        return employeeId;
    }
}
