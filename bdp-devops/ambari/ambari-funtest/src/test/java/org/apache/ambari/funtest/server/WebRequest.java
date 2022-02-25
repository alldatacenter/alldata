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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.funtest.server;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * Makes a request to a URL.
 */
public class WebRequest {
    private String httpMethod;
    private String url;
    private String queryString;
    private String content;
    private String contentType;
    private String contentEncoding;
    private String userName;
    private String password;

    private Map<String, String> headers = new HashMap<>();

    public WebRequest() {}

    public WebResponse getResponse() throws Exception {
        return null;
    }

    /**
     * Gets the HTTP Method (POST, GET, PUT, DELETE)
     *
     * @return - HTTP Method
     */
    public String getHttpMethod() { return this.httpMethod; }

    /**
     * Sets the HTTP Method to use.
     *
     * @param httpMethod
     */
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    /**
     * Gets the full URL to the request.
     *
     * @return
     */
    public String getUrl() { return this.url; }

    /**
     * Sets the full URL to the request.
     *
     * @param url
     */
    public void setUrl(String url) { this.url = url; }

    /**
     * Gets the query string (name1=value1?name2=value2)
     *
     * @return
     */
    public String getQueryString() { return this.queryString; }

    /**
     * Sets the query string
     *
     * @param queryString
     */
    public void setQueryString(String queryString) { this.queryString = queryString; }

    /**
     * Gets the request data.
     *
     * @return
     */
    public String getContent() { return this.content; }

    /**
     * Sets the request data.
     *
     * @param content
     */
    public void setContent(String content) { this.content = content; }

    /**
     * Gets the content type (application/json, application/text)
     *
     * @return
     */
    public String getContentType() { return this.contentType; }

    /**
     * Sets the content type.
     *
     * @param contentType
     */
    public void setContentType(String contentType) { this.contentType = contentType; }

    /**
     * Gets the content encoding (UTF-8)
     *
     * @return
     */
    public String getContentEncoding() { return this.contentEncoding; }

    /**
     * Sets the content encoding
     *
     * @param contentEncoding
     */
    public void setContentEncoding(String contentEncoding) { this.contentEncoding = contentEncoding; }

    /**
     * Gets the request headers.
     *
     * @return - A read-only collection of headers.
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(this.headers);
    }

    /**
     * Clear the request headers.
     */
    public void clearHeaders() { this.headers.clear(); }

    /**
     * Add a collection of request headers.
     *
     * @param headers
     */
    public void addHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            addHeader(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Add a name-value pair to the request headers.
     *
     * @param name
     * @param value
     */
    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    /**
     * Sets the user id for the REST API URL
     *
     * @param userName
     */
    public void setUserName(String userName) { this.userName = userName; }

    /**
     * Gets the user name
     *
     * @return - User name
     */
    public String getUserName() { return this.userName; }

    /**
     * Sets the password
     *
     * @param password - Password
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Gets the password
     *
     * @return - Password
     */
    public String getPassword() { return this.password; }
}
