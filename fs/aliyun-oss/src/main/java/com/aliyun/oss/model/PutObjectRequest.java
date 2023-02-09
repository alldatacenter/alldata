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

public class PutObjectRequest extends GenericRequest {

    private File file;
    private InputStream inputStream;

    private ObjectMetadata metadata;

    private Callback callback;
    private String process;

    // Traffic limit speed, its uint is bit/s
    private int trafficLimit;

    public PutObjectRequest(String bucketName, String key, File file) {
        this(bucketName, key, file, null);
    }

    public PutObjectRequest(String bucketName, String key, File file, ObjectMetadata metadata) {
        super(bucketName, key);
        this.file = file;
        this.metadata = metadata;
    }

    public PutObjectRequest(String bucketName, String key, InputStream input) {
        this(bucketName, key, input, null);
    }

    public PutObjectRequest(String bucketName, String key, InputStream input, ObjectMetadata metadata) {
        super(bucketName, key);
        this.inputStream = input;
        this.metadata = metadata;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public ObjectMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    /**
     * Sets traffic limit speed, its unit is bit/s
     *
     * @param trafficLimit
     *            traffic limit.
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
