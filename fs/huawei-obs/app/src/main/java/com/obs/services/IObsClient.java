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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.obs.services.exception.ObsException;
import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.AppendObjectRequest;
import com.obs.services.model.AppendObjectResult;
import com.obs.services.model.BaseBucketRequest;
import com.obs.services.model.BucketCors;
import com.obs.services.model.BucketDirectColdAccess;
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.BucketLocationResponse;
import com.obs.services.model.BucketLoggingConfiguration;
import com.obs.services.model.BucketMetadataInfoRequest;
import com.obs.services.model.BucketMetadataInfoResult;
import com.obs.services.model.BucketNotificationConfiguration;
import com.obs.services.model.BucketPolicyResponse;
import com.obs.services.model.BucketQuota;
import com.obs.services.model.BucketStorageInfo;
import com.obs.services.model.BucketStoragePolicyConfiguration;
import com.obs.services.model.BucketTagInfo;
import com.obs.services.model.BucketVersioningConfiguration;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.CopyObjectRequest;
import com.obs.services.model.CopyObjectResult;
import com.obs.services.model.CopyPartRequest;
import com.obs.services.model.CopyPartResult;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.CreateVirtualBucketRequest;
import com.obs.services.model.CreateVirtualBucketResult;
import com.obs.services.model.DeleteObjectRequest;
import com.obs.services.model.DeleteObjectResult;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.DeleteObjectsResult;
import com.obs.services.model.DownloadFileRequest;
import com.obs.services.model.DownloadFileResult;
import com.obs.services.model.GetObjectAclRequest;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.LifecycleConfiguration;
import com.obs.services.model.ListBucketAliasResult;
import com.obs.services.model.ListBucketsRequest;
import com.obs.services.model.ListBucketsResult;
import com.obs.services.model.ListMultipartUploadsRequest;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.ModifyObjectRequest;
import com.obs.services.model.ModifyObjectResult;
import com.obs.services.model.MultipartUploadListing;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.ObsObject;
import com.obs.services.model.PostSignatureRequest;
import com.obs.services.model.PostSignatureResponse;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.PutObjectsRequest;
import com.obs.services.model.ReadAheadQueryResult;
import com.obs.services.model.ReadAheadRequest;
import com.obs.services.model.ReadAheadResult;
import com.obs.services.model.RenameObjectRequest;
import com.obs.services.model.RenameObjectResult;
import com.obs.services.model.ReplicationConfiguration;
import com.obs.services.model.RequestPaymentConfiguration;
import com.obs.services.model.RequestPaymentEnum;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RestoreObjectRequest.RestoreObjectStatus;
import com.obs.services.model.RestoreObjectResult;
import com.obs.services.model.RestoreObjectsRequest;
import com.obs.services.model.SetBucketAclRequest;
import com.obs.services.model.SetBucketCorsRequest;
import com.obs.services.model.SetBucketDirectColdAccessRequest;
import com.obs.services.model.SetBucketEncryptionRequest;
import com.obs.services.model.SetBucketLifecycleRequest;
import com.obs.services.model.SetBucketLoggingRequest;
import com.obs.services.model.SetBucketNotificationRequest;
import com.obs.services.model.SetBucketPolicyRequest;
import com.obs.services.model.SetBucketQuotaRequest;
import com.obs.services.model.SetBucketReplicationRequest;
import com.obs.services.model.SetBucketRequestPaymentRequest;
import com.obs.services.model.SetBucketStoragePolicyRequest;
import com.obs.services.model.SetBucketTaggingRequest;
import com.obs.services.model.SetBucketVersioningRequest;
import com.obs.services.model.SetBucketWebsiteRequest;
import com.obs.services.model.SetObjectAclRequest;
import com.obs.services.model.SetObjectMetadataRequest;
import com.obs.services.model.TaskProgressStatus;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;
import com.obs.services.model.TruncateObjectRequest;
import com.obs.services.model.TruncateObjectResult;
import com.obs.services.model.UploadFileRequest;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;
import com.obs.services.model.UploadProgressStatus;
import com.obs.services.model.WebsiteConfiguration;
import com.obs.services.model.select.SelectObjectRequest;
import com.obs.services.model.select.SelectObjectResult;

/**
 * Basic OBS interface
 */
//CHECKSTYLE:OFF
public interface IObsClient extends IObsBucketExtendClient {
 
    /**
     *
     * Refresh the temporary access key.
     *
     * @param accessKey
     *            AK in the temporary access key
     * @param secretKey
     *            SK in the temporary access key
     * @param securityToken
     *            Security token
     *
     */
    void refresh(String accessKey, String secretKey, String securityToken);

    /**
     * Generate temporarily authorized access parameters.
     *
     * @param request
     *            Parameters in a request for temporarily authorized access
     * @return Response to the request for temporarily authorized access
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    TemporarySignatureResponse createTemporarySignature(TemporarySignatureRequest request);

    /**
     * Generate parameters for browser-based authorized access.
     *
     * @param request
     *            Request parameters for V4 browser-based authorized access
     * @return Response to the V4 browser-based authorized access
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    PostSignatureResponse createPostSignature(PostSignatureRequest request) throws ObsException;

    /**
     * Create a bucket. <br>
     * <p>
     * <b>Bucket naming rules: </b>
     * </p>
     * <ul>
     * <li>Contain only lowercase letters, digits, hyphens (-), and periods (.).
     * <li>Must start with a digit or a letter.
     * <li>Contain 3 to 63 characters.
     * <li>Cannot be an IP address.
     * <li>Cannot end with a hyphen (-).
     * <li>Cannot contain two consecutive periods (..).
     * <li>Cannot contain periods (.) and hyphens (-) adjacent to each other,
     * for example, "my-.bucket" and "my.-bucket".
     * </ul>
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket information
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObsBucket createBucket(String bucketName) throws ObsException;

    /**
     * Create a bucket. <br>
     * Create a bucket of a specific name in the given region.
     * <p>
     * <b>Bucket naming rules: </b>
     * </p>
     * <ul>
     * <li>Contain only lowercase letters, digits, hyphens (-), and periods (.).
     * <li>Must start with a digit or a letter.
     * <li>Contain 3 to 63 characters.
     * <li>Cannot be an IP address.s
     * <li>Cannot end with a hyphen (-).
     * <li>Cannot contain two consecutive periods (..).
     * <li>Cannot contain periods (.) and hyphens (-) adjacent to each other,
     * for example, "my-.bucket" and "my.-bucket".
     * </ul>
     *
     *
     * @param bucketName
     *            Bucket name
     * @param location
     *            Bucket location. This parameter is mandatory unless the
     *            endpoint belongs to the default region.
     * @return Bucket information
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObsBucket createBucket(String bucketName, String location) throws ObsException;

    /**
     * Create a bucket. <br>
     * Create a bucket of a specific name in the given region.
     * <p>
     * <b>Bucket naming rules: </b>
     * </p>
     * <ul>
     * <li>Contain only lowercase letters, digits, hyphens (-), and periods (.).
     * <li>Must start with a digit or a letter.
     * <li>Contain 3 to 63 characters.
     * <li>Cannot be an IP address.
     * <li>Cannot end with a hyphen (-).
     * <li>Cannot contain two consecutive periods (..).
     * <li>Cannot contain periods (.) and hyphens (-) adjacent to each other,
     * for example, "my-.bucket" and "my.-bucket".
     * </ul>
     *
     * @param bucket
     *            Bucket information, including the request parameters
     * @return Bucket information
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObsBucket createBucket(ObsBucket bucket) throws ObsException;

    /**
     * Create a bucket. <br>
     * <p>
     * <b>Bucket naming rules: </b>
     * </p>
     * <ul>
     * <li>Contain only lowercase letters, digits, hyphens (-), and periods (.).
     * <li>Must start with a digit or a letter.
     * <li>Contain 3 to 63 characters.
     * <li>Cannot be an IP address.
     * <li>Cannot end with a hyphen (-).
     * <li>Cannot contain two consecutive periods (..).
     * <li>Cannot contain periods (.) and hyphens (-) adjacent to each other,
     * for example, "my-.bucket" and "my.-bucket".
     * </ul>
     *
     * @param request
     *            Request parameters for creating a bucket
     * @return Bucket information
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     *
     */
    ObsBucket createBucket(CreateBucketRequest request) throws ObsException;

    /**
     * Create a virtual bucket.
     *
     * @param request Request parameters for creating a virtual bucket
     * @return Virtual bucket information
     * @throws ObsException
     */
    CreateVirtualBucketResult createVirtualBucket(CreateVirtualBucketRequest request) throws ObsException;

    /**
     * Obtain the bucket alias list.
     *
     * @return Alias Bucket list
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    ListBucketAliasResult listAliasBuckets() throws ObsException;

    /**
     * Rename a file or directory. Only the parallel file system supports this interface.
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File name or directory name
     * @param newObjectKey
     *            Name of the renamed file or directory
     * @return Response to the request for renaming a file
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    RenameObjectResult renameObject(String bucketName, String objectKey, String newObjectKey) throws ObsException;

    /**
     * Rename a file or directory. Only the parallel file system supports this interface.
     * 
     * @param request
     *            Parameters of a request for renaming a file
     * @return Response to the request for renaming a file
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    RenameObjectResult renameObject(final RenameObjectRequest request) throws ObsException;

    /**
     * Truncate a file. Only the parallel file system supports this interface.
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File name
     * @param newLength
     *            Size of the truncated file
     * @return Response to the request for truncating a file
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    TruncateObjectResult truncateObject(String bucketName, String objectKey, long newLength) throws ObsException;

    /**
     * Truncate a file. Only the parallel file system supports this interface.
     * 
     * @param request
     *            Parameters of a request for truncating a file
     * @return Response to the request for truncating a file
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    TruncateObjectResult truncateObject(final TruncateObjectRequest request) throws ObsException;

    /**
     * Write a file. Only the parallel file system supports this interface.
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File name
     * @param position
     *            Start position for writing data to a file
     * @param file
     *            Local file path
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    ModifyObjectResult modifyObject(String bucketName, String objectKey, long position, File file) throws ObsException;

    /**
     * Write a file. Only the parallel file system supports this interface.
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File name
     * @param position
     *            Start position for writing data to a file
     * @param input
     *            Data stream to be uploaded
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    ModifyObjectResult modifyObject(String bucketName, String objectKey, long position, InputStream input)
            throws ObsException;

    /**
     * Write a file. Only the parallel file system supports this interface.
     * 
     * @param request
     *            Request parameters for writing data to a file
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    ModifyObjectResult modifyObject(ModifyObjectRequest request) throws ObsException;

    /**
     * Obtain the bucket list.
     * 
     * @param request
     *            Obtain the request parameters for obtaining the bucket list.
     * @return Bucket list
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    List<ObsBucket> listBuckets(ListBucketsRequest request) throws ObsException;

    /**
     * Obtain the bucket list.
     *
     * @param request
     *            Obtain the request parameters for obtaining the bucket list.
     * @return Response to the request for obtaining the bucket list
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ListBucketsResult listBucketsV2(ListBucketsRequest request) throws ObsException;

    /**
     * Delete a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteBucket(String bucketName) throws ObsException;

    /**
     * Delete a bucket.
     * 
     * @param request
     *            Parameters of a request for deleting a bucket
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse deleteBucket(BaseBucketRequest request) throws ObsException;

    /**
     * List objects in the bucket.
     *
     * @param request
     *            Request parameters for listing objects in a bucket
     * @return Response to the request for listing objects in the bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObjectListing listObjects(ListObjectsRequest request) throws ObsException;

    /**
     * List objects in the bucket.
     *
     * @param bucketName
     *            Bucket name
     * @return Response to the request for listing objects in the bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObjectListing listObjects(String bucketName) throws ObsException;

    /**
     * Identify whether a bucket exists.
     *
     * @param bucketName
     *            Bucket name
     * @return Identifier indicating whether the bucket exists
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    boolean headBucket(String bucketName) throws ObsException;

    /**
     * Identify whether a bucket exists.
     * 
     * @param request
     *            Request parameters
     * @return Identifier indicating whether the bucket exists
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    boolean headBucket(BaseBucketRequest request) throws ObsException;

    /**
     * List versioning objects in a bucket.
     *
     * @param request
     *            Request parameters for listing versioning objects in the
     *            bucket
     * @return Response to the request for listing versioning objects in the
     *         bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ListVersionsResult listVersions(ListVersionsRequest request) throws ObsException;

    /**
     * List versioning objects in a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @return Response to the request for listing versioning objects in the
     *         bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ListVersionsResult listVersions(String bucketName) throws ObsException;

    /**
     * List versioning objects in a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @param maxKeys
     *            Maximum number of versioning objects to be listed
     * @return Response to the request for listing versioning objects in the
     *         bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ListVersionsResult listVersions(String bucketName, long maxKeys) throws ObsException;

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
     * @return Response to the request for listing versioning objects in the
     *         bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ListVersionsResult listVersions(String bucketName, String prefix, String delimiter, String keyMarker,
            String versionIdMarker, long maxKeys) throws ObsException;

    /**
     * Obtain bucket metadata.
     *
     * @param request
     *            Request parameters for obtaining bucket metadata
     * @return Response to the request for obtaining bucket metadata
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketMetadataInfoResult getBucketMetadata(BucketMetadataInfoRequest request) throws ObsException;

    /**
     * Obtain a bucket ACL.
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket ACL
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    AccessControlList getBucketAcl(String bucketName) throws ObsException;

    /**
     * Obtain a bucket ACL.
     * 
     * @param request
     *            Request parameters for obtaining the bucket ACL
     * @return Response to a request for obtaining the bucket ACL
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    AccessControlList getBucketAcl(BaseBucketRequest request) throws ObsException;

    /**
     * Set a bucket ACL. <br>
     *
     * @param bucketName
     *            Bucket name
     * @param acl
     *            ACL
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketAcl(String bucketName, AccessControlList acl) throws ObsException;

    /**
     * Set a bucket ACL. 
     * 
     * @param request
     *            Request parameters for setting a bucket ACL
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketAcl(SetBucketAclRequest request) throws ObsException;

    /**
     * Obtain the bucket location.
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket location
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    String getBucketLocation(String bucketName) throws ObsException;

    /**
     * Obtain the bucket location.
     * 
     * @param request
     *            Request parameters
     * @return Response to the request for obtaining the bucket location
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketLocationResponse getBucketLocation(BaseBucketRequest request) throws ObsException;

    /**
     * Obtain the bucket location.
     *
     * @param bucketName
     *            Bucket name
     * @return Response to the request for obtaining the bucket location
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketLocationResponse getBucketLocationV2(String bucketName) throws ObsException;

    /**
     * Obtain bucket storage information.
     *
     * @param bucketName
     *            Bucket name
     * @return Bcket storage information
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketStorageInfo getBucketStorageInfo(String bucketName) throws ObsException;

    /**
     * Obtain bucket storage information.
     * 
     * @param request
     *            Bucket name
     * @return Bucket storage information
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketStorageInfo getBucketStorageInfo(BaseBucketRequest request) throws ObsException;

    /**
     * Obtain the bucket quota.
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket quota
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketQuota getBucketQuota(String bucketName) throws ObsException;

    /**
     * Obtain the bucket quota.
     * 
     * @param request
     *            Request parameters
     * @return Bucket quota
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketQuota getBucketQuota(BaseBucketRequest request) throws ObsException;

    /**
     * Set the bucket quota.
     *
     * @param bucketName
     *            Bucket name
     * @param bucketQuota
     *            Bucket quota
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     * @return Common response headers
     */
    HeaderResponse setBucketQuota(String bucketName, BucketQuota bucketQuota) throws ObsException;

    /**
     * Set the bucket quota.
     * 
     * @param request
     *            Request parameters
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @return Common response headers
     * @since 3.20.3
     */
    HeaderResponse setBucketQuota(SetBucketQuotaRequest request) throws ObsException;

    /**
     * Obtain the bucket storage class.
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket storage policy
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketStoragePolicyConfiguration getBucketStoragePolicy(String bucketName) throws ObsException;

    /**
     * Obtain the bucket storage class.
     * 
     * @param request
     *            Request parameters
     * @return Bucket storage policy
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketStoragePolicyConfiguration getBucketStoragePolicy(BaseBucketRequest request) throws ObsException;

    /**
     * Set the bucket storage class.
     *
     * @param bucketName
     *            Bucket name
     * @param bucketStorage
     *            Bucket storage policy
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketStoragePolicy(String bucketName, BucketStoragePolicyConfiguration bucketStorage)
            throws ObsException;

    /**
     * Set the bucket storage class.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketStoragePolicy(SetBucketStoragePolicyRequest request) throws ObsException;

    /**
     * Configure the bucket CORS.
     *
     * @param bucketName
     *            Bucket name
     * @param bucketCors
     *            CORS rules
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketCors(String bucketName, BucketCors bucketCors) throws ObsException;

    /**
     * Configure the bucket CORS.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketCors(SetBucketCorsRequest request) throws ObsException;

    /**
     * Obtain the bucket CORS rules.
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket CORS rules
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketCors getBucketCors(String bucketName) throws ObsException;

    /**
     * Obtain the bucket CORS rules.
     * 
     * @param request
     *            Request parameters
     * @return Bucket CORS configuration
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketCors getBucketCors(BaseBucketRequest request) throws ObsException;

    /**
     * Delete the bucket CORS rules.
     *
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteBucketCors(String bucketName) throws ObsException;

    /**
     * Delete the bucket CORS rules.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse deleteBucketCors(BaseBucketRequest request) throws ObsException;

    /**
     * Obtain the logging settings of a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @return Logging settings of the bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketLoggingConfiguration getBucketLogging(String bucketName) throws ObsException;

    /**
     * Obtain the logging settings of a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Logging settings of the bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketLoggingConfiguration getBucketLogging(BaseBucketRequest request) throws ObsException;

    HeaderResponse setBucketLoggingConfiguration(String bucketName, BucketLoggingConfiguration loggingConfiguration,
            boolean updateTargetACLifRequired) throws ObsException;

    /**
     * Configure logging for a bucket.<br>
     *
     * @param bucketName
     *            Bucket name
     * @param loggingConfiguration
     *            Logging settings
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketLogging(String bucketName, BucketLoggingConfiguration loggingConfiguration)
            throws ObsException;

    /**
     * Configure logging for a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketLogging(SetBucketLoggingRequest request) throws ObsException;

    /**
     * Set the versioning status for a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @param versioningConfiguration
     *            Versioning status of the bucket
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketVersioning(String bucketName, BucketVersioningConfiguration versioningConfiguration)
            throws ObsException;

    /**
     * Set the versioning status for a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketVersioning(SetBucketVersioningRequest request) throws ObsException;

    /**
     * Obtain the versioning status for a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @return Versioning status of the bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketVersioningConfiguration getBucketVersioning(String bucketName) throws ObsException;

    /**
     * Obtain the versioning status for a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Versioning status of the bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketVersioningConfiguration getBucketVersioning(BaseBucketRequest request) throws ObsException;

    /**
     * Configure the requester-pays function for a bucket.
     * 
     * @param bucketName
     *            Bucket name
     * @param payer
     *            The status of the requester-pays function
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    HeaderResponse setBucketRequestPayment(String bucketName, RequestPaymentEnum payer) throws ObsException;

    /**
     * Configure the requester-pays function for a bucket.
     * 
     * @param request
     *            Configuration of the requester-pays function of a bucket
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketRequestPayment(SetBucketRequestPaymentRequest request) throws ObsException;

    /**
     * Obtain the status of the requester-pays function of a bucket.
     * 
     * @param bucketName
     *            Bucket name
     * @return Configuration of the requester-pays function of the bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    RequestPaymentConfiguration getBucketRequestPayment(String bucketName) throws ObsException;

    /**
     * Obtain the requester-pays status of a bucket.
     * 
     * @param request
     *            Basic bucket information
     * @return Configuration of the requester-pays function of the bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    RequestPaymentConfiguration getBucketRequestPayment(BaseBucketRequest request) throws ObsException;

    /**
     * Obtain the bucket lifecycle rules.
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket lifecycle rules
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    LifecycleConfiguration getBucketLifecycle(String bucketName) throws ObsException;

    /**
     * Obtain the bucket lifecycle rules.
     * 
     * @param request
     *            Request parameters
     * @return Bucket lifecycle rules
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    LifecycleConfiguration getBucketLifecycle(BaseBucketRequest request) throws ObsException;

    /**
     * Set the bucket lifecycle rules.
     *
     * @param bucketName
     *            Bucket name
     * @param lifecycleConfig
     *            Bucket lifecycle rules
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfig) throws ObsException;

    /**
     * Configure lifecycle rules for a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketLifecycle(SetBucketLifecycleRequest request) throws ObsException;

    /**
     * Delete the bucket lifecycle rules from a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteBucketLifecycle(String bucketName) throws ObsException;

    /**
     * Delete the bucket lifecycle rules from a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse deleteBucketLifecycle(BaseBucketRequest request) throws ObsException;

    /**
     * Obtain bucket policies.
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket policy, in the JSON format
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    String getBucketPolicy(String bucketName) throws ObsException;

    /**
     * Obtain a bucket policy.
     * 
     * @param request
     *            Request parameters
     * @return Bucket policy, in the JSON format
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    String getBucketPolicy(BaseBucketRequest request) throws ObsException;

    /**
     * Obtain bucket policies. <br>
     *
     * @param bucketName
     *            Bucket name
     * @return Response to a request for obtaining bucket policies
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketPolicyResponse getBucketPolicyV2(String bucketName) throws ObsException;

    /**
     * Obtain bucket policies. 
     * 
     * @param request
     *            Request parameters
     * @return Response to a request for obtaining bucket policies
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketPolicyResponse getBucketPolicyV2(BaseBucketRequest request) throws ObsException;

    /**
     * Set bucket policies.
     *
     * @param bucketName
     *            Bucket name
     * @param policy
     *            Bucket policy, in the JSON format
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketPolicy(String bucketName, String policy) throws ObsException;

    /**
     * Set bucket policies.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketPolicy(SetBucketPolicyRequest request) throws ObsException;

    /**
     * Delete bucket policies.
     *
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteBucketPolicy(String bucketName) throws ObsException;

    /**
     * Delete bucket policies.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse deleteBucketPolicy(BaseBucketRequest request) throws ObsException;

    /**
     * Obtain the website hosting configuration of a Bucket
     *
     * @param bucketName
     *            Bucket name
     * @return Website hosting configuration of a bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    WebsiteConfiguration getBucketWebsite(String bucketName) throws ObsException;

    /**
     * Obtain the website hosting settings of a Bucket
     * 
     * @param request
     *            Request parameters
     * @return Website hosting configuration of a bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    WebsiteConfiguration getBucketWebsite(BaseBucketRequest request) throws ObsException;

    /**
     * Configure website hosting for a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @param websiteConfig
     *            Website hosting configuration of a bucket
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketWebsite(String bucketName, WebsiteConfiguration websiteConfig) throws ObsException;

    /**
     * Configure website hosting for a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketWebsite(SetBucketWebsiteRequest request) throws ObsException;

    /**
     * Delete the website hosting configuration of a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteBucketWebsite(String bucketName) throws ObsException;

    /**
     * Delete the website hosting configuration of a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse deleteBucketWebsite(BaseBucketRequest request) throws ObsException;

    /**
     * Obtain bucket tags.
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket tag
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketTagInfo getBucketTagging(String bucketName) throws ObsException;

    /**
     * Obtain bucket tags.
     * 
     * @param request
     *            Request parameters
     * @return Bucket tag
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketTagInfo getBucketTagging(BaseBucketRequest request) throws ObsException;

    /**
     * Set bucket tags.
     *
     * @param bucketName
     *            Bucket name
     * @param bucketTagInfo
     *            Bucket tags
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketTagging(String bucketName, BucketTagInfo bucketTagInfo) throws ObsException;

    /**
     * Set bucket tags.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketTagging(SetBucketTaggingRequest request) throws ObsException;

    /**
     * Delete bucket tags.
     *
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteBucketTagging(String bucketName) throws ObsException;

    /**
     * Delete bucket tags.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse deleteBucketTagging(BaseBucketRequest request) throws ObsException;
  
    
    /**
     * Obtain bucket encryption configuration.
     * 
     * @param bucketName
     *            Bucket name
     * @return Bucket encryption configuration
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketEncryption getBucketEncryption(String bucketName) throws ObsException;

    /**
     * Obtain bucket encryption configuration.
     * 
     * @param request
     *            Request parameters
     * @return Bucket encryption configuration
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketEncryption getBucketEncryption(BaseBucketRequest request) throws ObsException;

    /**
     * Set bucket encryption.
     * 
     * @param bucketName
     *            Bucket name
     * @param bucketEncryption
     *            Bucket encryption configuration
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketEncryption(String bucketName, BucketEncryption bucketEncryption) throws ObsException;

    /**
     * Configure bucket encryption.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketEncryption(SetBucketEncryptionRequest request) throws ObsException;

    /**
     * Delete bucket encryption configuration.
     * 
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteBucketEncryption(String bucketName) throws ObsException;

    /**
     * Delete bucket encryption configuration.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse deleteBucketEncryption(BaseBucketRequest request) throws ObsException;

    /**
     * Configure cross-region replication for a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @param replicationConfiguration
     *            Cross-region replication configuration
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     *
     */
    HeaderResponse setBucketReplication(String bucketName, ReplicationConfiguration replicationConfiguration)
            throws ObsException;

    /**
     * Configure cross-region replication for a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketReplication(SetBucketReplicationRequest request) throws ObsException;

    /**
     * Obtain the cross-region replication configuration of a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @return Cross-region replication configuration
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ReplicationConfiguration getBucketReplication(String bucketName) throws ObsException;

    /**
     * Obtain the cross-region replication configuration of a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Cross-region replication configuration
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    ReplicationConfiguration getBucketReplication(BaseBucketRequest request) throws ObsException;

    /**
     * Delete the bucket cross-region replication configuration.
     *
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteBucketReplication(String bucketName) throws ObsException;

    /**
     * Delete the bucket cross-region replication configuration.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse deleteBucketReplication(BaseBucketRequest request) throws ObsException;

    /**
     * Obtain the notification configuration of a bucket.
     *
     * @param bucketName
     *            Bucket name
     * @return Bucket notification configuration
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketNotificationConfiguration getBucketNotification(String bucketName) throws ObsException;

    /**
     * Obtain the notification configuration of a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Bucket notification configuration
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketNotificationConfiguration getBucketNotification(BaseBucketRequest request) throws ObsException;

    /**
     * Configure bucket notification.
     *
     * @param bucketName
     *            Bucket name
     * @param bucketNotificationConfiguration
     *            Bucket notification configuration
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketNotification(String bucketName,
            BucketNotificationConfiguration bucketNotificationConfiguration) throws ObsException;

    /**
     * Set event notification for a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketNotification(SetBucketNotificationRequest request) throws ObsException;

    /**
     * Upload an object.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param input
     *            Data stream to be uploaded
     * @param metadata
     *            Object properties
     * @return Response to an object upload request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    PutObjectResult putObject(String bucketName, String objectKey, InputStream input, ObjectMetadata metadata)
            throws ObsException;

    /**
     * Upload an object.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param input
     *            Data stream to be uploaded
     * @return Response to an object upload request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    PutObjectResult putObject(String bucketName, String objectKey, InputStream input) throws ObsException;

    /**
     * Upload an object.
     *
     * @param request
     *            Parameters in an object upload request
     * @return Response to an object upload request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    PutObjectResult putObject(PutObjectRequest request) throws ObsException;

    /**
     * Upload an object.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param file
     *            File to be uploaded
     * @return Response to an object upload request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    PutObjectResult putObject(String bucketName, String objectKey, File file) throws ObsException;

    /**
     * Upload an object.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param file
     *            File to be uploaded
     * @param metadata
     *            Object properties
     * @return Response to an object upload request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    PutObjectResult putObject(String bucketName, String objectKey, File file, ObjectMetadata metadata)
            throws ObsException;

    /**
     * Perform an appendable upload.
     * 
     * @param request
     *            Parameters in an appendable upload request
     * @return Response to the appendable upload request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    AppendObjectResult appendObject(AppendObjectRequest request) throws ObsException;

    /**
     * Upload a file. The resumable upload mode is supported.
     *
     * @param uploadFileRequest
     *            Parameters in a file upload request
     * @return Result of part combination
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    CompleteMultipartUploadResult uploadFile(UploadFileRequest uploadFileRequest) throws ObsException;

    /**
     * Upload files in a batch.
     * 
     * @param request
     *            Request parameters for uploading files in a batch
     * @return Batch task execution status
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    UploadProgressStatus putObjects(PutObjectsRequest request) throws ObsException;

    /**
     * Check whether an object exists.
     * 
     * @param buckeName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @return Whether an object exists
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    boolean doesObjectExist(String buckeName, String objectKey) throws ObsException;

    /**
     * Check whether an object exists.
     * 
     * @param request
     *            Request parameters for obtaining the properties of an object
     * @return Whether an object exists
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    boolean doesObjectExist(GetObjectMetadataRequest request) throws ObsException;

    /**
     * Download a file. The resumable download mode is supported.
     *
     * @param downloadFileRequest
     *            Parameters in a request for downloading a file
     * @return File download result
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    DownloadFileResult downloadFile(DownloadFileRequest downloadFileRequest) throws ObsException;

    /**
     * Download an object.
     *
     * @param request
     *            Parameters in an object download request
     * @return Object information, including the object data stream
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObsObject getObject(GetObjectRequest request) throws ObsException;

    /**
     * Selects rows from an object using a SQL statement.
     *
     * @param selectRequest
     *            Parameters in an object select request
     * @return Select result container
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    SelectObjectResult selectObjectContent(final SelectObjectRequest selectRequest)
        throws ObsException;

    /**
     * Download an object.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Object version ID
     * @return Object information, including the object data stream
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObsObject getObject(String bucketName, String objectKey, String versionId) throws ObsException;

    /**
     * Download an object.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @return Object information, including the object data stream
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObsObject getObject(String bucketName, String objectKey) throws ObsException;

    /**
     * Obtain object properties.
     *
     * @param request
     *            Parameters in a request for obtaining the properties of an
     *            object
     * @return Object properties
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObjectMetadata getObjectMetadata(GetObjectMetadataRequest request) throws ObsException;

    /**
     * Obtain object properties.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Object version ID
     * @return Object properties
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObjectMetadata getObjectMetadata(String bucketName, String objectKey, String versionId) throws ObsException;

    /**
     * Obtain object properties.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @return Object properties
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObjectMetadata getObjectMetadata(String bucketName, String objectKey) throws ObsException;

    /**
     * Set object properties.
     * 
     * @param request
     *            Parameters in the request for obtaining object properties
     * @return Object properties
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ObjectMetadata setObjectMetadata(SetObjectMetadataRequest request) throws ObsException;

    /**
     * Restore an Archive object.
     *
     * @param request
     *            Parameters in a request for restoring an Archive object
     * @return Status of the to-be-restored Archive object
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     *
     */
    RestoreObjectStatus restoreObject(RestoreObjectRequest request) throws ObsException;

    /**
     * Restore an Archive object.
     * 
     * @param request
     *            Request parameters for restoring an Archive object
     * @return Result of restoring the Archive object
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     * 
     */
    RestoreObjectResult restoreObjectV2(RestoreObjectRequest request) throws ObsException;

    /**
     * Restore Archive objects in a batch.
     * 
     * @param request
     *            Request parameters for restoring Archive objects in a batch
     * @return Batch task execution status
     *
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     * 
     */

    TaskProgressStatus restoreObjects(RestoreObjectsRequest request) throws ObsException;

    /**
     * Delete an object.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Object version ID
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */

    DeleteObjectResult deleteObject(String bucketName, String objectKey, String versionId) throws ObsException;

    /**
     * Delete an object.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    DeleteObjectResult deleteObject(String bucketName, String objectKey) throws ObsException;

    /**
     * Delete an object.
     * 
     * @param request
     *            Request parameters for deleting an object
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    DeleteObjectResult deleteObject(DeleteObjectRequest request) throws ObsException;

    /**
     * Delete objects in a batch.
     *
     * @param deleteObjectsRequest
     *            Parameters in an object batch deletion request
     * @return Result of the object batch deletion request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest) throws ObsException;

    /**
     * Obtain an object ACL.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Object version ID
     * @return Object ACL
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    AccessControlList getObjectAcl(String bucketName, String objectKey, String versionId) throws ObsException;

    /**
     * Obtain an object ACL.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @return Object ACL
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    AccessControlList getObjectAcl(String bucketName, String objectKey) throws ObsException;

    /**
     * Obtain an object ACL.
     * 
     * @param request
     *            Request parameters for obtaining an object ACL
     * @return Object ACL
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    AccessControlList getObjectAcl(GetObjectAclRequest request) throws ObsException;

    /**
     * Set an object ACL.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param acl
     *            ACL
     * @param versionId
     *            Object version ID
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setObjectAcl(String bucketName, String objectKey, AccessControlList acl, String versionId)
            throws ObsException;

    /**
     * Set an object ACL.
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param acl
     *            ACL
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     */
    HeaderResponse setObjectAcl(String bucketName, String objectKey, AccessControlList acl) throws ObsException;

    /**
     * Set an object ACL.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setObjectAcl(SetObjectAclRequest request) throws ObsException;

    /**
     * Copy an object.
     *
     * @param request
     *            Parameters in a request for copying an object
     * @return Result of the object copy
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    CopyObjectResult copyObject(CopyObjectRequest request) throws ObsException;

    /**
     * Copy an object.
     *
     * @param sourceBucketName
     *            Source bucket name
     * @param sourceObjectKey
     *            Source object name
     * @param destBucketName
     *            Destination bucket name
     * @param destObjectKey
     *            Destination object name
     * @return Result of the object copy
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    CopyObjectResult copyObject(String sourceBucketName, String sourceObjectKey, String destBucketName,
            String destObjectKey) throws ObsException;

    /**
     * Initialize a multipart upload.
     *
     * @param request
     *            Parameters in a request for initializing a multipart upload
     * @return Result of the multipart upload
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws ObsException;

    /**
     * Abort a multipart upload.
     *
     * @param request
     *            Parameters in a request for aborting a multipart upload
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse abortMultipartUpload(AbortMultipartUploadRequest request) throws ObsException;

    /**
     * Upload a part.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param uploadId
     *            Multipart upload ID
     * @param partNumber
     *            Part number
     * @param input
     *            Data stream to be uploaded
     * @return Response to a part upload request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    UploadPartResult uploadPart(String bucketName, String objectKey, String uploadId, int partNumber, InputStream input)
            throws ObsException;

    /**
     * Upload a part.
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param uploadId
     *            Multipart upload ID
     * @param partNumber
     *            Part number
     * @param file
     *            File to be uploaded
     * @return Response to a part upload request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    UploadPartResult uploadPart(String bucketName, String objectKey, String uploadId, int partNumber, File file)
            throws ObsException;

    /**
     * Upload a part.
     *
     * @param request
     *            Parameters in a part upload request
     * @return Response to a part upload request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    UploadPartResult uploadPart(UploadPartRequest request) throws ObsException;

    /**
     * Copy a part.
     *
     * @param request
     *            Parameters in the request for copying a part
     * @return Response to a part copy request
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    CopyPartResult copyPart(CopyPartRequest request) throws ObsException;

    /**
     * Combine parts.
     *
     * @param request
     *            Parameters in a request for combining parts
     * @return Result of part combination
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) throws ObsException;

    /**
     * List uploaded parts.
     *
     * @param request
     *            Parameters in a request for listing uploaded parts
     * @return Response to a request for listing uploaded parts
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ListPartsResult listParts(ListPartsRequest request) throws ObsException;

    /**
     * List incomplete multipart uploads.
     *
     * @param request
     *            Parameters in a request for listing multipart uploads
     * @return List of multipart uploads
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest request) throws ObsException;

    /**
     * Read ahead objects.
     * 
     * @param request
     *            Request parameters for reading ahead objects
     * @return Response to the request for reading ahead objects
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ReadAheadResult readAheadObjects(ReadAheadRequest request) throws ObsException;

    /**
     * Delete the read-ahead cache.
     * 
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Name prefix of objects to be read ahead
     * @return Response to the request for reading ahead objects
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ReadAheadResult deleteReadAheadObjects(String bucketName, String prefix) throws ObsException;

    /**
     * Query the progress of a read-ahead task.
     * 
     * @param bucketName
     *            Bucket name
     * @param taskId
     *            ID of the read-ahead task
     * @return Response to the request for querying the progress of the
     *         read-ahead task
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    ReadAheadQueryResult queryReadAheadObjectsTask(String bucketName, String taskId) throws ObsException;

    /**
     * Set the direct reading policy for Archive objects in a bucket.
     * 
     * @param bucketName
     *            Bucket name
     * @param access
     *            Direct reading policy
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse setBucketDirectColdAccess(String bucketName, BucketDirectColdAccess access) throws ObsException;

    /**
     * Configure the direct reading policy for Archive objects in a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse setBucketDirectColdAccess(SetBucketDirectColdAccessRequest request) throws ObsException;

    /**
     * Obtain the direct reading policy for Archive objects in a bucket.
     * 
     * @param bucketName
     *            Bucket name
     * @return Direct reading policy for Archive objects of a bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    BucketDirectColdAccess getBucketDirectColdAccess(String bucketName) throws ObsException;

    /**
     * Obtain the direct reading policy for Archive objects in a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Direct reading policy for Archive objects in a bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    BucketDirectColdAccess getBucketDirectColdAccess(BaseBucketRequest request) throws ObsException;

    /**
     * Delete the direct reading policy for Archive objects in a bucket.
     * 
     * @param bucketName
     *            Bucket name
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    HeaderResponse deleteBucketDirectColdAccess(String bucketName) throws ObsException;

    /**
     * Delete the direct reading policy for Archive objects in a bucket.
     * 
     * @param request
     *            Request parameters
     * @return Common response headers
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.3
     */
    HeaderResponse deleteBucketDirectColdAccess(BaseBucketRequest request) throws ObsException;

    /**
     * Close ObsClient and release connection resources.
     * 
     * @throws IOException
     *             ObsClient close exception
     */
    void close() throws IOException;

}
