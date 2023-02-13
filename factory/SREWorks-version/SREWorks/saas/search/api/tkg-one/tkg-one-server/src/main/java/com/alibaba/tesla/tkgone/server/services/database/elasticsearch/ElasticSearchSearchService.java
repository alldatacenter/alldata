package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.DateUtil;
import com.alibaba.tesla.tkgone.server.common.FlatJson;
import com.alibaba.tesla.tkgone.server.common.MapUtil;
import com.alibaba.tesla.tkgone.server.domain.dto.BackendStoreDTO;
import com.alibaba.tesla.tkgone.server.services.config.BackendStoreConfigService;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetAggGrammar;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetQueryGrammar;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetSortGrammar;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetSourceGrammar;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.params.ElasticsearchSearchMeta;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jialiang.tjl
 */
@Service
public class ElasticSearchSearchService extends ElasticSearchBasic {

    public static final long QUERY_TIMERANGE_MAX_MS = 2 * 30 * 24 * 60 * 60 * 1000L;
    public static final String BIG_DATA_INDEX = "universe_event";
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

    @Autowired
    GetSortGrammar getSortGrammar;

    @Autowired
    GetAggGrammar getAggGrammar;

    @Autowired
    GetSourceGrammar getSourceGrammar;

    @Autowired
    ElasticSearchHttpApiBasic elasticSearchHttpApiBasic;

    @Autowired
    ElasticSearchDeleteByQueryService elasticSearchDeleteByQueryService;

    public JSONArray suggest(String category, String type, String query, Integer from, Integer size) throws Exception {
        JSONObject queryGrammarJson = getQueryGrammar.getByCategory(
            category, null, null, query, null, null, from, size * 2, true);

        List<BackendStoreDTO> backendStoreDTOs = elasticSearchHttpApiBasic.getSearchBackendStores(category, type);
        List<JSONObject> suggests = elasticSearchHttpApiBasic.searchByBackendStores(backendStoreDTOs, null,
                JSONObject.toJSONString(queryGrammarJson), RequestMethod.POST);
        return JSONObject.parseArray(JSONObject.toJSONString(suggests.subList(0, Math.min(suggests.size(), size))));
    }

    public JSONArray search(String category, String type, String query, Number from, Number size) throws Exception {
        JSONArray retArray = new JSONArray();
        Object[] types = type.split(",");
        JSONObject searchBody = getQueryGrammar.getByCategory(
            category, new JSONArray(Arrays.asList(types)), null, query, null, null, from, size, false);

        List<BackendStoreDTO> backendStoreDTOs = elasticSearchHttpApiBasic.getSearchBackendStores(category, "");
		List<JSONObject> retJsons = elasticSearchHttpApiBasic.searchByBackendStores(backendStoreDTOs, null,
                JSONObject.toJSONString(searchBody), RequestMethod.POST);
        for (JSONObject retJson : retJsons) {
            retArray.addAll(getNodesByRet(retJson));
        }
        return retArray;
    }

    public JSONObject queryByScrollPre(String scroll, String index, String cacheTime) throws Exception {
        if (StringUtils.isEmpty(cacheTime)) {
            cacheTime = "30m";
        }
        String uri = "/_search/scroll";
        JSONObject postJson = new JSONObject();
        postJson.put("scroll", cacheTime);
        postJson.put("scroll_id", scroll);
        return getOrPost(uri, index, null, JSONObject.toJSONString(postJson), RequestMethod.POST, true);
    }

    public List<JSONObject> queryByKvPre(JSONObject requestJson) throws Exception {
        JSONObject metaJson = requestJson.getJSONObject("meta");
        JSONObject queryJson = requestJson.getJSONObject("query");
        Object source = requestJson.get("source");
        ElasticsearchSearchMeta esMeta = new ElasticsearchSearchMeta(metaJson, queryJson);
        JSONArray sort = requestJson.getJSONArray("sort");
        String analyzer = requestJson.getString("analyzer");
        List<String> types = esMeta.getTypes();

        JSONObject postBody = new JSONObject();
        JSONObject queryGrammarJson = getQueryGrammar.get(queryJson, esMeta.getFrom(), esMeta.getSize());
        if (StringUtils.isNotEmpty(analyzer)) {
            queryGrammarJson = getQueryGrammar.get(queryJson, esMeta.getFrom(), esMeta.getSize(), analyzer);
        }
        JSONObject sortGrammarJson = getSortGrammar.getQuerySort(sort);
        JSONObject sourceGrammarJson = getSourceGrammar.get(source);
        postBody.putAll(queryGrammarJson);
        postBody.putAll(sortGrammarJson);
        postBody.putAll(sourceGrammarJson);

        Map<String, String> queryParams = new HashMap<>();
        final String scrollField = "scroll";
        if (requestJson.containsKey(scrollField)) {
            queryParams.put("scroll", requestJson.getString("scroll"));
        }
        return elasticSearchHttpApiBasic.searchByIndices(
                types, 
                queryParams, JSONObject.toJSONString(postBody),
                RequestMethod.POST, true);
    }

    public JSONArray queryByKv(JSONObject requestJson) throws Exception {
        return getNodesByRet(queryByKvPre(requestJson));
    }

    public JSONObject getUniqueNodeByKv(JSONObject kv) throws Exception {
        kv = FlatJson.jsonFormatter(kv);
        JSONObject requestJson = new JSONObject();
        requestJson.put("query", kv);
        JSONArray nodes = queryByKv(requestJson);
        if (nodes.size() == 1) {
            return nodes.getJSONObject(0);
        } else {
            throw new Exception("无法根据参数匹配到唯一节点, 匹配到节点个数: " + nodes.size());
        }
    }

    public JSONObject aggNestByKv(JSONObject requestJson) throws Exception {
        JSONObject metaJson = requestJson.getJSONObject("meta");
        JSONObject queryJson = requestJson.getJSONObject("query");
        JSONObject sortJson = requestJson.getJSONObject("sort");
        ElasticsearchSearchMeta esMeta = new ElasticsearchSearchMeta(metaJson, queryJson);
        List<String> types = esMeta.getTypes();
        JSONObject postBody = new JSONObject();
        JSONObject queryGrammar = getQueryGrammar.get(queryJson, 0, 0);
        JSONObject aggGrammar = getAggGrammar.getNestAggGrammars(metaJson, sortJson);
        postBody.putAll(queryGrammar);
        postBody.putAll(aggGrammar);

        return getNestAggsByRet(elasticSearchHttpApiBasic.searchByIndices(types, null, JSONObject.toJSONString(postBody),
                RequestMethod.POST, true), metaJson);
    }

    public JSONArray aggByKv(JSONObject requestJson, List<String> indices, boolean isLog) throws Exception {
        JSONObject metaJson = requestJson.getJSONObject("meta");
        JSONObject queryJson = requestJson.getJSONObject("query");
        checkQueryCondition(queryJson);

        ElasticsearchSearchMeta esMeta = new ElasticsearchSearchMeta(metaJson, queryJson);
        JSONObject sortJson = requestJson.getJSONObject("sort");
        String style = requestJson.getString("style");

        JSONObject postBody = new JSONObject();
        JSONObject queryGrammar = getQueryGrammar.get(queryJson, 0, 0);
        JSONObject aggGrammar;

        switch (esMeta.getAggType()) {
            case "range":
                aggGrammar = getAggGrammar.getRangeAggGrammar(
                        esMeta.getAggField(), esMeta.getFrom(), esMeta.getTo(), esMeta.getInterval());
                break;
            case "distinct":
                aggGrammar = getAggGrammar.getTermsAggGrammar(
                        esMeta.getAggField(), esMeta.getAggSize(), sortJson);
                break;
            default:
                aggGrammar = new JSONObject();
                break;
        }
        postBody.putAll(queryGrammar);
        postBody.putAll(aggGrammar);
        if (indices == null) {
            indices = esMeta.getTypes();
        }

        return getAggsByRet(searchByIndices(indices, null, JSONObject.toJSONString(postBody),
                    RequestMethod.POST, true), style);
    }

    public JSONArray aggByKv(JSONObject requestJson) throws Exception {
       return this.aggByKv(requestJson, null, true);
    }

    private void checkQueryCondition(JSONObject queryJson) {
        if (queryJson == null) {
            return;
        }
        Object type = MapUtil.getObject(queryJson, "match.__type");
        if (type == null || !BIG_DATA_INDEX.equalsIgnoreCase(type.toString())) {
            return;
        }

        JSONObject match = queryJson.getJSONObject("match");
        JSONObject range = queryJson.getJSONObject("range");
        JSONArray time = null;
        if (range != null) {
            time = range.getJSONArray("time");
        }

        if (match != null && time != null) {
            long timeRangeMS;
            try {
                timeRangeMS = DateUtil.parse(time.get(1).toString(), DateUtil.PATTERN_YYYYMMDD_HHMMSS).getTime() -
                    DateUtil.parse(time.get(0).toString(), DateUtil.PATTERN_YYYYMMDD_HHMMSS).getTime();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format("Query condition[time %s~%s] invalid", time.get(0), time.get(1)));
            }

            if (timeRangeMS >= QUERY_TIMERANGE_MAX_MS) {
                JSONObject terms = queryJson.getJSONObject("terms");
                if (terms == null || terms.isEmpty()) {
                    if (match.size() <= 3) {
                        throw new IllegalArgumentException("查询范围过大，请缩小查询范围。如修改查询时间跨度或增加查询对象");
                    }
                }
            }
        }
    }

    public JSONArray getNodesByRet(JSONObject jsonObject) {
        JSONArray retArray = new JSONArray();
        jsonObject = jsonObject.getJSONObject("hits");
        if (jsonObject != null) {
            retArray.addAll(jsonObject.getJSONArray("hits").toJavaList(JSONObject.class).stream()
                .map(x -> x.getJSONObject("_source")).collect(Collectors.toList()));
        }
        return retArray;
    }

    public JSONArray getNodesByRet(List<JSONObject> jsonObjects) {
        JSONArray retArray = new JSONArray();
        for (JSONObject jsonObject : jsonObjects) {
            retArray.addAll(getNodesByRet(jsonObject));
        }
        return retArray;
    }

    public String getScrollIdByRet(JSONObject jsonObject) {
        return jsonObject.getString("_scroll_id");
    }

    public int getTotalSizeByRet(JSONObject jsonObject) {
        try {
            return jsonObject.getJSONObject("hits").getIntValue("total");
        } catch (Exception e) {
            return 0;
        }
    }

    public int getTotalSizeByRet(List<JSONObject> jsonObjects) {
        int total = 0;
        for (JSONObject jsonObject : jsonObjects) {
            total += getTotalSizeByRet(jsonObject);
        }
        return total;
    }

    public JSONArray getAggsByRet(JSONObject jsonObject, String style) throws Exception {
        style = StringUtils.isEmpty(style) ? "kv" : style;
        JSONArray retArray = new JSONArray();
        switch (style) {
            case "kv":
                for (JSONObject eachJson : jsonObject.getJSONObject("aggregations").getJSONObject("agg")
                    .getJSONArray("buckets").toJavaList(JSONObject.class)) {
                    JSONArray tmpArray = new JSONArray();

                    tmpArray.add(eachJson.getOrDefault("from", eachJson.get("key")));
                    tmpArray.add(eachJson.get("doc_count"));
                    retArray.add(tmpArray);
                }
                break;
            case "options":
                for (JSONObject eachJson : jsonObject.getJSONObject("aggregations").getJSONObject("agg")
                    .getJSONArray("buckets").toJavaList(JSONObject.class)) {
                    JSONObject tmpJson = new JSONObject();
                    Object key = eachJson.getOrDefault("from", eachJson.get("key"));
                    tmpJson.put("label", key);
                    tmpJson.put("value", key);
                    retArray.add(tmpJson);
                }
                break;
            default:
                throw new Exception("没有这样类型的展示方案: " + style);
        }
        return retArray;
    }

    public JSONArray getAggsByRet(List<JSONObject> jsonObjects, String style) throws Exception {
        style = StringUtils.isEmpty(style) ? "kv" : style;
        JSONArray retArray = new JSONArray();
        for (JSONObject jsonObject : jsonObjects) {
            retArray.addAll(getAggsByRet(jsonObject, style));
        }
        return retArray;
    }

    private JSONArray parseAggs(JSONArray buckets, List<String> groupFields) {
        String field = groupFields.get(0);
        JSONArray retArray = new JSONArray();
        for (JSONObject bucket : buckets.toJavaList(JSONObject.class)) {
            JSONObject bucketJson = new JSONObject();
            String fieldValue = bucket.getString("key");
            int num = bucket.getIntValue("doc_count");
            bucketJson.put(field, fieldValue);
            bucketJson.put("num", num);
            bucketJson.put(field + ":num", num);
            JSONObject sonName = bucket.getJSONObject("agg");
            JSONArray sonBuckets = sonName == null ? null : sonName.getJSONArray("buckets");
            if (!CollectionUtils.isEmpty(sonBuckets)) {
                for (JSONObject sonJson :
                    parseAggs(sonBuckets, groupFields.subList(1, groupFields.size())).toJavaList(JSONObject.class)) {
                    for (String key : bucketJson.keySet()) {
                        if (!"num".equals(key)) {
                            sonJson.put(key, bucketJson.get(key));
                        }
                    }
                    retArray.add(sonJson);
                }
            } else {
                retArray.add(bucketJson);
            }
        }
        return retArray;
    }

    public JSONObject getNestAggsByRet(JSONObject retJson, JSONObject metaJson) throws Exception {
        List<String> groupFields = getAggGrammar.getNestAggFields(metaJson);
        ElasticsearchSearchMeta esMeta = new ElasticsearchSearchMeta(metaJson, new JSONObject());
        int from = esMeta.getFrom().intValue();
        int size = esMeta.getSize().intValue();
        
        JSONArray buckets = retJson.getJSONObject("aggregations").getJSONObject("agg").getJSONArray("buckets");
        JSONArray retArray = parseAggs(buckets, groupFields);
        int retArraySize = retArray.size();

        JSONObject ret = new JSONObject();
        ret.put("total", retArraySize);
        if (from >= retArraySize) {
            ret.put("nodes", new JSONArray());
        } else {
            ret.put("nodes", retArray.subList(from, Math.min(retArraySize, from + size)));
        }
        return ret;
    }

    public JSONObject getNestAggsByRet(List<JSONObject> retJsons, JSONObject metaJson) throws Exception {
        JSONObject ret = new JSONObject();

        ElasticsearchSearchMeta esMeta = new ElasticsearchSearchMeta(metaJson, new JSONObject());
        int from = esMeta.getFrom().intValue();
        int size = esMeta.getSize().intValue();

        int retArraySize = 0;
        JSONArray nodes = new JSONArray();
        for (JSONObject retJson : retJsons) {
            JSONObject aggObj = getNestAggsByRet(retJson, metaJson);
            retArraySize += aggObj.getInteger("total");
            nodes.addAll(aggObj.getJSONArray("nodes"));
        }
        ret.put("total", retArraySize);
        if (from >= retArraySize) {
            ret.put("nodes", new JSONArray());
        } else {
            ret.put("nodes", nodes.subList(from, Math.min(retArraySize, from + size)));
        }
        return ret;
    }

    public JSONArray getSuggestByRet(String category, JSONObject jsonObject) {
        JSONArray retArray = new JSONArray();
        Set<String> tmpSet = new HashSet<>();
        jsonObject = jsonObject.getJSONObject("hits");
        if (jsonObject != null) {
            jsonObject.getJSONArray("hits").toJavaList(JSONObject.class).forEach(node -> {
                String type = node.getJSONObject("_source").getString(Constant.INNER_TYPE);
                JSONObject highlightJson = node.getJSONObject("highlight");
                highlightJson = highlightJson == null ? new JSONObject() : highlightJson;
                for (String field : highlightJson.keySet()) {
                    String realValue = node.getJSONObject("highlight").getJSONArray(field).getString(0)
                        .replace("<em>", "").replace("</em>", "");
                    String tmpSetNode = type + "." + realValue;
                    if (!tmpSet.contains(tmpSetNode)) {
                        JSONObject retArrayJson = new JSONObject();
                        retArrayJson.put(Constant.INNER_TYPE, type);
                        retArrayJson.put("type", categoryConfigService.getCategoryTypeAlias(category, type));
                        retArrayJson.put("content",
                            node.getJSONObject("highlight").getJSONArray(field).getString(0)
                                .replace("<em>", "").replace("</em>", ""));
                        tmpSet.add(tmpSetNode);
                        retArray.add(retArrayJson);
                    }
                }
            });
        }
        return retArray;
    }

    public JSONArray getSuggestByRet(String category, List<JSONObject> jsonObjects) {
        JSONArray retArray = new JSONArray();
        for (JSONObject jsonObject : jsonObjects) {
            retArray.addAll(getSuggestByRet(category, jsonObject));
        }
        return retArray;
    }
}
