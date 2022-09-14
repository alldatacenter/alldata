package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jialiang.tjl
 */
@Service
public class ElasticSearchSqlService extends ElasticSearchBasic {

    @Autowired
    ElasticSearchConfigService elasticSearchConfigService;

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    ElasticSearchIndicesService elasticSearchIndicesService;

    public JSONObject executeQuery(int from, int size, String sql) throws Exception {

        String uri = "_xpack/sql";
        Map<String, String> queryParams = new HashMap<>(0);
        queryParams.put("format", "json");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("query", StringUtils.strip(sql, "\""));
        jsonObject.put("from", from);
        jsonObject.put("size", size);
        return getOrPost(uri, getSqlIndex(sql), queryParams, JSONObject.toJSONString(jsonObject), RequestMethod.POST,
                true);
    }

    private String getSqlIndex(String sql) {
        String subStr = sql.toLowerCase().split("from")[1];
        String indexStr = subStr.split("where")[0];
        return indexStr.trim();
    }

}
