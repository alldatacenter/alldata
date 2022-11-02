package com.alibaba.tesla.tkgone.server.controllers.database.elasticsearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchClusterService;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jialiang.tjl
 * @date 2018/08/24 说明:
 *
 *       没有处理
 */

@RestController
@RequestMapping(value = "/database/elasticsearch")
public class ElasticSearchClusterController extends BaseController {

    @Autowired
    ElasticSearchClusterService elasticSearchClusterService;

    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public TeslaBaseResult getElasticHealth() {
        TeslaBaseResult teslaBaseResult;
        try {
            JSONArray health = elasticSearchClusterService.getHealth();
            teslaBaseResult = this.buildSucceedResult(health);
            return teslaBaseResult;
        } catch (Exception e) {
            teslaBaseResult = this.buildExceptionResult(e);
            return teslaBaseResult;
        }
    }
}
