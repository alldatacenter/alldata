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

import java.io.IOException;
import java.net.URL;

import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.HttpRequest;
import com.aliyuncs.http.HttpResponse;

public interface CredentialsFetcher {

    /**
     * Constructs the url of authorization server.
     * 
     * @return the url of authorization server
     * @throws ClientException
     *           If any errors occurred in OSS while processing the request.
     */
    public URL buildUrl() throws ClientException;

    /**
     * Sends http request to authorization server.
     * 
     * @param request
     *            http request.
     * @return http response
     * @throws IOException
     *            An IO errors occurred while sending the request to authorization server.
     */
    public HttpResponse send(HttpRequest request) throws IOException;

    /**
     * Parses the response to get credentials.
     * 
     * @param response
     *            http response.
     * @return credentials
     * @throws ClientException
     *           If any errors occurred in OSS while processing the request.
     */
    public Credentials parse(HttpResponse response) throws ClientException;

    /**
     * Fetches credentials from the authorization server.
     * 
     * @return credentials
     * @throws ClientException
     *           If any errors occurred in OSS while processing the request.
     */
    public Credentials fetch() throws ClientException;

    /**
     * Fetches credentials from the authorization server.
     * 
     * @param retryTimes
     *            retry times.
     * @return credentials
     * @throws ClientException
     *           If any errors occurred in OSS while processing the request.
     */
    public Credentials fetch(int retryTimes) throws ClientException;
}
