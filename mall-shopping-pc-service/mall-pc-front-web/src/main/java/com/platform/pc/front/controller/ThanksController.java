package com.platform.pc.front.controller;

import com.platform.pc.common.pojo.DataTablesResult;
import com.platform.pc.common.pojo.Result;
import com.platform.pc.common.utils.ResultUtil;
import com.platform.pc.manager.service.ThanksService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wulinhao
 */
@RestController
@Api(description = "捐赠列表")
public class ThanksController {

    private final static Logger log= LoggerFactory.getLogger(ThanksController.class);

    @Autowired
    private ThanksService thanksService;

    @RequestMapping(value = "/member/thanks",method = RequestMethod.GET)
    @ApiOperation(value = "捐赠列表")
    public Result<DataTablesResult> getThanksList(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size){

        DataTablesResult result=thanksService.getThanksListByPage(page,size);
        return new ResultUtil<DataTablesResult>().setData(result);
    }
}
