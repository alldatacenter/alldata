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

import java.security.SecureRandom;

/**
 * COS cryptographic scheme that includes the content crypto scheme and key
 * wrapping scheme (for the content-encrypting-key).
 */
final class COSCryptoScheme {
    static final String AES = "AES"; 
    static final String RSA = "RSA"; 
    private static final SecureRandom srand = new SecureRandom();
    private final COSKeyWrapScheme kwScheme;

    private final ContentCryptoScheme contentCryptoScheme;

    private COSCryptoScheme(ContentCryptoScheme contentCryptoScheme,
            COSKeyWrapScheme kwScheme) {
        this.contentCryptoScheme = contentCryptoScheme;
        this.kwScheme = kwScheme;
    }

    SecureRandom getSecureRandom() { return srand; }
    
    ContentCryptoScheme getContentCryptoScheme() {
        return contentCryptoScheme;
    }

    COSKeyWrapScheme getKeyWrapScheme() { return kwScheme; }

    /**
     * Convenient method.
     */
    static boolean isAesGcm(String cipherAlgorithm) {
        return ContentCryptoScheme.AES_GCM.getCipherAlgorithm().equals(cipherAlgorithm);
    }

    static COSCryptoScheme from(CryptoMode mode) {
        switch (mode) {
        case AesCbcEncryption:
            return new COSCryptoScheme(ContentCryptoScheme.AES_CBC,
                    new COSKeyWrapScheme());
        case AesCtrEncryption:
            return new COSCryptoScheme(ContentCryptoScheme.AES_CTR,
                    new COSKeyWrapScheme());
        case AuthenticatedEncryption:
        case StrictAuthenticatedEncryption:
            return new COSCryptoScheme(ContentCryptoScheme.AES_GCM,
                    new COSKeyWrapScheme());
        default:
            throw new IllegalStateException();
        }
    }
}