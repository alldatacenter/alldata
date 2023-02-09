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
import com.obs.services.model.AccessControlList;
import com.obs.services.model.AppendObjectRequest;
import com.obs.services.model.AppendObjectResult;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.CopyObjectRequest;
import com.obs.services.model.CopyObjectResult;
import com.obs.services.model.DeleteObjectRequest;
import com.obs.services.model.DeleteObjectResult;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.DeleteObjectsResult;
import com.obs.services.model.GetObjectAclRequest;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;
import com.obs.services.model.OptionsInfoRequest;
import com.obs.services.model.OptionsInfoResult;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RestoreObjectResult;
import com.obs.services.model.SetObjectAclRequest;
import com.obs.services.model.SetObjectMetadataRequest;
import com.obs.services.model.select.SelectObjectRequest;
import com.obs.services.model.select.SelectObjectResult;

public abstract class AbstractObjectClient extends AbstractBucketAdvanceClient {
    @Override
    public boolean doesObjectExist(final String bucketName, final String objectKey) throws ObsException {
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(bucketName, objectKey);
        return this.doesObjectExist(request);
    }

    @Override
    public boolean doesObjectExist(final GetObjectMetadataRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request.getBucketName(), "bucket is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        return doActionWithResult("doesObjectExist", request.getBucketName(), new ActionCallbackWithResult<Boolean>() {
            @Override
            public Boolean action() throws ServiceException {
                try {
                    return AbstractObjectClient.this.doesObjectExistImpl(request);
                } catch (ServiceException e) {
                    if (AbstractObjectClient.this.isAuthTypeNegotiation() && e.getResponseCode() == 400
                            && "Unsupported Authorization Type".equals(e.getErrorMessage())
                            && AbstractObjectClient.this.getProviderCredentials()
                            .getLocalAuthType(request.getBucketName()) == AuthTypeEnum.OBS) {
                        AbstractObjectClient.this.getProviderCredentials()
                                .setLocalAuthType(request.getBucketName(), AuthTypeEnum.V2);
                        return AbstractObjectClient.this.doesObjectExistImpl(request);
                    } else {
                        throw e;
                    }
                }
            }
        });
    }
    
    /**
     * Perform a preflight on a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param optionInfo
     *            Parameters in an object preflight request
     * @return Response to the object preflight request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public OptionsInfoResult optionsObject(final String bucketName, final String objectKey,
            final OptionsInfoRequest optionInfo) throws ObsException {
        return this.doActionWithResult("optionsObject", bucketName, new ActionCallbackWithResult<OptionsInfoResult>() {

            @Override
            public OptionsInfoResult action() throws ServiceException {
                ServiceUtils.assertParameterNotNull(optionInfo, "OptionsInfoRequest is null");
                return AbstractObjectClient.this.optionsImpl(bucketName, objectKey, optionInfo);
            }
        });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#listObjects(com.obs.services.model.
     * ListObjectsRequest)
     */
    @Override
    public ObjectListing listObjects(final ListObjectsRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "ListObjectsRequest is null");
        return this.doActionWithResult("listObjects", request.getBucketName(),
                new ActionCallbackWithResult<ObjectListing>() {
                    @Override
                    public ObjectListing action() throws ServiceException {
                        return AbstractObjectClient.this.listObjectsImpl(request);
                    }

                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#listObjects(java.lang.String)
     */
    @Override
    public ObjectListing listObjects(String bucketName) throws ObsException {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
        return this.listObjects(listObjectsRequest);
    }

    /**
     * List versioning objects in a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Object name prefix used for listing versioning objects
     * @param delimiter
     *            Character for grouping object names
     * @param keyMarker
     *            Start position for listing versioning objects (sorted by
     *            object name)
     * @param versionIdMarker
     *            Start position for listing versioning objects (sorted by
     *            version ID)
     * @param maxKeys
     *            Maximum number of versioning objects to be listed
     * @param nextVersionIdMarker
     *            Deprecated field
     * @return Response to the request for listing versioning objects in the
     *         bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    @Deprecated
    public ListVersionsResult listVersions(final String bucketName, final String prefix, final String delimiter,
            final String keyMarker, final String versionIdMarker, final long maxKeys, final String nextVersionIdMarker)
                    throws ObsException {
        ListVersionsRequest request = new ListVersionsRequest();
        request.setBucketName(bucketName);
        request.setPrefix(prefix);
        request.setKeyMarker(keyMarker);
        request.setMaxKeys((int) maxKeys);
        request.setVersionIdMarker(versionIdMarker);
        request.setDelimiter(delimiter);
        return this.listVersions(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#listVersions(com.obs.services.model.
     * ListVersionsRequest)
     */
    @Override
    public ListVersionsResult listVersions(final ListVersionsRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "ListVersionsRequest is null");
        return this.doActionWithResult("listVersions", request.getBucketName(),
                new ActionCallbackWithResult<ListVersionsResult>() {
                    @Override
                    public ListVersionsResult action() throws ServiceException {
                        return AbstractObjectClient.this.listVersionsImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#listVersions(java.lang.String)
     */
    @Override
    public ListVersionsResult listVersions(final String bucketName) throws ObsException {
        ListVersionsRequest request = new ListVersionsRequest();
        request.setBucketName(bucketName);
        return this.listVersions(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#listVersions(java.lang.String, long)
     */
    @Override
    public ListVersionsResult listVersions(final String bucketName, final long maxKeys) throws ObsException {
        ListVersionsRequest request = new ListVersionsRequest();
        request.setBucketName(bucketName);
        request.setMaxKeys((int) maxKeys);
        return this.listVersions(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#listVersions(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * long)
     */
    @Override
    public ListVersionsResult listVersions(final String bucketName, final String prefix, final String delimiter,
            final String keyMarker, final String versionIdMarker, final long maxKeys) throws ObsException {
        return this.listVersions(bucketName, prefix, delimiter, keyMarker, versionIdMarker, maxKeys, null);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#putObject(java.lang.String,
     * java.lang.String, java.io.InputStream,
     * com.obs.services.model.ObjectMetadata)
     */
    @Override
    public PutObjectResult putObject(String bucketName, String objectKey, InputStream input, ObjectMetadata metadata)
            throws ObsException {
        PutObjectRequest request = new PutObjectRequest();
        request.setBucketName(bucketName);
        request.setInput(input);
        request.setMetadata(metadata);
        request.setObjectKey(objectKey);
        return this.putObject(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#putObject(java.lang.String,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public PutObjectResult putObject(String bucketName, String objectKey, InputStream input) throws ObsException {
        return this.putObject(bucketName, objectKey, input, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#putObject(com.obs.services.model.
     * PutObjectRequest)
     */
    @Override
    public PutObjectResult putObject(final PutObjectRequest request) throws ObsException {

        ServiceUtils.assertParameterNotNull(request, "PutObjectRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");

        return this.doActionWithResult("putObject", request.getBucketName(),
                new ActionCallbackWithResult<PutObjectResult>() {
                    @Override
                    public PutObjectResult action() throws ServiceException {
                        if (null != request.getInput() && null != request.getFile()) {
                            throw new ServiceException("Both input and file are set, only one is allowed");
                        }
                        return AbstractObjectClient.this.putObjectImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#putObject(java.lang.String,
     * java.lang.String, java.io.File)
     */
    @Override
    public PutObjectResult putObject(String bucketName, String objectKey, File file) throws ObsException {
        return this.putObject(bucketName, objectKey, file, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#putObject(java.lang.String,
     * java.lang.String, java.io.File, com.obs.services.model.ObjectMetadata)
     */
    @Override
    public PutObjectResult putObject(String bucketName, String objectKey, File file, ObjectMetadata metadata)
            throws ObsException {
        PutObjectRequest request = new PutObjectRequest();
        request.setBucketName(bucketName);
        request.setFile(file);
        request.setObjectKey(objectKey);
        request.setMetadata(metadata);
        return this.putObject(request);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getObject(com.obs.services.model.
     * GetObjectRequest)
     */
    @Override
    public ObsObject getObject(final GetObjectRequest request) throws ObsException {

        ServiceUtils.assertParameterNotNull(request, "GetObjectRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        return this.doActionWithResult("getObject", request.getBucketName(), new ActionCallbackWithResult<ObsObject>() {

            @Override
            public ObsObject action() throws ServiceException {
                return AbstractObjectClient.this.getObjectImpl(request);
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getObject(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public ObsObject getObject(final String bucketName, final String objectKey, final String versionId)
            throws ObsException {
        return this.getObject(new GetObjectRequest(bucketName, objectKey, versionId));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getObject(java.lang.String,
     * java.lang.String)
     */
    @Override
    public ObsObject getObject(final String bucketName, final String objectKey) throws ObsException {
        return this.getObject(bucketName, objectKey, null);
    }

    @Override
    public SelectObjectResult selectObjectContent(final SelectObjectRequest selectRequest)
        throws ObsException {
        ServiceUtils.assertParameterNotNull(selectRequest, "SelectObjectRequest is null");
        ServiceUtils.assertParameterNotNull2(selectRequest.getBucketName(), "bucket-name is null");
        ServiceUtils.assertParameterNotNull2(selectRequest.getKey(), "object-key is null");
        ServiceUtils.assertParameterNotNull2(selectRequest.getExpression(), "sql-expression is null");

        return this.doActionWithResult(
            "selectObjectContent",
            selectRequest.getBucketName(),
            new ActionCallbackWithResult<SelectObjectResult>() {
                @Override
                public SelectObjectResult action() throws ServiceException {
                    return AbstractObjectClient.this.selectObjectContentImpl(selectRequest);
                }
            });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteObject(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public DeleteObjectResult deleteObject(final String bucketName, final String objectKey, final String versionId)
            throws ObsException {
        DeleteObjectRequest request = new DeleteObjectRequest(bucketName, objectKey, versionId);
        return this.deleteObject(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteObject(java.lang.String,
     * java.lang.String)
     */
    @Override
    public DeleteObjectResult deleteObject(final String bucketName, final String objectKey) throws ObsException {
        DeleteObjectRequest request = new DeleteObjectRequest(bucketName, objectKey);
        return this.deleteObject(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteObject(java.lang.String,
     * java.lang.String)
     */
    @Override
    public DeleteObjectResult deleteObject(final DeleteObjectRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "DeleteObjectRequest is null");
        return this.doActionWithResult("deleteObject", request.getBucketName(),
                new ActionCallbackWithResult<DeleteObjectResult>() {

                    @Override
                    public DeleteObjectResult action() throws ServiceException {
                        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
                        return AbstractObjectClient.this.deleteObjectImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteObjects(com.obs.services.model.
     * DeleteObjectsRequest)
     */
    @Override
    public DeleteObjectsResult deleteObjects(final DeleteObjectsRequest deleteObjectsRequest) throws ObsException {
        ServiceUtils.assertParameterNotNull(deleteObjectsRequest, "DeleteObjectsRequest is null");
        return this.doActionWithResult("deleteObjects", deleteObjectsRequest.getBucketName(),
                new ActionCallbackWithResult<DeleteObjectsResult>() {

                    @Override
                    public DeleteObjectsResult action() throws ServiceException {
                        return AbstractObjectClient.this.deleteObjectsImpl(deleteObjectsRequest);
                    }
                });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#copyObject(com.obs.services.model.
     * CopyObjectRequest)
     */
    @Override
    public CopyObjectResult copyObject(final CopyObjectRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "CopyObjectRequest is null");
        ServiceUtils.assertParameterNotNull(request.getDestinationBucketName(), "destinationBucketName is null");
        ServiceUtils.assertParameterNotNull2(request.getSourceObjectKey(), "sourceObjectKey is null");
        ServiceUtils.assertParameterNotNull2(request.getDestinationObjectKey(), "destinationObjectKey is null");
        return this.doActionWithResult("copyObject", request.getSourceBucketName(),
                new ActionCallbackWithResult<CopyObjectResult>() {
                    @Override
                    public CopyObjectResult action() throws ServiceException {
                        return AbstractObjectClient.this.copyObjectImpl(request);
                    }
                });

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#copyObject(java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public CopyObjectResult copyObject(String sourceBucketName, String sourceObjectKey, String destBucketName,
            String destObjectKey) throws ObsException {
        return this.copyObject(new CopyObjectRequest(sourceBucketName, sourceObjectKey, destBucketName, destObjectKey));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getObjectAcl(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public AccessControlList getObjectAcl(final String bucketName, final String objectKey, final String versionId)
            throws ObsException {
        return getObjectAcl(new GetObjectAclRequest(bucketName, objectKey, versionId));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getObjectAcl(java.lang.String,
     * java.lang.String)
     */
    @Override
    public AccessControlList getObjectAcl(final String bucketName, final String objectKey) throws ObsException {
        return getObjectAcl(new GetObjectAclRequest(bucketName, objectKey, null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getObjectAcl(java.lang.String,
     * java.lang.String)
     */
    @Override
    public AccessControlList getObjectAcl(final GetObjectAclRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "GetObjectAclRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        return this.doActionWithResult("getObjectAcl", request.getBucketName(),
                new ActionCallbackWithResult<AccessControlList>() {
                    @Override
                    public AccessControlList action() throws ServiceException {
                        return AbstractObjectClient.this.getObjectAclImpl(request);
                    }

                });
    }

    /**
     * Set an object ACL.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param cannedACL
     *            Pre-defined access control policy
     * @param acl
     *            ACL ("acl" and "cannedACL" cannot be used together.)
     * @param versionId
     *            Object version ID
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    @Deprecated
    public HeaderResponse setObjectAcl(final String bucketName, final String objectKey, final String cannedACL,
            final AccessControlList acl, final String versionId) throws ObsException {
        SetObjectAclRequest request = new SetObjectAclRequest(bucketName, objectKey, acl, versionId);
        request.setCannedACL(cannedACL);
        return setObjectAcl(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setObjectAcl(java.lang.String,
     * java.lang.String, com.obs.services.model.AccessControlList,
     * java.lang.String)
     */
    @Override
    public HeaderResponse setObjectAcl(final SetObjectAclRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetObjectAclRequest is null");
        return this.doActionWithResult("setObjectAcl", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        if (request.getAcl() == null && null == request.getCannedACL()) {
                            throw new IllegalArgumentException("Both cannedACL and AccessControlList is null");
                        }
                        return AbstractObjectClient.this.setObjectAclImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setObjectAcl(java.lang.String,
     * java.lang.String, com.obs.services.model.AccessControlList,
     * java.lang.String)
     */
    @Override
    public HeaderResponse setObjectAcl(final String bucketName, final String objectKey, final AccessControlList acl,
            final String versionId) throws ObsException {
        return this.setObjectAcl(new SetObjectAclRequest(bucketName, objectKey, acl, versionId));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setObjectAcl(java.lang.String,
     * java.lang.String, com.obs.services.model.AccessControlList)
     */
    @Override
    public HeaderResponse setObjectAcl(final String bucketName, final String objectKey, final AccessControlList acl)
            throws ObsException {
        return this.setObjectAcl(new SetObjectAclRequest(bucketName, objectKey, acl));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#getObjectMetadata(com.obs.services.model.
     * GetObjectMetadataRequest)
     */
    @Override
    public ObjectMetadata getObjectMetadata(final GetObjectMetadataRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "GetObjectMetadataRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
        return this.doActionWithResult("getObjectMetadata", request.getBucketName(),
                new ActionCallbackWithResult<ObjectMetadata>() {

                    @Override
                    public ObjectMetadata action() throws ServiceException {
                        return AbstractObjectClient.this.getObjectMetadataImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#setObjectMetadata(com.obs.services.model.
     * SetObjectMetadataRequest)
     */
    @Override
    public ObjectMetadata setObjectMetadata(final SetObjectMetadataRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetObjectMetadataRequest is null");
        return this.doActionWithResult("setObjectMetadata", request.getBucketName(),
                new ActionCallbackWithResult<ObjectMetadata>() {
                    @Override
                    public ObjectMetadata action() throws ServiceException {
                        return AbstractObjectClient.this.setObjectMetadataImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getObjectMetadata(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String objectKey, String versionId) throws ObsException {
        GetObjectMetadataRequest request = new GetObjectMetadataRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setVersionId(versionId);
        return this.getObjectMetadata(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getObjectMetadata(java.lang.String,
     * java.lang.String)
     */
    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String objectKey) throws ObsException {
        return this.getObjectMetadata(bucketName, objectKey, null);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#appendObject(com.obs.services.model.
     * AppendObjectRequest)
     */
    @Override
    public AppendObjectResult appendObject(final AppendObjectRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "AppendObjectRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");

        return this.doActionWithResult("appendObject", request.getBucketName(),
                new ActionCallbackWithResult<AppendObjectResult>() {
                    @Override
                    public AppendObjectResult action() throws ServiceException {
                        if (null != request.getInput() && null != request.getFile()) {
                            throw new ServiceException("Both input and file are set, only one is allowed");
                        }
                        return AbstractObjectClient.this.appendObjectImpl(request);
                    }
                });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#restoreObject(com.obs.services.model.
     * RestoreObjectRequest)
     */
    public RestoreObjectRequest.RestoreObjectStatus restoreObject(
            final RestoreObjectRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "RestoreObjectRequest is null");
        return this.doActionWithResult("restoreObject", request.getBucketName(),
                new ActionCallbackWithResult<RestoreObjectRequest.RestoreObjectStatus>() {

                    @Override
                    public RestoreObjectRequest.RestoreObjectStatus action()
                            throws ServiceException {
                        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
                        return AbstractObjectClient.this.restoreObjectImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#restoreObjectV2(com.obs.services.model.
     * RestoreObjectRequest)
     */
    @Override
    public RestoreObjectResult restoreObjectV2(final RestoreObjectRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "RestoreObjectRequest is null");
        return this.doActionWithResult("restoreObjectV2", request.getBucketName(),
                new ActionCallbackWithResult<RestoreObjectResult>() {

                    @Override
                    public RestoreObjectResult action() throws ServiceException {
                        ServiceUtils.assertParameterNotNull2(request.getObjectKey(), "objectKey is null");
                        return AbstractObjectClient.this.restoreObjectV2Impl(request);
                    }
                });
    }
}
