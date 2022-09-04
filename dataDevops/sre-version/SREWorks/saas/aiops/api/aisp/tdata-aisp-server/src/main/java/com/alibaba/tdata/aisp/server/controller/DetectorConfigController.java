package com.alibaba.tdata.aisp.server.controller;

import com.alibaba.tdata.aisp.server.common.constant.AispResult;
import com.alibaba.tdata.aisp.server.controller.param.DetectorGitRegisterParam;
import com.alibaba.tdata.aisp.server.controller.param.DetectorQueryParam;
import com.alibaba.tdata.aisp.server.controller.param.DetectorRegisterParam;
import com.alibaba.tdata.aisp.server.controller.param.DetectorUpdateParam;
import com.alibaba.tdata.aisp.server.service.DetectorService;

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
 * @ClassName: DetectorConfigController
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Api(tags = "检测器接口")
@Slf4j
@RestController
@RequestMapping("/detector/")
public class DetectorConfigController {
    @Autowired
    private DetectorService detectorService;

    @ApiOperation("注册检测器")
    @PostMapping(value = "register")
    @ResponseBody
    public AispResult register(@RequestBody @Validated DetectorRegisterParam param){
        return buildSuccessResult(detectorService.register(param));
    }

    @ApiOperation("删除")
    @DeleteMapping(value = "delete")
    @ResponseBody
    public AispResult delete(@RequestParam(name = "detectorCode") String detectorCode){
        return buildSuccessResult(detectorService.delete(detectorCode));
    }

    @ApiOperation("更新检测器")
    @PostMapping(value = "update")
    @ResponseBody
    public AispResult update(@RequestBody @Validated DetectorUpdateParam param){
        return buildSuccessResult(detectorService.updateById(param));
    }

    @ApiOperation("检测器查看")
    @PostMapping(value = "list")
    @ResponseBody
    public AispResult list(@RequestBody @Validated DetectorQueryParam param){
        return buildSuccessResult(detectorService.list(param));
    }

    @ApiOperation("注册git仓库")
    @PostMapping(value = "registerGit")
    @ResponseBody
    public AispResult registerGit(@RequestBody @Validated DetectorGitRegisterParam param){
        return buildSuccessResult(detectorService.registerGit(param));
    }
}
