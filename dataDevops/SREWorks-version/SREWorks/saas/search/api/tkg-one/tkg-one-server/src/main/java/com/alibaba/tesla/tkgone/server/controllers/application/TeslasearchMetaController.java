package com.alibaba.tesla.tkgone.server.controllers.application;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xueyong.zxy
 */
@RestController
@RequestMapping("/teslasearch/get_meta")
public class TeslasearchMetaController extends BaseController {
    @RequestMapping(value = "/get_common_keyword", method = RequestMethod.GET)
    public TeslaBaseResult getCommonKeyword() {
        return buildSucceedResult(null);
    }
}
