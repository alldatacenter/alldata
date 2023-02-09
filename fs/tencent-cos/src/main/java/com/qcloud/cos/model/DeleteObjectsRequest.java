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
import java.util.ArrayList;
import java.util.List;

import com.qcloud.cos.internal.CosServiceRequest;

/**
 * Provides options for deleting multiple objects in a specified bucket. Once deleted, the object(s)
 * can only be restored if versioning was enabled when the object(s) was deleted.You may specify up
 * to 1000 keys
 * </p>
 *
 * @see COS#deleteObjects(DeleteObjectsRequest)
 */
public class DeleteObjectsRequest extends CosServiceRequest implements Serializable {

    /**
     * The name of the COS bucket containing the object(s) to delete.
     */
    private String bucketName;

    /**
     * Whether to enable quiet mode for the response. In quiet mode, only errors are reported.
     * Defaults to false.
     */
    private boolean quiet;

    /**
     * List of keys to delete, with optional versions.
     */
    private final List<KeyVersion> keys = new ArrayList<KeyVersion>();


    /**
     * Constructs a new {@link DeleteObjectsRequest}, specifying the objects' bucket name.
     *
     * @param bucketName The name of the COS bucket containing the object(s) to delete.
     */
    public DeleteObjectsRequest(String bucketName) {
        setBucketName(bucketName);
    }

    /**
     * Gets the name of the COS bucket containing the object(s) to delete.
     *
     * @return The name of the COS bucket containing the object(s) to delete.
     * @see DeleteObjectsRequest#setBucketName(String)
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the COS bucket containing the object(s) to delete.
     *
     * @param bucketName The name of the COS bucket containing the object(s) to delete.
     * @see DeleteObjectsRequest#getBucketName()
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the COS bucket containing the object(s) to delete and returns this
     * object, enabling additional method calls to be chained together.
     *
     * @param bucketName The name of the COS bucket containing the object(s) to delete.
     * @return The updated {@link DeleteObjectsRequest} object, enabling additional method calls to
     *         be chained together.
     */
    public DeleteObjectsRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }


    /**
     * Sets the quiet element for this request. When true, only errors will be returned in the
     * service response.
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    /**
     * Returns the quiet element for this request. When true, only errors will be returned in the
     * service response.
     */
    public boolean getQuiet() {
        return quiet;
    }

    /**
     * Sets the quiet element for this request. When true, only errors will be returned in the
     * service response.
     *
     * @return this, to chain multiple calls together.
     */
    public DeleteObjectsRequest withQuiet(boolean quiet) {
        this.setQuiet(quiet);
        return this;
    }

    /**
     * Sets the list of keys to delete from this bucket, clearing any existing list of keys.
     *
     * @param keys The list of keys to delete from this bucket
     */
    public void setKeys(List<KeyVersion> keys) {
        this.keys.clear();
        this.keys.addAll(keys);
    }

    /**
     * Sets the list of keys to delete from this bucket, clearing any existing list of keys.
     *
     * @param keys The list of keys to delete from this bucket
     *
     * @return this, to chain multiple calls togethers.
     */
    public DeleteObjectsRequest withKeys(List<KeyVersion> keys) {
        setKeys(keys);
        return this;
    }

    /**
     * Returns the list of keys to delete from this bucket.
     */
    public List<KeyVersion> getKeys() {
        return keys;
    }

    /**
     * Convenience method to specify a set of keys without versions.
     *
     * @see DeleteObjectsRequest#withKeys(List)
     */
    public DeleteObjectsRequest withKeys(String... keys) {
        List<KeyVersion> keyVersions = new ArrayList<KeyVersion>(keys.length);
        for (String key : keys) {
            keyVersions.add(new KeyVersion(key));
        }
        setKeys(keyVersions);
        return this;
    }


    /**
     * A key to delete, with an optional version attribute.
     */
    public static class KeyVersion implements Serializable {

        private final String key;
        private final String version;

        /**
         * Constructs a key without a version.
         */
        public KeyVersion(String key) {
            this(key, null);
        }

        /**
         * Constructs a key-version pair.
         */
        public KeyVersion(String key, String version) {
            this.key = key;
            this.version = version;
        }

        public String getKey() {
            return key;
        }

        public String getVersion() {
            return version;
        }
    }
}
