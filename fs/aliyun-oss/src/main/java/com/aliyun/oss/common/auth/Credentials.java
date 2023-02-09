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

package com.aliyun.oss.common.auth;

/**
 * Provides access to credentials used for accessing OSS, these credentials are
 * used to securely sign requests to OSS.
 */
public interface Credentials {
    /**
     * Returns the access key ID for this credentials.
     *
     * @return A access key id.
     */
    public String getAccessKeyId();

    /**
     * Returns the secret access key for this credentials.
     *
     * @return A access key secret.
     */
    public String getSecretAccessKey();

    /**
     * Returns the security token for this credentials.
     *
     * @return A security token.
     */
    public String getSecurityToken();

    /**
     * Determines whether to use security token for http requests.
     *
     * @return True if has security token; False if not.
     */
    public boolean useSecurityToken();
}