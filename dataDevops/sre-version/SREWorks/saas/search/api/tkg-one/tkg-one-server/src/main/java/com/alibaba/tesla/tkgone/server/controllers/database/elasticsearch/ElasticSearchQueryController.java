package com.alibaba.tesla.tkgone.server.controllers.database.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchAnalyzeService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSpecSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSqlService;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author jialiang.tjl
 * @date 2018/08/24 说明:
 * <p>
 * <p>
 */

@Log4j
@RestController
@RequestMapping(value = "/data/elasticsearch")
public class ElasticSearchQueryController extends BaseController {

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    @Autowired
    ElasticSearchSpecSearchService elasticSearchSpecSearchService;

    @Autowired
    ElasticSearchSqlService elasticSearchSqlService;

    @Autowired
    ElasticSearchAnalyzeService elasticSearchAnalyzeService;

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public TeslaBaseResult search(String category, String type, String query, Integer page, Integer size)
        throws Exception {

        if (StringUtils.isEmpty(category)) { category = Constant.DEFAULT_CATEGORY; }
        if (page == null) { page = 1; }
        if (size == null) { size = 10; }
        Integer from = (page - 1) * size;
        return buildSucceedResult(elasticSearchSearchService.search(category, type, query, from, size));

    }

    @RequestMapping(value = "/suggest", method = RequestMethod.GET)
    public TeslaBaseResult suggest(String category, String type, String query, Integer page, Integer size)
        throws Exception {

        if (page == null) { page = 1; }
        if (size == null) { size = 10; }
        Integer from = (page - 1) * size;
        return buildSucceedResult(elasticSearchSearchService.suggest(category, type, query, from, size));

    }

    @RequestMapping(value = "/getByScroll", method = RequestMethod.POST)
    public TeslaBaseResult getByScroll(String scroll, String index) throws Exception {
        JSONObject retJson = elasticSearchSearchService.queryByScrollPre(scroll, index, "10m");
        JSONObject ret = new JSONObject();
        ret.put("scroll", elasticSearchSearchService.getScrollIdByRet(retJson));
        ret.put("nodes", elasticSearchSearchService.getNodesByRet(retJson));
        ret.put("total", elasticSearchSearchService.getTotalSizeByRet(retJson));
        return buildSucceedResult(ret);
    }

    @RequestMapping(value = "/queryByKvPre", method = RequestMethod.POST)
    public TeslaBaseResult queryByKvPre(@RequestBody JSONObject requestJson) {
        try {
            List<JSONObject> retJsons = elasticSearchSearchService.queryByKvPre(requestJson);
            JSONObject ret = new JSONObject();
            ret.put("nodes", elasticSearchSearchService.getNodesByRet(retJsons));
            ret.put("total", elasticSearchSearchService.getTotalSizeByRet(retJsons));
            ret.put("scroll", elasticSearchSearchService.getScrollIdByRet(requestJson));
            return buildSucceedResult(ret);
        } catch (Exception e) {
            log.error(String.format("queryByKvPre failed: %s", requestJson), e);
            return buildResult(HttpStatus.SC_BAD_REQUEST, "Bad Request", e);
        }

    }

    @RequestMapping(value = "/queryByKv", method = RequestMethod.POST)
    public TeslaBaseResult queryByKv(@RequestBody JSONObject requestJson) throws Exception {

        return buildSucceedResult(elasticSearchSearchService.queryByKv(requestJson));

    }

    @RequestMapping(value = "/aggByKv", method = RequestMethod.POST)
    public TeslaBaseResult aggByKv(@RequestBody JSONObject requestJson) throws Exception {

        return buildSucceedResult(elasticSearchSearchService.aggByKv(requestJson));

    }

    @RequestMapping(value = "/aggNestByKv", method = RequestMethod.POST)
    public TeslaBaseResult aggNestByKv(@RequestBody JSONObject requestJson) throws Exception {

        return buildSucceedResult(elasticSearchSearchService.aggNestByKv(requestJson));

    }

    @RequestMapping(value = "/queryBySql", method = RequestMethod.POST)
    public TeslaBaseResult queryBySql(int page, int size, @RequestBody String sql) throws Exception {

        int from = (page - 1) * size;
        return buildSucceedResult(elasticSearchSqlService.executeQuery(from, size, sql));

    }

    @RequestMapping(value = "/analyze", method = RequestMethod.POST)
    public TeslaBaseResult analyze(@RequestBody JSONObject requestBody) throws Exception {
        String index = requestBody.getString("index");
        if (null == index) {
            index = "document";
        }
        String analyzer = requestBody.getString("analyzer");
        if (null == analyzer) {
            analyzer = "ik";
        }
        String text = requestBody.getString("text");
        return buildSucceedResult(elasticSearchAnalyzeService.getWords(index, analyzer, text));
    }

}
