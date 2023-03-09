/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.metric;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.griffin.core.metric.model.MetricValue;
import org.apache.griffin.core.util.JsonUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class MetricStoreImpl implements MetricStore {

    private static final String INDEX = "griffin";
    private static final String TYPE = "accuracy";

    private RestClient client;
    private HttpHeaders responseHeaders;
    private String urlGet;
    private String urlDelete;
    private String urlPost;
    private ObjectMapper mapper;
    private String indexMetaData;

    public MetricStoreImpl(@Value("${elasticsearch.host}") String host,
                           @Value("${elasticsearch.port}") int port,
                           @Value("${elasticsearch.scheme:http}") String scheme,
                           @Value("${elasticsearch.user:}") String user,
                           @Value("${elasticsearch.password:}") String password) {
        HttpHost httpHost = new HttpHost(host, port, scheme);
        RestClientBuilder builder = RestClient.builder(httpHost);
        if (!user.isEmpty() && !password.isEmpty()) {
            String encodedAuth = buildBasicAuthString(user, password);
            Header[] requestHeaders = new Header[]{
                new BasicHeader(org.apache.http.HttpHeaders.AUTHORIZATION,
                    encodedAuth)};
            builder.setDefaultHeaders(requestHeaders);
        }
        this.client = builder.build();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        this.responseHeaders = responseHeaders;
        String urlBase = String.format("/%s/%s", INDEX, TYPE);
        this.urlGet = urlBase.concat("/_search?filter_path=hits.hits._source");
        this.urlPost = urlBase.concat("/_bulk");
        this.urlDelete = urlBase.concat("/_delete_by_query");
        this.indexMetaData = String.format(
            "{ \"index\" : { \"_index\" : " +
                "\"%s\",\"_type\" : \"%s\" } }%n",
            INDEX,
            TYPE);
        this.mapper = new ObjectMapper();
    }

    @Override
    public List<MetricValue> getMetricValues(String metricName, int from,
                                             int size, long tmst)
        throws IOException {
        HttpEntity entity = getHttpEntityForSearch(metricName, from, size,
            tmst);
        try {
            Response response = client.performRequest("GET", urlGet,
                Collections.emptyMap(), entity);
            return getMetricValuesFromResponse(response);
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    private HttpEntity getHttpEntityForSearch(String metricName, int from, int
        size, long tmst)
        throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> queryParam = new HashMap<>();
        Map<String, Object> termQuery = Collections.singletonMap("name.keyword",
            metricName);
        queryParam.put("filter", Collections.singletonMap("term", termQuery));
        Map<String, Object> sortParam = Collections
            .singletonMap("tmst", Collections.singletonMap("order",
                "desc"));
        map.put("query", Collections.singletonMap("bool", queryParam));
        map.put("sort", sortParam);
        map.put("from", from);
        map.put("size", size);
        return new NStringEntity(JsonUtil.toJson(map),
            ContentType.APPLICATION_JSON);
    }

    private List<MetricValue> getMetricValuesFromResponse(Response response)
        throws IOException {
        List<MetricValue> metricValues = new ArrayList<>();
        JsonNode jsonNode = mapper.readTree(EntityUtils.toString(response
            .getEntity()));
        if (jsonNode.hasNonNull("hits") && jsonNode.get("hits")
            .hasNonNull("hits")) {
            for (JsonNode node : jsonNode.get("hits").get("hits")) {
                JsonNode sourceNode = node.get("_source");
                Map<String, Object> value = JsonUtil.toEntity(
                    sourceNode.get("value").toString(),
                    new TypeReference<Map<String, Object>>() {
                    });
                Map<String, Object> meta = JsonUtil.toEntity(
                    Objects.toString(sourceNode.get("metadata"), null),
                    new TypeReference<Map<String, Object>>() {
                    });
                MetricValue metricValue = new MetricValue(
                    sourceNode.get("name").asText(),
                    Long.parseLong(sourceNode.get("tmst").asText()),
                    meta,
                    value);
                metricValues.add(metricValue);
            }
        }
        return metricValues;
    }

    @Override
    public ResponseEntity<?> addMetricValues(List<MetricValue> metricValues)
        throws IOException {
        String bulkRequestBody = getBulkRequestBody(metricValues);
        HttpEntity entity = new NStringEntity(bulkRequestBody,
            ContentType.APPLICATION_JSON);
        Response response = client.performRequest("POST", urlPost,
            Collections.emptyMap(), entity);
        return getResponseEntityFromResponse(response);
    }

    private String getBulkRequestBody(List<MetricValue> metricValues) throws
        JsonProcessingException {
        StringBuilder bulkRequestBody = new StringBuilder();
        for (MetricValue metricValue : metricValues) {
            bulkRequestBody.append(indexMetaData);
            bulkRequestBody.append(JsonUtil.toJson(metricValue));
            bulkRequestBody.append(System.lineSeparator());
        }
        return bulkRequestBody.toString();
    }

    @Override
    public ResponseEntity<?> deleteMetricValues(String metricName) throws
        IOException {
        Map<String, Object> param = Collections.singletonMap("query",
            Collections.singletonMap("term",
                Collections.singletonMap("name.keyword", metricName)));
        HttpEntity entity = new NStringEntity(
            JsonUtil.toJson(param),
            ContentType.APPLICATION_JSON);
        Response response = client.performRequest("POST", urlDelete,
            Collections.emptyMap(), entity);
        return getResponseEntityFromResponse(response);
    }

    private ResponseEntity<?> getResponseEntityFromResponse(Response response)
        throws IOException {
        String body = EntityUtils.toString(response.getEntity());
        HttpStatus status = HttpStatus.valueOf(response.getStatusLine()
            .getStatusCode());
        return new ResponseEntity<>(body, responseHeaders, status);
    }

    private static String buildBasicAuthString(String user, String password) {
        String auth = user + ":" + password;
        return String.format("Basic %s", Base64.getEncoder().encodeToString(
            auth.getBytes()));
    }

    @Override
    public MetricValue getMetric(String applicationId) throws IOException {
        Response response = client.performRequest(
            "GET", urlGet,
            Collections.singletonMap(
                "q", "metadata.applicationId:" + applicationId));
        List<MetricValue> metricValues = getMetricValuesFromResponse(response);
        return metricValues.get(0);
    }
}
