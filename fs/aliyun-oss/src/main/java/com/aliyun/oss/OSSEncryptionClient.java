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

package com.aliyun.oss;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.crypto.*;
import com.aliyun.oss.internal.OSSDownloadOperationEncrypted;
import com.aliyun.oss.internal.OSSUploadOperationEncrypted;
import com.aliyun.oss.model.*;
import static com.aliyun.oss.crypto.CryptoModuleBase.hasEncryptionInfo;

/**
 * OSSEncryptionClient is used to do client-side encryption.
 */
public class OSSEncryptionClient extends OSSClient {
    public static final String USER_AGENT_SUFFIX = "/OSSEncryptionClient";
    private final EncryptionMaterials encryptionMaterials;
    private final CryptoConfiguration cryptoConfig;
    private final OSSDirect ossDirect = new OSSDirectImpl();

    public OSSEncryptionClient(String endpoint, CredentialsProvider credsProvider, ClientConfiguration clientConfig,
            EncryptionMaterials encryptionMaterials, CryptoConfiguration cryptoConfig) {
        super(endpoint, credsProvider, clientConfig);
        assertParameterNotNull(credsProvider, "CredentialsProvider");
        assertParameterNotNull(encryptionMaterials, "EncryptionMaterials");
        if(encryptionMaterials instanceof KmsEncryptionMaterials) {
            ((KmsEncryptionMaterials)encryptionMaterials).setKmsCredentialsProvider(credsProvider);
        }
        this.cryptoConfig = cryptoConfig == null ? CryptoConfiguration.DEFAULT : cryptoConfig;
        this.encryptionMaterials = encryptionMaterials;
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest req) throws OSSException, ClientException {
        CryptoModule crypto = new CryptoModuleDispatcher(ossDirect, encryptionMaterials, cryptoConfig);
        return crypto.putObjectSecurely(req);
    }

    @Override
    public OSSObject getObject(GetObjectRequest req) throws OSSException, ClientException {
        if(req.isUseUrlSignature()) {
            throw new ClientException("Encryption client error, get object with url opreation is disabled in encryption client." + 
                    "Please use normal oss client method {@OSSClient#getObject(GetObjectRequest req)}."); 
        }
        CryptoModule crypto = new CryptoModuleDispatcher(ossDirect, encryptionMaterials, cryptoConfig);
        return crypto.getObjectSecurely(req);
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest req, File file) throws OSSException, ClientException {
        CryptoModule crypto = new CryptoModuleDispatcher(ossDirect, encryptionMaterials, cryptoConfig);
        return crypto.getObjectSecurely(req, file);
    }

    @Override
    public UploadFileResult uploadFile(UploadFileRequest uploadFileRequest) throws Throwable {
        OSSUploadOperationEncrypted ossUploadOperationEncrypted = new OSSUploadOperationEncrypted(this, encryptionMaterials);
        this.setUploadOperation(ossUploadOperationEncrypted);
        return super.uploadFile(uploadFileRequest);
    }

    @Override
    public DownloadFileResult downloadFile(DownloadFileRequest downloadFileRequest) throws Throwable {
        GenericRequest genericRequest = new GenericRequest(downloadFileRequest.getBucketName(), downloadFileRequest.getKey());
        String versionId = downloadFileRequest.getVersionId();
        if (versionId != null) {
            genericRequest.setVersionId(versionId);
        }
        Payer payer = downloadFileRequest.getRequestPayer();
        if (payer != null) {
            genericRequest.setRequestPayer(payer);
        }
        ObjectMetadata objectMetadata = getObjectMetadata(genericRequest);

        long objectSize = objectMetadata.getContentLength();
        if (objectSize <= downloadFileRequest.getPartSize()) {
            GetObjectRequest getObjectRequest = convertToGetObjectRequest(downloadFileRequest);
            objectMetadata = this.getObject(getObjectRequest, new File(downloadFileRequest.getDownloadFile()));
            DownloadFileResult downloadFileResult = new DownloadFileResult();
            downloadFileResult.setObjectMetadata(objectMetadata);
            return downloadFileResult;
        } else {
            if (!hasEncryptionInfo(objectMetadata)) {
                return super.downloadFile(downloadFileRequest);
            } else {
                long partSize = downloadFileRequest.getPartSize();
                if (0 != (partSize % CryptoScheme.BLOCK_SIZE) || partSize <= 0) {
                    throw new IllegalArgumentException("download file part size is not 16 bytes alignment.");
                }
                OSSDownloadOperationEncrypted ossDownloadOperationEncrypted = new OSSDownloadOperationEncrypted(this);
                this.setDownloadOperation(ossDownloadOperationEncrypted);
                return super.downloadFile(downloadFileRequest);
            }
        }
    }

    private static GetObjectRequest convertToGetObjectRequest(DownloadFileRequest downloadFileRequest) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(downloadFileRequest.getBucketName(),
                downloadFileRequest.getKey());
        getObjectRequest.setMatchingETagConstraints(downloadFileRequest.getMatchingETagConstraints());
        getObjectRequest.setNonmatchingETagConstraints(downloadFileRequest.getNonmatchingETagConstraints());
        getObjectRequest.setModifiedSinceConstraint(downloadFileRequest.getModifiedSinceConstraint());
        getObjectRequest.setUnmodifiedSinceConstraint(downloadFileRequest.getUnmodifiedSinceConstraint());
        getObjectRequest.setResponseHeaders(downloadFileRequest.getResponseHeaders());

        long[] range = downloadFileRequest.getRange();
        if (range != null) {
            getObjectRequest.setRange(range[0], range[1]);
        }

        String versionId = downloadFileRequest.getVersionId();
        if (versionId != null) {
            getObjectRequest.setVersionId(versionId);
        }

        Payer payer = downloadFileRequest.getRequestPayer();
        if (payer != null) {
            getObjectRequest.setRequestPayer(payer);
        }

        int limit = downloadFileRequest.getTrafficLimit();
        if (limit > 0) {
            getObjectRequest.setTrafficLimit(limit);
        }

        return getObjectRequest;
    }

    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request,
                                                                 MultipartUploadCryptoContext context)
                                                                 throws OSSException, ClientException {
        CryptoModule crypto = new CryptoModuleDispatcher(ossDirect, encryptionMaterials, cryptoConfig);
        return crypto.initiateMultipartUploadSecurely(request, context);
    }

    public UploadPartResult uploadPart(UploadPartRequest request, MultipartUploadCryptoContext context) {
        CryptoModule crypto = new CryptoModuleDispatcher(ossDirect, encryptionMaterials, cryptoConfig);
        return crypto.uploadPartSecurely(request, context);
    }

    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request, 
                                                                 MultipartUploadCryptoContext context) 
                                                                 throws OSSException, ClientException {
        return super.completeMultipartUpload(request);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////// Disabled api in encryption client///////////////////////////
    /**
     * Note: This method is disabled in encryption client.
     *  
     * @deprecated please use encryption client method
     *     {@link OSSEncryptionClient#initiateMultipartUpload(InitiateMultipartUploadRequest request, MultipartUploadCryptoContext context)}.             
     */
    @Override
    @Deprecated 
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws ClientException {
        throw new ClientException("Encryption client error, you should provide a multipart upload context to the encryption client. " + 
                 "Please use  encryption client method {@link OSSEncryptionClient#initiateMultipartUpload(InitiateMultipartUploadRequest request, " + 
                 "MultipartUploadCryptoContext context)}.");
    }

    /**
     * Note: This method is disabled in encryption client.
     *  
     * @deprecated please use encryption client method
     *     {@link OSSEncryptionClient#uploadPart(UploadPartRequest request, MultipartUploadCryptoContext context)}.             
     */
    @Override 
    @Deprecated
    public UploadPartResult uploadPart(UploadPartRequest request) throws ClientException {
        throw new ClientException("Encryption client error, you should provide a multipart upload context to the encryption client. " + 
                "Please use  encryption client method {@link OSSEncryptionClient#uploadPart(UploadPartRequest request, MultipartUploadCryptoContext context)}.");
    }
    
    /**
     * Note: This method is disabled in encryption client.
     *  
     * @deprecated please use normal oss client method
     *     {@link OSSClient#appendObject(AppendObjectRequest appendObjectRequest)}.             
     */
    @Override
    @Deprecated
    public AppendObjectResult appendObject(AppendObjectRequest appendObjectRequest) throws ClientException {
        throw new ClientException("Encryption client error, this method is disabled in encryption client." + 
            "Please use normal oss client method {@link OSSClient#appendObject(AppendObjectRequest appendObjectRequest)} method");
    } 
    
    /**
     * Note: This method is disabled in encryption client.
     *  
     * @deprecated please use normal oss client method
     *     {@link OSSClient#uploadPartCopy(UploadPartCopyRequest request)}.             
     */
    @Override
    @Deprecated
    public UploadPartCopyResult uploadPartCopy(UploadPartCopyRequest request) throws ClientException {
        throw new ClientException("Encryption client error, this method is disabled in encryption client." + 
                "Please use normal oss client method {@link OSSClient#uploadPartCopy(UploadPartCopyRequest request)}");
    }
    
    /**
     * Note: This method is disabled in encryption client.
     */
    @Override
    @Deprecated
    public PutObjectResult putObject(URL signedUrl, InputStream requestContent, long contentLength,
            Map<String, String> requestHeaders, boolean useChunkEncoding) throws ClientException {
        throw new ClientException("Encryption client error, this method is disabled in encryption client." + 
                "Please use normal oss client method {@link OSSClient#putObject(URL signedUrl, InputStream requestContent, "
                + "long contentLength, Map<String, String> requestHeaders, boolean useChunkEncoding)");
    }


    private final class OSSDirectImpl implements OSSDirect {
        @Override
        public ClientConfiguration getInnerClientConfiguration() {
            return OSSEncryptionClient.this.getClientConfiguration();
        }

        @Override
        public PutObjectResult putObject(PutObjectRequest putObjectRequest) {
            return OSSEncryptionClient.super.putObject(putObjectRequest);
        }

        @Override
        public OSSObject getObject(GetObjectRequest getObjectRequest) {
            return OSSEncryptionClient.super.getObject(getObjectRequest);
        }

        @Override
        public void abortMultipartUpload(AbortMultipartUploadRequest request) {
            OSSEncryptionClient.super.abortMultipartUpload(request);     
        }

        @Override
        public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) {
            return OSSEncryptionClient.super.completeMultipartUpload(request);
        }

        @Override
        public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) {
            return OSSEncryptionClient.super.initiateMultipartUpload(request);
        }

        @Override
        public UploadPartResult uploadPart(UploadPartRequest request) {
            return OSSEncryptionClient.super.uploadPart(request);
        }
    }

    private void assertParameterNotNull(Object parameterValue,
            String errorMessage) {
        if (parameterValue == null)
            throw new IllegalArgumentException(errorMessage);
    }
}
