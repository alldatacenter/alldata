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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.qcloud.cos.COS;
import com.qcloud.cos.event.ProgressListenerChain;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.UploadResult;

/**
 * Initiates a complete multi-part upload request for a
 * TransferManager multi-part parallel upload.
 */
public class CompleteMultipartUpload implements Callable<UploadResult> {

    /** The upload id associated with the multi-part upload. */
    private final String uploadId;

    /**
     * The reference to underlying Qcloud COS client to be used for initiating
     * requests to Qcloud COS.
     */
    private final COS cos;

    /** The reference to the request initiated by the user. */
    private final PutObjectRequest origReq;

    /** The futures of threads that upload individual parts. */
    private final List<Future<PartETag>> futures;

    /**
     * The eTags of the parts that had been successfully uploaded before
     * resuming a paused upload.
     */
    private final List<PartETag> eTagsBeforeResume;

    /** The monitor to which the upload progress has to be communicated. */
    private final UploadMonitor monitor;

    /** The listener where progress of the upload needs to be published. */
    private final ProgressListenerChain listener;

    public CompleteMultipartUpload(String uploadId, COS cos,
            PutObjectRequest putObjectRequest, List<Future<PartETag>> futures,
            List<PartETag> eTagsBeforeResume, ProgressListenerChain progressListenerChain,
            UploadMonitor monitor) {
        this.uploadId = uploadId;
        this.cos = cos;
        this.origReq = putObjectRequest;
        this.futures = futures;
        this.eTagsBeforeResume = eTagsBeforeResume;
        this.listener = progressListenerChain;
        this.monitor = monitor;
    }

    @Override
    public UploadResult call() throws Exception {
        CompleteMultipartUploadResult res;

        try {
            CompleteMultipartUploadRequest req = new CompleteMultipartUploadRequest(
                    origReq.getBucketName(), origReq.getKey(), uploadId,
                    collectPartETags())
                .withGeneralProgressListener(origReq.getGeneralProgressListener());

            ObjectMetadata origMeta = origReq.getMetadata();
            if (origMeta != null) {
                ObjectMetadata objMeta = req.getObjectMetadata();
                if (objMeta == null) {
                    objMeta = new ObjectMetadata();
                }

                objMeta.setUserMetadata(origMeta.getUserMetadata());
                req.setObjectMetadata(objMeta);
            }
            if(origReq.getPicOperations() != null) {
                req.setPicOperations(origReq.getPicOperations());
            }

            TransferManagerUtils.populateEndpointAddr(origReq, req);
            res = cos.completeMultipartUpload(req);
        } catch (Exception e) {
            monitor.uploadFailed();
            throw e;
        }

        UploadResult uploadResult = new UploadResult();
        uploadResult.setBucketName(origReq
                .getBucketName());
        uploadResult.setKey(origReq.getKey());
        uploadResult.setETag(res.getETag());
        uploadResult.setVersionId(res.getVersionId());
        uploadResult.setRequestId(res.getRequestId());
        uploadResult.setDateStr(res.getDateStr());
        uploadResult.setCrc64Ecma(res.getCrc64Ecma());
        uploadResult.setCiUploadResult(res.getCiUploadResult());

        monitor.uploadComplete();

        return uploadResult;
    }

    /**
     * Collects the Part ETags for initiating the complete multi-part upload
     * request. This is blocking as it waits until all the upload part threads
     * complete.
     */
    private List<PartETag> collectPartETags() {

        final List<PartETag> partETags = new ArrayList<PartETag>();
        partETags.addAll(eTagsBeforeResume);
        for (Future<PartETag> future : futures) {
            try {
                partETags.add(future.get());
            } catch (Exception e) {
                throw new CosClientException(
                        "Unable to complete multi-part upload. Individual part upload failed : "
                                + e.getCause().getMessage(), e.getCause());
            }
        }
        return partETags;
    }
}
