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


package com.qcloud.cos.internal;

import java.io.File;
import java.io.FileInputStream;

import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.SSECustomerKey;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferManagerUtils;

/**
 * Factory for creating all the individual UploadPartRequest objects for a
 * multipart upload.
 * <p>
 * This allows us to delay creating each UploadPartRequest until we're ready for
 * it, instead of immediately creating thousands of UploadPartRequest objects
 * for each large upload, when we won't need most of those request objects for a
 * while.
 */
public class UploadPartRequestFactory {
    private final String bucketName;
    private final String key;
    private final String uploadId;
    private final long optimalPartSize;
    private final File file;
    private final PutObjectRequest origReq;
    private int partNumber = 1;
    private long offset = 0;
    private long remainingBytes;
    private SSECustomerKey sseCustomerKey;
    private final int totalNumberOfParts;
    /**
     * traffic limit speed in second, the unit is bit/s
     */
    private int trafficLimit = 0;

    /**
     * Wrapped to provide necessary mark-and-reset support for the underlying
     * input stream. In particular, it provides support for unlimited
     * mark-and-reset if the underlying stream is a {@link FileInputStream}.
     */
    private ReleasableInputStream wrappedStream;

    public UploadPartRequestFactory(PutObjectRequest origReq, String uploadId, long optimalPartSize) {
        this.origReq = origReq;
        this.uploadId = uploadId;
        this.optimalPartSize = optimalPartSize;
        this.bucketName = origReq.getBucketName();
        this.key = origReq.getKey();
        this.file = TransferManagerUtils.getRequestFile(origReq);
        this.remainingBytes = TransferManagerUtils.getContentLength(origReq);
        this.sseCustomerKey = origReq.getSSECustomerKey();
        this.totalNumberOfParts = (int) Math.ceil((double) this.remainingBytes
                / this.optimalPartSize);
        if (origReq.getInputStream() != null) {
            wrappedStream = ReleasableInputStream.wrap(origReq.getInputStream());
        }
        this.trafficLimit = origReq.getTrafficLimit();
    }

    public synchronized boolean hasMoreRequests() {
        return (remainingBytes > 0);
    }

    public synchronized UploadPartRequest getNextUploadPartRequest() {
        long partSize = Math.min(optimalPartSize, remainingBytes);
        boolean isLastPart = (remainingBytes - partSize <= 0);

        UploadPartRequest req = null;
        if (wrappedStream != null) {
            req = new UploadPartRequest()
                .withBucketName(bucketName)
                .withKey(key)
                .withUploadId(uploadId)
                .withInputStream(new InputSubstream(wrappedStream, 0, partSize, isLastPart))
                .withPartNumber(partNumber++)
                .withPartSize(partSize)
                .withTrafficLimit(trafficLimit);
        } else {
            req = new UploadPartRequest()
                .withBucketName(bucketName)
                .withKey(key)
                .withUploadId(uploadId)
                .withFile(file)
                .withFileOffset(offset)
                .withPartNumber(partNumber++)
                .withPartSize(partSize)
                .withTrafficLimit(trafficLimit);
        }
        TransferManager.appendMultipartUserAgent(req);

        if (sseCustomerKey != null) req.setSSECustomerKey(sseCustomerKey);
        TransferManagerUtils.populateEndpointAddr(origReq, req);

        offset += partSize;
        remainingBytes -= partSize;

        req.setLastPart(isLastPart);

        req.withGeneralProgressListener(origReq.getGeneralProgressListener())
           ;
        req.getRequestClientOptions().setReadLimit(origReq.getReadLimit());
        return req;
    }

    public int getTotalNumberOfParts() {
        return totalNumberOfParts;
    }

}
