package com.alibaba.tdata.aisp.server.controller;

import com.alibaba.tdata.aisp.server.common.constant.AispResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildSuccessResult;

/**
 * @ClassName: AdminController
 * @Author: dyj
 * @DATE: 2021-12-06
 * @Description:
 **/
@Api(tags = "管理员接口")
@Slf4j
@RestController
@RequestMapping("/admin/")
public class AdminController {
    @ApiOperation("缓存查看")
    @GetMapping(value = "cache")
    @ResponseBody
    public AispResult queryCache(){
        return buildSuccessResult();
    }
}
