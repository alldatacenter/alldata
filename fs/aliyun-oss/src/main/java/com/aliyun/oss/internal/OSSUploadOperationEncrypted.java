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

package com.aliyun.oss.internal;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSEncryptionClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.crypto.ContentCryptoMaterial;
import com.aliyun.oss.crypto.ContentCryptoMaterialRW;
import com.aliyun.oss.crypto.EncryptionMaterials;
import com.aliyun.oss.crypto.MultipartUploadCryptoContext;
import com.aliyun.oss.model.*;

import java.io.*;

/**
 * OSSUploadOperationEncrypted
 */
public class OSSUploadOperationEncrypted extends OSSUploadOperation {
    private OSSEncryptionClient ossEncryptionClient;
    private EncryptionMaterials encryptionMaterials;

    public OSSUploadOperationEncrypted(OSSEncryptionClient ossEncryptionClient, EncryptionMaterials encryptionMaterials) {
        super(ossEncryptionClient.getMultipartOperation());
        this.ossEncryptionClient = ossEncryptionClient;
        this.encryptionMaterials = encryptionMaterials;
    }

    static class UploadCheckPointEncryption extends UploadCheckPoint {
        private MultipartUploadCryptoContext context;

        public MultipartUploadCryptoContext getContext() {
            return context;
        }

        public void setContext(MultipartUploadCryptoContext context) {
            this.context = context;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            final int prime = 31;
            result = prime * result + ((context == null) ? 0 : context.hashCode());
            return result;
        }

       @Override
        public void assign(UploadCheckPoint ucp) {
            assertUploadCheckPointIsLegal(ucp);
            super.assign(ucp);
            this.context = ((UploadCheckPointEncryption) ucp).context;
        }
    }

    @Override
    public UploadCheckPoint createUploadCheckPointWrap() {
        return new UploadCheckPointEncryption();
    }

    @Override
    public void loadUploadCheckPointWrap(UploadCheckPoint uploadCheckPoint, String checkpointFile) throws Throwable {
        assertUploadCheckPointIsLegal(uploadCheckPoint);
        uploadCheckPoint.load(checkpointFile);
        ContentCryptoMaterial cryptoMaterial = ((UploadCheckPointEncryption)uploadCheckPoint).getContext().getContentCryptoMaterial();

        if (cryptoMaterial instanceof ContentCryptoMaterialRW) {
            encryptionMaterials.decryptCEK(((ContentCryptoMaterialRW)cryptoMaterial));
        }
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUploadWrap(
            UploadCheckPoint uploadCheckPoint, InitiateMultipartUploadRequest initiateMultipartUploadRequest)
            throws OSSException, ClientException {
        assertUploadCheckPointIsLegal(uploadCheckPoint);

        MultipartUploadCryptoContext context = new MultipartUploadCryptoContext();
        context.setPartSize(uploadCheckPoint.originPartSize);
        File file = new File(uploadCheckPoint.uploadFile);
        context.setDataSize(file.length());

        InitiateMultipartUploadResult result = ossEncryptionClient.initiateMultipartUpload(initiateMultipartUploadRequest, context);
        ((UploadCheckPointEncryption) uploadCheckPoint).setContext(context);

        return result;
    }

    @Override
    public UploadPartResult uploadPartWrap(UploadCheckPoint uploadCheckPoint, UploadPartRequest uploadPartRequest)
            throws OSSException, ClientException {
        assertUploadCheckPointIsLegal(uploadCheckPoint);
        return ossEncryptionClient.uploadPart(uploadPartRequest, ((UploadCheckPointEncryption) uploadCheckPoint).getContext());
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUploadWrap(
            UploadCheckPoint uploadCheckPoint, CompleteMultipartUploadRequest request)
            throws OSSException, ClientException {
        assertUploadCheckPointIsLegal(uploadCheckPoint);
        return ossEncryptionClient.completeMultipartUpload(request, ((UploadCheckPointEncryption) uploadCheckPoint).getContext());
    }

    private static void assertUploadCheckPointIsLegal(UploadCheckPoint uploadCheckPoint) {
        if (!(uploadCheckPoint instanceof UploadCheckPointEncryption)) {
            throw new ClientException("the uploadCheckPoint of encryption client operation should instance of UploadCheckPointEncryption.");
        }
    }
}
