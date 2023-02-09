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

package com.aliyun.oss.model;

public class AsyncFetchTaskConfiguration {
    private String url;
    private String objectName;
    private String host;
    private String contentMd5;
    private String callback;
    private Boolean ignoreSameKey;

    /**
     * Gets the source object url.
     * @return the object url.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the source object url.
     *
     * @param url
     *            the object url.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the source object url and returns the updated AsyncFetchTaskConfiguration object.
     *
     * @param url
     *            object url.
     * @return  The {@link AsyncFetchTaskConfiguration} instance.
     */
    public AsyncFetchTaskConfiguration withUrl(String url) {
        setUrl(url);
        return this;
    }

    /**
     * Gets the destination object name
     * @return the object name.
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * Sets the destination object name
     *
     * @param objectName
     *            the object name.
     */
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    /**
     * Sets the destination object name and returns the updated AsyncFetchTaskConfiguration object.
     *
     * @param objectName
     *            object name.
     * @return  The {@link AsyncFetchTaskConfiguration} instance.
     */
    public AsyncFetchTaskConfiguration withObjectName(String objectName) {
        setObjectName(objectName);
        return this;
    }

    /**
     * Gets the host that you specified.
     * @return the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the host that you want to fetch, and it also can be null or empty.
     *
     * @param host
     *            the host.
     */
    public void setHost(String host) {
        this.host = host;
    }
    /**
     * Sets the host that you want to fetch, it also can be null or empty,
     * and returns the updated AsyncFetchTaskConfiguration object.
     *
     * @param host
     *            host.
     * @return  The {@link AsyncFetchTaskConfiguration} instance.
     */
    public AsyncFetchTaskConfiguration withHost(String host) {
        setHost(host);
        return this;
    }

    /**
     * Gets the contentMd5 that you specified.
     * @return the content md5.
     */
    public String getContentMd5() {
        return contentMd5;
    }

    /**
     * Sets the contentMd5 of the source object, it also can be null or empty.
     *
     * @param contentMd5
     *           the content md5.
     */
    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    /**
     * Sets the contentMd5 of the source file, it also can be null or empty,
     * and returns the updated AsyncFetchTaskConfiguration object.
     *
     * @param contentMd5
     *           the content md5.
     * @return  The {@link AsyncFetchTaskConfiguration} instance.
     */
    public AsyncFetchTaskConfiguration withContentMd5(String contentMd5) {
        setContentMd5(contentMd5);
        return this;
    }

    /**
     * Gets the callback that you specified.
     *
     * @return  return the callback that you specified.
     */
    public String getCallback() {
        return callback;
    }

    /**
     * Sets the callback after fetch object success.
     *
     * @param callback
     *            call back.
     */
    public void setCallback(String callback) {
        this.callback = callback;
    }

    /**
     * Sets the callback after fetch object success, and returns the updated AsyncFetchTaskConfiguration object.
     *
     * @param callback
     *            call back.
     * @return  The {@link AsyncFetchTaskConfiguration} instance.
     */
    public AsyncFetchTaskConfiguration withCallback(String callback) {
        setCallback(callback);
        return this;
    }

    /**
     * Gets the ignoreSameKey option that you specified.
     *
     * @return True if ignore same key; False if not.
     */
    public Boolean getIgnoreSameKey() {
        return ignoreSameKey;
    }

    /**
     * Sets the optional operation of ignore the task or not when the destination object already exists,
     * true means ignore the task and false means it allows object coving.
     *
     * @param ignoreSameKey
     *            ignore same key.
     */
    public void setIgnoreSameKey(Boolean ignoreSameKey) {
        this.ignoreSameKey = ignoreSameKey;
    }

    /**
     * Sets the optional operation of ignore the task or not when the destination object already exists,
     * true means ignore the task and false means it allows object coving,
     * and returns the updated AsyncFetchTaskConfiguration object.
     *
     * @param ignoreSameKey
     *            ignore same key.
     * @return The {@link AsyncFetchTaskConfiguration} instance.
     */
    public AsyncFetchTaskConfiguration withIgnoreSameKey(Boolean ignoreSameKey) {
        setIgnoreSameKey(ignoreSameKey);
        return this;
    }
}
