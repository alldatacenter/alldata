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

import com.obs.services.exception.ObsException;
import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.CopyObjectRequest;
import com.obs.services.model.CopyObjectResult;
import com.obs.services.model.CopyPartRequest;
import com.obs.services.model.CopyPartResult;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.DeleteObjectsResult;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListMultipartUploadsRequest;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.MultipartUploadListing;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;
import com.obs.services.model.OptionsInfoRequest;
import com.obs.services.model.OptionsInfoResult;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RestoreObjectRequest.RestoreObjectStatus;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;

/**
 * ObsClient that supports transparent transfer of AK/SK, inherited from
 * {@link com.obs.services.ObsClient}
 *
 */
public class SecretFlexibleObsClient extends SecretFlexibleBucketObsClient {
    /**
     * Constructor
     * 
     * @param config
     *            Configuration parameters of ObsClient
     */
    public SecretFlexibleObsClient(ObsConfiguration config) {
        this("", "", config);
    }

    /**
     * Constructor
     * 
     * @param endPoint
     *            OBS endpoint
     */
    public SecretFlexibleObsClient(String endPoint) {
        this("", "", endPoint);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the access key
     * @param secretKey
     *            SK in the access key
     * @param config
     *            Configuration parameters of ObsClient
     */
    public SecretFlexibleObsClient(String accessKey, String secretKey, ObsConfiguration config) {
        super(accessKey, secretKey, config);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the access key
     * @param secretKey
     *            SK in the access key
     * @param endPoint
     *            OBS endpoint
     */
    public SecretFlexibleObsClient(String accessKey, String secretKey, String endPoint) {
        super(accessKey, secretKey, endPoint);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the temporary access key
     * @param secretKey
     *            SK in the temporary access key
     * @param securityToken
     *            Security token
     * @param config
     *            Configuration parameters of ObsClient
     */
    public SecretFlexibleObsClient(String accessKey, String secretKey, String securityToken, ObsConfiguration config) {
        super(accessKey, secretKey, securityToken, config);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the temporary access key
     * @param secretKey
     *            SK in the temporary access key
     * @param securityToken
     *            Security token
     * @param endPoint
     *            OBS endpoint
     */
    public SecretFlexibleObsClient(String accessKey, String secretKey, String securityToken, String endPoint) {
        super(accessKey, secretKey, securityToken, endPoint);
    }
    
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(listObjectsRequest.getBucketName(), accessKey, secretKey);
        try {
            return super.listObjects(listObjectsRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(listObjectsRequest.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.listObjects(listObjectsRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    //CHECKSTYLE:OFF
    public ListVersionsResult listVersions(String bucketName, String prefix, String delimiter, String keyMarker,
            String versionIdMarker, long maxKeys, String nextVersionIdMarker, String accessKey, String secretKey)
                    throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.listVersions(bucketName, prefix, delimiter, keyMarker, versionIdMarker, maxKeys,
                    nextVersionIdMarker);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    //CHECKSTYLE:OFF
    public ListVersionsResult listVersions(String bucketName, String prefix, String delimiter, String keyMarker,
            String versionIdMarker, long maxKeys, String nextVersionIdMarker, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.listVersions(bucketName, prefix, delimiter, keyMarker, versionIdMarker, maxKeys,
                    nextVersionIdMarker);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ListVersionsResult listVersions(ListVersionsRequest request, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey);
        try {
            return super.listVersions(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ListVersionsResult listVersions(ListVersionsRequest request, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.listVersions(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ListVersionsResult listVersions(String bucketName, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.listVersions(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ListVersionsResult listVersions(String bucketName, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.listVersions(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ListVersionsResult listVersions(String bucketName, long maxKeys, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.listVersions(bucketName, maxKeys);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ListVersionsResult listVersions(String bucketName, long maxKeys, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.listVersions(bucketName, maxKeys);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public OptionsInfoResult optionsObject(String bucketName, String objectKey, OptionsInfoRequest optionInfo,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.optionsObject(bucketName, objectKey, optionInfo);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public OptionsInfoResult optionsObject(String bucketName, String objectKey, OptionsInfoRequest optionInfo,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.optionsObject(bucketName, objectKey, optionInfo);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public PutObjectResult putObject(PutObjectRequest request, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey);
        try {
            return super.putObject(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public PutObjectResult putObject(PutObjectRequest request, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.putObject(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ObsObject getObject(GetObjectRequest getObjectRequest, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(getObjectRequest.getBucketName(), accessKey, secretKey);
        try {
            return super.getObject(getObjectRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ObsObject getObject(GetObjectRequest getObjectRequest, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(getObjectRequest.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.getObject(getObjectRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ObjectMetadata getObjectMetadata(GetObjectMetadataRequest request, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey);
        try {
            return super.getObjectMetadata(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ObjectMetadata getObjectMetadata(GetObjectMetadataRequest request, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.getObjectMetadata(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public RestoreObjectStatus restoreObject(RestoreObjectRequest restoreObjectRequest, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(restoreObjectRequest.getBucketName(), accessKey, secretKey);
        try {
            return super.restoreObject(restoreObjectRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public RestoreObjectStatus restoreObject(RestoreObjectRequest restoreObjectRequest, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(restoreObjectRequest.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.restoreObject(restoreObjectRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteObject(String bucketName, String objectKey, String versionId, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteObject(bucketName, objectKey, versionId);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteObject(String bucketName, String objectKey, String versionId, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteObject(bucketName, objectKey, versionId);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteObject(String bucketName, String objectKey, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteObject(bucketName, objectKey);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(deleteObjectsRequest.getBucketName(), accessKey, secretKey);
        try {
            return super.deleteObjects(deleteObjectsRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(deleteObjectsRequest.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.deleteObjects(deleteObjectsRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public AccessControlList getObjectAcl(String bucketName, String objectKey, String versionId, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getObjectAcl(bucketName, objectKey, versionId);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public AccessControlList getObjectAcl(String bucketName, String objectKey, String versionId, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getObjectAcl(bucketName, objectKey, versionId);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public AccessControlList getObjectAcl(String bucketName, String objectKey, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getObjectAcl(bucketName, objectKey);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setObjectAcl(String bucketName, String objectKey, String cannedACL, AccessControlList acl,
            String versionId, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setObjectAcl(bucketName, objectKey, cannedACL, acl, versionId);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setObjectAcl(String bucketName, String objectKey, String cannedACL, AccessControlList acl,
            String versionId, String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setObjectAcl(bucketName, objectKey, cannedACL, acl, versionId);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setObjectAcl(String bucketName, String objectKey, AccessControlList acl, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setObjectAcl(bucketName, objectKey, acl);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setObjectAcl(String bucketName, String objectKey, AccessControlList acl, String versionId,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setObjectAcl(bucketName, objectKey, acl, versionId);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setObjectAcl(String bucketName, String objectKey, AccessControlList acl, String versionId,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setObjectAcl(bucketName, objectKey, acl, versionId);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(copyObjectRequest.getBucketName(), accessKey, secretKey);
        try {
            return super.copyObject(copyObjectRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(copyObjectRequest.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.copyObject(copyObjectRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey);
        try {
            return super.initiateMultipartUpload(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.initiateMultipartUpload(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse abortMultipartUpload(AbortMultipartUploadRequest request, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey);
        try {
            return super.abortMultipartUpload(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse abortMultipartUpload(AbortMultipartUploadRequest request, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.abortMultipartUpload(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public UploadPartResult uploadPart(UploadPartRequest request, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey);
        try {
            return super.uploadPart(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public UploadPartResult uploadPart(UploadPartRequest request, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.uploadPart(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public CopyPartResult copyPart(CopyPartRequest request, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(request.getSourceBucketName(), accessKey, secretKey);
        try {
            return super.copyPart(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public CopyPartResult copyPart(CopyPartRequest request, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(request.getSourceBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.copyPart(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey);
        try {
            return super.completeMultipartUpload(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.completeMultipartUpload(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ListPartsResult listParts(ListPartsRequest request, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey);
        try {
            return super.listParts(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ListPartsResult listParts(ListPartsRequest request, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.listParts(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest request, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey);
        try {
            return super.listMultipartUploads(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest request, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(request.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.listMultipartUploads(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }
}
