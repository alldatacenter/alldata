/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.internal.crypto;

/**
 * Denotes the different cryptographic modes available for securing an COS object via client-side
 * encryption. Crypto mode can be configured via {@link CryptoConfiguration} when the COS encryption
 * client is constructed.
 */
public enum CryptoMode {
    /**
     * Authenticated encryption mode using AES/CTR. If you want to encrypt with Java SDK and decrypt
     * with other COS SDKs, you must use this mode.
     */
    AesCtrEncryption,
    /**
     * Authenticated encryption mode using AES/CBC.
     */
    AesCbcEncryption,
    /**
     * Authenticated encryption mode using AES/GCM, AESWrap, etc. Please note the limitation on the
     * maximum message size in bytes that can be encrypted under this mode is 2^36-32, or ~64G, due
     * to the security limitation of AES/GCM as recommended by NIST.
     */
    AuthenticatedEncryption,
    /**
     * Strictly enforce the use of authenticated encryption via AES/GCM, AESWrap, etc., and will
     * throw security exception if an COS object retrieved is found to be not protected using
     * authenticated encryption. This means range-get operation is not supported in this mode, since
     * range-get is not authenticated.
     * <p>
     * Please note the limitation on the maximum message size in bytes that can be encrypted under
     * this mode is 2^36-32, or ~64G, due to the security limitation of AES/GCM as recommended by
     * NIST.
     */
    StrictAuthenticatedEncryption,;
}
