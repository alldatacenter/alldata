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

import java.util.HashMap;
import java.util.Map;

import com.aliyun.oss.internal.OSSHeaders;

/**
 * OSS Bucket's metadata.
 */
public class BucketMetadata {

    /**
     * Gets bucket region.
     * 
     * @return bucket region.
     */
    public String getBucketRegion() {
        return httpMetadata.get(OSSHeaders.OSS_BUCKET_REGION);
    }

    /**
     * Sets bucket region. SDK uses.
     * 
     * @param bucketRegion
     *            bucket Region.
     */
    public void setBucketRegion(String bucketRegion) {
        httpMetadata.put(OSSHeaders.OSS_BUCKET_REGION, bucketRegion);
    }

    /**
     * Add http metadata.
     * 
     * @param key
     *            The key of header.
     * @param value
     *            The value of the key.
     */
    public void addHttpMetadata(String key, String value) {
        httpMetadata.put(key, value);
    }

    /**
     * Gets the http metadata.
     * 
     * @return http metadata
     */
    public Map<String, String> getHttpMetadata() {
        return httpMetadata;
    }

    // http headers.
    private Map<String, String> httpMetadata = new HashMap<String, String>();

}
