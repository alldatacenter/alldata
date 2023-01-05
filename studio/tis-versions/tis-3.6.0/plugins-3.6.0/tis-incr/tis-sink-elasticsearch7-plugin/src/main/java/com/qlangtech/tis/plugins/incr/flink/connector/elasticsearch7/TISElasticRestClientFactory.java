/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.incr.flink.connector.elasticsearch7;

import com.qlangtech.org.apache.http.auth.AuthScope;
import com.qlangtech.org.apache.http.auth.UsernamePasswordCredentials;
import com.qlangtech.org.apache.http.client.CredentialsProvider;
import com.qlangtech.org.apache.http.impl.client.BasicCredentialsProvider;
import com.qlangtech.org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.flink.streaming.connectors.elasticsearch7.RestClientFactory;
import org.elasticsearch.client.RestClientBuilder;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-12 14:11
 **/
public class TISElasticRestClientFactory implements RestClientFactory {

    private final String userName;
    private final String password;

    public TISElasticRestClientFactory(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public void configureRestClientBuilder(RestClientBuilder restClientBuilder) {

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(this.userName, this.password));
        restClientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        });
    }

}
