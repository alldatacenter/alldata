package com.alibaba.tesla.tkgone.server.controllers.database.elasticsearch;

import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchMiscellaneousService;
import com.alibaba.tesla.web.controller.BaseController;
import org.elasticsearch.client.core.MainResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jialiang.tjl
 * @date 2018/08/24 说明:
 *
 * 没有处理
 */

@RestController
@RequestMapping("/api/v1/database/elasticsearch/Miscellaneous")
public class ElasticSearchMiscellaneousController extends BaseController {

    @Autowired
    ElasticSearchMiscellaneousService elasticSearchMiscellaneousService;

    @RequestMapping(value = "/getInfo", method = RequestMethod.GET)
    public TeslaBaseResult getInfo() {
        try {
            MainResponse info = elasticSearchMiscellaneousService.getInfo();
            return buildSucceedResult(info);
        } catch (Exception e) {
            return buildExceptionResult(e);
        }
    }

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public TeslaBaseResult ping() {
        try {
            boolean ping = this.elasticSearchMiscellaneousService.ping();
            return buildSucceedResult(ping);
        } catch (Exception e) {
            return buildExceptionResult(e);
        }
    }
}
