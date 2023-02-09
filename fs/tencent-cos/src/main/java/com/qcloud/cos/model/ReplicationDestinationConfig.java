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

import java.io.Serializable;
import com.qcloud.cos.utils.Jackson;

/**
 * Destination configuration for Qcloud bucket replication rule. 
 */
public class ReplicationDestinationConfig implements Serializable {

    /**
     * The QCS of the QCloud bucket where the replicas are sent.
     */
    private String bucketQCS;

    /**
     * Storage class for the replica. If not specified, QCloud uses the storage class of the
     * source object to create object replica.
     */
    private String storageClass;

    /**
     * Returns the bucket QCS where the replicas are present.
     */
    public String getBucketQCS() {
        return bucketQCS;
    }

    /**
     * Sets the destination bucket QCS for the replication rule.
     *
     * @throws IllegalArgumentException if the bucket qcs is null.
     */
    public void setBucketQCS(String bucketQCS) {
        if (bucketQCS == null) {
            throw new IllegalArgumentException("Bucket QCS cannot be null");
        }
        this.bucketQCS = bucketQCS;
    }

    /**
     * Sets the destination bucket QCS for the replication rule. Returns the updated object.
     *
     * @throws IllegalArgumentException if the bucket qcs is null.
     * @return the updated {@link ReplicationDestinationConfig} object
     */
    public ReplicationDestinationConfig withBucketQCS(String bucketARN) {
        setBucketQCS(bucketARN);
        return this;
    }

    /**
     * Sets the storage class for the replication destination. If not specified, QCloud uses the
     * storage class of the source object to create object replica.
     */
    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    /**
     * Sets the storage class for the replication destination. If not specified, QCloud uses the
     * storage class of the source object to create object replica.
     */
    public void setStorageClass(StorageClass storageClass) {
        setStorageClass(storageClass == null ? (String) null : storageClass.toString());
    }

    /**
     * Sets the storage class for the replication destination. If not specified, QCloud uses the
     * storage class of the source object to create object replica. Returns the updated object.
     *
     * @return the updated {@link ReplicationDestinationConfig} object
     */
    public ReplicationDestinationConfig withStorageClass(String storageClass) {
        setStorageClass(storageClass);
        return this;
    }

    /**
     * Sets the storage class for the replication destination. If not specified, QCloud uses the
     * storage class of the source object to create object replica. Returns the updated object.
     *
     * @return the updated {@link ReplicationDestinationConfig} object
     */
    public ReplicationDestinationConfig withStorageClass(StorageClass storageClass) {
        setStorageClass(storageClass == null ? (String) null : storageClass.toString());
        return this;
    }

    /**
     * Returns the storage class associated with the replication destination configuration.
     */
    public String getStorageClass() {
        return storageClass;
    }

    @Override
    public String toString() {
        return Jackson.toJsonString(this);
    }
}
