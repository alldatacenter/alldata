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


package com.qcloud.cos.model;

import java.io.Serializable;

public class SSECOSKeyManagementParams implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * The COS Key Management Key id to be used for Server Side Encryption of
     * the Qcloud COS object.
     */
    private final String COSKmsKeyId;

    private final String encryptionContext;
    /**
     * Constructs a new instance of SSECOSKeyManagementParams. The default COS
     * KMS Key id is used for encryption.
     */
    public SSECOSKeyManagementParams() {
        this.COSKmsKeyId = null;
        this.encryptionContext = null;
    }

    /**
     * Constructs a new instance of SSECOSKeyManagementParams with the user
     * specified COS Key Management System Key Id.
     */
    public SSECOSKeyManagementParams(String COSKmsKeyId, String encryptionContext) {
        this.COSKmsKeyId = COSKmsKeyId;
        this.encryptionContext = encryptionContext;
    }

    /**
     * Returns the COS Key Management System Key Id used for encryption. Returns
     * null if default Key Id is used.
     */
    public String getCOSKmsKeyId() {
        return COSKmsKeyId;
    }

    /**
     * Returns the scheme used for encrypting the Qcloud COS object. Currently
     * the encryption is always "COS:kms".
     */
    public String getEncryption() {
        return SSEAlgorithm.KMS.getAlgorithm();
    }

    public String getEncryptionContext() {
        return encryptionContext;
    }
}
