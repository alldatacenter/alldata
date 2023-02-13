package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.services.config.BackendStoreConfigService;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetQueryGrammar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;


/**
 * @author jialiang.tjl
 */
@Service
@Slf4j
public class ElasticSearchDeleteByQueryService extends ElasticSearchBasic {
    private static final int SCROLL_SIZE = 5000;

    @Autowired
    ElasticSearchConfigService elasticSearchConfigService;

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    BackendStoreConfigService njehConfigService;

    @Autowired
    GetQueryGrammar getQueryGrammar;

    public void deleteInvalidNode(String type, String field, String border) throws Exception {
            JSONObject queryJson = JSONObject.parseObject(String.format("{"
                + "    \"range\": {"
                + "        \"%s\": [null, \"%s\"]"
                + "    }"
                + "}", field, border));
            deleteByKvFull(type, true, queryJson);
        }

    public void deleteByKv(String index, JSONObject kv) throws Exception {
        deleteByKvFull(index, true, kv);
    }

    public void deleteByKvFull(String index, boolean waitForCompletion, JSONObject kv) throws Exception {
        JSONObject queryJson = getQueryGrammar.get(kv);
        String uri = String.format("/%s/_delete_by_query", index);
        Map<String, String> queryParams = new HashMap<>(0);
        queryParams.put("conflicts", "proceed");
        queryParams.put("wait_for_completion", Boolean.toString(waitForCompletion));

        getOrPost(uri, index, queryParams, JSONObject.toJSONString(queryJson), RequestMethod.POST, true);
    }

    public String deleteInvalidNodeNoWait(String type, String field, String border) throws Exception {
        JSONObject queryJson = JSONObject.parseObject(String.format("{"
                + "    \"range\": {"
                + "        \"%s\": [null, \"%s\"]"
                + "    }"
                + "}", field, border));
        return deleteByQueryNoWait(type, queryJson);
    }

    public String deleteByQueryNoWait(String index, JSONObject queryObject) throws Exception {
        JSONObject queryJson = getQueryGrammar.get(queryObject);
        String uri = String.format("/%s/_delete_by_query", index);
        Map<String, String> queryParams = new HashMap<>(8);
        queryParams.put("conflicts", "proceed");
        queryParams.put("wait_for_completion", Boolean.toString(false));
        queryParams.put("scroll_size", Integer.toString(SCROLL_SIZE));
        JSONObject result = getOrPost(uri, index, queryParams, queryJson.toJSONString(), RequestMethod.POST,
                true);
        log.info(String.format("delete by query uri: %s", uri));
        log.info(String.format("delete by query queryParams: %s", queryParams));
        log.info(String.format("delete by query queryJson: %s", queryJson.toJSONString()));
        log.info(String.format("delete by query result: %s",result.toJSONString()));
        return result.getString("task");
    }

    public JSONObject queryDeletingTasks(String index) throws Exception {
        String uri = String.format("/_tasks/?detailed=true&actions=*/delete/byquery");
        return getOrPost(uri, index, null, null, RequestMethod.GET, true);
    }
}
