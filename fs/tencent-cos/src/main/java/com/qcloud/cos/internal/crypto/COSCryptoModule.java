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

import java.io.File;

import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.CopyPartRequest;
import com.qcloud.cos.model.CopyPartResult;
import com.qcloud.cos.model.EncryptedGetObjectRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutInstructionFileRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.model.UploadPartResult;

public abstract class COSCryptoModule {
    /**
     * @return the result of the putting the COS object.
     */
    public abstract PutObjectResult putObjectSecurely(PutObjectRequest req);

    public abstract COSObject getObjectSecurely(GetObjectRequest req);

    public abstract ObjectMetadata getObjectSecurely(GetObjectRequest req,
            File dest);

    public abstract CompleteMultipartUploadResult completeMultipartUploadSecurely(
            CompleteMultipartUploadRequest req);

    public abstract InitiateMultipartUploadResult initiateMultipartUploadSecurely(
            InitiateMultipartUploadRequest req);

    public abstract UploadPartResult uploadPartSecurely(UploadPartRequest req);

    public abstract CopyPartResult copyPartSecurely(CopyPartRequest req);

    public abstract void abortMultipartUploadSecurely(AbortMultipartUploadRequest req);

    /**
     * @return the result of putting the instruction file in COS; or null if the
     *         specified COS object doesn't exist. The COS object can be
     *         subsequently retrieved using the new instruction file via the
     *         usual get operation by specifying a
     *         {@link EncryptedGetObjectRequest}.
     * 
     * @throws IllegalArgumentException
     *             if the specified COS object doesn't exist.
     * @throws SecurityException
     *             if the protection level of the material in the new
     *             instruction file is lower than that of the original.
     *             Currently, this means if the original material has been
     *             secured via authenticated encryption, then the new
     *             instruction file cannot be created via an COS encryption
     *             client configured with {@link CryptoMode#EncryptionOnly}.
     */
    public abstract PutObjectResult putInstructionFileSecurely(
            PutInstructionFileRequest req);

}
