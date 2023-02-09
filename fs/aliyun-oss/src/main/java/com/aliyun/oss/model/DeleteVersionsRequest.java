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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides options for deleting multiple objects in a specified bucket. Once
 * deleted, the object(s) can only be restored if versioning was enabled when
 * the object(s) was deleted.
 * 
 * @see com.aliyun.oss.OSS#deleteVersions(DeleteVersionsRequest)
 */
public class DeleteVersionsRequest extends GenericRequest {
	
    /**
     * A key to delete, with an optional version attribute.
     */
    public static class KeyVersion implements Serializable {

        private static final long serialVersionUID = 6665103581584979327L;

        private final String key;
        private final String version;

        /**
         * Constructs a key without a version.
         *
         * @param key
         *            key info.
         */
        public KeyVersion(String key) {
            this(key, null);
        }

        /**
         * Constructs a key-version pair.
         *
         * @param key
         *            key info.
         * @param version
         *            version info.
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

    /**
     * Whether to enable quiet mode for the response. In quiet mode, only errors
     * are reported. Defaults to false.
     */
    private boolean quiet;

    /**
     * List of keys to delete, with optional versions.
     */
    private final List<KeyVersion> keys = new ArrayList<KeyVersion>();

    /**
     * Constructs a new {@link DeleteVersionsRequest}, specifying the objects'
     * bucket name.
     *
     * @param bucketName
     *            The name of the OSS bucket containing the object(s) to delete.
     */
    public DeleteVersionsRequest(String bucketName) {
        super(bucketName);
    }

    /**
     * Sets the name of the OSS bucket containing the object(s) to delete
     * and returns this object, enabling additional method calls to be chained
     * together.
     *
     * @param bucketName
     *            The name of the OSS bucket containing the object(s) to delete.
     *            
     * @return The updated {@link DeleteVersionsRequest} object, enabling
     *         additional method calls to be chained together.
     */
    public DeleteVersionsRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Sets the quiet element for this request. When true, only errors will be
     * returned in the service response.
     *
     * @param quiet
     *            Sets the quiet element for this request.
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    /**
     * Gets the quiet element for this request. When true, only errors will be
     * returned in the service response.
     * @return the flag for this request.
     */
    public boolean getQuiet() {
        return quiet;
    }

    /**
     * Sets the quiet element for this request. When true, only errors will be
     * returned in the service response.
     *
     * @param quiet
     *            the flag for this request.
     *
     * @return this, to chain multiple calls together.
     */
    public DeleteVersionsRequest withQuiet(boolean quiet) {
        this.setQuiet(quiet);
        return this;
    }

    /**
     * Sets the list of keys to delete from this bucket, clearing any existing
     * list of keys.
     *
     * @param keys
     *            The list of keys to delete from this bucket
     */
    public void setKeys(List<KeyVersion> keys) {
        this.keys.clear();
        this.keys.addAll(keys);
    }

    /**
     * Sets the list of keys to delete from this bucket, clearing any existing
     * list of keys.
     *
     * @param keys
     *            The list of keys to delete from this bucket
     *
     * @return this, to chain multiple calls togethers.
     */
    public DeleteVersionsRequest withKeys(List<KeyVersion> keys) {
        setKeys(keys);
        return this;
    }

    /**
     * Gets the list of keys to delete from this bucket.
     * @return the list of keys.
     */
    public List<KeyVersion> getKeys() {
        return keys;
    }
    
}
