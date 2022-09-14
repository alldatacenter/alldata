package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class GetSourceGrammar {

    public JSONObject get(Object source) {
        JSONObject sourceJson = new JSONObject();
        if (source != null) {
            sourceJson.put("_source", source);
        }
        return sourceJson;
    }

}
