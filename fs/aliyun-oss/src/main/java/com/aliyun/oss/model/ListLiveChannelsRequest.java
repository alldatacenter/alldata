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

import com.aliyun.oss.internal.OSSUtils;

/**
 * This is the request class is used to list Live Channels under a bucket.
 */
public class ListLiveChannelsRequest extends LiveChannelGenericRequest {

    private static final int MAX_RETURNED_KEYS_LIMIT = 100;

    /**
     * Constructor.
     * 
     * @param bucketName
     *            Bucket name.
     * @param prefix
     *            Prefix filter---returned Live Channels must start with this
     *            prefix.
     * @param marker
     *            Marker filter----returned Live Channels must be greater than
     *            this marker in lexicographical order.
     * @param maxKeys
     *            Max Live Channels to return, By default it's 100.
     */
    public ListLiveChannelsRequest(String bucketName, String prefix, String marker, int maxKeys) {
        super(bucketName, null);
        setPrefix(prefix);
        setMarker(marker);
        setMaxKeys(maxKeys);
    }

    public ListLiveChannelsRequest(String bucketName) {
        super(bucketName, null);
    }

    public ListLiveChannelsRequest(String bucketName, String prefix, String marker) {
        super(bucketName, null);
        setPrefix(prefix);
        setMarker(marker);
    }

    /**
     * Gets the prefix filter---the returned Live Channels must start with this
     * prefix.
     * 
     * @return The prefix filter.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the prefix filter---the returned Live Channels must start with this
     * prefix.
     * 
     * @param prefix
     *            The prefix filter.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the marker filter---returned Live Channels must be greater than this
     * marker in lexicographical order.
     * 
     * @return The marker filter.
     */
    public String getMarker() {
        return marker;
    }

    /**
     * Sets the marker filter---returned Live Channels must be greater than this
     * marker in lexicographical order.
     * 
     * @param marker
     *            The marker filter.
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }

    /**
     * Gets max number of live channel. By default it's 100.
     * 
     * @return The max number of live channel.
     */
    public Integer getMaxKeys() {
        return maxKeys;
    }

    /**
     * Sets max number of live channel. By default it's 100.
     * 
     * @param maxKeys
     *            The max number of live channel.
     */
    public void setMaxKeys(int maxKeys) {
        if (maxKeys < 0 || maxKeys > MAX_RETURNED_KEYS_LIMIT) {
            throw new IllegalArgumentException(OSSUtils.OSS_RESOURCE_MANAGER.getString("MaxKeysOutOfRange"));
        }

        this.maxKeys = maxKeys;
    }

    /**
     * Sets the prefix and returns the current ListLiveChannelsRequest instance
     * (this).
     * 
     * @param prefix
     *            The prefix filter.
     *
     * @return  The {@link ListLiveChannelsRequest} instance.
     */
    public ListLiveChannelsRequest withPrefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    /**
     * Sets the max number of Live Channels and returns the current
     * ListLiveChannelsRequest instance(this).
     * 
     * @param maxKeys
     *            The max number of Live Channels, by default is 100 and max
     *            value is also 100.
     *
     * @return  The {@link ListLiveChannelsRequest} instance.
     */
    public ListLiveChannelsRequest withMaxKeys(Integer maxKeys) {
        setMaxKeys(maxKeys);
        return this;
    }

    /**
     * Sets the marker filter.
     * 
     * @param marker
     *            The marker filter.
     *
     * @return  The {@link ListLiveChannelsRequest} instance.
     */
    public ListLiveChannelsRequest withMarker(String marker) {
        setMarker(marker);
        return this;
    }

    // The prefix filter.
    private String prefix;

    // The marker filter.
    private String marker;

    // The max number of Live Channels to return.
    private Integer maxKeys;

}
