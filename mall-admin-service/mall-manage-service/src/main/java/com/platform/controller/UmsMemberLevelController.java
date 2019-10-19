package com.platform.controller;

import com.platform.mall.common.CommonResult;
import com.platform.mall.entity.admin.UmsMemberLevel;
import com.platform.mall.service.admin.UmsMemberLevelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 会员等级管理Controller
 * Created by wulinhao on 2019/9/26.
 */
@Controller
@Api(tags = "UmsMemberLevelController", description = "会员等级管理")
@RequestMapping("/memberLevel")
public class UmsMemberLevelController {
    @Autowired
    private UmsMemberLevelService memberLevelService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiOperation("查询所有会员等级")
    @ResponseBody
    public CommonResult<List<UmsMemberLevel>> list(@RequestParam("defaultStatus") Integer defaultStatus) {
        List<UmsMemberLevel> memberLevelList = memberLevelService.list(defaultStatus);
        return CommonResult.success(memberLevelList);
    }
}
