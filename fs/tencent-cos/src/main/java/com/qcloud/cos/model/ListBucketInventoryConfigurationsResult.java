/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */
package com.qcloud.cos.model;

import com.qcloud.cos.model.inventory.InventoryConfiguration;

import java.io.Serializable;
import java.util.List;

/**
 * Result object to contain the response returned from
 * {@link com.qcloud.cos.COSClient#listBucketInventoryConfigurations(ListBucketInventoryConfigurationsRequest)}
 * operation.
 */
public class ListBucketInventoryConfigurationsResult implements Serializable {

    /** The list of inventory configurations for a bucket. */
    private List<InventoryConfiguration> inventoryConfigurationList;

    /**
     * Optional parameter which allows list to be continued from a specific point.
     * This is the continuationToken that was sent in the current
     * {@link ListBucketInventoryConfigurationsResult}.
     */
    private String continuationToken;

    /**
     * Indicates if this is a complete listing, or if the caller needs to make
     * additional requests to COS to see the full inventory configuration
     * listing for an COS bucket.
     */
    private boolean isTruncated;

    /**
     * NextContinuationToken is sent when isTruncated is true meaning there are
     * more inventory configurations in the bucket that can be listed. The next list requests to Amazon
     * COS can be continued by providing this NextContinuationToken.
     */
    private String nextContinuationToken;

    /**
     * Returns the list of inventory configurations for a bucket.
     */
    public List<InventoryConfiguration> getInventoryConfigurationList() {
        return inventoryConfigurationList;
    }

    /**
     * Sets the list of inventory configurations for a bucket.
     */
    public void setInventoryConfigurationList(List<InventoryConfiguration> inventoryConfigurationList) {
        this.inventoryConfigurationList = inventoryConfigurationList;
    }

    /**
     * Returns the list of inventory configurations for a bucket and returns
     * {@link ListBucketInventoryConfigurationsResult} object for method chaining.
     */
    public ListBucketInventoryConfigurationsResult withInventoryConfigurationList(List<InventoryConfiguration> inventoryConfigurationList) {
        setInventoryConfigurationList(inventoryConfigurationList);
        return this;
    }

    /**
     * Gets whether or not this inventory configuration listing is complete.
     *
     * @return The value <code>true</code> if the inventory configuration listing is <b>not complete</b>.
     *         Returns the value <code>false</code> if otherwise.
     *         When returning <code>true</code>,
     *         additional calls to COS may be needed in order to
     *         obtain more results.
     */
    public boolean isTruncated() {
        return isTruncated;
    }

    /**
     * For internal use only. Sets the truncated property for
     * this inventory configuration listing, indicating if this is a complete listing or not and
     * whether the caller needs to make additional calls to COS to get more results.
     *
     * @param isTruncated
     *            The value <code>true</code> if the inventory configuration listing is <b>not complete</b>.
     *            The value <code>false</code> if otherwise.
     */
    public void setTruncated(boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    /**
     * For internal use only. Sets the truncated property for
     * this inventory configuration listing, indicating if this is a complete listing or not and
     * whether the caller needs to make additional calls to COS to get more results.
     *
     * @param isTruncated
     *            The value <code>true</code> if the inventory configuration listing is <b>not complete</b>.
     *            The value <code>false</code> if otherwise.
     *
     * @return
     *          This object for method chaining.
     */
    public ListBucketInventoryConfigurationsResult withTruncated(boolean isTruncated) {
        setTruncated(isTruncated);
        return this;
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
     * Sets the optional continuation token.  Continuation token allows a list to be
     * continued from a specific point. This is the continuationToken that was sent in the current
     * {@link ListBucketInventoryConfigurationsResult}.
     *
     * @param continuationToken
     *                     The optional continuation token to associate with this request.
     *
     * @return
     *          This object for method chaining.
     */
    public ListBucketInventoryConfigurationsResult withContinuationToken(String continuationToken) {
        setContinuationToken(continuationToken);
        return this;
    }

    /**
     * Gets the optional NextContinuationToken.
     * NextContinuationToken is sent when isTruncated is true meaning there are
     * more keys in the bucket that can be listed. The next list requests to Amazon
     * COS can be continued by providing this NextContinuationToken.
     *
     * @return The optional NextContinuationToken parameter.
     */
    public String getNextContinuationToken() {
        return nextContinuationToken;
    }

    /**
     * Sets the optional NextContinuationToken.
     * NextContinuationToken is sent when isTruncated is true meaning there are
     * more keys in the bucket that can be listed. The next list requests to Amazon
     * COS can be continued by providing this NextContinuationToken.
     *
     * @param nextContinuationToken
     *              The optional NextContinuationToken parameter to associate with this request.
     */
    public void setNextContinuationToken(String nextContinuationToken) {
        this.nextContinuationToken = nextContinuationToken;
    }


    /**
     * Sets the optional NextContinuationToken.
     * NextContinuationToken is sent when isTruncated is true meaning there are
     * more keys in the bucket that can be listed. The next list requests to Amazon
     * COS can be continued by providing this NextContinuationToken.
     *
     * @param nextContinuationToken
     *              The optional NextContinuationToken parameter to associate with this request.
     *
     * @return
     *          This object for method chaining.
     */
    public ListBucketInventoryConfigurationsResult withNextContinuationToken(String nextContinuationToken) {
        setNextContinuationToken(nextContinuationToken);
        return this;
    }

}
