package com.alibaba.tdata.aisp.server.controller;

import com.alibaba.tdata.aisp.server.common.constant.AispResult;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceBacthUpsertModelParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceCreateParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceFeedbackParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceUpdateParam;
import com.alibaba.tdata.aisp.server.controller.param.AnalyseInstanceUpsertModelParam;
import com.alibaba.tdata.aisp.server.service.AnalyseInstanceService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildSuccessResult;

/**
 * @ClassName: AnalyseInstanceController
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Api(tags = "Entity接口")
@Slf4j
@RestController
@RequestMapping("/entity/")
public class AnalyseEntityController {
    @Autowired
    private AnalyseInstanceService instanceService;

    @ApiOperation("新建")
    @PostMapping(value = "create")
    @ResponseBody
    public AispResult create(@RequestBody @Validated AnalyseInstanceCreateParam param){
        return buildSuccessResult(instanceService.create(param));
    }

    @ApiOperation("更新")
    @PostMapping(value = "update")
    @ResponseBody
    public AispResult update(@RequestBody @Validated AnalyseInstanceUpdateParam param){
        return buildSuccessResult(instanceService.updateById(param));
    }

    @ApiOperation("upsert模型参数")
    @PostMapping(value = "upsertModel")
    @ResponseBody
    public AispResult upsertModel(@RequestBody @Validated AnalyseInstanceUpsertModelParam param){
        return buildSuccessResult(instanceService.upsertModel(param));
    }

    @ApiOperation("批量upsert模型参数")
    @PostMapping(value = "batchUpsertModel")
    @ResponseBody
    public AispResult batchUpsertModel(@RequestBody @Validated AnalyseInstanceBacthUpsertModelParam param){
        return buildSuccessResult(instanceService.batchUpsertModel(param));
    }

    @ApiOperation("查询")
    @PostMapping(value = "query")
    @ResponseBody
    public AispResult query(@RequestBody @Validated AnalyseInstanceQueryParam param){
        return buildSuccessResult(instanceService.query(param));
    }

    @ApiOperation("删除")
    @GetMapping(value = "delete")
    @ResponseBody
    public AispResult delete(@RequestParam("instanceCode") String instanceCode){
        return buildSuccessResult(instanceService.delete(instanceCode));
    }


}
