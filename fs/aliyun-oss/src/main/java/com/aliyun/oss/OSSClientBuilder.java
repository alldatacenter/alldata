/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss;

import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.common.utils.StringUtils;
import com.aliyun.oss.internal.OSSConstants;

import static com.aliyun.oss.common.utils.CodingUtils.assertParameterNotNull;

/**
 * Fluent builder for OSS Client. Use of the builder is preferred over using
 * constructors of the client class.
 */
public class OSSClientBuilder implements OSSBuilder {

    @Override
    public OSS build(String endpoint, String accessKeyId, String secretAccessKey) {
        return new OSSClient(endpoint, getDefaultCredentialProvider(accessKeyId, secretAccessKey),
                getClientConfiguration());
    }

    @Override
    public OSS build(String endpoint, String accessKeyId, String secretAccessKey, String securityToken) {
        return new OSSClient(endpoint, getDefaultCredentialProvider(accessKeyId, secretAccessKey, securityToken),
                getClientConfiguration());
    }

    @Override
    public OSS build(String endpoint, String accessKeyId, String secretAccessKey, ClientBuilderConfiguration config) {
        return new OSSClient(endpoint, getDefaultCredentialProvider(accessKeyId, secretAccessKey),
                getClientConfiguration(config));
    }

    @Override
    public OSS build(String endpoint, String accessKeyId, String secretAccessKey, String securityToken,
                     ClientBuilderConfiguration config) {
        return new OSSClient(endpoint, getDefaultCredentialProvider(accessKeyId, secretAccessKey, securityToken),
                getClientConfiguration(config));
    }

    @Override
    public OSS build(String endpoint, CredentialsProvider credsProvider) {
        return new OSSClient(endpoint, credsProvider, getClientConfiguration());
    }

    @Override
    public OSS build(String endpoint, CredentialsProvider credsProvider, ClientBuilderConfiguration config) {
        return new OSSClient(endpoint, credsProvider, getClientConfiguration(config));
    }

    private static ClientBuilderConfiguration getClientConfiguration() {
        return new ClientBuilderConfiguration();
    }

    private static ClientBuilderConfiguration getClientConfiguration(ClientBuilderConfiguration config) {
        if (config == null) {
            config = new ClientBuilderConfiguration();
        }
        return config;
    }

    private static DefaultCredentialProvider getDefaultCredentialProvider(String accessKeyId, String secretAccessKey) {
        return new DefaultCredentialProvider(accessKeyId, secretAccessKey);
    }

    private static DefaultCredentialProvider getDefaultCredentialProvider(String accessKeyId, String secretAccessKey,
                                                                          String securityToken) {
        return new DefaultCredentialProvider(accessKeyId, secretAccessKey, securityToken);
    }

    public static OSSClientBuilderImpl create() {
        return new OSSClientBuilderImpl();
    }

    public static final class OSSClientBuilderImpl {
        private String endpoint;
        private CredentialsProvider credentialsProvider;
        private ClientConfiguration clientConfiguration;
        private String region;
        private String cloudBoxId;

        private OSSClientBuilderImpl() {
            this.clientConfiguration = OSSClientBuilder.getClientConfiguration();
        }

        public OSSClientBuilderImpl endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public OSSClientBuilderImpl credentialsProvider(CredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public OSSClientBuilderImpl clientConfiguration(ClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return this;
        }

        public OSSClientBuilderImpl region(String region) {
            this.region = region;
            return this;
        }

        public OSSClientBuilderImpl cloudBoxId(String cloudBoxId) {
            this.cloudBoxId = cloudBoxId;
            return this;
        }

        public OSS build() {
            assertParameterNotNull(endpoint, "endpoint");
            assertParameterNotNull(credentialsProvider, "credentialsProvider");
            assertParameterNotNull(clientConfiguration, "clientConfiguration");
            if (SignVersion.V4.equals(clientConfiguration.getSignatureVersion())) {
                assertParameterNotNull(region, "region");
            }
            OSSClient client = new OSSClient(endpoint, credentialsProvider, clientConfiguration);
            if (!StringUtils.isNullOrEmpty(region)) {
                client.setRegion(region);
            }
            if (!StringUtils.isNullOrEmpty(cloudBoxId)) {
                client.setProduct(OSSConstants.PRODUCT_CLOUD_BOX);
                client.setCloudBoxId(cloudBoxId);
            }
            return client;
        }
    }
}
