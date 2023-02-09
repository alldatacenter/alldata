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


package com.qcloud.cos.transfer;

import static com.qcloud.cos.event.SDKProgressPublisher.publishProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.qcloud.cos.COS;
import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.event.ProgressListenerChain;
import com.qcloud.cos.internal.CopyImpl;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.CopyObjectResult;
import com.qcloud.cos.model.CopyPartRequest;
import com.qcloud.cos.model.CopyResult;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.Transfer.TransferState;
import com.qcloud.cos.Headers;

public class CopyCallable implements Callable<CopyResult> {

    /**
     * A reference to the COS client using which copy or copy part requests are initiated.
     */
    private final COS cos;
    /** Thread pool used during multi-part copy is performed. */
    private final ExecutorService threadPool;
    /** A reference to the original copy request received. */
    private final CopyObjectRequest copyObjectRequest;
    /** Upload id to be used when sending copy part requests. */
    private String multipartUploadId;
    /** Metadata of the object in the source bucket to be copied. */
    private final ObjectMetadata metadata;
    private final CopyImpl copy;

    private static final Log log = LogFactory.getLog(CopyCallable.class);
    /**
     * <code>TransferManager</code> configuration that provides details on when to use multi-part
     * copy, part size etc.,
     */
    private final TransferManagerConfiguration configuration;
    /**
     * A list of future objects to be returned when multi-part copy is initiated.
     */
    private final List<Future<PartETag>> futures = new ArrayList<Future<PartETag>>();

    private final ProgressListenerChain listenerChain;

    public CopyCallable(TransferManager transferManager, ExecutorService threadPool, CopyImpl copy,
            CopyObjectRequest copyObjectRequest, ObjectMetadata metadata,
            ProgressListenerChain progressListenerChain) {
        this.cos = transferManager.getCOSClient();
        this.configuration = transferManager.getConfiguration();
        this.threadPool = threadPool;
        this.copyObjectRequest = copyObjectRequest;
        this.metadata = metadata;
        this.listenerChain = progressListenerChain;
        this.copy = copy;
    }

    List<Future<PartETag>> getFutures() {
        return futures;
    }

    String getMultipartUploadId() {
        return multipartUploadId;
    }

    /**
     * Returns true if this CopyCallable is processing a multi-part copy.
     *
     * @return True if this CopyCallable is processing a multi-part copy.
     */
    public boolean isMultipartCopy() {
        Region sourceRegion = copyObjectRequest.getSourceBucketRegion();
        Region destRegion = cos.getClientConfig().getRegion();
        String sourceStorageClass = (String)metadata.getRawMetadataValue(Headers.STORAGE_CLASS);
        String destStorageClass = copyObjectRequest.getStorageClass();
        if(sourceStorageClass == null) {
            sourceStorageClass = "Standard";
        }

        // 如果源和目的对象的存储类型相同
        if(sourceStorageClass.equalsIgnoreCase(destStorageClass)) {
            // 如果没有设置source region, 表示使用的和clientconfig里面同一region,
            // 或者设置了相同的region，使用put object copy即可
            if(sourceRegion == null || sourceRegion.equals(destRegion)) {
                return false;
            }
        }

        return (metadata.getContentLength() > configuration.getMultipartCopyThreshold());
    }

    public CopyResult call() throws Exception {
        copy.setState(TransferState.InProgress);
        if (isMultipartCopy()) {
            publishProgress(listenerChain, ProgressEventType.TRANSFER_STARTED_EVENT);
            copyInParts();
            return null;
        } else {
            return copyInOneChunk();
        }
    }

    /**
     * Performs the copy of the COS object from source bucket to destination bucket. The COS object
     * is copied to destination in one single request.
     *
     * @returns CopyResult response information from the server.
     */
    private CopyResult copyInOneChunk() {
        CopyObjectResult copyObjectResult = cos.copyObject(copyObjectRequest);
        if (copyObjectResult == null) {
            return null;
        }

        CopyResult copyResult = new CopyResult();
        copyResult.setSourceBucketName(copyObjectRequest.getSourceBucketName());
        copyResult.setSourceKey(copyObjectRequest.getSourceKey());
        copyResult.setDestinationBucketName(copyObjectRequest.getDestinationBucketName());
        copyResult.setDestinationKey(copyObjectRequest.getDestinationKey());
        copyResult.setETag(copyObjectResult.getETag());
        copyResult.setVersionId(copyObjectResult.getVersionId());
        copyResult.setRequestId(copyObjectResult.getRequestId());
        copyResult.setDateStr(copyObjectResult.getDateStr());
        copyResult.setCrc64Ecma(copyObjectResult.getCrc64Ecma());
        return copyResult;
    }

    /**
     * Performs the copy of an COS object from source bucket to destination bucket as multiple copy
     * part requests. The information about the part to be copied is specified in the request as a
     * byte range (first-last)
     *
     * @throws Exception Any Exception that occurs while carrying out the request.
     */
    private void copyInParts() throws Exception {
        multipartUploadId = initiateMultipartUpload(copyObjectRequest);

        long optimalPartSize = getOptimalPartSize(metadata.getContentLength());

        try {
            CopyPartRequestFactory requestFactory = new CopyPartRequestFactory(copyObjectRequest,
                    multipartUploadId, optimalPartSize, metadata.getContentLength());
            copyPartsInParallel(requestFactory);
        } catch (Exception e) {
            publishProgress(listenerChain, ProgressEventType.TRANSFER_FAILED_EVENT);
            abortMultipartCopy();
            throw new RuntimeException("Unable to perform multipart copy", e);
        }
    }

    /**
     * Computes and returns the optimal part size for the copy operation.
     */
    private long getOptimalPartSize(long contentLengthOfSource) {

        long optimalPartSize = TransferManagerUtils.calculateOptimalPartSizeForCopy(
                copyObjectRequest, configuration, contentLengthOfSource);
        log.debug("Calculated optimal part size: " + optimalPartSize);
        return optimalPartSize;
    }

    /**
     * Submits a callable for each part to be copied to our thread pool and records its
     * corresponding Future.
     */
    private void copyPartsInParallel(CopyPartRequestFactory requestFactory) {
        while (requestFactory.hasMoreRequests()) {
            if (threadPool.isShutdown())
                throw new CancellationException("TransferManager has been shutdown");
            CopyPartRequest request = requestFactory.getNextCopyPartRequest();
            futures.add(threadPool.submit(new CopyPartCallable(cos, request)));
        }
    }

    /**
     * Initiates a multipart upload and returns the upload id
     */
    private String initiateMultipartUpload(CopyObjectRequest origReq) {

        InitiateMultipartUploadRequest req =
                new InitiateMultipartUploadRequest(origReq.getDestinationBucketName(),
                        origReq.getDestinationKey())
                                .withCannedACL(origReq.getCannedAccessControlList())
                                .withAccessControlList(origReq.getAccessControlList())
                                .withStorageClass(origReq.getStorageClass())
                                .withGeneralProgressListener(origReq.getGeneralProgressListener());

        ObjectMetadata newObjectMetadata = origReq.getNewObjectMetadata();
        if (newObjectMetadata == null) {
            newObjectMetadata = new ObjectMetadata();
        }
        if (newObjectMetadata.getContentType() == null) {
            newObjectMetadata.setContentType(metadata.getContentType());
        }

        req.setObjectMetadata(newObjectMetadata);
        TransferManagerUtils.populateEndpointAddr(origReq, req);

        String uploadId = cos.initiateMultipartUpload(req).getUploadId();
        log.debug("Initiated new multipart upload: " + uploadId);

        return uploadId;
    }


    private void abortMultipartCopy() {
        try {
            AbortMultipartUploadRequest abortRequest =
                    new AbortMultipartUploadRequest(copyObjectRequest.getDestinationBucketName(),
                            copyObjectRequest.getDestinationKey(), multipartUploadId);
            TransferManagerUtils.populateEndpointAddr(copyObjectRequest, abortRequest);
            cos.abortMultipartUpload(abortRequest);
        } catch (Exception e) {
            log.info(
                    "Unable to abort multipart upload, you may need to manually remove uploaded parts: "
                            + e.getMessage(),
                    e);
        }
    }
}
