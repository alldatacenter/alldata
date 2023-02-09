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

import java.io.File;
import java.io.InputStream;

public class AppendObjectRequest extends PutObjectRequest {

    private Long position;
    private Long initCRC;

    // Traffic limit speed, its uint is bit/s
    private int trafficLimit;

    public AppendObjectRequest(String bucketName, String key, File file) {
        this(bucketName, key, file, null);
    }

    public AppendObjectRequest(String bucketName, String key, File file, ObjectMetadata metadata) {
        super(bucketName, key, file, metadata);
    }

    public AppendObjectRequest(String bucketName, String key, InputStream input) {
        this(bucketName, key, input, null);
    }

    public AppendObjectRequest(String bucketName, String key, InputStream input, ObjectMetadata metadata) {
        super(bucketName, key, input, metadata);
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public Long getInitCRC() {
        return initCRC;
    }

    public void setInitCRC(Long iniCRC) {
        this.initCRC = iniCRC;
    }

    public AppendObjectRequest withPosition(Long position) {
        setPosition(position);
        return this;
    }

    public AppendObjectRequest withInitCRC(Long iniCRC) {
        setInitCRC(iniCRC);
        return this;
    }

    /**
     * Sets traffic limit speed, its unit is bit/s
     */
    public void setTrafficLimit(int trafficLimit) {
        this.trafficLimit = trafficLimit;
    }

    /**
     * Gets traffic limit speed, its unit is bit/s
     * @return traffic limit speed
     */
    public int getTrafficLimit() {
        return trafficLimit;
    }

}
