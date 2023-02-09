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

import com.qcloud.cos.internal.CosServiceRequest;

public class DeleteObjectRequest extends CosServiceRequest implements Serializable {
    /**
     * The name of the Qcloud COS bucket containing the object to delete.
     */
    private String bucketName;

    /**
     * The key of the object to delete.
     */
    private String key;

    /**
     * The flag used in merge bucket to recursive delete dirs
     */
    private boolean isRecursive;


    /**
     * Constructs a new {@link DeleteObjectRequest}, specifying the object's bucket name and key.
     *
     * @param bucketName The name of the Qcloud COS bucket containing the object to delete.
     * @param key The key of the object to delete.
     */
    public DeleteObjectRequest(String bucketName, String key) {
        setBucketName(bucketName);
        setKey(key);
        setRecursive(false);
    }

    /**
     * Gets the name of the Qcloud COS bucket containing the object to delete.
     *
     * @return The name of the Qcloud COS bucket containing the object to delete.
     *
     * @see DeleteObjectRequest#setBucketName(String)
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the Qcloud COS bucket containing the object to delete.
     *
     * @param bucketName The name of the Qcloud COS bucket containing the object to delete.
     * @see DeleteObjectRequest#getBucketName()
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the Qcloud COS bucket containing the object to delete and returns this
     * object, enabling additional method calls to be chained together.
     *
     * @param bucketName The name of the Qcloud COS bucket containing the object to delete.
     *
     * @return The updated {@link DeleteObjectRequest} object, enabling additional method calls to
     *         be chained together.
     */
    public DeleteObjectRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Gets the key of the object to delete.
     *
     * @return The key of the object to delete.
     *
     * @see DeleteObjectRequest#setKey(String)
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key of the object to delete.
     *
     * @param key The key of the object to delete.
     *
     * @see DeleteObjectRequest#getKey()
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets the key of the object to delete and returns this object, enabling additional method
     * calls to be chained together.
     *
     * @param key The key of the object to delete.
     *
     * @return The updated {@link DeleteObjectRequest} object, enabling additional method calls to
     *         chained together.
     */
    public DeleteObjectRequest withKey(String key) {
        setKey(key);
        return this;
    }

    public void setRecursive(boolean isRecursive) {
        this.isRecursive = isRecursive;
    }

    public boolean isRecursive() {
        return this.isRecursive;
    }
}
