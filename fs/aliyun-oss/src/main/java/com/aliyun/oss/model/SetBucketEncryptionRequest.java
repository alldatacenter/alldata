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

/**
 * Represents the input of a <code>PutBucketEncryption</code> operation.
 */
public class SetBucketEncryptionRequest extends GenericRequest {

    private ServerSideEncryptionConfiguration serverSideEncryptionConfiguration;

    public SetBucketEncryptionRequest(String bucketName) {
    	super(bucketName);
    }
    
    public SetBucketEncryptionRequest(String bucketName, ServerSideEncryptionConfiguration serverSideEncryptionConfiguration) {
    	super(bucketName);
    	setServerSideEncryptionConfiguration(serverSideEncryptionConfiguration);
    }
    
    /**
     * @return Container for server-side encryption configuration rules. Currently OSS supports one rule only.
     */
    public ServerSideEncryptionConfiguration getServerSideEncryptionConfiguration() {
        return serverSideEncryptionConfiguration;
    }

    /**
     * Sets the container for server-side encryption configuration rules. Currently OSS supports one rule only.
     *
     * @param serverSideEncryptionConfiguration New configuration.
     */
    public void setServerSideEncryptionConfiguration(ServerSideEncryptionConfiguration serverSideEncryptionConfiguration) {
        this.serverSideEncryptionConfiguration = serverSideEncryptionConfiguration;
    }

    /**
     * Sets the container for server-side encryption configuration rules. Currently OSS supports one rule only.
     *
     * @param serverSideEncryptionConfiguration New configuration.
     * @return This object for method chaining.
     */
    public SetBucketEncryptionRequest withServerSideEncryptionConfiguration(ServerSideEncryptionConfiguration serverSideEncryptionConfiguration) {
        setServerSideEncryptionConfiguration(serverSideEncryptionConfiguration);
        return this;
    }
    
}
