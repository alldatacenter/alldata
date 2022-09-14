package com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author yangjinghua
 */
@Service
public class GetSearchHighlightGrammar {

    JSONObject init() {
        return JSONObject.parseObject("{"
            + "    \"highlight\" : {"
            + "        \"fields\" : {"
            + "        }"
            + "    }"
            + "}");
    }

    public JSONObject add(JSONObject searchHighlightJson, JSONArray fields) {
        if (CollectionUtils.isEmpty(searchHighlightJson)) {
            searchHighlightJson = init();
        }
        for (String field : fields.toJavaList(String.class)) {
            field = field.split("\\^")[0];
            searchHighlightJson.getJSONObject("highlight").getJSONObject("fields").put(field, new JSONObject());
        }
        return searchHighlightJson;
    }
}
