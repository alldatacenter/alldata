package com.alibaba.tesla.tkgone.server.controllers.database.elasticsearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.RedisHelper;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchDocumentService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchIndicesService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchUpsertService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import com.alibaba.tesla.tkgone.server.services.tsearch.BackendStoreService;
import com.alibaba.tesla.web.controller.BaseController;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jialiang.tjl
 * @date 2018/08/24 说明:
 * <p>
 * 没有处理
 */

@RestController
@RequestMapping("/database/elasticsearch/document")
public class ElasticSearchDocumentController extends BaseController {

    @Autowired
    ElasticSearchDocumentService elasticSearchDocumentService;

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    ElasticSearchUpsertService elasticSearchUpsertService;

    @Autowired
    BackendStoreService njehService;

    @Autowired
    RedisHelper redisHelper;

    @Autowired
    private IndexMapper indexMapper;

    private JSONObject getPartitionAndId(String index, String partition, String simpleId) {
        if (StringUtils.isEmpty(partition) || "null".equals(partition.toLowerCase())) {
            partition = categoryConfigService.getCategoryIndexPartition(null, index);
        }
        partition = StringUtils.isEmpty(partition) ? Constant.DEFAULT_PARTITION : partition;
        String id = simpleId + Constant.AFTER_SEPARATOR + partition;
        JSONObject retJson = new JSONObject();
        retJson.put("partition", partition);
        retJson.put("id", id);
        return retJson;
    }

    @RequestMapping(value = "/upsert", method = RequestMethod.POST)
    public TeslaBaseResult upsert(@RequestBody JSONObject node) throws Exception {
        node = elasticSearchUpsertService.fillNodeMeta(node);
        String index = node.getString(Constant.INNER_TYPE);
        if (!indexMapper.isAliasContains(index)) {
            elasticSearchIndicesService.create(index);
        }
        return buildSucceedResult(elasticSearchUpsertService.upsert(Collections.singletonList(node)));
    }

    @RequestMapping(value = "/upserts", method = RequestMethod.POST)
    public TeslaBaseResult upserts(@RequestBody JSONArray jsonArray) throws Exception {
        jsonArray = JSONArray.parseArray(JSONArray.toJSONString(jsonArray));
        List<JSONObject> nodes = new ArrayList<>();
        try {
            for (JSONArray subJsonArray : jsonArray.toJavaList(JSONArray.class)) {
                nodes.addAll(subJsonArray.toJavaList(JSONObject.class));
            }
        } catch (Exception ignored) {
            nodes.addAll(jsonArray.toJavaList(JSONObject.class));
        }
        nodes = elasticSearchUpsertService.fillNodesMeta(nodes);
        String index = nodes.get(0).getString(Constant.INNER_TYPE);
        if (!indexMapper.isAliasContains(index)) {
            elasticSearchIndicesService.create(index);
        }
        return buildSucceedResult(elasticSearchUpsertService.upsert(nodes));
    }

    @RequestMapping(value = "/strictUpserts", method = RequestMethod.POST)
    public TeslaBaseResult strictUpserts(@RequestBody JSONArray jsonArray) throws Exception {
        jsonArray = JSONArray.parseArray(JSONArray.toJSONString(jsonArray));
        List<JSONObject> upsertNodes = new ArrayList<>();
        try {
            for (JSONArray subJsonArray : jsonArray.toJavaList(JSONArray.class)) {
                upsertNodes.addAll(subJsonArray.toJavaList(JSONObject.class));
            }
        } catch (Exception e) {
            upsertNodes.addAll(jsonArray.toJavaList(JSONObject.class));
        }
        upsertNodes = elasticSearchUpsertService.fillNodesMeta(upsertNodes);
        elasticSearchUpsertService.addNodesIndex(upsertNodes);
        JSONObject upsertResult = elasticSearchUpsertService.upsert(upsertNodes);
        return buildResult(
                upsertResult.getBoolean("errors") ? HttpStatus.SC_BAD_REQUEST : HttpStatus.SC_OK,
                upsertResult.getBoolean("errors") ? "NOK" : "OK", upsertResult);
    }

    @RequestMapping(value = "/updateByQuery", method = RequestMethod.POST)
    public TeslaBaseResult updateByQuery(@RequestBody JSONObject jsonObject, String type) throws Exception {
        JSONObject queryJson = jsonObject.getJSONObject("query");
        JSONObject updateJson = jsonObject.getJSONObject("update");
        return buildSucceedResult(elasticSearchUpsertService.updateByQuery(type, queryJson, updateJson));
    }

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public TeslaBaseResult get(String index, String id, String partition)
        throws Exception {
        id = getPartitionAndId(index, partition, id).getString("id");
        return buildSucceedResult(elasticSearchDocumentService.get(index, id));
    }

    @RequestMapping(value = "/exists", method = RequestMethod.GET)
    public TeslaBaseResult exists(String index, String id, String partition)
        throws Exception {
        id = getPartitionAndId(index, partition, id).getString("id");
        return buildSucceedResult(elasticSearchDocumentService.exists(index, id));
    }

    @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(String index, String id, String partition)
        throws Exception {
        id = getPartitionAndId(index, partition, id).getString("id");
        return buildSucceedResult(elasticSearchDocumentService.delete(index, id));
    }

}
