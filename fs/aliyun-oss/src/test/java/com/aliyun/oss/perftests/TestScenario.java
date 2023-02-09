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

package com.aliyun.oss.perftests;

public class TestScenario {
    
    public static enum Type {
        UNKNOWN("unknown"),
        ONLY_PUT_1MB("onlyput-1MB"),
        ONLY_PUT_4MB("onlyput-4MB"),
        GET_AND_PUT_1VS1_1KB("get-and-put-1vs1-1KB"),
        GET_AND_PUT_1VS1_100KB("get-and-put-1vs1-100KB"),
        GET_AND_PUT_1VS1_1MB("get-and-put-1vs1-1MB"),
        GET_AND_PUT_1VS1_4MB("get-and-put-1vs1-4MB"),
        GET_AND_PUT_1VS4_1MB("get-and-put-1vs4-1MB"),
        GET_AND_PUT_1VS4_4MB("get-and-put-1vs4-4MB");
        
        private final String typeAsString;
        
        private Type(String type) {
            this.typeAsString = type;
        }
        
        @Override
        public String toString() {
            return typeAsString;
        }
    }
    
    private Type type;
    private long contentLength;
    private int putThreadNumber;
    private int getThreadNumber;
    private int durationInSeconds;
    
    private int putQPS;
    private int getQPS;
    
    private String host;
    private String accessId;
    private String accessKey;
    private String bucketName;
    
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public long getContentLength() {
        return contentLength;
    }
    
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
    
    public int getPutThreadNumber() {
        return putThreadNumber;
    }
    
    public void setPutThreadNumber(int putThreadNumber) {
        this.putThreadNumber = putThreadNumber;
    }
    
    public int getGetThreadNumber() {
        return getThreadNumber;
    }
    
    public void setGetThreadNumber(int getThreadNumber) {
        this.getThreadNumber = getThreadNumber;
    }

    public int getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setDurationInSeconds(int durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public String getHost() {
        return host;
    }

    public int getPutQPS() {
        return putQPS;
    }

    public void setPutQPS(int putQPS) {
        this.putQPS = putQPS;
    }

    public int getGetQPS() {
        return getQPS;
    }

    public void setGetQPS(int getQPS) {
        this.getQPS = getQPS;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAccessId() {
        return accessId;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
