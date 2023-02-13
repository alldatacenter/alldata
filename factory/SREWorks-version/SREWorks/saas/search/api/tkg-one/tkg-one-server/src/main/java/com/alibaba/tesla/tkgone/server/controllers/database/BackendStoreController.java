package com.alibaba.tesla.tkgone.server.controllers.database;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.RedisHelper;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.config.ApplicationProperties;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchDeleteByQueryService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchIndicesService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchUpsertService;
import com.alibaba.tesla.tkgone.server.services.tsearch.BackendStoreService;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author yangjinghua
 */
@RestController
@RequestMapping("/upsertData/njeh")
public class BackendStoreController extends BaseController {

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    BackendStoreService njehService;

    @Autowired
    ElasticSearchUpsertService elasticSearchUpsertService;

    @Autowired
    ElasticSearchDeleteByQueryService elasticSearchDeleteByQueryService;

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    RedisHelper redisHelper;

    @RequestMapping(value = "/upsert", method = RequestMethod.POST)
    public TeslaBaseResult upsert(@RequestBody List<List<JSONObject>> njehData, String partition) throws Exception {
        
        if (StringUtils.isEmpty(partition)) { partition = Constant.DEFAULT_PARTITION; }
        return buildSucceedResult(njehService.importData("api/upsert", njehData, partition));

    }

    @RequestMapping(value = "/createType", method = RequestMethod.POST)
    public TeslaBaseResult createType(String type) throws Exception {
        String id = UUID.randomUUID().toString();
        JSONObject node = new JSONObject();
        node.put(Constant.INNER_ID, id);
        node.put(Constant.INNER_TYPE, type);
        node.put(Constant.PARTITION_FIELD, Constant.DEFAULT_PARTITION);
        node.put(Constant.UPSERT_TIME_FIELD, 0);
        elasticSearchUpsertService.upsert(Collections.singletonList(node));
        Tools.sleepOneSecond();
        elasticSearchDeleteByQueryService.deleteByKv(type, node);
        return buildSucceedResult("OK");
    }
}
