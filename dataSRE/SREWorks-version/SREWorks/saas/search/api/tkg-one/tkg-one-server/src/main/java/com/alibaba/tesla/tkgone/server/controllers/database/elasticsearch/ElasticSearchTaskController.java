package com.alibaba.tesla.tkgone.server.controllers.database.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchClusterService;
import com.alibaba.tesla.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping(value = "/database/elasticsearch/task")
public class ElasticSearchTaskController extends BaseController {

    @Autowired
    ElasticSearchClusterService elasticSearchClusterService;

    @RequestMapping(value = "/getState/{type}/{taskId}", method = RequestMethod.GET)
    public TeslaBaseResult getState(@PathVariable String type, @PathVariable String taskId) throws Exception {
        String uri = String.format("_tasks/%s", taskId);
        JSONObject retJson = elasticSearchClusterService.getOrPost(uri, type, null, null, RequestMethod.GET, true);
        return buildSucceedResult(retJson);
    }
}
