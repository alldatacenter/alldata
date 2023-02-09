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

import static com.aliyun.oss.internal.OSSUtils.validateObjectKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for deleting multiple objects in a specified bucket.
 */
public class DeleteObjectsRequest extends GenericRequest {

    public static final int DELETE_OBJECTS_ONETIME_LIMIT = 1000;

    /* List of keys to delete */
    private final List<String> keys = new ArrayList<String>();

    /* Whether to enable quiet mode for response, default is false */
    private boolean quiet;

    /*
     * Optional parameter indicating the encoding method to be applied on the
     * response.
     */
    private String encodingType;

    public DeleteObjectsRequest(String bucketName) {
        super(bucketName);
    }

    public boolean isQuiet() {
        return quiet;
    }

    public DeleteObjectsRequest withQuiet(boolean quiet) {
        setQuiet(quiet);
        return this;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        if (keys == null || keys.size() == 0) {
            throw new IllegalArgumentException("Keys to delete must be specified");
        }

        if (keys.size() > DELETE_OBJECTS_ONETIME_LIMIT) {
            throw new IllegalArgumentException(
                    "The count of keys to delete exceed max limit " + DELETE_OBJECTS_ONETIME_LIMIT);
        }

        for (String key : keys) {
            if (key == null || key.equals("") || !validateObjectKey(key)) {
                throw new IllegalArgumentException("Illegal object key " + key);
            }
        }

        this.keys.clear();
        this.keys.addAll(keys);
    }

    public DeleteObjectsRequest withKeys(List<String> keys) {
        setKeys(keys);
        return this;
    }

    public String getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    public DeleteObjectsRequest withEncodingType(String encodingType) {
        setEncodingType(encodingType);
        return this;
    }
}
