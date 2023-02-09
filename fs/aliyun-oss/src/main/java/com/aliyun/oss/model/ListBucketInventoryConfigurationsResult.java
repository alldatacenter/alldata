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

import java.util.List;

/**
 * Result of the list bucket inventory configuration opreation.
 */
public class ListBucketInventoryConfigurationsResult extends GenericResult {

    /** The list of inventory configurations for a bucket. */
    private List<InventoryConfiguration> inventoryConfigurationList;

    /**
     * Optional parameter which allows list to be continued from a specific point.
     * This is the continuationToken that was sent in the current
     * {@link ListBucketInventoryConfigurationsResult}.
     */
    private String continuationToken;

    /**
     * Indicates if this is a truncated listing or not。
     */
    private boolean isTruncated;

    /**
     * NextContinuationToken is sent when isTruncated is true meaning there are
     * more inventory configurations in the bucket that can be listed.
     */
    private String nextContinuationToken;

    /**
     * Returns the list of inventory configurations for a bucket.
     *
     * @return  The {@link InventoryConfiguration} instance.
     */
    public List<InventoryConfiguration> getInventoryConfigurationList() {
        return inventoryConfigurationList;
    }

    /**
     * Sets the list of inventory configurations for a bucket.
     *
     * @param inventoryConfigurationList
     *            list of inventory configurations for a bucket.
     */
    public void setInventoryConfigurationList(List<InventoryConfiguration> inventoryConfigurationList) {
        this.inventoryConfigurationList = inventoryConfigurationList;
    }

    /**
     * Gets whether or not this inventory configuration listing is truncated.
     *
     * @return The value true if the inventory configuration listing is truancated,
     *         and the value false if the inventory configuration listing is completed.
     */
    public boolean isTruncated() {
        return isTruncated;
    }

    /**
     * For internal use only. Sets the truncated property for this inventory configuration listing.
     *
     * @param isTruncated
     *              The value true if the inventory configuration listing is truancated,
     *              and the value false if the inventory configuration listing is completed.
     */
    public void setTruncated(boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    /**
     * Gets the optional continuation token.  Continuation token allows a list to be
     * continued from a specific point. This is the continuationToken that was sent in the current
     * {@link ListBucketInventoryConfigurationsResult}.
     *
     * @return The optional continuation token associated with this request.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * Sets the optional continuation token.  Continuation token allows a list to be
     * continued from a specific point. This is the continuationToken that was sent in the current
     * {@link ListBucketInventoryConfigurationsResult}.
     *
     * @param continuationToken
     *                     The optional continuation token to associate with this request.
     */
    public void setContinuationToken(String continuationToken) {
        this.continuationToken = continuationToken;
    }

    /**
     * Gets the optional NextContinuationToken.
     * NextContinuationToken is sent when isTruncated is true meaning there are
     * more keys in the bucket that can be listed.
     *
     * @return The optional NextContinuationToken parameter.
     */
    public String getNextContinuationToken() {
        return nextContinuationToken;
    }

    /**
     * Sets the optional NextContinuationToken.
     * NextContinuationToken is sent when isTruncated is true meaning there are
     * more keys in the bucket that can be listed.
     *
     * @param nextContinuationToken
     *              The optional NextContinuationToken parameter to associate with this request.
     */
    public void setNextContinuationToken(String nextContinuationToken) {
        this.nextContinuationToken = nextContinuationToken;
    }
}
