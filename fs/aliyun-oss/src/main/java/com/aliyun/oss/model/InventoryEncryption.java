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

import com.aliyun.oss.ClientException;

import java.io.Serializable;

public class InventoryEncryption implements Serializable {
    private static final long serialVersionUID = -5598814704819339238L;

    /**
     * Server side encryption by kms.
     * Note: Only one encryption method between KMS and OSS can be chosen.
     */
    private InventoryServerSideEncryptionKMS serverSideKmsEncryption;

    /**
     * Server side encryption by oss.
     * Note: Only one encryption method between KMS and OSS can be chosen.
     */
    private InventoryServerSideEncryptionOSS serverSideOssEncryption;

    /**
     * Sets the server side kms encryption.
     * Note: Only one encryption method between KMS and OSS can be chosen.
     *
     * @param  kmsServerSideEncryption
     *              The server-side kms encryption.
     */
    public void setServerSideKmsEncryption(InventoryServerSideEncryptionKMS kmsServerSideEncryption) {
        if (this.serverSideOssEncryption != null) {
            throw new ClientException("The KMS and OSS encryption only one of them can be specified.");
        }
        this.serverSideKmsEncryption = kmsServerSideEncryption;
    }

    /**
     * Gets the server-side kms encryption.
     *
     * @return  The {@link InventoryServerSideEncryptionKMS} instance.
     */
    public InventoryServerSideEncryptionKMS getServerSideKmsEncryption() {
        return serverSideKmsEncryption;
    }

    /**
     * Sets the KMS encryption to encrypt the inventory contents.
     * Note: Only one encryption method between KMS and OSS can be chosen.
     *
     * @param serverSideKmsEncryption
     *              A {@link InventoryServerSideEncryptionKMS} instance.
     * @return the {@link InventoryEncryption} object itself.
     */
    public InventoryEncryption withServerSideKmsEncryption(InventoryServerSideEncryptionKMS serverSideKmsEncryption) {
        setServerSideKmsEncryption(serverSideKmsEncryption);
        return this;
    }

    /**
     * Sets the server side oss encryption.
     * Note: Only one encryption method between KMS and OSS can be chosen.
     *
     * @param  ossServerSideEncryption
     *              The server-side oss encryption.
     */
    public void setServerSideOssEncryption(InventoryServerSideEncryptionOSS ossServerSideEncryption) {
        if (this.serverSideKmsEncryption != null) {
            throw new ClientException("The KMS and OSS encryption only one of them can be specified.");
        }
        this.serverSideOssEncryption = ossServerSideEncryption;
    }

    /**
     * Gets the server-side oss encryption.
     *
     * @return  The {@link InventoryServerSideEncryptionOSS} instance.
     */
    public InventoryServerSideEncryptionOSS getServerSideOssEncryption() {
        return serverSideOssEncryption;
    }

    /**
     * Sets the OSS encryption to encrypt the inventory contents.
     * Note: Only one encryption method between KMS and OSS can be chosen.
     *
     * @param serverSideOssEncryption
     *              A {@link InventoryServerSideEncryptionOSS} instance.
     * @return the {@link InventoryEncryption} object itself.
     */
    public InventoryEncryption withServerSideOssEncryption(InventoryServerSideEncryptionOSS serverSideOssEncryption) {
        setServerSideOssEncryption(serverSideOssEncryption);
        return this;
    }


}
