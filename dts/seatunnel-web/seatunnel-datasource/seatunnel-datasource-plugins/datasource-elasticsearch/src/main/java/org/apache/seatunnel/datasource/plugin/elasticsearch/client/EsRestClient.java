/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.datasource.plugin.elasticsearch.client;

import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.JsonNode;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.seatunnel.shade.com.typesafe.config.Config;

import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.datasource.plugin.elasticsearch.ElasticSearchOptionRule;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class EsRestClient implements AutoCloseable {

    private static final int CONNECTION_REQUEST_TIMEOUT = 10 * 1000;

    private static final int SOCKET_TIMEOUT = 5 * 60 * 1000;

    private final RestClient restClient;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private EsRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public static EsRestClient createInstance(Config pluginConfig) {
        try {
            List<String> hosts =
                    OBJECT_MAPPER.readValue(
                            pluginConfig.getString(ElasticSearchOptionRule.HOSTS.key()),
                            List.class);
            Optional<String> username = Optional.empty();
            Optional<String> password = Optional.empty();
            if (pluginConfig.hasPath(ElasticSearchOptionRule.USERNAME.key())) {
                username =
                        Optional.of(pluginConfig.getString(ElasticSearchOptionRule.USERNAME.key()));
                if (pluginConfig.hasPath(ElasticSearchOptionRule.PASSWORD.key())) {
                    password =
                            Optional.of(
                                    pluginConfig.getString(ElasticSearchOptionRule.PASSWORD.key()));
                }
            }
            Optional<String> keystorePath = Optional.empty();
            Optional<String> keystorePassword = Optional.empty();
            Optional<String> truststorePath = Optional.empty();
            Optional<String> truststorePassword = Optional.empty();
            boolean tlsVerifyCertificate =
                    ElasticSearchOptionRule.TLS_VERIFY_CERTIFICATE.defaultValue();
            if (pluginConfig.hasPath(ElasticSearchOptionRule.TLS_VERIFY_CERTIFICATE.key())) {
                tlsVerifyCertificate =
                        pluginConfig.getBoolean(
                                ElasticSearchOptionRule.TLS_VERIFY_CERTIFICATE.key());
            }
            if (tlsVerifyCertificate) {
                if (pluginConfig.hasPath(ElasticSearchOptionRule.TLS_KEY_STORE_PATH.key())) {
                    keystorePath =
                            Optional.of(
                                    pluginConfig.getString(
                                            ElasticSearchOptionRule.TLS_KEY_STORE_PATH.key()));
                }
                if (pluginConfig.hasPath(ElasticSearchOptionRule.TLS_KEY_STORE_PASSWORD.key())) {
                    keystorePassword =
                            Optional.of(
                                    pluginConfig.getString(
                                            ElasticSearchOptionRule.TLS_KEY_STORE_PASSWORD.key()));
                }
                if (pluginConfig.hasPath(ElasticSearchOptionRule.TLS_TRUST_STORE_PATH.key())) {
                    truststorePath =
                            Optional.of(
                                    pluginConfig.getString(
                                            ElasticSearchOptionRule.TLS_TRUST_STORE_PATH.key()));
                }
                if (pluginConfig.hasPath(ElasticSearchOptionRule.TLS_TRUST_STORE_PASSWORD.key())) {
                    truststorePassword =
                            Optional.of(
                                    pluginConfig.getString(
                                            ElasticSearchOptionRule.TLS_TRUST_STORE_PASSWORD
                                                    .key()));
                }
            }
            boolean tlsVerifyHostnames = ElasticSearchOptionRule.TLS_VERIFY_HOSTNAME.defaultValue();
            if (pluginConfig.hasPath(ElasticSearchOptionRule.TLS_VERIFY_HOSTNAME.key())) {
                tlsVerifyHostnames =
                        pluginConfig.getBoolean(ElasticSearchOptionRule.TLS_VERIFY_HOSTNAME.key());
            }
            return createInstance(
                    hosts,
                    username,
                    password,
                    tlsVerifyCertificate,
                    tlsVerifyHostnames,
                    keystorePath,
                    keystorePassword,
                    truststorePath,
                    truststorePassword);
        } catch (Exception e) {
            throw new RuntimeException("Create EsRestClient failed", e);
        }
    }

    public static EsRestClient createInstance(
            List<String> hosts,
            Optional<String> username,
            Optional<String> password,
            boolean tlsVerifyCertificate,
            boolean tlsVerifyHostnames,
            Optional<String> keystorePath,
            Optional<String> keystorePassword,
            Optional<String> truststorePath,
            Optional<String> truststorePassword) {
        RestClientBuilder restClientBuilder =
                getRestClientBuilder(
                        hosts,
                        username,
                        password,
                        tlsVerifyCertificate,
                        tlsVerifyHostnames,
                        keystorePath,
                        keystorePassword,
                        truststorePath,
                        truststorePassword);
        return new EsRestClient(restClientBuilder.build());
    }

    private static RestClientBuilder getRestClientBuilder(
            List<String> hosts,
            Optional<String> username,
            Optional<String> password,
            boolean tlsVerifyCertificate,
            boolean tlsVerifyHostnames,
            Optional<String> keystorePath,
            Optional<String> keystorePassword,
            Optional<String> truststorePath,
            Optional<String> truststorePassword) {
        HttpHost[] httpHosts = new HttpHost[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            httpHosts[i] = HttpHost.create(hosts.get(i));
        }

        RestClientBuilder restClientBuilder =
                RestClient.builder(httpHosts)
                        .setRequestConfigCallback(
                                requestConfigBuilder ->
                                        requestConfigBuilder
                                                .setConnectionRequestTimeout(
                                                        CONNECTION_REQUEST_TIMEOUT)
                                                .setSocketTimeout(SOCKET_TIMEOUT));

        restClientBuilder.setHttpClientConfigCallback(
                httpClientBuilder -> {
                    if (username.isPresent()) {
                        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(
                                AuthScope.ANY,
                                new UsernamePasswordCredentials(username.get(), password.get()));
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }

                    try {
                        if (tlsVerifyCertificate) {
                            Optional<SSLContext> sslContext =
                                    SSLUtils.buildSSLContext(
                                            keystorePath,
                                            keystorePassword,
                                            truststorePath,
                                            truststorePassword);
                            sslContext.ifPresent(e -> httpClientBuilder.setSSLContext(e));
                        } else {
                            SSLContext sslContext =
                                    SSLContexts.custom()
                                            .loadTrustMaterial(new TrustAllStrategy())
                                            .build();
                            httpClientBuilder.setSSLContext(sslContext);
                        }
                        if (!tlsVerifyHostnames) {
                            httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return httpClientBuilder;
                });
        return restClientBuilder;
    }

    public ElasticsearchClusterInfo getClusterInfo() {
        Request request = new Request("GET", "/");
        try {
            Response response = restClient.performRequest(request);
            String result = EntityUtils.toString(response.getEntity());
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(result);
            JsonNode versionNode = jsonNode.get("version");
            return ElasticsearchClusterInfo.builder()
                    .clusterVersion(versionNode.get("number").asText())
                    .distribution(
                            Optional.ofNullable(versionNode.get("distribution"))
                                    .map(JsonNode::asText)
                                    .orElse(null))
                    .build();
        } catch (IOException e) {
            throw new ResponseException("fail to get elasticsearch version.", e);
        }
    }

    public void close() {
        try {
            restClient.close();
        } catch (IOException e) {
            log.warn("close elasticsearch connection error", e);
        }
    }

    public List<String> listIndex() {
        String endpoint = "/_cat/indices?format=json";
        Request request = new Request("GET", endpoint);
        try {
            Response response = restClient.performRequest(request);
            if (response == null) {
                throw new ResponseException("GET " + endpoint + " response null");
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String entity = EntityUtils.toString(response.getEntity());
                return JsonUtils.toList(entity, Map.class).stream()
                        .map(map -> map.get("index").toString())
                        .collect(Collectors.toList());
            } else {
                throw new ResponseException(
                        String.format(
                                "GET %s response status code=%d",
                                endpoint, response.getStatusLine().getStatusCode()));
            }
        } catch (IOException ex) {
            throw new ResponseException(ex);
        }
    }

    public void dropIndex(String tableName) {
        String endpoint = String.format("/%s", tableName);
        Request request = new Request("DELETE", endpoint);
        try {
            Response response = restClient.performRequest(request);
            if (response == null) {
                throw new ResponseException("DELETE " + endpoint + " response null");
            }
            // todo: if the index doesn't exist, the response status code is 200?
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new ResponseException(
                        String.format(
                                "DELETE %s response status code=%d",
                                endpoint, response.getStatusLine().getStatusCode()));
            }
        } catch (IOException ex) {
            throw new ResponseException(ex);
        }
    }

    /**
     * get es field name and type mapping relation
     *
     * @param index index name
     * @return {key-> field name,value->es type}
     */
    public Map<String, String> getFieldTypeMapping(String index) {
        String endpoint = String.format("/%s/_mappings", index);
        Request request = new Request("GET", endpoint);
        Map<String, String> mapping = new HashMap<>();
        try {
            Response response = restClient.performRequest(request);
            if (response == null) {
                throw new ResponseException("GET " + endpoint + " response null");
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new ResponseException(
                        String.format(
                                "GET %s response status code=%d",
                                endpoint, response.getStatusLine().getStatusCode()));
            }
            String entity = EntityUtils.toString(response.getEntity());
            log.info(String.format("GET %s response=%s", endpoint, entity));
            ObjectNode responseJson = JsonUtils.parseObject(entity);
            for (Iterator<JsonNode> it = responseJson.elements(); it.hasNext(); ) {
                JsonNode indexProperty = it.next();
                JsonNode mappingsProperty = indexProperty.get("mappings");
                if (mappingsProperty.has("mappingsProperty")) {
                    JsonNode properties = mappingsProperty.get("properties");
                    mapping = getFieldTypeMappingFromProperties(properties);
                } else {
                    for (JsonNode typeNode : mappingsProperty) {
                        JsonNode properties;
                        if (typeNode.has("properties")) {
                            properties = typeNode.get("properties");
                        } else {
                            properties = typeNode;
                        }
                        mapping.putAll(getFieldTypeMappingFromProperties(properties));
                    }
                }
            }
        } catch (IOException ex) {
            throw new ResponseException(ex);
        }
        return mapping;
    }

    private static Map<String, String> getFieldTypeMappingFromProperties(JsonNode properties) {
        Map<String, String> mapping = new HashMap<>();
        for (Iterator<String> it = properties.fieldNames(); it.hasNext(); ) {
            String field = it.next();
            JsonNode fieldProperty = properties.get(field);
            if (fieldProperty == null) {
                mapping.put(field, "text");
            } else {
                if (fieldProperty.has("type")) {
                    String type = fieldProperty.get("type").asText();
                    mapping.put(field, type);
                } else {
                    log.warn(
                            String.format(
                                    "fail to get elasticsearch field %s mapping type,so give a default type text",
                                    field));
                    mapping.put(field, "text");
                }
            }
        }
        return mapping;
    }
}
