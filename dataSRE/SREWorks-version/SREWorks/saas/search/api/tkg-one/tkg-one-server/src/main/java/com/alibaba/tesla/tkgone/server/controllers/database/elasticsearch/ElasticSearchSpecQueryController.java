package com.alibaba.tesla.tkgone.server.controllers.database.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSpecSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSqlService;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jialiang.tjl
 * @date 2018/08/24 说明:
 * <p>
 * <p>
 */

@RestController
@RequestMapping(value = "/data/elasticsearch/spec")
public class ElasticSearchSpecQueryController extends BaseController {

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    @Autowired
    ElasticSearchSpecSearchService elasticSearchSpecSearchService;

    @Autowired
    ElasticSearchSqlService elasticSearchSqlService;

    @RequestMapping(value = "/universeEventAggTimeSeriesSample", method = RequestMethod.POST)
    public TeslaBaseResult universeEventAggTimeSeriesSample(@RequestBody JSONObject requestJson) throws Exception {

        return buildSucceedResult(elasticSearchSpecSearchService.universeEventAggTimeSeriesSample(requestJson));

    }


}
