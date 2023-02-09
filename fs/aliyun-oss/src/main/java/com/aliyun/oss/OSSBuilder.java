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

/**
 * Fluent builder for OSS Client. Use of the builder is preferred over using
 * constructors of the client class.
 */
public interface OSSBuilder {

    /**
     * Uses the specified OSS Endpoint and Access Id/Access Key to create a new
     * {@link OSSClient} instance.
     * 
     * @param endpoint
     *            OSS endpoint.
     * @param accessKeyId
     *            Access Key ID.
     * @param secretAccessKey
     *            Secret Access Key.
     * @return An instance that implements the {@link OSS}.
     */
    public OSS build(String endpoint, String accessKeyId, String secretAccessKey);

    /**
     * Uses the specified OSS Endpoint, a security token from AliCloud STS and
     * Access Id/Access Key to create a new {@link OSSClient} instance.
     * 
     * @param endpoint
     *            OSS Endpoint.
     * @param accessKeyId
     *            Access Id from STS.
     * @param secretAccessKey
     *            Access Key from STS
     * @param securityToken
     *            Security Token from STS.
     * @return An instance that implements the {@link OSS}.
     */
    public OSS build(String endpoint, String accessKeyId, String secretAccessKey, String securityToken);

    /**
     * Uses a specified OSS Endpoint, Access Id, Access Key, Client side
     * configuration to create a {@link OSSClient} instance.
     * 
     * @param endpoint
     *            OSS Endpoint.
     * @param accessKeyId
     *            Access Key ID.
     * @param secretAccessKey
     *            Secret Access Key.
     * @param config
     *            A {@link ClientBuilderConfiguration} instance. The method would use
     *            default configuration if it's null.
     * @return An instance that implements the {@link OSS}.
     */
    public OSS build(String endpoint, String accessKeyId, String secretAccessKey, ClientBuilderConfiguration config);

    /**
     * Uses specified OSS Endpoint, the temporary (Access Id/Access Key/Security
     * Token) from STS and the client configuration to create a new
     * {@link OSSClient} instance.
     * 
     * @param endpoint
     *            OSS Endpoint.
     * @param accessKeyId
     *            Access Key Id provided by STS.
     * @param secretAccessKey
     *            Secret Access Key provided by STS.
     * @param securityToken
     *            Security token provided by STS.
     * @param config
     *            A {@link ClientBuilderConfiguration} instance. The method would use
     *            default configuration if it's null.
     * @return An instance that implements the {@link OSS}.
     */
    public OSS build(String endpoint, String accessKeyId, String secretAccessKey, String securityToken,
            ClientBuilderConfiguration config);

    /**
     * Uses the specified {@link CredentialsProvider} and OSS Endpoint to create
     * a new {@link OSSClient} instance.
     * 
     * @param endpoint
     *            OSS services Endpoint.
     * @param credsProvider
     *            Credentials provider which has access key Id and access Key
     *            secret.
     * @return An instance that implements the {@link OSS}.
     */
    public OSS build(String endpoint, CredentialsProvider credsProvider);

    /**
     * Uses the specified {@link CredentialsProvider}, client configuration and
     * OSS endpoint to create a new {@link OSSClient} instance.
     * 
     * @param endpoint
     *            OSS services Endpoint.
     * @param credsProvider
     *            Credentials provider.
     * @param config
     *            client configuration.
     * @return An instance that implements the {@link OSS}.
     */
    public OSS build(String endpoint, CredentialsProvider credsProvider, ClientBuilderConfiguration config);

}
