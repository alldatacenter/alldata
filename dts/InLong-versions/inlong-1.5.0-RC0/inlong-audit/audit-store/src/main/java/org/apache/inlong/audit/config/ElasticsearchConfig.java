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

package org.apache.inlong.audit.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@Getter
@Setter
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.connTimeout:3000}")
    private int connTimeout;

    @Value("${elasticsearch.socketTimeout:5000}")
    private int socketTimeout;

    @Value("${elasticsearch.connectionRequestTimeout:500}")
    private int connectionRequestTimeout;

    @Value("${elasticsearch.authEnable:false}")
    private boolean authEnable;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;

    @Value("${elasticsearch.shardsNum:5}")
    private int shardsNum;

    @Value("${elasticsearch.replicaNum:1}")
    private int replicaNum;

    @Value("${elasticsearch.indexDeleteDay:5}")
    private int indexDeleteDay;

    @Value("${elasticsearch.enableCustomDocId:true}")
    private boolean enableCustomDocId;

    @Value("${elasticsearch.bulkInterval:10}")
    private int bulkInterval;

    @Value("${elasticsearch.bulkThreshold:5000}")
    private int bulkThreshold;

    @Value("${elasticsearch.auditIdSet}")
    private String auditIdSet;

    @Bean(destroyMethod = "close", name = "restClient")
    public RestHighLevelClient initRestClient() {

        // support es cluster with multi hosts
        List<HttpHost> hosts = new ArrayList<>();
        String[] hostArrays = host.split(",");
        for (String host : hostArrays) {
            if (StringUtils.isNotEmpty(host)) {
                hosts.add(new HttpHost(host.trim(), port, "http"));
            }
        }

        RestClientBuilder restClientBuilder = RestClient.builder(hosts.toArray(new HttpHost[0]));

        // configurable auth
        if (authEnable) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            restClientBuilder.setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider));
        }

        restClientBuilder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(connTimeout).setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout));

        return new RestHighLevelClient(restClientBuilder);
    }

    public List<String> getAuditIdList() {
        List<String> auditIdList = new ArrayList<>();
        if (!StringUtils.isEmpty(auditIdSet)) {
            auditIdList = Arrays.asList(auditIdSet.split(","));
        }
        return auditIdList;
    }
}
