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

import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.exception.CosClientException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.kms.v20190118.KmsClient;
import com.tencentcloudapi.kms.v20190118.models.DecryptRequest;
import com.tencentcloudapi.kms.v20190118.models.DecryptResponse;
import com.tencentcloudapi.kms.v20190118.models.EncryptRequest;
import com.tencentcloudapi.kms.v20190118.models.EncryptResponse;
import com.tencentcloudapi.kms.v20190118.models.GenerateDataKeyRequest;
import com.tencentcloudapi.kms.v20190118.models.GenerateDataKeyResponse;

/**
 * Client for accessing TencentCloud KMS.
 */
public class TencentCloudKMSClient implements QCLOUDKMS {
    private final KmsClient kmsClient;

    public TencentCloudKMSClient(COSCredentialsProvider cosCredentialsProvider, String region) {
        COSCredentials cosCredentials = cosCredentialsProvider.getCredentials();
        String secretId = cosCredentials.getCOSAccessKeyId();
        String secretKey = cosCredentials.getCOSSecretKey();

        Credential credential = new Credential(secretId, secretKey);
        this.kmsClient = new KmsClient(credential, region);
    } 

    /**
     * Generates a unique symmetric data key for client-side encryption. This operation returns a plaintext copy of the
     * data key and a copy that is encrypted under a customer master key (CMK) that you specify. You can use the
     * plaintext key to encrypt your data outside of KMS and store the encrypted data key with the encrypted data.
     * 
     * @param generateDataKeyRequest GenerateDataKeyRequest
     * @return GenerateDataKeyResponse
     * @throws CosClientException
     */
    @Override
    public GenerateDataKeyResponse generateDataKey(GenerateDataKeyRequest generateDataKeyRequest) {
        try {
            GenerateDataKeyResponse generateDataKeyRes = this.kmsClient.GenerateDataKey(generateDataKeyRequest);
            return generateDataKeyRes;
        } catch (TencentCloudSDKException e) {
            throw new CosClientException("TencentCloudKMS Service got exception while GenerateDataKey", e);
        }
    }


    /**
    * Encrypts plaintext into ciphertext by using a customer master key (CMK).
    *
    * @param encryptRequest EncryptRequest
    * @return EncryptResponse
    * @throws TencentCloudSDKException
    */
    @Override
    public EncryptResponse encrypt(EncryptRequest encryptRequest) {
        try {
            EncryptResponse encryptResponse = this.kmsClient.Encrypt(encryptRequest);
            return encryptResponse;
        } catch (TencentCloudSDKException e) {
            throw new CosClientException("TencentCloudKMS Service got exception while Encrypt", e);
        }
    }

    /**
    * Decrypts ciphertext that was encrypted by a KMS customer master key (CMK) using any of the following
    * operations:
    *
    * generateDataKey
    *
    * @param decryptRequest DecryptRequest
    * @return DecrypResponse
    * @throws CosClientException
    */
    public DecryptResponse decrypt(DecryptRequest decryptRequest) {
        try{
            DecryptResponse decryptResponse = this.kmsClient.Decrypt(decryptRequest);
            return decryptResponse;
        } catch (TencentCloudSDKException e) {
            throw new CosClientException("TencentCloudKMS Service got exception while Decrypt", e);
        }
    }

    @Override
    public void shutdown() {
    }
}
