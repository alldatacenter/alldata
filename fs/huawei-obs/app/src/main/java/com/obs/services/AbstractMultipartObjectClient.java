/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */


package com.obs.services;

import java.io.File;
import java.io.InputStream;

import com.obs.services.exception.ObsException;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.CopyPartRequest;
import com.obs.services.model.CopyPartResult;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListMultipartUploadsRequest;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.MultipartUploadListing;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;

public abstract class AbstractMultipartObjectClient extends AbstractObjectClient {
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#initiateMultipartUpload(com.obs.services.
     * model.InitiateMultipartUploadRequest)
     */
    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(final InitiateMultipartUploadRequest request)
            throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "InitiateMultipartUploadRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        return this.doActionWithResult("initiateMultipartUpload", request.getBucketName(),
                new ActionCallbackWithResult<InitiateMultipartUploadResult>() {
                    @Override
                    public InitiateMultipartUploadResult action() throws ServiceException {
                        return AbstractMultipartObjectClient.this.initiateMultipartUploadImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#abortMultipartUpload(com.obs.services.model.
     * AbortMultipartUploadRequest)
     */
    @Override
    public HeaderResponse abortMultipartUpload(final AbortMultipartUploadRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "AbortMultipartUploadRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        ServiceUtils.assertParameterNotNull(request.getUploadId(), "uploadId is null");
        return this.doActionWithResult("abortMultipartUpload", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractMultipartObjectClient.this.abortMultipartUploadImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#uploadPart(java.lang.String,
     * java.lang.String, java.lang.String, int, java.io.InputStream)
     */
    @Override
    public UploadPartResult uploadPart(String bucketName, String objectKey, String uploadId, int partNumber,
            InputStream input) throws ObsException {
        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setUploadId(uploadId);
        request.setPartNumber(partNumber);
        request.setInput(input);
        return this.uploadPart(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#uploadPart(java.lang.String,
     * java.lang.String, java.lang.String, int, java.io.File)
     */
    @Override
    public UploadPartResult uploadPart(String bucketName, String objectKey, String uploadId, int partNumber, File file)
            throws ObsException {
        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setUploadId(uploadId);
        request.setPartNumber(partNumber);
        request.setFile(file);
        return this.uploadPart(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#uploadPart(com.obs.services.model.
     * UploadPartRequest)
     */
    @Override
    public UploadPartResult uploadPart(final UploadPartRequest request) throws ObsException {

        ServiceUtils.assertParameterNotNull(request, "UploadPartRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        ServiceUtils.assertParameterNotNull(request.getUploadId(), "uploadId is null");
        return this.doActionWithResult("uploadPart", request.getBucketName(),
                new ActionCallbackWithResult<UploadPartResult>() {

                    @Override
                    public UploadPartResult action() throws ServiceException {
                        if (null != request.getInput() && null != request.getFile()) {
                            throw new ServiceException("Both input and file are set, only one is allowed");
                        }
                        return AbstractMultipartObjectClient.this.uploadPartImpl(request);
                    }
                });

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#copyPart(com.obs.services.model.
     * CopyPartRequest)
     */
    @Override
    public CopyPartResult copyPart(final CopyPartRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "CopyPartRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getSourceObjectKey(), "sourceObjectKey is null");
        ServiceUtils.assertParameterNotNull(request.getDestinationBucketName(), "destinationBucketName is null");
        ServiceUtils.assertParameterNotNull2(request.getDestinationObjectKey(), "destinationObjectKey is null");
        ServiceUtils.assertParameterNotNull(request.getUploadId(), "uploadId is null");
        return this.doActionWithResult("copyPart", request.getSourceBucketName(),
                new ActionCallbackWithResult<CopyPartResult>() {

                    @Override
                    public CopyPartResult action() throws ServiceException {
                        return AbstractMultipartObjectClient.this.copyPartImpl(request);
                    }
                });

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#completeMultipartUpload(com.obs.services.
     * model.CompleteMultipartUploadRequest)
     */
    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(final CompleteMultipartUploadRequest request)
            throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "CompleteMultipartUploadRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        ServiceUtils.assertParameterNotNull(request.getUploadId(), "uploadId is null");
        return this.doActionWithResult("completeMultipartUpload", request.getBucketName(),
                new ActionCallbackWithResult<CompleteMultipartUploadResult>() {
                    @Override
                    public CompleteMultipartUploadResult action() throws ServiceException {
                        return AbstractMultipartObjectClient.this.completeMultipartUploadImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#listParts(com.obs.services.model.
     * ListPartsRequest)
     */
    @Override
    public ListPartsResult listParts(final ListPartsRequest request) throws ObsException {

        ServiceUtils.assertParameterNotNull(request, "ListPartsRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        ServiceUtils.assertParameterNotNull(request.getUploadId(), "uploadId is null");
        return this.doActionWithResult("listParts", request.getBucketName(),
                new ActionCallbackWithResult<ListPartsResult>() {

                    @Override
                    public ListPartsResult action() throws ServiceException {
                        return AbstractMultipartObjectClient.this.listPartsImpl(request);
                    }
                });

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#listMultipartUploads(com.obs.services.model.
     * ListMultipartUploadsRequest)
     */
    @Override
    public MultipartUploadListing listMultipartUploads(final ListMultipartUploadsRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "ListMultipartUploadsRequest is null");
        return this.doActionWithResult("listMultipartUploads", request.getBucketName(),
                new ActionCallbackWithResult<MultipartUploadListing>() {

                    @Override
                    public MultipartUploadListing action() throws ServiceException {
                        return AbstractMultipartObjectClient.this.listMultipartUploadsImpl(request);
                    }
                });

    }
}
