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

import com.tencentcloudapi.kms.v20190118.models.DecryptRequest;
import com.tencentcloudapi.kms.v20190118.models.DecryptResponse;
import com.tencentcloudapi.kms.v20190118.models.EncryptRequest;
import com.tencentcloudapi.kms.v20190118.models.EncryptResponse;
import com.tencentcloudapi.kms.v20190118.models.GenerateDataKeyRequest;
import com.tencentcloudapi.kms.v20190118.models.GenerateDataKeyResponse;

public interface QCLOUDKMS {

    /**
    * Generates a unique symmetric data key for client-side encryption. This operation returns a plaintext copy of the
    * data key and a copy that is encrypted under a customer master key (CMK) that you specify. You can use the
    * plaintext key to encrypt your data outside of KMS and store the encrypted data key with the encrypted data.
    */
    GenerateDataKeyResponse generateDataKey(GenerateDataKeyRequest generateDataKeyRequest);

    /**
    * Encrypts plaintext into ciphertext by using a customer master key (CMK).
    */
    EncryptResponse encrypt(EncryptRequest encryptRequest);
    
    /**
    * Decrypts ciphertext that was encrypted by a KMS customer master key (CMK) using any of the following
    * operations:
    *
    * encrypt
    *
    * generateDataKey
    *
    */
    DecryptResponse decrypt(DecryptRequest decryptRequest);

    /**
     * Shuts down this client object, releasing any resources that might be held open. This is an optional method, and
     * callers are not expected to call it, but can if they want to explicitly release any open resources. Once a client
     * has been shutdown, it should not be used to make any more requests.
     */
    void shutdown();
}
