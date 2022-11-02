package com.alibaba.tesla.tkgone.server.controllers.sreworks;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.domain.dto.BackendStoreDTO;
import com.alibaba.tesla.tkgone.server.services.category.AddEx;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchHttpApiBasic;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetAggGrammar;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetQueryGrammar;
import com.alibaba.tesla.tkgone.server.services.sreworks.SreworksSearchQueryService;
import com.alibaba.tesla.tkgone.server.services.tsearch.TeslasearchService;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.log4j.Log4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Log4j
@RestController
@RequestMapping("/sreworkssearch/query")
@SuppressWarnings("Duplicates")
public class SreworksSearchQueryController extends BaseController {

    @Autowired
    TeslasearchService teslasearchService;

    @Autowired
    SreworksSearchQueryService sreworksSearchQueryService;

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

    @RequestMapping(value = "/query_simple_nodes_from_size", method = RequestMethod.GET)
    public TeslaBaseResult querySimpleNodesFromSize(String category, @RequestParam(required = false) String types, String query, int page, int size,
                                                    @RequestHeader(value = "app_debug", required = false) Boolean debug) throws Exception {
        List<JSONObject> allIndex = new ArrayList<>();
        if (StringUtils.isEmpty(types)) {
            allIndex = sreworksSearchQueryService.getAllTypes(category);
        } else {
            for(String type : types.split(",")) {
                JSONObject index = new JSONObject();
                index.put("index", type);
                index.put("alias", categoryConfigService.getCategoryTypeAlias(category, type));
                allIndex.add(index);
            }
        }

        List<JSONObject> results = new ArrayList<>();
        for (JSONObject index : allIndex) {
            JSONArray jsonArray = teslasearchService.getNodesByTypeQuery(category, index.getString("index"), query, (page - 1) * size, size);
            List<JSONObject> indexResult = jsonArray.toJavaList(JSONObject.class).stream().map(node -> {
                JSONObject result = new JSONObject();
                result.put(Constant.INNER_TYPE, index.getString("index"));
                result.put(Constant.INNER_ID, node.get(Constant.INNER_ID));
                result.put(Constant.INNER_GMT_CREATE, node.get(Constant.INNER_GMT_CREATE));
                result.put(Constant.INNER_GMT_MODIFIED, node.get(Constant.INNER_GMT_MODIFIED));
                result.put("type", index.getString("alias"));
                result.put("title", addEx.getTitle(category, node));
                result.put("url", addEx.getUrl(category, node));
                result.put("icon", categoryConfigService.getCategoryTypeIdIcon(category, node.getString(Constant.INNER_TYPE), node.getString(Constant.INNER_ID)));
                JSONObject buildNode = addEx.buildSimpleRetNode(category, node);
                result.putAll(buildNode.getJSONObject("content"));
                result.put(Constant.INNER_CONTENT, buildNode.getJSONObject("content_str"));

                return result;
            }).collect(Collectors.toList());

            results.addAll(indexResult);
        }

        // 添加热点词汇
        if ((debug == null || !debug) && StringUtils.isNotEmpty(query) && CollectionUtils.isNotEmpty(results)) {
            teslasearchService.setHotSpot(category, query, getUserEmployeeId());
        }


        return buildSucceedResult(results);
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
                    tmpJson.put(Constant.INNER_TYPE, "hot");
                    tmpJson.put("type", "hot");
                    return tmpJson;
                }).collect(Collectors.toList());
        return buildSucceedResult(retList);
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

    @RequestMapping(value = "/listTypes", method = RequestMethod.GET)
    public TeslaBaseResult listTypes(String category){
        return buildSucceedResult(sreworksSearchQueryService.getAllTypes(category));
    }


}
