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


package com.qcloud.cos;

import java.io.File;

import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.internal.COSDirect;
import com.qcloud.cos.internal.crypto.COSCryptoModule;
import com.qcloud.cos.internal.crypto.CryptoConfiguration;
import com.qcloud.cos.internal.crypto.CryptoModuleDispatcher;
import com.qcloud.cos.internal.crypto.EncryptionMaterialsProvider;
import com.qcloud.cos.internal.crypto.QCLOUDKMS;
import com.qcloud.cos.internal.crypto.TencentCloudKMSClient;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectId;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.CopyPartRequest;
import com.qcloud.cos.model.CopyPartResult;
import com.qcloud.cos.model.DeleteObjectRequest;
import com.qcloud.cos.model.EncryptedInitiateMultipartUploadRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.InstructionFileId;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutInstructionFileRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;

public class COSEncryptionClient extends COSClient implements COSEncryption {

    private final COSCryptoModule crypto;
    /**
     * True if the a default KMS client is constructed, which will be shut down when this instance
     * of COS encryption client is shutdown. False otherwise, which means the users who provided the
     * KMS client would be responsible to shut down the KMS client.
     */
    private final boolean isKMSClientInternal;
    private final QCLOUDKMS kms;

    public COSEncryptionClient(COSCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider, ClientConfig clientConfig,
            CryptoConfiguration cryptoConfig) {
        this(null, credentialsProvider, kekMaterialsProvider, clientConfig, cryptoConfig);
    }

    public COSEncryptionClient(QCLOUDKMS kms, COSCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider, ClientConfig clientConfig,
            CryptoConfiguration cryptoConfig) {
        super(credentialsProvider.getCredentials(), clientConfig);
        assertParameterNotNull(kekMaterialsProvider,
                "EncryptionMaterialsProvider parameter must not be null.");
        assertParameterNotNull(cryptoConfig, "CryptoConfiguration parameter must not be null.");
        this.isKMSClientInternal = kms == null;
        this.kms = isKMSClientInternal ?
                newTencentCloudKMSClient(credentialsProvider, clientConfig, cryptoConfig) : kms;
        this.crypto = new CryptoModuleDispatcher(this.kms, new COSDirectImpl(), credentialsProvider,
                kekMaterialsProvider, cryptoConfig);
    }

    private TencentCloudKMSClient newTencentCloudKMSClient(
            COSCredentialsProvider credentialsProvider,
            ClientConfig clientConfig,
            CryptoConfiguration cryptoConfig) {
        String region = cryptoConfig.getKmsRegion();
        if (region == null) {
            region = clientConfig.getRegion().getRegionName();
        }

        final TencentCloudKMSClient kmsClient = new TencentCloudKMSClient(credentialsProvider, region);

        return kmsClient;
    }

    private void assertParameterNotNull(Object parameterValue, String errorMessage) {
        if (parameterValue == null)
            throw new IllegalArgumentException(errorMessage);
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest req) {
        return crypto.putObjectSecurely(req.clone());
    }

    @Override
    public COSObject getObject(GetObjectRequest req) {
        return crypto.getObjectSecurely(req);
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest req, File dest) {
        return crypto.getObjectSecurely(req, dest);
    }

    @Override
    public void deleteObject(DeleteObjectRequest req) {
        // Delete the object
        super.deleteObject(req);
        // If it exists, delete the instruction file.
        InstructionFileId ifid =
                new COSObjectId(req.getBucketName(), req.getKey()).instructionFileId();

        DeleteObjectRequest instructionDeleteRequest = (DeleteObjectRequest) req.clone();
        instructionDeleteRequest.withBucketName(ifid.getBucket()).withKey(ifid.getKey());
        super.deleteObject(instructionDeleteRequest);
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(
            CompleteMultipartUploadRequest req) {
        return crypto.completeMultipartUploadSecurely(req);
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest req) {
        boolean isCreateEncryptionMaterial = true;
        if (req instanceof EncryptedInitiateMultipartUploadRequest) {
            EncryptedInitiateMultipartUploadRequest cryptoReq =
                    (EncryptedInitiateMultipartUploadRequest) req;
            isCreateEncryptionMaterial = cryptoReq.isCreateEncryptionMaterial();
        }
        return isCreateEncryptionMaterial ? crypto.initiateMultipartUploadSecurely(req)
                : super.initiateMultipartUpload(req);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Because the encryption process requires context from block N-1 in order to encrypt block N,
     * parts uploaded with the COSEncryptionClient (as opposed to the normal COSClient) must be
     * uploaded serially, and in order. Otherwise, the previous encryption context isn't available
     * to use when encrypting the current part.
     */
    @Override
    public UploadPartResult uploadPart(UploadPartRequest uploadPartRequest)
            throws CosClientException, CosServiceException {
        return crypto.uploadPartSecurely(uploadPartRequest);
    }

    @Override
    public CopyPartResult copyPart(CopyPartRequest copyPartRequest) {
        return crypto.copyPartSecurely(copyPartRequest);
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest req) {
        crypto.abortMultipartUploadSecurely(req);
    }

    /**
     * Creates a new crypto instruction file by re-encrypting the CEK of an existing encrypted COS
     * object with a new encryption material identifiable via a new set of material description.
     * <p>
     * User of this method is responsible for explicitly deleting/updating the instruction file so
     * created should the corresponding COS object is deleted/created.
     * 
     * @return the result of the put (instruction file) operation.
     */
    public PutObjectResult putInstructionFile(PutInstructionFileRequest req) {
        return crypto.putInstructionFileSecurely(req);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the a default internal KMS client has been constructed, it will also be shut down by
     * calling this method. Otherwise, users who provided the KMS client would be responsible to
     * shut down the KMS client extrinsic to this method.
     */
    @Override
    public void shutdown() {
        super.shutdown();
        if (isKMSClientInternal)
            kms.shutdown();
    }

    // /////////////////// Access to the methods in the super class //////////
    /**
     * An internal implementation used to provide limited but direct access to the underlying
     * methods of COSClient without any encryption or decryption operations.
     */
    private final class COSDirectImpl extends COSDirect {
        @Override
        public PutObjectResult putObject(PutObjectRequest req) {
            return COSEncryptionClient.super.putObject(req);
        }

        @Override
        public COSObject getObject(GetObjectRequest req) {
            return COSEncryptionClient.super.getObject(req);
        }

        @Override
        public ObjectMetadata getObject(GetObjectRequest req, File dest) {
            return COSEncryptionClient.super.getObject(req, dest);
        }

        @Override
        public CompleteMultipartUploadResult completeMultipartUpload(
                CompleteMultipartUploadRequest req) {
            return COSEncryptionClient.super.completeMultipartUpload(req);
        }

        @Override
        public InitiateMultipartUploadResult initiateMultipartUpload(
                InitiateMultipartUploadRequest req) {
            return COSEncryptionClient.super.initiateMultipartUpload(req);
        }

        @Override
        public UploadPartResult uploadPart(UploadPartRequest req)
                throws CosClientException, CosServiceException {
            return COSEncryptionClient.super.uploadPart(req);
        }

        @Override
        public CopyPartResult copyPart(CopyPartRequest req) {
            return COSEncryptionClient.super.copyPart(req);
        }

        @Override
        public void abortMultipartUpload(AbortMultipartUploadRequest req) {
            COSEncryptionClient.super.abortMultipartUpload(req);
        }
    }

}
