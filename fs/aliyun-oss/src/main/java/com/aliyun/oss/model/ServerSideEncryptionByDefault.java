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
 * Describes the default server-side encryption to apply to new objects in the bucket. If Put Object request does not specify any
 * server-side encryption, this default encryption will be applied.
 */
public class ServerSideEncryptionByDefault {

    private String sseAlgorithm;
    private String kmsMasterKeyID;
    private String kmsDataEncryption;
    /**
     * Creates a default instance.
     * 
     */
    public ServerSideEncryptionByDefault() {
	}
    
    /**
     * Creates an instance.
     * 
     * @param sseAlgorithm SSE algorithm to use.
     */
    public ServerSideEncryptionByDefault(String sseAlgorithm) {
    	setSSEAlgorithm(sseAlgorithm);
	}
    
    /**
     * Creates an instance.
     *      
     * @param sseAlgorithm SSE algorithm to use.
     */
    public ServerSideEncryptionByDefault(SSEAlgorithm sseAlgorithm) {
    	setSSEAlgorithm(sseAlgorithm);
	}
    
    /**
     * @return Server-side encryption algorithm to use for the default encryption.
     */
    public String getSSEAlgorithm() {
        return sseAlgorithm;
    }

    /**
     * Sets the server-side encryption algorithm to use for the default encryption.
     *
     * @param sseAlgorithm SSE algorithm to use.
     */
    public void setSSEAlgorithm(String sseAlgorithm) {
        this.sseAlgorithm = sseAlgorithm;
    }
    
    /**
     * Sets the server-side encryption algorithm to use for the default encryption.
     *
     * @param sseAlgorithm SSE algorithm to use.
     */
    public void setSSEAlgorithm(SSEAlgorithm sseAlgorithm) {
        setSSEAlgorithm(sseAlgorithm == null ? null : sseAlgorithm.toString());
    }

    /**
     * Sets the server-side encryption algorithm to use for the default encryption.
     *
     * @param sseAlgorithm SSE algorithm to use.
     * @return This object for method chaining.
     */
    public ServerSideEncryptionByDefault withSSEAlgorithm(String sseAlgorithm) {
        setSSEAlgorithm(sseAlgorithm);
        return this;
    }

    /**
     * Sets the server-side encryption algorithm to use for the default encryption.
     *
     * @param sseAlgorithm SSE algorithm to use.
     * @return This object for method chaining.
     */
    public ServerSideEncryptionByDefault withSSEAlgorithm(SSEAlgorithm sseAlgorithm) {
        setSSEAlgorithm(sseAlgorithm == null ? null : sseAlgorithm.toString());
        return this;
    }

    /**
     * @return KMS master key ID to use for the default encryption. This parameter is allowed if SSEAlgorithm is kms.
     */
    public String getKMSMasterKeyID() {
        return kmsMasterKeyID;
    }

    /**
     * Sets the KMS master key ID to use for the default encryption. This parameter is allowed if SSEAlgorithm is kms.
     *
     * @param kmsMasterKeyID KMS key to use.
     */
    public void setKMSMasterKeyID(String kmsMasterKeyID) {
        this.kmsMasterKeyID = kmsMasterKeyID;
    }

    /**
     * Sets the KMS master key ID to use for the default encryption. This parameter is allowed if SSEAlgorithm is kms.
     *
     * @param kmsMasterKeyID KMS key to use.
     * @return This object for method chaining.
     */
    public ServerSideEncryptionByDefault withKMSMasterKeyID(String kmsMasterKeyID) {
        setKMSMasterKeyID(kmsMasterKeyID);
        return this;
    }

    /**
     * @return This parameter is allowed if SSEAlgorithm is kms.
     */
    public String getKMSDataEncryption() {
        return kmsDataEncryption;
    }

    /**
     * Sets the KMS data encryption. This parameter is allowed if SSEAlgorithm is kms.
     *
     * @param kmsDataEncryption KMS data encryption to use.
     */
    public void setKMSDataEncryption(String kmsDataEncryption) {
        this.kmsDataEncryption = kmsDataEncryption;
    }

    /**
     * Sets the KMS data encryption. This parameter is allowed if SSEAlgorithm is kms.
     *
     * @param kmsDataEncryption KMS data encryption to use.
     * @return This object for method chaining.
     */
    public ServerSideEncryptionByDefault withKMSDataEncryption(String kmsDataEncryption) {
        setKMSDataEncryption(kmsDataEncryption);
        return this;
    }
}
