package com.alibaba.tdata.aisp.server.controller;

import com.alibaba.tdata.aisp.server.common.constant.AispResult;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static com.alibaba.tdata.aisp.server.common.factory.AispResponseFactory.buildSuccessResult;

/**
 * @ClassName: BaseController
 * @Author: dyj
 * @DATE: 2021-12-17
 * @Description:
 **/
@Slf4j
@RestController
public class BaseController {
    @GetMapping(value = "status.taobao")
    @ResponseBody
    public AispResult queryCache(){
        return buildSuccessResult("success");
    }
}
