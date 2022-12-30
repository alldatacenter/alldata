package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.config.ApplicationProperties;
import com.alibaba.tesla.tkgone.server.domain.dto.BackendStoreDTO;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangjinghua
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Log4j
public class ElasticSearchBasic extends ElasticSearchHttpApiBasic {

    @Autowired
    public ApplicationProperties applicationProperties;

    @Autowired
    public ElasticSearchConfigService elasticSearchConfigService;

    public Map<String, Object> cache = new ConcurrentHashMap<>();

    Map<String, RestHighLevelClient> restHighLevelClientMap = new ConcurrentHashMap<>();

    public List<RestHighLevelClient> getRestHighLevelClients(List<String> indices) {
        List<RestHighLevelClient> restHighLevelClients = new ArrayList<>();

        List<BackendStoreDTO> backendStoreDTOs = getSearchBackendStores(indices);
        for (BackendStoreDTO backendStoreDTO : backendStoreDTOs) {
            if (!restHighLevelClientMap.containsKey(backendStoreDTO.getHost())) {
                HttpHost httpHost = new HttpHost(backendStoreDTO.getHost(), backendStoreDTO.getPort(),
                        backendStoreDTO.getSchema());
                RestClientBuilder restClientBuilder = RestClient.builder(httpHost).setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(elasticSearchConfigService.getElasticsearchWriteTimeout() * 1000)
                                .setSocketTimeout(elasticSearchConfigService.getElasticsearchWriteTimeout() * 1000));
                if (StringUtils.isNotEmpty(backendStoreDTO.getUser())
                        && StringUtils.isNotEmpty(backendStoreDTO.getPassword())) {
                    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(backendStoreDTO.getUser(), backendStoreDTO.getPassword()));
                    restClientBuilder.setHttpClientConfigCallback(
                            httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
                }
                restHighLevelClientMap.put(backendStoreDTO.getHost(), new RestHighLevelClient(restClientBuilder));
            }
            restHighLevelClients.add(restHighLevelClientMap.get(backendStoreDTO.getHost()));
        }

        return restHighLevelClients;
    }

    public RestHighLevelClient getRestHighLevelClient(String index) {
        try {
            return getRestHighLevelClients(Arrays.asList(index)).get(0);
        } catch (Exception e) {
            log.error(String.format("get %s rest high level client failed", index), e);
            return null;
        }
    }

    public JSONObject adjustJsonObjectToEs(JSONObject jsonObject) {

        String index = jsonObject.getString(Constant.INNER_TYPE);

        JSONObject primaryJsonObject = new JSONObject();
        String extraPool = elasticSearchConfigService.getTypeNameContentWithDefault(index, "extraPool", "false");
        if ("true".equals(extraPool)) {
            JSONObject newJson = JSONObject.parseObject(JSONObject.toJSONString(jsonObject));
            Tools.poolDoc(newJson);
            for (String key : newJson.keySet()) {
                primaryJsonObject.put(key + "___string", newJson.get(key));
            }
        }

        String adjustType = elasticSearchConfigService.getAdjustJsonObjectToEs(index);
        if ("rich".equals(adjustType)) {
            jsonObject = (JSONObject) Tools.richDoc(jsonObject);
        } else {
            Tools.poolDoc(jsonObject);
        }
        jsonObject = (JSONObject) analysisDoc(jsonObject);
        jsonObject = (JSONObject) trimDoc(jsonObject);

        jsonObject.putAll(primaryJsonObject);
        return jsonObject;

    }

    private Object trimDoc(Object object) {
        object = JSONObject.toJSON(object);
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            for (String key : jsonObject.keySet()) {
                jsonObject.put(key, trimDoc(jsonObject.get(key)));
            }
            return jsonObject;
        } else if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) object;
            for (int index = 0; index < jsonArray.size(); index++) {
                jsonArray.set(index, trimDoc(jsonArray.get(index)));
            }
            return jsonArray;
        } else if (object instanceof String) {
            String string = (String) object;
            return string.trim().replace("\n", "\\n");
        } else if (object instanceof BigInteger) {
            return ((BigInteger) object).longValue();
        } else if (object instanceof Number) {
            return object;
        } else if (object instanceof Boolean) {
            return object;
        } else {
            return JSONObject.toJSONString(object).trim().replace("\n", "\\n");
        }
    }

    private Object analysisDoc(Object object) {
        object = JSONObject.toJSON(object);
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            for (String key : jsonObject.keySet()) {
                jsonObject.put(key, analysisDoc(jsonObject.get(key)));
            }
            return jsonObject;
        } else if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) object;
            for (int index = 0; index < jsonArray.size(); index++) {
                jsonArray.set(index, analysisDoc(jsonArray.get(index)));
            }
            return jsonArray;
        } else if (object instanceof String) {
            return object;
        } else if (object instanceof BigInteger) {
            return ((BigInteger) object).longValue();
        } else if (object instanceof Number) {
            return object;
        } else if (object instanceof Boolean) {
            return object;
        } else {
            return JSONObject.toJSONString(object);
        }
    }

}
