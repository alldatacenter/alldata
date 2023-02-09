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

public class ListBucketsRequest extends WebServiceRequest {

    public static final int MAX_RETURNED_KEYS = 1000;

    // The prefix filter for the buckets to return. In the other words, all
    // buckets returned must start with this prefix.
    private String prefix;

    // The maker filter for the buckets to return. In the other words, all
    // buckets returned must be greater than the marker
    // in the lexicographical order.
    private String marker;

    // The max number of buckets to return.By default it's 100.
    private Integer maxKeys;

    // The OSS's Bid is 26842.
    private String bid;

    // The tag key.
    private String tagKey;

    // The tag value.
    private String tagValue;

    // The resouce group id
    private String resouceGroupId;

    /**
     * Constructor.
     */
    public ListBucketsRequest() {
    }

    /**
     * Constructor.
     * 
     * @param prefix
     *            Prefix filter, all buckets returned must start with this
     *            prefix.
     * @param marker
     *            Maker filter, all buckets returned must be greater than the
     *            marker.
     * @param maxKeys
     *            Max number of buckets to return, by default is 100,
     */
    public ListBucketsRequest(String prefix, String marker, Integer maxKeys) {
        setPrefix(prefix);
        setMarker(marker);
        if (maxKeys != null) {
            setMaxKeys(maxKeys);
        }
    }

    /**
     * Gets the prefix filter.
     * 
     * @return The prefix filter.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the prefix filter.
     * 
     * @param prefix
     *            The prefix filter.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Sets the prefix filter and return the current ListBucketsRequest instance
     * (this).
     * 
     * @param prefix
     *            The prefix filter.
     *
     * @return  The {@link ListBucketsRequest} instance.
     */
    public ListBucketsRequest withPrefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    /**
     * Gets the marker filter.
     * 
     * @return The marker filter.
     */
    public String getMarker() {
        return marker;
    }

    /**
     * Sets the marker filter.
     * 
     * @param marker
     *            The marker filter.
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }

    /**
     * Sets the marker filter and return the current ListBucketRequest instance
     * (this).
     * 
     * @param marker
     *            The marker filter.
     *
     * @return  The {@link ListBucketsRequest} instance.
     */
    public ListBucketsRequest withMarker(String marker) {
        setMarker(marker);
        return this;
    }

    /**
     * Gets the max number of buckets to return. By default it's 100.
     * 
     * @return The max number of buckets.
     */
    public Integer getMaxKeys() {
        return maxKeys;
    }

    /**
     * Sets the max number of buckets to return. By default it's 100, the max is
     * 1000.
     * 
     * @param maxKeys
     *            The max number of buckets.
     */
    public void setMaxKeys(Integer maxKeys) {
        int tmp = maxKeys.intValue();
        if (tmp < 0 || tmp > MAX_RETURNED_KEYS) {
            throw new IllegalArgumentException(OSSUtils.OSS_RESOURCE_MANAGER.getString("MaxKeysOutOfRange"));
        }
        this.maxKeys = maxKeys;
    }

    /**
     * Sets the max number of buckets to return the current ListBucketsRequest
     * instance (this).
     * 
     * @param maxKeys
     *            The max number of buckets.
     *
     * @return  The {@link ListBucketsRequest} instance.
     */
    public ListBucketsRequest withMaxKeys(Integer maxKeys) {
        setMaxKeys(maxKeys);
        return this;
    }

    /**
     * Sets the bid。
     * 
     * @param bid
     *            bid。
     */
    public void setBid(String bid) {
        this.bid = bid;
    }

    /**
     * Gets the bid.
     * 
     * @return bid。
     * 
     */
    public String getBid() {
        return bid;
    }

    /**
     * Sets the bucket tag.
     *
     * @param tagKey
     *            bucket tag key.
     * @param tagValue
     *            bucket tag value.
     */
    public void setTag(String tagKey, String tagValue) {
        this.tagKey = tagKey;
        this.tagValue = tagValue;
    }

    /**
     * Gets the tag key.
     *
     * @return tag key.
     *
     */
    public String getTagKey() {
        return this.tagKey;
    }

    /**
     * Gets the tag value.
     *
     * @return tag value.
     *
     */
    public String getTagValue() {
        return this.tagValue;
    }

    /**
     * Gets the resouce group id.
     *
     * @return The resouce group id.
     */
    public String getResourceGroupId() {
        return resouceGroupId;
    }

    /**
     * Sets the resouce group id.
     *
     * @param resouceGroupId
     *            The resouce group id.
     */
    public void setResourceGroupId(String resouceGroupId) {
        this.resouceGroupId = resouceGroupId;
    }

    /**
     * Sets the resouce group id and return the current ListBucketRequest instance
     * (this).
     *
     * @param resouceGroupId
     *            The resouce group id.
     *
     * @return  The {@link ListBucketsRequest} instance.
     */
    public ListBucketsRequest withResourceGroupId(String resouceGroupId) {
        setResourceGroupId(resouceGroupId);
        return this;
    }

}
