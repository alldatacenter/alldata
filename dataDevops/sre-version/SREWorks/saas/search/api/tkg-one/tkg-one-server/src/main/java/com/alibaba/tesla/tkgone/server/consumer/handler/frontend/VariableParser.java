package com.alibaba.tesla.tkgone.server.consumer.handler.frontend;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.alibaba.tesla.tkgone.server.common.http.HttpOperator;
import com.alibaba.tesla.tkgone.server.domain.frontend.ApiField;
import com.alibaba.tesla.tkgone.server.domain.frontend.ApiVarConfig;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 前端变量解析
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/05/23 11:13
 */
@Slf4j
@Service
public class VariableParser {

    @Autowired
    HttpOperator httpOperator;

    public List<JSONObject> api(ApiVarConfig apiVarConfig) {
        List<JSONObject> result = new ArrayList<>();

        String resp;
        Map<String, String> headers = JSONObject.toJavaObject(apiVarConfig.getHeaders(), Map.class);
        Map<String, String> params = JSONObject.toJavaObject(apiVarConfig.getParams(), Map.class);
        if (apiVarConfig.getMethod().equalsIgnoreCase("GET")) {
            try {
                resp = httpOperator.get(apiVarConfig.getUrl(), headers, params);
            } catch (Exception ex) {
                log.warn(String.format("frontend api variable request exception; %s", ex));
                return result;
            }
        } else if (apiVarConfig.getMethod().equalsIgnoreCase("POST")) {
            try {
                resp = httpOperator.post(apiVarConfig.getUrl(), headers, params, MediaType.get(apiVarConfig.getContentType()), apiVarConfig.getBody());
            } catch (Exception ex) {
                log.warn(String.format("frontend api variable request exception; %s", ex));
                return result;
            }
        } else {
            log.warn(String.format("frontend api variable request method error, %s", apiVarConfig.getMethod()));
            return result;
        }

        // JSON格式非法
        if (!JSONValidator.from(resp).validate()) {
            log.warn(String.format("frontend api variable response illegal, %s", resp));
            return result;
        }

        Configuration conf = Configuration.builder().options(Option.ALWAYS_RETURN_LIST).build();
        DocumentContext documentContext = JsonPath.using(conf).parse(resp);

        Map<String, List<Object>> varValueMap = new HashMap<>();
        int valueSize = -1;
        for (ApiField field : apiVarConfig.getFields()) {
            List<Object> values = documentContext.read(field.getField());
            int curValueSize = values.size();
            String fieldAlias = field.getAlias();
            if (valueSize != -1 && valueSize != curValueSize) {
                log.warn(String.format("variable[%s]: not return the same number of values", fieldAlias));
                return result;
            }
            if (varValueMap.containsKey(fieldAlias)) {
                log.warn(String.format("variable[%s] conflict: check if the same variable is included", fieldAlias));
                return result;
            }
            valueSize = curValueSize;
            varValueMap.put(fieldAlias, values);
        }

        result = new ArrayList<>(Arrays.asList(new JSONObject[valueSize]));
        for (String fieldAlias : varValueMap.keySet()) {
            List<Object> values = varValueMap.get(fieldAlias);
            for (int i = 0; i < valueSize; i++) {
                JSONObject varValue = result.get(i) == null ? new JSONObject() : result.get(i);
                varValue.put(fieldAlias, values.get(i));
                result.set(i, varValue);
            }
        }
        return result;
    }

}
