/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * SSE-C encryption/decryption headers
 */
public class SseCHeader {

    private ServerAlgorithm algorithm;

    private SSEAlgorithmEnum sseAlgorithm = SSEAlgorithmEnum.AES256;

    private byte[] sseCKey;

    private String sseCKeyBase64;

    /**
     * Obtain the encryption algorithm type. Only AES256 is supported. This
     * parameter must be used together with "sseCKey."
     * 
     * @return Encryption algorithm type
     */
    @Deprecated
    public ServerAlgorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the encryption algorithm type. Only AES256 is supported. This
     * parameter must be used together with "sseCKey."
     * 
     * @param algorithm
     *            Encryption algorithm type
     */
    @Deprecated
    public void setAlgorithm(ServerAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Obtain the encryption algorithm type. Only AES256 is supported. This
     * parameter must be used together with "sseCKey".
     * 
     * @return Encryption algorithm type
     */
    public SSEAlgorithmEnum getSSEAlgorithm() {
        return sseAlgorithm;
    }

    /**
     * Obtain the key used in the SSE-C mode. The key is used to encrypt and
     * decrypt an object. The value is not encoded using Base64.
     * 
     * @return Key used in the SSE-C mode
     */
    public byte[] getSseCKey() {
        if (null != this.sseCKey) {
            return this.sseCKey.clone(); 
        }
        return new byte[0];
    }

    /**
     * Set the key used in the SSE-C mode. The key is used to encrypt and
     * decrypt an object. The value is not encoded using Base64.
     * 
     * @param sseCKey
     *            Key used in the SSE-C mode. The key is used to encrypt and
     *            decrypt an object.
     */
    @Deprecated
    public void setSseCKey(String sseCKey) {
        if (sseCKey != null) {
            this.sseCKey = sseCKey.getBytes(StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * Set the key used in the SSE-C mode. The key is used to encrypt and
     * decrypt an object. The value is not encoded using Base64.
     * 
     * @param sseCKey
     *            Key used in the SSE-C mode. The key is used to encrypt and
     *            decrypt an object.
     */
    public void setSseCKey(byte[] sseCKey) {
        if (null != sseCKey) {
            this.sseCKey = sseCKey.clone();
        } else {
            this.sseCKey = null;
        }
    }

    /**
     * Obtain the key used in the SSE-C mode. The key is used to encrypt and
     * decrypt an object. The value is a Base64-encoded value.
     * 
     * @return Key used in the SSE-C mode
     */
    public String getSseCKeyBase64() {
        return sseCKeyBase64;
    }

    /**
     * Set the key used in the SSE-C mode. The key is used to encrypt and
     * decrypt an object. The value is a Base64-encoded value.
     * 
     * @param sseCKeyBase64
     *            Key used in the SSE-C mode. The key is used to encrypt and
     *            decrypt an object.
     */
    public void setSseCKeyBase64(String sseCKeyBase64) {
        this.sseCKeyBase64 = sseCKeyBase64;
    }

    @Override
    public String toString() {
        return "SseCHeader [algorithm=" + algorithm + ", sseCKey=" + Arrays.toString(sseCKey) + ", sseCKeyBase64="
                + sseCKeyBase64 + "]";
    }

}
