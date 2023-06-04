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

package com.bytedance.bitsail.connector.elasticsearch.rest;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.connector.elasticsearch.base.NetUtil;
import com.bytedance.bitsail.connector.elasticsearch.option.ElasticsearchWriterOptions;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EsRestClientBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(EsRestClientBuilder.class);

  private final RestClientBuilder builder;

  public EsRestClientBuilder(BitSailConfiguration jobConf) {
    List<String> hostAddressList = jobConf.get(ElasticsearchWriterOptions.ES_HOSTS);
    List<HttpHost> hosts = parseHostsAddress(hostAddressList);
    Preconditions.checkState(!hosts.isEmpty(), "cannot find any valid host from configurations.");
    LOG.info("Elasticsearch http client hosts: {}", hosts);

    builder = RestClient.builder(hosts.toArray(new HttpHost[0]));
    configureBuilder(jobConf);
  }

  /**
   * Transform hosts strings into a list of {@link HttpHost} hosts.
   *
   * @param hosts A list of host strings.
   * @return A list of hosts.
   */
  private static List<HttpHost> parseHostsAddress(List<String> hosts) {
    if (Objects.isNull(hosts) || hosts.isEmpty()) {
      return Collections.emptyList();
    }
    return hosts.stream().map(host -> {
      if (NetUtil.isIpv6Address(host)) {
        return new HttpHost(Preconditions.checkNotNull(NetUtil.getIpv6Ip(host)), NetUtil.getIpv6Port(host));
      } else if (NetUtil.isIpv4Address(host)) {
        return new HttpHost(Preconditions.checkNotNull(NetUtil.getIpv4Ip(host)), NetUtil.getIpv4Port(host));
      }
      throw new BitSailException(CommonErrorCode.CONFIG_ERROR, "invalid elasticsearch host: " + host);
    }).collect(Collectors.toList());
  }

  public RestHighLevelClient build() {
    return new RestHighLevelClient(builder);
  }

  private void configureBuilder(BitSailConfiguration jobConf) {
    String userName = jobConf.get(ElasticsearchWriterOptions.USER_NAME);
    String password = jobConf.get(ElasticsearchWriterOptions.PASSWORD);
    String requestPathPrefix = jobConf.get(ElasticsearchWriterOptions.REQUEST_PATH_PREFIX);
    int connectionRequestTimeout = jobConf.get(ElasticsearchWriterOptions.CONNECTION_REQUEST_TIMEOUT_MS);
    int connectionTimeout = jobConf.get(ElasticsearchWriterOptions.CONNECTION_TIMEOUT_MS);
    int socketTimeout = jobConf.get(ElasticsearchWriterOptions.SOCKET_TIMEOUT_MS);

    if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
      final CredentialsProvider provider = new BasicCredentialsProvider();
      provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
      builder.setHttpClientConfigCallback(httpAsyncClientBuilder ->
          httpAsyncClientBuilder.setDefaultCredentialsProvider(provider));
    }

    if (StringUtils.isNotEmpty(requestPathPrefix)) {
      builder.setPathPrefix(requestPathPrefix);
    }

    builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
        .setConnectionRequestTimeout(connectionRequestTimeout)
        .setConnectTimeout(connectionTimeout)
        .setSocketTimeout(socketTimeout));
  }

}
