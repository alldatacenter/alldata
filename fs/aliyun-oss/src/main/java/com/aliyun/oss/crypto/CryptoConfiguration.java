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

package com.aliyun.oss.crypto;

import static com.aliyun.oss.crypto.ContentCryptoMode.*;

import java.io.Serializable;
import java.security.Provider;
import java.security.SecureRandom;

/**
 * The crypto configuration used to configure storage method of crypto info,
 * content crypto mode, secure random generator and content crypto provider.
 */
public class CryptoConfiguration implements Cloneable, Serializable {
    private static final long serialVersionUID = -610360281849259785L;

    private static final SecureRandom SRAND = new SecureRandom();
    private ContentCryptoMode contentCryptoMode;
    private CryptoStorageMethod storageMethod;
    private Provider contentCryptoProvider;
    private SecureRandom secureRandom;

    /**
     * Default crypto configuration.
     */
    public static final CryptoConfiguration DEFAULT = new CryptoConfiguration();

    public CryptoConfiguration() {
        this.contentCryptoMode = AES_CTR_MODE;
        this.storageMethod = CryptoStorageMethod.ObjectMetadata;
        this.secureRandom = SRAND;
        this.contentCryptoProvider = null;
    }

    public CryptoConfiguration(ContentCryptoMode contentCryptoMode, 
                               CryptoStorageMethod storageMethod,
                               SecureRandom secureRandom,
                               Provider contentCryptoProvider) {
        this.contentCryptoMode = contentCryptoMode;
        this.storageMethod = storageMethod;
        this.secureRandom = secureRandom;
        this.contentCryptoProvider = contentCryptoProvider;
    }

    /**
     * Sets the content crypto mode to the specified crypto mode.
     *
     * @param contentCryptoMode
     *            the content crypto mode {@link ContentCryptoMode}.
     */
    public void setContentCryptoMode(ContentCryptoMode contentCryptoMode) {
        this.contentCryptoMode = contentCryptoMode;
    }

    /**
     * Gets the content crypto mode to the specified crypto mode.
     *
     * @return contentCryptoMode
     *            the content crypto mode {@link ContentCryptoMode}.
     */
    public ContentCryptoMode getContentCryptoMode() {
        return this.contentCryptoMode;
    }

    /**
     * Sets the storage method to the specified storage method.
     *
     * @param storageMethod
     *            the storage method of the cryto information sotoring.
     */
    public void setStorageMethod(CryptoStorageMethod storageMethod) {
        this.storageMethod = storageMethod;
    }

    /**
     * Gets the storage method.
     * 
     * @return the storage method of the cryto information storing.
     */
    public CryptoStorageMethod getStorageMethod() {
        return this.storageMethod;
    }

    /**
     * Sets the secure random to the specified secure random generator, and returns the updated
     * CryptoConfiguration object.
     *
     * @param secureRandom
     *            the secure random generator.
     */
    public void setSecureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    /**
     * Sets the secure random to the specified secure random generator, and returns the updated
     * CryptoConfiguration object.
     *
     * @param secureRandom
     *            the secure random generator.
     * @return The updated CryptoConfiguration object.
     */
    public CryptoConfiguration withSecureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
        return this;
    }

    /**
     * Gets the secure random to the specified secure random generator.
     *
     * @return secureRandom
     *            the secure random generator.
     */
    public SecureRandom getSecureRandom() {
        return secureRandom;
    }

    /**
     * Sets the content crypto provider the specified provider.
     * 
     * @param contentCryptoProvider
     *            The provider to be used for crypto content.
     */
    public void setContentCryptoProvider(Provider contentCryptoProvider) {
        this.contentCryptoProvider = contentCryptoProvider;
    }

    /**
     * Sets the content crypto provider the specified provider, and returns the updated
     * CryptoConfiguration object.
     *
     * @param contentCryptoProvider
     *            The provider to be used for crypto content.
     * @return The updated CryptoConfiguration object.
     */
    public CryptoConfiguration withContentCryptoProvider(Provider contentCryptoProvider) {
        this.contentCryptoProvider = contentCryptoProvider;
        return this;
    }

    /**
     * Gets the content crypto provider
     * 
     * @return the provider to be used for crypto content.
     */
    public Provider getContentCryptoProvider() {
        return this.contentCryptoProvider;
    }

    @Override
    public CryptoConfiguration clone() {
        CryptoConfiguration config = new CryptoConfiguration();
        config.setContentCryptoMode(contentCryptoMode);
        config.setSecureRandom(secureRandom);
        config.setStorageMethod(storageMethod);
        config.setContentCryptoProvider(contentCryptoProvider);
        return config;
    }
}
