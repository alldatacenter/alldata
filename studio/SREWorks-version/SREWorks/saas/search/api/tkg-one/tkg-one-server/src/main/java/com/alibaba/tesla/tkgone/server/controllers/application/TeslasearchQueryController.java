package com.alibaba.tesla.tkgone.server.controllers.application;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.dto.BackendStoreDTO;
import com.alibaba.tesla.tkgone.server.services.category.AddEx;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchHttpApiBasic;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetAggGrammar;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetQueryGrammar;
import com.alibaba.tesla.tkgone.server.services.tsearch.TeslasearchService;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.log4j.Log4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Log4j
@RestController
@RequestMapping("/teslasearch/query")
@SuppressWarnings("Duplicates")
public class TeslasearchQueryController extends BaseController {

    @Autowired
    TeslasearchService teslasearchService;

    @Autowired
    BaseConfigService baseConfigService;

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    AddEx addEx;

    @Autowired
    GetQueryGrammar getQueryGrammar;

    @Autowired
    ElasticSearchHttpApiBasic elasticSearchHttpApiBasic;

    @Autowired
    GetAggGrammar getAggGrammar;

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    @RequestMapping(value = "/neo4j_meta_type_nums", method = RequestMethod.GET)
    public TeslaBaseResult neo4jMetaTypeNum(String category) throws Exception {
        List<JSONObject> retList = new ArrayList<>();
        JSONArray jsonArray = teslasearchService.metaTypeNum(category, null, getUserEmployeeId());
        jsonArray.toJavaList(JSONArray.class).forEach(jsonArray1 -> {
            JSONObject jsonObject = new JSONObject();
            String type = jsonArray1.getString(0);
            String count = jsonArray1.getString(1);
            String typeAlias = categoryConfigService.getCategoryTypeAlias(category, type);
            jsonObject.put("type", typeAlias);
            jsonObject.put("count", count);
            jsonObject.put("icon", baseConfigService.getCategoryTypeNameContent(category, type, "icon"));
            retList.add(jsonObject);
        });
        return buildSucceedResult(JSONArray.parse(retList.toString()));

    }

    @RequestMapping(value = "/node_nums_group_by_type", method = RequestMethod.GET)
    public TeslaBaseResult nodeNumsGroupByType(String category, String query,
            @RequestHeader(value = "app_debug", required = false) Boolean debug) throws Exception {
        if (debug == null || !debug) {
            teslasearchService.setHotSpot(category, query, getUserEmployeeId());
        }
        List<JSONObject> retList = new ArrayList<>();
        JSONArray jsonArray = teslasearchService.metaTypeNum(category, query, getUserEmployeeId());
        jsonArray.toJavaList(JSONArray.class).parallelStream().forEach(jsonArray1 -> {
            try {
                JSONObject jsonObject = new JSONObject();
                String type = jsonArray1.getString(0);
                jsonObject.put("realType", type);
                type = categoryConfigService.getCategoryTypeAlias(category, type);
                Object count = jsonArray1.get(1);
                jsonObject.put("type", type);
                jsonObject.put("count", count);
                retList.add(jsonObject);
            } catch (Exception e) {
                log.error("get nodes: ", e);
            }
        });

        String orderGroupsText = categoryConfigService.getCategoryNameContent(category, "categorySearchGroupOrder");
        if (StringUtils.isEmpty(orderGroupsText)) {
            orderGroupsText = "[]";
        }
        final List<String> orderGroups = JSONObject.parseArray(orderGroupsText.toLowerCase(), String.class);

        retList.parallelStream().forEach(jsonObject -> {
            try {
                String realType = jsonObject.getString("realType");
                JSONArray nodes = teslasearchService.getPreciseNodesByTypeQuery(category, realType, query);
                jsonObject.put("precise", CollectionUtils.isEmpty(nodes) ? 0 : nodes.size());

                if (orderGroups != null) {
                    int index = orderGroups.indexOf(realType.toLowerCase());
                    jsonObject.put("index", index < 0 ? 99 : index);
                }
            } catch (Exception e) {
                log.error("get precise:", e);
            }
        });
        retList.remove(null);
        retList.sort((o1, o2) -> {
            int result = ((Integer) o1.getOrDefault("index", 99)).compareTo((Integer) o2.getOrDefault("index", 99));
            if (result == 0) {
                result = ((Integer) o2.getOrDefault("precise", 0)).compareTo((Integer) o1.getOrDefault("precise", 0));
            }
            return result;
        });
        JSONArray retArray = new JSONArray();
        retArray.addAll(retList);
        return buildSucceedResult(JSONArray.parseArray(retArray.toString(), JSONObject.class));
    }

    @RequestMapping(value = "/precise_match_node", method = RequestMethod.GET)
    public TeslaBaseResult preciseMatchNode(String category, String type, String query) throws Exception {
        type = categoryConfigService.getRealTypeByAlias(category, type);
        JSONArray retNodes = new JSONArray();
        JSONArray nodes = teslasearchService.getPreciseNodesByTypeQuery(category, type, query);
        for (JSONObject node : nodes.toJavaList(JSONObject.class)) {
            retNodes.add(addEx.buildRetNode(category, node));
        }
        return buildSucceedResult(JSONArray.parse(retNodes.toString()));
    }

    @RequestMapping(value = "/query_simple_nodes_from_size", method = RequestMethod.GET)
    public TeslaBaseResult querySimpleNodesFromSize(String category, @RequestParam(defaultValue = "站点导航") String type, String query, int page, int size,
                                                    @RequestHeader(value = "app_debug", required = false) Boolean debug)
            throws Exception {
        if (debug == null || !debug) {
            teslasearchService.setHotSpot(category, query, getUserEmployeeId());
        }
        String realType = categoryConfigService.getRealTypeByAlias(category, type);

        JSONArray jsonArray = teslasearchService.getNodesByTypeQuery(category, realType, query, (page - 1) * size, size);
        return buildSucceedResult(jsonArray.toJavaList(JSONObject.class).stream().map(node -> {
            node.put(addEx.getExTitleField(), addEx.getTitle(category, node));
            JSONObject retNode = addEx.buildRetNode(category, node);

            JSONObject result = new JSONObject();
            result.putAll(retNode.getJSONObject("content").getJSONObject("kv"));
            result.put(Constant.INNER_TYPE, realType);
            result.put("type", type);
            result.put("url", addEx.getUrl(category, node));

            return result;
        }).collect(Collectors.toList()));
    }

    @RequestMapping(value = "/query_nodes_from_size", method = RequestMethod.GET)
    public TeslaBaseResult queryNodesFromSize(String category, String type, String query, int page, int size)
            throws Exception {
        type = categoryConfigService.getRealTypeByAlias(category, type);
        JSONArray jsonArray = teslasearchService.getNodesByTypeQuery(category, type, query, (page - 1) * size, size);
        return buildSucceedResult(jsonArray.toJavaList(JSONObject.class).stream().map(node -> {
            JSONObject preciseNode = addEx.getPreciseNode(category, node);
            preciseNode = addEx.delNodeMetaField(addEx.getNodeAlias(category, preciseNode));
            preciseNode.put(addEx.getExTitleField(), addEx.getTitle(category, node));
            return preciseNode;
        }).collect(Collectors.toList()));
    }

    @RequestMapping(value = "/query_nodes_from_size_add_exlink", method = RequestMethod.GET)
    public TeslaBaseResult queryNodesFromSizeAddExlink(String category, String type, String query, int page, int size)
            throws Exception {
        type = categoryConfigService.getRealTypeByAlias(category, type);
        JSONArray jsonArray = teslasearchService.getNodesByTypeQuery(category, type, query, (page - 1) * size, size);
        return buildSucceedResult(jsonArray.toJavaList(JSONObject.class).stream().map(node -> {
            node.put(addEx.getExTitleField(), addEx.getTitle(category, node));
            return addEx.buildRetNode(category, node);
        }).collect(Collectors.toList()));
    }

    @RequestMapping(value = "/get_ex_data", method = RequestMethod.GET)
    public TeslaBaseResult getExData(String category, String type, String id, String name) throws Exception {
        type = categoryConfigService.getRealTypeByAlias(category, type);
        return buildSucceedResult(teslasearchService.getExData(category, type, id, name));
    }

    @RequestMapping(value = "/get_hot_keywords", method = RequestMethod.GET)
    public TeslaBaseResult getHotKeywords(String category, Integer limit) throws Exception {

        if (limit == null || limit == 0) {
            limit = 10;
        }
        if (StringUtils.isEmpty(category)) {
            category = Constant.DEFAULT_CATEGORY;
        }
        List<BackendStoreDTO> backendStoreDTOs = elasticSearchHttpApiBasic.getSearchBackendStores(category,
                "sreworks_query_hotspot");
        JSONObject sortJson = new JSONObject();
        sortJson.put("field", "count");
        sortJson.put("reverse", true);

        JSONObject queryJson = new JSONObject();
        queryJson.put("category", category);
        JSONObject queryGrammarJson = getQueryGrammar.get(queryJson, 0, 0);
        JSONObject aggGrammarJson = getAggGrammar.getTermsAggGrammar("query", limit, sortJson);
        queryGrammarJson.putAll(aggGrammarJson);
        List<Object> retList = elasticSearchSearchService
                .getAggsByRet(elasticSearchSearchService.searchByBackendStores(backendStoreDTOs, null,
                        JSONObject.toJSONString(queryGrammarJson), RequestMethod.POST), null)
                .toJavaList(JSONArray.class).stream().map(kv -> {
                    JSONObject tmpJson = new JSONObject();
                    tmpJson.put("content", kv.getString(0));
                    tmpJson.put("type", "hot");
                    return tmpJson;
                }).collect(Collectors.toList());
        return buildSucceedResult(retList);
    }

    @RequestMapping(value = "/_log", method = RequestMethod.GET)
    public TeslaBaseResult queryLog() {
        return buildSucceedResult(null);
    }

    @RequestMapping(value = "/suggest", method = RequestMethod.GET)
    public TeslaBaseResult suggest(String category, String query, int page, int size) throws Exception {
        int from = (page - 1) * size;
        List<BackendStoreDTO> backendStoreDTOs = elasticSearchHttpApiBasic.getSearchBackendStores(category, "");
        JSONObject params = new JSONObject();
        // 新的规范约定category以_拼接，第二位为产品名称
        String product = category.split("_").length == 3 ? category.split("_")[1] : null;
        if (StringUtils.isNotEmpty(product)) {
            params.put("product", String.format("%s", product));
            params.put("buCode", teslasearchService.getUserBuCode(getUserEmployeeId()));
        }
        JSONObject queryGrammarJson = getQueryGrammar.getByCategory(category, null, null, query, params, null, from,
                size * 2, true);
        addFunctionScore(queryGrammarJson, category);
        log.info(queryGrammarJson.toJSONString());
        System.out.println(queryGrammarJson.toJSONString());

        JSONArray suggests = new JSONArray();
        List<JSONObject> retJsons = elasticSearchSearchService.searchByBackendStores(backendStoreDTOs, null,
                JSONObject.toJSONString(queryGrammarJson), RequestMethod.POST);
        suggests.addAll(elasticSearchSearchService.getSuggestByRet(category, retJsons));

        JSONArray retArray = new JSONArray();
        retArray.addAll(suggests.subList(0, Math.min(suggests.size(), size)));

        return buildSucceedResult(JSONArray.parseArray(retArray.toString(), JSONObject.class));

    }

    private void addFunctionScore(JSONObject queryGrammarJson, String category) {
        String orderGroupsText = categoryConfigService.getCategoryNameContent(category, "categorySearchGroupOrder");
        if (StringUtils.isEmpty(orderGroupsText)) {
            orderGroupsText = categoryConfigService.getCategoryIndexes(category).toJSONString();
        }
        final List<String> indices = JSONObject.parseArray(orderGroupsText.toLowerCase(), String.class);
        int indexSize = indices.size();

        JSONArray functions = new JSONArray();
        int multi = indexSize;
        for (String index : indices) {
            JSONObject function = JSONObject.parseObject(String
                    .format("{\n" + "    \"filter\":{\n" + "        \"term\":{\n" + "            \"__type\":\"%s\"\n"
                            + "        }\n" + "    },\n" + "    \"weight\":%s\n" + "}", index, 10000 * multi));
            functions.add(function);
            multi--;
        }

        JSONObject functionScore = new JSONObject();
        functionScore.put("query", queryGrammarJson.getJSONObject("query"));
        functionScore.put("functions", functions);
        functionScore.put("score_mode", "sum");

        JSONObject queryObj = new JSONObject();
        queryObj.put("function_score", functionScore);
        queryGrammarJson.put("query", queryObj);
    }

    @RequestMapping(value = "/ttLogTmpApi", method = RequestMethod.GET)
    public JSONObject ttLogTmpApi(HttpServletRequest request) throws Exception {
        String msg = request.getParameter("sys.用户输入");
        StringBuilder sb = new StringBuilder();
        JSONArray retArray = teslasearchService.getNodesByTypeQuery("ttLogChat", "", msg, 0, 2);
        JSONArray jsonArray = new JSONArray();
        for (JSONObject tmpJson : retArray.toJavaList(JSONObject.class)) {
            try {
                jsonArray.add(addEx.buildRetNode("ttLogChat", tmpJson));
            } catch (Exception e) {
                log.error(e);
            }
        }
        retArray = jsonArray;
        for (JSONObject jsonObject : retArray.toJavaList(JSONObject.class)) {
            sb.append(String.format("\n\n####[%s]\n\n", jsonObject.getJSONObject("content").getString("_title")));
            JSONObject kv = jsonObject.getJSONObject("content").getJSONObject("kv");
            for (String key : kv.keySet()) {
                sb.append(String.format("* **%s**: %s\n", key, kv.getString(key)));
            }
        }
        sb.append(String.format("\n\n[查看更多](https://tesla.alibaba-inc.com/\\#/search/s/tesla-search/%s)\n\n", msg));
        log.info(sb.toString());

        JSONObject retJson = JSONObject.parseObject("{" + "    \"success\": \"true\"," + "    \"errorCode\": \"200\","
                + "    \"errorMsg\": \"\"," + "    \"fields\": {" + "        \"msg\": \"\"" + "    }" + "}");
        retJson.getJSONObject("fields").put("msg", sb.toString());
        return retJson;
    }

    @RequestMapping(value = "/list_by_type", method = RequestMethod.POST)
    public TeslaBaseResult listByType(@RequestBody JSONObject requestBody) throws Exception {
        final String blinkJobIndex = "blink_job";
        Integer from = requestBody.getInteger("from");
        Integer size = requestBody.getInteger("size");
        String type = requestBody.getString("type");
        JSONArray matchFieldArray = requestBody.getJSONArray("match_fields");
        String query = requestBody.getString("query");
        JSONArray displayFields = requestBody.getJSONArray("display_fields");
        JSONArray notFilters = requestBody.getJSONArray("not_filters");
        JSONArray filters = requestBody.getJSONArray("filters");
        JSONArray existFilters = requestBody.getJSONArray("exist_filters");
        JSONObject requestObj = JSONObject.parseObject(String.format("{\n" + "    \"meta\": {\n"
                + "        \"from\": %s,\n" + "        \"size\": %s,\n" + "        \"type\": \"%s\"\n" + "    },\n"
                + "    \"query\": {\n" + "      \"AND\": []\n" + "    }\n" + "}", from, size, type));
        if (CollectionUtils.isNotEmpty(notFilters)) {
            requestObj.getJSONObject("query").getJSONArray("AND")
                    .add(new JSONObject(Collections.singletonMap("NOT", notFilters)));
        }
        if (CollectionUtils.isNotEmpty(filters)) {
            List<Map> filterObjs = JSONObject.parseArray(filters.toJSONString(), Map.class);
            for (Map<?, ?> filetrObj : filterObjs) {
                requestObj.getJSONObject("query").getJSONArray("AND")
                        .add(new JSONObject(Collections.singletonMap("term", filetrObj)));
            }
            ;
        }
        if (CollectionUtils.isNotEmpty(existFilters)) {
            for (Map<?, ?> existFilter : existFilters.toJavaList(Map.class)) {
                requestObj.getJSONObject("query").getJSONArray("AND")
                        .add(new JSONObject(Collections.singletonMap("exists", existFilter)));
            }
        }
        if (StringUtils.isNotEmpty(query)) {
            requestObj
                    .getJSONObject(
                            "query")
                    .getJSONArray(
                            "AND")
                    .add(new JSONObject(Collections.singletonMap("match", Collections
                            .singletonMap(String.join(";", matchFieldArray.toJavaList(String.class)), query))));
        }
        if (blinkJobIndex.equals(type)) {
            requestObj.put("sort", JSONArray.parseArray("[{\n" + "    \"_script\":{\n"
                    + "        \"script\":\"return 'RUNNING' == doc['state'].value ? 3 : ('UNKNOWN' == doc['state'].value ? 1 : 2)\",\n"
                    + "        \"type\":\"number\",\n" + "        \"order\":\"desc\"\n" + "    }\n" + "},\n" + "{\n"
                    + "    \"_score\": {\n" + "    \"order\": \"desc\"\n" + "}\n" + "}]"));
        }
        JSONArray queryArray = elasticSearchSearchService.queryByKv(requestObj);
        JSONArray userQueryArray = new JSONArray();

        if (StringUtils.isEmpty(query) && blinkJobIndex.equals(type)) {
            // 当blink_job没有查询条件时，优先查询当前用户的，且用户ID必须为number
            String empId = getUserEmployeeId();
            if (Tools.isNumber(empId)) {
                requestObj.getJSONObject("query").getJSONArray("AND").add(new JSONObject(
                        Collections.singletonMap("term", Collections.singletonMap("user", getUserEmployeeId()))));
                log.info(String.format("requestObj is: %s", requestObj.toJSONString()));
                userQueryArray = elasticSearchSearchService.queryByKv(requestObj);
            }
        }
        if (!userQueryArray.isEmpty()) {
            queryArray = userQueryArray;
        }
        List<JSONObject> results = new ArrayList<>();
        for (JSONObject queryObj : queryArray.toJavaList(JSONObject.class)) {
            JSONObject resultObj = new JSONObject();
            if (CollectionUtils.isEmpty(displayFields)) {
                resultObj = queryObj;
            } else {
                for (String displayField : displayFields.toJavaList(String.class)) {
                    if (queryObj.containsKey(displayField)) {
                        resultObj.put(displayField, queryObj.getString(displayField));
                    }
                }
            }
            results.add(resultObj);
        }
        return buildSucceedResult(results);
    }
}
