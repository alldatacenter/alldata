package com.alibaba.tesla.tkgone.server.services.database.elasticsearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.config.ApplicationProperties;
import com.alibaba.tesla.tkgone.server.domain.dto.BackendStoreDTO;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ElasticSearchConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.mapper.IndexMapper;
import io.prometheus.client.Counter;
import lombok.extern.log4j.Log4j;
import okhttp3.*;
import okhttp3.OkHttpClient.Builder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author yangjinghua
 */
@Service
@Log4j
public class ElasticSearchHttpApiBasic {
    private static final int CALL_LOG_MIN_TIME = 500;

    private static final Counter esRequestSuccessCount = Counter.build()
        .name("tkgone_es_request_success").help("Tkgone es request success").register();
    private static final Counter esRequestFailureCount = Counter.build()
        .name("tkgone_es_request_failure").help("Tkgone es request failure").register();

    @Autowired
    public ApplicationProperties applicationProperties;

    @Autowired
    public ElasticSearchConfigService elasticSearchConfigService;

    @Autowired
    public CategoryConfigService categoryConfigService;

    @Autowired
    public ElasticSearchSearchService elasticSearchSearchService;

    @Autowired
    public IndexMapper indexMapper;

    public Map<String, OkHttpClient> httpClientMap = new ConcurrentHashMap<>();

    private OkHttpClient getHttpClient(BackendStoreDTO backendStoreDTO) {
        if (!httpClientMap.containsKey(backendStoreDTO.getHost())) {
            Builder okHttpClientBuilder = new OkHttpClient().newBuilder()
                    .connectionPool(new ConnectionPool(Integer.MAX_VALUE, 30, TimeUnit.MINUTES))
                    .readTimeout(elasticSearchConfigService.getElasticsearchReadTimeout(), TimeUnit.SECONDS)
                    .writeTimeout(elasticSearchConfigService.getElasticsearchWriteTimeout(), TimeUnit.SECONDS)
                    .connectTimeout(elasticSearchConfigService.getElasticsearchConnectTimeout(), TimeUnit.SECONDS);

            if (StringUtils.isNotEmpty(backendStoreDTO.getUser())
                    && StringUtils.isNotEmpty(backendStoreDTO.getPassword())) {
                okHttpClientBuilder.authenticator(new Authenticator() {

                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        String credential = Credentials.basic(backendStoreDTO.getUser(), backendStoreDTO.getPassword());
                        return response.request().newBuilder().header("Authorization", credential).build();
                    }
                });
            }
            httpClientMap.put(backendStoreDTO.getHost(), okHttpClientBuilder.build());
        }
        return httpClientMap.get(backendStoreDTO.getHost());
    }

    public String getOrPostReturnString(BackendStoreDTO backendStoreDTO, String uri, Map<String, String> queryParams,
            String pBody, RequestMethod requestMethod, boolean exceptionOnResponseError) throws Exception {

        long startTimeMillis = System.currentTimeMillis();
        log.debug(String.format("getOrPost uri: %s %s\nqueryParams: %s,\npBody: %s", requestMethod, uri, queryParams,
                StringUtils.isEmpty(pBody) ? "" : pBody.substring(0, Math.min(pBody.length(), 1000))));

        String elasticsearchHost = String.format("%s:%s", backendStoreDTO.getHost(), backendStoreDTO.getPort());

        String headUrl = String.format("%s/%s", elasticsearchHost, uri);
        headUrl = headUrl.replaceAll("//", "/");
        headUrl = backendStoreDTO.getSchema() + "://" + headUrl;

        HttpUrl.Builder queryUrl = Objects.requireNonNull(HttpUrl.parse(headUrl)).newBuilder();
        if (null != queryParams && !queryParams.isEmpty()) {
            for (String name : queryParams.keySet()) {
                queryUrl.addQueryParameter(name, queryParams.get(name));
            }
        }
        HttpUrl url = queryUrl.build();

        Request.Builder requestBuilder = new Request.Builder().url(url).addHeader("Accept",
                "application/json; charset=UTF-8");

        if (StringUtils.isEmpty(pBody)) {
            pBody = "{}";
        }

        Request request;
        if (requestMethod.equals(RequestMethod.GET)) {
            request = requestBuilder.get().build();
        } else if (requestMethod.equals(RequestMethod.POST)) {
            request = requestBuilder.post(RequestBody.create(MediaType.parse("application/json"), pBody)).build();
        } else if (requestMethod.equals(RequestMethod.PUT)) {
            RequestBody.create(MediaType.parse("application/json"), pBody);
            request = requestBuilder.put(RequestBody.create(MediaType.parse("application/json"), pBody)).build();
        } else if (requestMethod.equals(RequestMethod.DELETE)) {
            request = requestBuilder.delete().build();
        } else {
            throw new Exception("不支持GET、POST、PUT、DELETE之外的访问方式");
        }

        long startCallMillis = System.currentTimeMillis();
        Response response;
        try {
            response = getHttpClient(backendStoreDTO).newCall(request).execute();
        } catch (Exception e) {
            esRequestFailureCount.inc();
            String queryString = JSONObject.toJSONString(queryParams, true);
            String postString = JSONObject.toJSONString(pBody, true);
            log.error("elasticSearch接口调用失败: " + String.format("URI: %s; queryParams: %s; pBody: %s", uri,
                    queryString.substring(0, Math.min(queryString.length(), 100)),
                    postString.substring(0, Math.min(postString.length(), 100))));
            throw e;
        }
        long endCallMillis = System.currentTimeMillis();
        log.debug("END - START call: " + (endCallMillis - startCallMillis));

        if (exceptionOnResponseError) {
            if (!response.isSuccessful()) {
                esRequestFailureCount.inc();
                String body = response.body() == null ? "" : response.body().string();
                throw new Exception(
                        String.format("response错误: %s; \nbody: %s; \npostData: %s", response.toString(), body, pBody));
            }
        }
        long endTimeMillis = System.currentTimeMillis();
        log.debug("END - START getOrPost: " + (endTimeMillis - startTimeMillis));
        if (endCallMillis - startCallMillis > CALL_LOG_MIN_TIME) {
            log.info(String.format("[getOrPost]接口：%s,方法：%s,body：%s，耗时：%s", uri, requestMethod,
                    StringUtils.isEmpty(pBody) ? "" : pBody.substring(0, Math.min(pBody.length(), 1000)),
                    endCallMillis - startCallMillis));
        }

        assert response.body() != null;

        esRequestSuccessCount.inc();
        String retString = response.body().string();
        response.close();
        return retString;
    }

    public JSONObject getOrPost(String uri, BackendStoreDTO backendStoreDTO, Map<String, String> queryParams,
            String pBody, RequestMethod requestMethod, boolean exceptionOnResponseError) throws Exception {
        JSONObject retJson = null;
        String retString = getOrPostReturnString(backendStoreDTO, uri, queryParams, pBody, requestMethod,
                exceptionOnResponseError);
        try {
            retJson = JSONObject.parseObject(retString);
        } catch (Exception e) {
            throw new Exception("返回值不是json: " + retString);
        }
        return retJson;
    }

    public JSONObject getOrPost(String uri, String index, Map<String, String> queryParams, String pBody,
            RequestMethod requestMethod, boolean exceptionOnResponseError) throws Exception {
        BackendStoreDTO backendStoreDTO = getSearchBackendStore(index);
        return getOrPost(uri, backendStoreDTO, queryParams, pBody, requestMethod, exceptionOnResponseError);
    }

    public JSONArray getOrPostByIndices(String uri, List<String> indices, Map<String, String> queryParams, String pBody,
            RequestMethod requestMethod, boolean exceptionOnResponseError) throws Exception {
        JSONArray retArray = new JSONArray();
        List<BackendStoreDTO> backendStoreDTOs = getSearchBackendStores(indices);
        for (BackendStoreDTO backendStoreDTO : backendStoreDTOs) {
            retArray.add(getOrPost(uri, backendStoreDTO, queryParams, pBody, requestMethod, exceptionOnResponseError));
        }
        return retArray;
    }

    public JSONArray getOrPostByBackendStores(String uri, List<BackendStoreDTO> backendStoreDTOs,
            Map<String, String> queryParams, String pBody, RequestMethod requestMethod,
            boolean exceptionOnResponseError) throws Exception {
        JSONArray retArray = new JSONArray();
        for (BackendStoreDTO backendStoreDTO : backendStoreDTOs) {
            retArray.add(getOrPost(uri, backendStoreDTO, queryParams, pBody, requestMethod, exceptionOnResponseError));
        }
        return retArray;
    }

    public JSONObject search(String index, Map<String, String> queryParams, String pBody, RequestMethod requestMethod,
            boolean exceptionOnResponseError) throws Exception {
        BackendStoreDTO backendStoreDTO = getSearchBackendStore(index);
        return getOrPost(backendStoreDTO.getUri(), backendStoreDTO, queryParams, pBody, requestMethod,
                exceptionOnResponseError);
    }

    public List<JSONObject> searchByIndices(List<String> indices, Map<String, String> queryParams, String pBody,
            RequestMethod requestMethod, boolean exceptionOnResponseError) throws Exception {
        List<JSONObject> result = new ArrayList<>();
        List<BackendStoreDTO> searchBackendStoreDTOs = getSearchBackendStores(indices);
        for (BackendStoreDTO backendStoreDTO : searchBackendStoreDTOs) {
            result.add(getOrPost(backendStoreDTO.getUri(), backendStoreDTO, queryParams, pBody, requestMethod, true));
        }
        return result;
    }

    public List<JSONObject> searchByBackendStores(List<BackendStoreDTO> backendStoreDTOs,
            Map<String, String> queryParams, String pBody, RequestMethod requestMethod) throws Exception {
        List<JSONObject> result = new ArrayList<>();
        for (BackendStoreDTO backendStoreDTO : backendStoreDTOs) {
            result.add(getOrPost(backendStoreDTO.getUri(), backendStoreDTO, queryParams, pBody, requestMethod, true));
        }
        return result;
    }

    public Map<String, JSONObject> getBackendStoreIndices(List<String> indices, BackendStoreDTO backendStoreDTO) {
        Map<String, JSONObject> backendStoreIndices = new HashMap<>();
        Map<Pattern, JSONObject> backendStoreIndexPatterns = backendStoreDTO.getIndexPatternMap();

        for (String index : indices) {
            for (Pattern pattern : backendStoreIndexPatterns.keySet()) {
                if (pattern.matcher(index).find()) {
                    backendStoreIndices.put(index, backendStoreIndexPatterns.get(pattern));
                }
            }
        }
        return backendStoreIndices;
    }

    public List<BackendStoreDTO> getSearchBackendStores(String category, String type) {
        return getSearchBackendStores(category, Arrays.asList(type.split(",")));
    }

    public List<BackendStoreDTO> getSearchBackendStores(String category, List<String> types) {
        types = new ArrayList<>(types);
        types.removeAll(Arrays.asList(null, ""));
        if (CollectionUtils.isEmpty(types)) {
            types = categoryConfigService.getCategoryIndexes(category).toJavaList(String.class);
        }
        types.retainAll(indexMapper.getAliasIndexes());
        log.info(String.format("query index list: %s", types));
        return getSearchBackendStores(types);
    }

    /**
     * 当前已经Hack死，只支持es了
     * @param types
     * @return
     */
    public List<BackendStoreDTO> getSearchBackendStores(List<String> types) {
        List<String> typeList = new ArrayList<>(types);
        List<BackendStoreDTO> resultObjs = new ArrayList<>();
        JSONArray backendStores = elasticSearchConfigService.getBackendStores();
        BackendStoreDTO defaultBackendStoreDTO = null;
        for (BackendStoreDTO backendStoreDTO : backendStores.toJavaList(BackendStoreDTO.class)) {
            if (backendStoreDTO.isDefaultStore()) {
                defaultBackendStoreDTO = backendStoreDTO;
            }
            log.debug(String.format("backendStoreDTO is %s", backendStoreDTO));
            Map<String, JSONObject> indices = getBackendStoreIndices(typeList, backendStoreDTO);
            if (CollectionUtils.isNotEmpty(indices.keySet())) {
                typeList.removeAll(indices.keySet());
                backendStoreDTO.setUri(String.format("%s/_search", String.join(",", indices.keySet())));
                backendStoreDTO.setIndices(indices);
                resultObjs.add(backendStoreDTO);
            }
        }

        if (!CollectionUtils.isEmpty(typeList) && null != defaultBackendStoreDTO) {
            defaultBackendStoreDTO.setUri(String.format("%s/_search", String.join(",", typeList)));
            Map<String, JSONObject> indices = new HashMap<>();
            typeList.forEach(e -> indices.put(e, null));
            defaultBackendStoreDTO.setIndices(indices);
            resultObjs.add(defaultBackendStoreDTO);
        }

        return resultObjs;
    }

    public BackendStoreDTO getSearchBackendStore(String index) {
        List<BackendStoreDTO> backendStoreDTOs = getSearchBackendStores(Arrays.asList(index));
        if (CollectionUtils.isEmpty(backendStoreDTOs) || backendStoreDTOs.size() != 1) {
            log.error(String.format("%s belongs to more than one store backend: %s", index, backendStoreDTOs));
            return null;
        }
        return backendStoreDTOs.get(0);
    }

    public List<BackendStoreDTO> getAllBackendStores() {
        JSONArray backendStores = elasticSearchConfigService.getBackendStores();
        return backendStores.toJavaList(BackendStoreDTO.class);
    }

    public BackendStoreDTO getBackendStoreByHost(String host) {
        List<BackendStoreDTO> backendStoreDTOs = getAllBackendStores();
        for (BackendStoreDTO backendStoreDTO : backendStoreDTOs) {
            if (host.equals(backendStoreDTO.getHost())) {
                return backendStoreDTO;
            }
        }
        return null;
    }
}
