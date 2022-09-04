package com.alibaba.sreworks.dataset.controllers.root;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


/**
 * ROOT Control
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2020/12/23 14:55
 */

@Slf4j
@RestController
@RequestMapping("/")
@Api(tags = "ROOT_PATH")
public class RootControl extends BaseController {


    @ApiOperation(value = "rootPath")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public TeslaBaseResult rootPath() {
        return buildSucceedResult(null);
    }
}
