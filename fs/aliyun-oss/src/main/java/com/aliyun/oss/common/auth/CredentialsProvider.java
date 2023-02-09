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
 * Abstract credentials provider that maintains only one user credentials. Users
 * can switch to other valid credentials with
 * {@link com.aliyun.oss.OSS#switchCredentials(com.aliyun.oss.common.auth.Credentials)} Note
 * that <b>implementations of this interface must be thread-safe.</b>
 */
public interface CredentialsProvider {

    public void setCredentials(Credentials creds);

    public Credentials getCredentials();
}
