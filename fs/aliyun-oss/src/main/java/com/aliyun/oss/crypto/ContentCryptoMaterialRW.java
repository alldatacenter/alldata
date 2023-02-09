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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.SecretKey;

/**
 * ContentCryptoMaterialRW has the setting accessor of {@link ContentCryptoMaterial}.
 */
public class ContentCryptoMaterialRW extends ContentCryptoMaterial {
    /**
     * Sets the content crypto algorithm to the specified algorithm.
     *
     * @param cententCryptoAlgorithm
     *            crypto algorithm.
     */
    public void setContentCryptoAlgorithm(String cententCryptoAlgorithm) {
        this.contentCryptoAlgorithm = cententCryptoAlgorithm;
    }

    /**
     * Sets the content encryption key to the specified key.
     *
     * @param cek
     *            secret key.
     */
    public void setCEK(SecretKey cek) {
        this.cek = cek;
    }

    /**
     * Sets the content crypto cipher start counter to the specified counter.
     *
     * @param iv
     *            initialize vector.
     */
    public void setIV(byte[] iv) {
        this.iv = iv;
    }

    /**
     * Sets the encrypted content encryption key to the specified array.
     *
     * @param encryptedCEK
     *            encrypted secret key.
     */
    public void setEncryptedCEK(byte[] encryptedCEK) {
        this.encryptedCEK = encryptedCEK.clone();
    }

    /**
     * Sets the encrypted content crypto cipher start counter to the specified array.
     *
     * @param encryptedIV
     *            encrypted initialize vector.
     */
    public void setEncryptedIV(byte[] encryptedIV) {
        this.encryptedIV = encryptedIV.clone();
    }

    /**
     * Sets the key wrap algorithm to the specified algorithm.
     *
     * @param keyWrapAlgorithm
     *            key wrap algorithm.
     */
    public void setKeyWrapAlgorithm(String keyWrapAlgorithm) {
        this.keyWrapAlgorithm = keyWrapAlgorithm;
    }

    /**
     * Sets the description of the encryption materials
     *
     * @param matdesc
     *            description of the encryption materials.
     */
    public void setMaterialsDescription(Map<String, String> matdesc) {
        this.matdesc = Collections.unmodifiableMap(new TreeMap<String, String>(matdesc));
    }
}
