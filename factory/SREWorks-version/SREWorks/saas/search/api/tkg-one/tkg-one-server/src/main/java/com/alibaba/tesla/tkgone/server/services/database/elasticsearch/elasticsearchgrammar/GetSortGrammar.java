package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author yangjinghua
 */
@Service
public class GetSortGrammar {

    public JSONObject getQuerySort(Object inArray) {
        JSONObject retJson = new JSONObject();
        retJson.put("sort", inArray);
        return retJson;
    }

}
