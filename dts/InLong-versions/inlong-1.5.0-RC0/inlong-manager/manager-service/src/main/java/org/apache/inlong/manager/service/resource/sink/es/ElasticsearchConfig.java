/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.resource.sink.es;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch config information, including host, port, etc.
 */
@Data
@Component
public class ElasticsearchConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfig.class);
    private static RestHighLevelClient highLevelClient;
    @Value("${es.index.search.hostname}")
    private String hosts;
    @Value("${es.auth.enable}")
    private Boolean authEnable = false;
    @Value("${es.auth.user}")
    private String username;
    @Value("${es.auth.password}")
    private String password;

    /**
     * highLevelClient
     *
     * @return RestHighLevelClient
     */
    public RestHighLevelClient highLevelClient() {
        if (highLevelClient != null) {
            return highLevelClient;
        }
        try {
            synchronized (RestHighLevelClient.class) {
                if (highLevelClient == null) {
                    List<HttpHost> hosts = new ArrayList<>();
                    String[] hostArrays = this.hosts.split(InlongConstants.SEMICOLON);
                    for (String host : hostArrays) {
                        if (StringUtils.isNotBlank(host)) {
                            host = host.trim();
                            hosts.add(HttpHost.create(host));
                        }
                    }
                    RestClientBuilder clientBuilder = RestClient.builder(hosts.toArray(new HttpHost[0]));
                    this.setEsAuth(clientBuilder);
                    highLevelClient = new RestHighLevelClient(clientBuilder);
                }
            }
        } catch (Exception e) {
            logger.error("get es high level client error", e);
        }
        return highLevelClient;
    }

    /**
     * Elasticsearch authentication
     *
     * @param builder The builder
     */
    private void setEsAuth(RestClientBuilder builder) {
        try {
            logger.info("set es auth of enable={}", authEnable);
            if (authEnable) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                builder.setHttpClientConfigCallback(
                        httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));

            }
        } catch (Exception e) {
            logger.error("set es auth error ", e);
        }
    }

}
