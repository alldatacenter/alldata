package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jialiang.tjl
 */
@Service
public class ElasticSearchAnalyzeService extends ElasticSearchBasic {

    @Autowired
    private IndexMapper indexMapper;

    public List<String> getWords(String index, String analyzer, String text) throws Exception {
        JSONObject postJson = new JSONObject();
        postJson.put("analyzer", analyzer);
        postJson.put("text", text);
        JSONObject jsonObject = getOrPost("/" + index + "/_analyze", index, null,
                JSONObject.toJSONString(postJson), RequestMethod.POST, true);
        try {
            return jsonObject.getJSONArray("tokens").toJavaList(JSONObject.class).stream()
                    .map(x -> x.getString("token")).collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> getWords(String analyzer, String text) throws Exception {
        String index = indexMapper.getAliasIndexes().toArray(new String[0])[0];
        return getWords(index, analyzer, text);
    }

    public List<String> getIkWords(String text) throws Exception {
        return getWords("ik", text);
    }

}
