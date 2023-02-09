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

/**
 * Userd for server side encryption with KMS.
 */
public class InventoryServerSideEncryptionKMS implements Serializable {
    private static final long serialVersionUID = 4780140470042030123L;

    private String keyId;

    /**
     * @return KMS key id used to encrypt the inventory contents.
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Sets the KMS key id to use to encrypt the inventory contents.
     *
     * @param keyId
     *              The KMS key id.
     */
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    /**
     * Sets the KMS key id to use to encrypt the inventory contents.
     *
     * @param keyId
     *              The KMS key id.
     * @return the {@link InventoryServerSideEncryptionKMS} object itself.
     */
    public InventoryServerSideEncryptionKMS withKeyId(String keyId) {
        setKeyId(keyId);
        return this;
    }
}
