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

import static com.qcloud.cos.internal.crypto.CryptoMode.AuthenticatedEncryption;

import java.io.File;

import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.internal.COSDirect;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.CopyPartRequest;
import com.qcloud.cos.model.CopyPartResult;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutInstructionFileRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;

public class CryptoModuleDispatcher extends COSCryptoModule {
    private final CryptoMode defaultCryptoMode;
    /** Authenticated encryption (AE) cryptographic module. */
    private final COSCryptoModuleAE ae;

    public CryptoModuleDispatcher(QCLOUDKMS kms, COSDirect cos,
            COSCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider encryptionMaterialsProvider,
            CryptoConfiguration cryptoConfig) {
        cryptoConfig = cryptoConfig.clone(); // make a clone
        CryptoMode cryptoMode = cryptoConfig.getCryptoMode();
        if (cryptoMode == null) {
            cryptoMode = AuthenticatedEncryption;
            cryptoConfig.setCryptoMode(cryptoMode); // defaults to AE
        }
        cryptoConfig = cryptoConfig.readOnly(); // make read-only
        this.defaultCryptoMode = cryptoConfig.getCryptoMode();
        switch (this.defaultCryptoMode) {
            case StrictAuthenticatedEncryption:
                this.ae = new COSCryptoModuleAEStrict(kms, cos, credentialsProvider,
                        encryptionMaterialsProvider, cryptoConfig);
                break;
            case AuthenticatedEncryption:
                this.ae = new COSCryptoModuleAE(kms, cos, credentialsProvider,
                        encryptionMaterialsProvider, cryptoConfig);
                break;
            case AesCtrEncryption:
                this.ae = new COSCryptoModuleAE(kms, cos, credentialsProvider,
                        encryptionMaterialsProvider, cryptoConfig);
                break;
            case AesCbcEncryption:
                this.ae = new COSCryptoModuleAECbc(kms, cos, credentialsProvider,
                        encryptionMaterialsProvider, cryptoConfig);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public PutObjectResult putObjectSecurely(PutObjectRequest putObjectRequest) {
        return ae.putObjectSecurely(putObjectRequest);
    }

    @Override
    public COSObject getObjectSecurely(GetObjectRequest req) {
        // AE module can handle COS objects encrypted in either AE format
        return ae.getObjectSecurely(req);
    }

    @Override
    public ObjectMetadata getObjectSecurely(GetObjectRequest req, File destinationFile) {
        // AE module can handle COS objects encrypted in either AE or EO format
        return ae.getObjectSecurely(req, destinationFile);
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUploadSecurely(
            CompleteMultipartUploadRequest req) throws CosClientException, CosServiceException {
        return ae.completeMultipartUploadSecurely(req);
    }

    @Override
    public void abortMultipartUploadSecurely(AbortMultipartUploadRequest req) {
        ae.abortMultipartUploadSecurely(req);
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUploadSecurely(
            InitiateMultipartUploadRequest req) throws CosClientException, CosServiceException {
        return ae.initiateMultipartUploadSecurely(req);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * <b>NOTE:</b> Because the encryption process requires context from block N-1 in order to
     * encrypt block N, parts uploaded with the COSEncryptionClient (as opposed to the normal
     * COSClient) must be uploaded serially, and in order. Otherwise, the previous encryption
     * context isn't available to use when encrypting the current part.
     */
    @Override
    public UploadPartResult uploadPartSecurely(UploadPartRequest req)
            throws CosClientException, CosServiceException {
        return ae.uploadPartSecurely(req);
    }

    @Override
    public CopyPartResult copyPartSecurely(CopyPartRequest req) {
        return ae.copyPartSecurely(req);
    }

    @Override
    public PutObjectResult putInstructionFileSecurely(PutInstructionFileRequest req) {
        return ae.putInstructionFileSecurely(req);
    }
}
