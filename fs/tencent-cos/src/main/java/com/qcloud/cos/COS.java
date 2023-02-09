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


package com.qcloud.cos;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.exception.MultiObjectDeleteException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.internal.COSDirectSpi;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.auditing.AudioAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.AudioAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.DocumentAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.DocumentAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.VideoAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.VideoAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.WebpageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.WebpageAuditingResponse;
import com.qcloud.cos.model.ciModel.bucket.DocBucketRequest;
import com.qcloud.cos.model.ciModel.bucket.DocBucketResponse;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketRequest;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketResponse;
import com.qcloud.cos.model.ciModel.common.ImageProcessRequest;
import com.qcloud.cos.model.ciModel.image.ImageLabelRequest;
import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import com.qcloud.cos.model.ciModel.image.ImageLabelV2Request;
import com.qcloud.cos.model.ciModel.image.ImageLabelV2Response;
import com.qcloud.cos.model.ciModel.job.DocHtmlRequest;
import com.qcloud.cos.model.ciModel.job.DocJobListRequest;
import com.qcloud.cos.model.ciModel.job.DocJobListResponse;
import com.qcloud.cos.model.ciModel.job.DocJobRequest;
import com.qcloud.cos.model.ciModel.job.DocJobResponse;
import com.qcloud.cos.model.ciModel.job.MediaJobResponse;
import com.qcloud.cos.model.ciModel.job.MediaJobsRequest;
import com.qcloud.cos.model.ciModel.job.MediaListJobResponse;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoRequest;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoResponse;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.queue.DocListQueueResponse;
import com.qcloud.cos.model.ciModel.queue.DocQueueRequest;
import com.qcloud.cos.model.ciModel.queue.MediaListQueueResponse;
import com.qcloud.cos.model.ciModel.queue.MediaQueueRequest;
import com.qcloud.cos.model.ciModel.queue.MediaQueueResponse;
import com.qcloud.cos.model.ciModel.snapshot.SnapshotRequest;
import com.qcloud.cos.model.ciModel.snapshot.SnapshotResponse;
import com.qcloud.cos.model.ciModel.template.MediaListTemplateResponse;
import com.qcloud.cos.model.ciModel.template.MediaTemplateRequest;
import com.qcloud.cos.model.ciModel.template.MediaTemplateResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowExecutionResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowExecutionsResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowListRequest;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowListResponse;
import com.qcloud.cos.model.inventory.InventoryConfiguration;
import com.squareup.okhttp.internal.http.HttpMethod;


public interface COS extends COSDirectSpi {

    /**
     * return the client config. client config include the region info, default expired sign time,
     * etc.
     *
     * @return ClientConfig.
     */
    public ClientConfig getClientConfig();

    /**
     * <p>
     * Uploads a new object to the specified bucket. The <code>PutObjectRequest</code> contains all
     * the details of the request, including the bucket to upload to, the key the object will be
     * uploaded under, and the file or input stream containing the data to upload.
     * </p>
     * <p>
     * never stores partial objects; if during this call an exception wasn't thrown, the entire
     * object was stored.
     * </p>
     * <p>
     * Depending on whether a file or input stream is being uploaded, this method has slightly
     * different behavior.
     * </p>
     * <p>
     * When uploading a file:
     * </p>
     * <ul>
     * <li>The client automatically computes a checksum of the file. uses checksums to validate the
     * data in each file.</li>
     * <li>Using the file extension, attempts to determine the correct content type and content
     * disposition to use for the object.</li>
     * </ul>
     * <p>
     * When uploading directly from an input stream:
     * </p>
     * <ul>
     * <li>Be careful to set the correct content type in the metadata object before directly sending
     * a stream. Unlike file uploads, content types from input streams cannot be automatically
     * determined. If the caller doesn't explicitly set the content type, it will not be set in .
     * </li>
     * <li>Content length <b>must</b> be specified before data can be uploaded to . Qcloud COS
     * explicitly requires that the content length be sent in the request headers before it will
     * accept any of the data. If the caller doesn't provide the length, the library must buffer the
     * contents of the input stream in order to calculate it.
     * </ul>
     *
     * <p>
     * The specified bucket must already exist and the caller must have {@link Permission#Write}
     * permission to the bucket to upload an object.
     * </p>
     *
     * @param putObjectRequest The request object containing all the parameters to upload a new
     *                         object to .
     * @return A {@link PutObjectResult} object containing the information returned by for the newly
     * created object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#putObject(String, String, File)
     * @see COS#putObject(String, String, InputStream, ObjectMetadata)
     */
    public PutObjectResult putObject(PutObjectRequest putObjectRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Uploads the specified file to under the specified bucket and key name.
     * </p>
     * <p>
     * never stores partial objects; if during this call an exception wasn't thrown, the entire
     * object was stored.
     * </p>
     * <p>
     * The client automatically computes a checksum of the file. uses checksums to validate the data
     * in each file.
     * </p>
     * <p>
     * Using the file extension, attempts to determine the correct content type and content
     * disposition to use for the object.
     * </p>
     *
     * <p>
     * The specified bucket must already exist and the caller must have {@link Permission#Write}
     * permission to the bucket to upload an object.
     * </p>
     *
     * @param bucketName The name of an existing bucket, to which you have {@link Permission#Write}
     *                   permission.
     * @param key        The key under which to store the specified file.
     * @param file       The file containing the data to be uploaded to .
     * @return A {@link PutObjectResult} object containing the information returned by for the newly
     * created object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#putObject(PutObjectRequest)
     * @see COS#putObject(String, String, InputStream, ObjectMetadata)
     */
    public PutObjectResult putObject(String bucketName, String key, File file)
            throws CosClientException, CosServiceException;


    /**
     * <p>
     * Uploads the specified file to under the specified bucket and key name.
     * </p>
     * <p>
     * never stores partial objects; if during this call an exception wasn't thrown, the entire
     * object was stored.
     * </p>
     * <p>
     * When uploading directly from an input stream:
     * </p>
     * <ul>
     * <li>Be careful to set the correct content type in the metadata object before directly sending
     * a stream. Unlike file uploads, content types from input streams cannot be automatically
     * determined. If the caller doesn't explicitly set the content type, it will not be set in .
     * </li>
     * <li>Content length <b>must</b> be specified before data can be uploaded to . Qcloud COS
     * explicitly requires that the content length be sent in the request headers before it will
     * accept any of the data. If the caller doesn't provide the length, the library must buffer the
     * contents of the input stream in order to calculate it.
     * </ul>
     *
     * <p>
     * The specified bucket must already exist and the caller must have {@link Permission#Write}
     * permission to the bucket to upload an object.
     * </p>
     *
     * @param bucketName The name of an existing bucket, to which you have {@link Permission#Write}
     *                   permission.
     * @param key        The key under which to store the specified file.
     * @param input      The input stream containing the data to be uploaded to .
     * @param metadata   Additional metadata instructing how to handle the uploaded data (e.g. custom
     *                   user metadata, hooks for specifying content type, etc.).
     * @return A {@link PutObjectResult} object containing the information returned by for the newly
     * created object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#putObject(PutObjectRequest)
     * @see COS#putObject(String, String, File)
     */
    public PutObjectResult putObject(String bucketName, String key, InputStream input,
                                     ObjectMetadata metadata) throws CosClientException, CosServiceException;

    /**
     * <p>
     * upload string content to a cos object. content will be encoded to bytes with UTF-8 encoding
     * </p>
     *
     * @param bucketName The name of an existing bucket, to which you have {@link Permission#Write}
     *                   permission.
     * @param key        The key under which to store the specified file.
     * @param content    the object content, content will be encoded to bytes with UTF-8 encoding.
     * @return A {@link PutObjectResult} object containing the information returned by for the newly
     * created object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#putObject(PutObjectRequest)
     * @see COS#putObject(String, String, File)
     */
    public PutObjectResult putObject(String bucketName, String key, String content)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the object stored in under the specified bucket and key.
     * </p>
     * <p>
     * Be extremely careful when using this method; the returned COS object contains a direct stream
     * of data from the HTTP connection. The underlying HTTP connection cannot be closed until the
     * user finishes reading the data and closes the stream. Therefore:
     * </p>
     * <ul>
     * <li>Use the data from the input stream in object as soon as possible</li>
     * <li>Close the input stream in object as soon as possible</li>
     * </ul>
     * If these rules are not followed, the client can run out of resources by allocating too many
     * open, but unused, HTTP connections.
     * </p>
     * <p>
     * To get an object from , the caller must have {@link Permission#Read} access to the object.
     * </p>
     * <p>
     * If the object fetched is publicly readable, it can also read it by pasting its URL into a
     * browser.
     * </p>
     * <p>
     * For more advanced options (such as downloading only a range of an object's content, or
     * placing constraints on when the object should be downloaded) callers can use
     * {@link #getObject(GetObjectRequest)}.
     * </p>
     *
     * @param bucketName The name of the bucket containing the desired object.
     * @param key        The key under which the desired object is stored.
     * @return The object stored in in the specified bucket and key.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#getObject(GetObjectRequest)
     * @see COS#getObject(GetObjectRequest, File)
     */
    public COSObject getObject(String bucketName, String key)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the object stored in under the specified bucket and key. Returns <code>null</code> if
     * the specified constraints weren't met.
     * </p>
     * <p>
     * Be extremely careful when using this method; the returned COS object contains a direct stream
     * of data from the HTTP connection. The underlying HTTP connection cannot be closed until the
     * user finishes reading the data and closes the stream. Therefore:
     * </p>
     * <ul>
     * <li>Use the data from the input stream in object as soon as possible,</li>
     * <li>Close the input stream in object as soon as possible.</li>
     * </ul>
     * <p>
     * If callers do not follow those rules, then the client can run out of resources if allocating
     * too many open, but unused, HTTP connections.
     * </p>
     * <p>
     * To get an object from , the caller must have {@link Permission#Read} access to the object.
     * </p>
     * <p>
     * If the object fetched is publicly readable, it can also read it by pasting its URL into a
     * browser.
     * </p>
     * <p>
     * When specifying constraints in the request object, the client needs to be prepared to handle
     * this method returning <code>null</code> if the provided constraints aren't met when Qcloud
     * COS receives the request.
     * </p>
     * <p>
     * If the advanced options provided in {@link GetObjectRequest} aren't needed, use the simpler
     * {@link COS#getObject(String bucketName, String key)} method.
     * </p>
     *
     * @param getObjectRequest The request object containing all the options on how to download the
     *                         object.
     * @return The object stored in in the specified bucket and key. Returns <code>null</code> if
     * constraints were specified but not met.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#getObject(String, String)
     * @see COS#getObject(GetObjectRequest, File)
     */
    public COSObject getObject(GetObjectRequest getObjectRequest)
            throws CosClientException, CosServiceException;


    /**
     * <p>
     * Gets the object metadata for the object stored in under the specified bucket and key, and
     * saves the object contents to the specified file. Returns <code>null</code> if the specified
     * constraints weren't met.
     * </p>
     * <p>
     * Instead of using {@link COS#getObject(GetObjectRequest)}, use this method to ensure that the
     * underlying HTTP stream resources are automatically closed as soon as possible. The clients
     * handles immediate storage of the object contents to the specified file.
     * </p>
     * <p>
     * To get an object from , the caller must have {@link Permission#Read} access to the object.
     * </p>
     * <p>
     * If the object fetched is publicly readable, it can also read it by pasting its URL into a
     * browser.
     * </p>
     * <p>
     * When specifying constraints in the request object, the client needs to be prepared to handle
     * this method returning <code>null</code> if the provided constraints aren't met when Qcloud
     * COS receives the request.
     * </p>
     *
     * @param getObjectRequest The request object containing all the options on how to download the
     *                         object content.
     * @param destinationFile  Indicates the file (which might already exist) where to save the
     *                         object content being downloading from .
     * @return All COS object metadata for the specified object. Returns <code>null</code> if
     * constraints were specified but not met.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request, handling the response, or writing the incoming data from COS to the
     *                             specified destination file.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#getObject(String, String)
     * @see COS#getObject(GetObjectRequest)
     */
    public ObjectMetadata getObject(GetObjectRequest getObjectRequest, File destinationFile)
            throws CosClientException, CosServiceException;

    /**
     * @param bucketName Name of bucket that presumably contains object
     * @param objectName Name of object that has to be checked
     * @return true if exist. otherwise false;
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request, handling the response, or writing the incoming data from COS to the
     *                             specified destination file.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    boolean doesObjectExist(String bucketName, String objectName)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the metadata for the specified object without actually fetching the object itself. This
     * is useful in obtaining only the object metadata, and avoids wasting bandwidth on fetching the
     * object data.
     * </p>
     * <p>
     * The object metadata contains information such as content type, content disposition, etc., as
     * well as custom user metadata that can be associated with an object in .
     * </p>
     *
     * @param bucketName bucket name
     * @param key        cos path
     * @return All COS object metadata for the specified object.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public ObjectMetadata getObjectMetadata(String bucketName, String key)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the metadata for the specified object without actually fetching the object itself. This
     * is useful in obtaining only the object metadata, and avoids wasting bandwidth on fetching the
     * object data.
     * </p>
     * <p>
     * The object metadata contains information such as content type, content disposition, etc., as
     * well as custom user metadata that can be associated with an object in .
     * </p>
     *
     * @param getObjectMetadataRequest The request object specifying the bucket, key and optional
     *                                 version ID of the object whose metadata is being retrieved.
     * @return All COS object metadata for the specified object.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#getObjectMetadata(String, String)
     */
    public ObjectMetadata getObjectMetadata(GetObjectMetadataRequest getObjectMetadataRequest)
            throws CosClientException, CosServiceException;


    /**
     * <p>
     * Deletes the specified object in the specified bucket. Once deleted, the object can only be
     * restored if versioning was enabled when the object was deleted.
     * </p>
     * <p>
     * If attempting to delete an object that does not exist, will return a success message instead
     * of an error message.
     * </p>
     *
     * @param bucketName bucket name
     * @param key        cos path
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */

    public void deleteObject(String bucketName, String key)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Deletes the specified object in the specified bucket. Once deleted, the object can only be
     * restored if versioning was enabled when the object was deleted.
     * </p>
     * <p>
     * If attempting to delete an object that does not exist, will return a success message instead
     * of an error message.
     * </p>
     *
     * @param deleteObjectRequest The request object containing all options for deleting an Qcloud
     *                            COS object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COSClient#deleteObject(String, String)
     */
    public void deleteObject(DeleteObjectRequest deleteObjectRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Deletes a specific version of the specified object in the specified bucket. Once deleted,
     * there is no method to restore or undelete an object version. This is the only way to
     * permanently delete object versions that are protected by versioning.
     * </p>
     * <p>
     * Deleting an object version is permanent and irreversible. It is a privileged operation that
     * only the owner of the bucket containing the version can perform.
     * </p>
     * <p>
     * Users can only delete a version of an object if versioning is enabled for the bucket. For
     * more information about enabling versioning for a bucket, see
     * {@link #setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     * <p>
     * If attempting to delete an object that does not exist, COS will return a success message
     * instead of an error message.
     * </p>
     *
     * @param bucketName The name of the COS bucket containing the object to delete.
     * @param key        The key of the object to delete.
     * @param versionId  The version of the object to delete.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void deleteVersion(String bucketName, String key, String versionId)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Deletes a specific version of the specified object in the specified bucket. Once deleted,
     * there is no method to restore or undelete an object version. This is the only way to
     * permanently delete object versions that are protected by versioning.
     * </p>
     * <p>
     * Deleting an object version is permanent and irreversible. It is a privileged operation that
     * only the owner of the bucket containing the version can perform.
     * </p>
     * <p>
     * Users can only delete a version of an object if versioning is enabled for the bucket. For
     * more information about enabling versioning for a bucket, see
     * {@link #setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     * <p>
     * If attempting to delete an object that does not exist, COS will return a success message
     * instead of an error message.
     * </p>
     *
     * @param deleteVersionRequest The request object containing all options for deleting a specific
     *                             version of an COS object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void deleteVersion(DeleteVersionRequest deleteVersionRequest)
            throws CosClientException, CosServiceException;

    ;

    /**
     * Deletes multiple objects in a single bucket from COS.
     * <p>
     * In some cases, some objects will be successfully deleted, while some attempts will cause an
     * error. If any object in the request cannot be deleted, this method throws a
     * {@link MultiObjectDeleteException} with details of the error.
     *
     * @param deleteObjectsRequest The request object containing all options for deleting multiple
     *                             objects.
     * @throws MultiObjectDeleteException if one or more of the objects couldn't be deleted.
     * @throws CosClientException         If any errors are encountered in the client while making the
     *                                    request or handling the response.
     * @throws CosServiceException        If any errors occurred in while processing the request.
     */
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest)
            throws MultiObjectDeleteException, CosClientException, CosServiceException;


    /**
     * <p>
     * Creates a new bucket in the region which is set in ClientConfig
     * </p>
     * <p>
     * Every object stored in is contained within a bucket. Appid and Bucket partition the namespace
     * of objects stored in at the top level. Within a bucket, any name can be used for objects.
     * </p>
     * <p>
     * There are no limits to the number of objects that can be stored in a bucket. Performance does
     * not vary based on the number of buckets used. Store all objects within a single bucket or
     * organize them across several buckets.
     * </p>
     * <p>
     * Buckets cannot be nested; buckets cannot be created within other buckets.
     * </p>
     * <p>
     * Do not make bucket create or delete calls in the high availability code path of an
     * application. Create or delete buckets in a separate initialization or setup routine that runs
     * less often.
     * </p>
     * <p>
     * To create a bucket, authenticate with an account that has a valid Qcloud Access Key ID and is
     * registered with . Anonymous requests are never allowed to create buckets.
     * </p>
     *
     * @param bucketName The name of the bucket to be created
     * @return The newly created bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public Bucket createBucket(String bucketName) throws CosClientException, CosServiceException;


    /**
     * <p>
     * Creates a new bucket in the region which is set in ClientConfig
     * </p>
     * <p>
     * Every object stored in is contained within a bucket. Appid and Bucket partition the namespace
     * of objects stored in at the top level. Within a bucket, any name can be used for objects.
     * </p>
     * <p>
     * There are no limits to the number of objects that can be stored in a bucket. Performance does
     * not vary based on the number of buckets used. Store all objects within a single bucket or
     * organize them across several buckets.
     * </p>
     * <p>
     * Buckets cannot be nested; buckets cannot be created within other buckets.
     * </p>
     * <p>
     * Do not make bucket create or delete calls in the high availability code path of an
     * application. Create or delete buckets in a separate initialization or setup routine that runs
     * less often.
     * </p>
     * <p>
     * To create a bucket, authenticate with an account that has a valid Qcloud Access Key ID and is
     * registered with . Anonymous requests are never allowed to create buckets.
     * </p>
     *
     * @param createBucketRequest The request object containing all options for creating an Qcloud
     *                            COS bucket.
     * @return The newly created bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public Bucket createBucket(CreateBucketRequest createBucketRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Deletes the specified bucket. All objects (and all object versions, if versioning was ever
     * enabled) in the bucket must be deleted before the bucket itself can be deleted.
     * </p>
     * <p>
     * Only the owner of a bucket can delete it, regardless of the bucket's access control policy
     * (ACL).
     * </p>
     *
     * @param bucketName The name of the bucket to be deleted
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void deleteBucket(String bucketName) throws CosClientException, CosServiceException;

    /**
     * <p>
     * Deletes the specified bucket. All objects (and all object versions, if versioning was ever
     * enabled) in the bucket must be deleted before the bucket itself can be deleted.
     * </p>
     * <p>
     * Only the owner of a bucket can delete it, regardless of the bucket's access control policy
     * (ACL).
     * </p>
     *
     * @param deleteBucketRequest The request object containing all options for deleting an Qcloud
     *                            COS bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#deleteBucket(String)
     */
    public void deleteBucket(DeleteBucketRequest deleteBucketRequest)
            throws CosClientException, CosServiceException;

    /**
     * Checks if the specified bucket exists. use this method to determine if a specified bucket
     * name already exists, and therefore can't be used to create a new bucket.
     *
     * <p>
     * Internally this uses the {@link #getBucketAcl(String)} operation to determine whether the
     * bucket exists.
     * </p>
     *
     * @param bucketName The name of the bucket to check.
     * @return The value <code>true</code> if the specified bucket exists ; the value
     * <code>false</code> if there is no bucket with that name.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public boolean doesBucketExist(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Performs a head bucket operation on the requested bucket name. This operation is useful to
     * determine if a bucket exists and you have permission to access it.
     *
     * @param headBucketRequest The request containing the bucket name.
     * @return This method returns a {@link HeadBucketResult} if the bucket exists and you have
     * permission to access it. Otherwise, the method will throw an
     * {@link CosServiceException} with status code {@code '404 Not Found'} if the bucket
     * does not exist, {@code '403 Forbidden'} if the user does not have access to the
     * bucket
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public HeadBucketResult headBucket(HeadBucketRequest headBucketRequest)
            throws CosClientException, CosServiceException;

    ;

    /**
     * <p>
     * Returns a list of all buckets that the authenticated sender of the request owns.
     * </p>
     *
     * @return A list of all of the buckets owned by the authenticated sender of the request.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public List<Bucket> listBuckets() throws CosClientException, CosServiceException;

    /**
     * <p>
     * Returns a list of all buckets that the authenticated sender of the request owns.
     * </p>
     *
     * @param listBucketsRequest The request containing all of the options related to the listing of
     *                           buckets.
     * @return A list of all of the buckets owned by the authenticated sender of the request.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public List<Bucket> listBuckets(ListBucketsRequest listBucketsRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the geographical region where stores the specified bucket.
     * </p>
     * <p>
     * To view the location constraint of a bucket, the user must be the bucket owner.
     * </p>
     *
     * @param bucketName The name of the bucket to get location
     * @return The location of the specified bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public String getBucketLocation(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the geographical region where stores the specified bucket.
     * </p>
     * <p>
     * To view the location constraint of a bucket, the user must be the bucket owner.
     * </p>
     *
     * @param getBucketLocationRequest The request object containing the name of the bucket to look
     *                                 up. This must be a bucket the user owns.
     * @return The location of the specified bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public String getBucketLocation(GetBucketLocationRequest getBucketLocationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Initiates a multipart upload and returns an InitiateMultipartUploadResult which contains an
     * upload ID. This upload ID associates all the parts in the specific upload and is used in each
     * of your subsequent {@link #uploadPart(UploadPartRequest)} requests. You also include this
     * upload ID in the final request to either complete, or abort the multipart upload request.
     * <p>
     * <b>Note:</b> After you initiate a multipart upload and upload one or more parts, you must
     * either complete or abort the multipart upload in order to stop getting charged for storage of
     * the uploaded parts. Once you complete or abort the multipart upload will release the stored
     * parts and stop charging you for their storage.
     * </p>
     *
     * @param request The InitiateMultipartUploadRequest object that specifies all the parameters of
     *                this operation.
     * @return An InitiateMultipartUploadResult from .
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest request) throws CosClientException, CosServiceException;

    /**
     * Uploads a part in a multipart upload. You must initiate a multipart upload before you can
     * upload any part.
     * <p>
     * Your UploadPart request must include an upload ID and a part number. The upload ID is the ID
     * returned by in response to your Initiate Multipart Upload request. Part number can be any
     * number between 1 and 10,000, inclusive. A part number uniquely identifies a part and also
     * defines its position within the object being uploaded. If you upload a new part using the
     * same part number that was specified in uploading a previous part, the previously uploaded
     * part is overwritten.
     * <p>
     * To ensure data is not corrupted traversing the network, specify the Content-MD5 header in the
     * Upload Part request. checks the part data against the provided MD5 value. If they do not
     * match, returns an error.
     * <p>
     * When you upload a part, the returned UploadPartResult contains an ETag property. You should
     * record this ETag property value and the part number. After uploading all parts, you must send
     * a CompleteMultipartUpload request. At that time constructs a complete object by concatenating
     * all the parts you uploaded, in ascending order based on the part numbers. The
     * CompleteMultipartUpload request requires you to send all the part numbers and the
     * corresponding ETag values.
     * <p>
     * <b>Note:</b> After you initiate a multipart upload and upload one or more parts, you must
     * either complete or abort the multipart upload in order to stop getting charged for storage of
     * the uploaded parts. Once you complete or abort the multipart upload will release the stored
     * parts and stop charging you for their storage.
     * </p>
     *
     * @param request The UploadPartRequest object that specifies all the parameters of this
     *                operation.
     * @return An UploadPartResult from containing the part number and ETag of the new part.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public UploadPartResult uploadPart(UploadPartRequest uploadPartRequest)
            throws CosClientException, CosServiceException;

    /**
     * Lists the parts that have been uploaded for a specific multipart upload.
     * <p>
     * This method must include the upload ID, returned by the
     * {@link #initiateMultipartUpload(InitiateMultipartUploadRequest)} operation. This request
     * returns a maximum of 1000 uploaded parts by default. You can restrict the number of parts
     * returned by specifying the MaxParts property on the ListPartsRequest. If your multipart
     * upload consists of more parts than allowed in the ListParts response, the response returns a
     * IsTruncated field with value true, and a NextPartNumberMarker property. In subsequent
     * ListParts request you can include the PartNumberMarker property and set its value to the
     * NextPartNumberMarker property value from the previous response.
     *
     * @param request The ListPartsRequest object that specifies all the parameters of this
     *                operation.
     * @return Returns a PartListing from .
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public PartListing listParts(ListPartsRequest request)
            throws CosClientException, CosServiceException;

    /**
     * Aborts a multipart upload. After a multipart upload is aborted, no additional parts can be
     * uploaded using that upload ID. The storage consumed by any previously uploaded parts will be
     * freed.
     *
     * @param request The AbortMultipartUploadRequest object that specifies all the parameters of
     *                this operation.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void abortMultipartUpload(AbortMultipartUploadRequest request)
            throws CosClientException, CosServiceException;

    /**
     * Completes a multipart upload by assembling previously uploaded parts.
     * <p>
     * You first upload all parts using the {@link #uploadPart(UploadPartRequest)} method. After
     * successfully uploading all individual parts of an upload, you call this operation to complete
     * the upload. Upon receiving this request, concatenates all the parts in ascending order by
     * part number to create a new object. In the CompleteMultipartUpload request, you must provide
     * the parts list. For each part in the list, you provide the part number and the ETag header
     * value, returned after that part was uploaded.
     * <p>
     * Processing of a CompleteMultipartUpload request may take several minutes to complete.
     * </p>
     *
     * @param request The CompleteMultipartUploadRequest object that specifies all the parameters of
     *                this operation.
     * @return A CompleteMultipartUploadResult from COS containing the ETag for the new object
     * composed of the individual parts.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public CompleteMultipartUploadResult completeMultipartUpload(
            CompleteMultipartUploadRequest request) throws CosClientException, CosServiceException;

    /**
     * Lists in-progress multipart uploads. An in-progress multipart upload is a multipart upload
     * that has been initiated, using the InitiateMultipartUpload request, but has not yet been
     * completed or aborted.
     * <p>
     * This operation returns at most 1,000 multipart uploads in the response by default. The number
     * of multipart uploads can be further limited using the MaxUploads property on the request
     * parameter. If there are additional multipart uploads that satisfy the list criteria, the
     * response will contain an IsTruncated property with the value set to true. To list the
     * additional multipart uploads use the KeyMarker and UploadIdMarker properties on the request
     * parameters.
     *
     * @param request The ListMultipartUploadsRequest object that specifies all the parameters of
     *                this operation.
     * @return A MultipartUploadListing from .
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest request)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Returns a list of summary information about the objects in the specified buckets. List
     * results are <i>always</i> returned in lexicographic (alphabetical) order.
     * </p>
     * <p>
     * Because buckets can contain a virtually unlimited number of keys, the complete results of a
     * list query can be extremely large. To manage large result sets, uses pagination to split them
     * into multiple responses. Always check the {@link ObjectListing#isTruncated()} method to see
     * if the returned listing is complete or if additional calls are needed to get more results.
     * Alternatively, use the {@link COS#listNextBatchOfObjects(ObjectListing)} method as an easy
     * way to get the next page of object listings.
     * </p>
     * <p>
     * The total number of keys in a bucket doesn't substantially affect list performance.
     * </p>
     *
     * @param bucketName The name of the bucket to list.
     * @return A listing of the objects in the specified bucket, along with any other associated
     * information, such as common prefixes (if a delimiter was specified), the original
     * request parameters, etc.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listObjects(String, String)
     * @see COS#listObjects(ListObjectsRequest)
     */
    public ObjectListing listObjects(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Returns a list of summary information about the objects in the specified bucket. Depending on
     * request parameters, additional information is returned, such as common prefixes if a
     * delimiter was specified. List results are <i>always</i> returned in lexicographic
     * (alphabetical) order.
     * </p>
     * <p>
     * Because buckets can contain a virtually unlimited number of keys, the complete results of a
     * list query can be extremely large. To manage large result sets, uses pagination to split them
     * into multiple responses. Always check the {@link ObjectListing#isTruncated()} method to see
     * if the returned listing is complete or if additional calls are needed to get more results.
     * Alternatively, use the {@link COS#listNextBatchOfObjects(ObjectListing)} method as an easy
     * way to get the next page of object listings.
     * </p>
     * <p>
     * For example, consider a bucket that contains the following keys:
     * <ul>
     * <li>"foo/bar/baz"</li>
     * <li>"foo/bar/bash"</li>
     * <li>"foo/bar/bang"</li>
     * <li>"foo/boo"</li>
     * </ul>
     * If calling <code>listObjects</code> with a <code>prefix</code> value of "foo/" and a
     * <code>delimiter</code> value of "/" on this bucket, an <code>ObjectListing</code> is returned
     * that contains one key ("foo/boo") and one entry in the common prefixes list ("foo/bar/"). To
     * see deeper into the virtual hierarchy, make another call to <code>listObjects</code> setting
     * the prefix parameter to any interesting common prefix to list the individual keys under that
     * prefix.
     * </p>
     * <p>
     * The total number of keys in a bucket doesn't substantially affect list performance.
     * </p>
     *
     * @param bucketName The name of the bucket to list.
     * @param prefix     An optional parameter restricting the response to keys beginning with the
     *                   specified prefix. Use prefixes to separate a bucket into different sets of keys,
     *                   similar to how a file system organizes files into directories.
     * @return A listing of the objects in the specified bucket, along with any other associated
     * information, such as common prefixes (if a delimiter was specified), the original
     * request parameters, etc.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listObjects(String)
     * @see COS#listObjects(ListObjectsRequest)
     */
    public ObjectListing listObjects(String bucketName, String prefix)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Returns a list of summary information about the objects in the specified bucket. Depending on
     * the request parameters, additional information is returned, such as common prefixes if a
     * delimiter was specified. List results are <i>always</i> returned in lexicographic
     * (alphabetical) order.
     * </p>
     * <p>
     * Because buckets can contain a virtually unlimited number of keys, the complete results of a
     * list query can be extremely large. To manage large result sets, uses pagination to split them
     * into multiple responses. Always check the {@link ObjectListing#isTruncated()} method to see
     * if the returned listing is complete or if additional calls are needed to get more results.
     * Alternatively, use the {@link COS#listNextBatchOfObjects(ObjectListing)} method as an easy
     * way to get the next page of object listings.
     * </p>
     * <p>
     * Calling {@link ListObjectsRequest#setDelimiter(String)} sets the delimiter, allowing groups
     * of keys that share the delimiter-terminated prefix to be included in the returned listing.
     * This allows applications to organize and browse their keys hierarchically, similar to how a
     * file system organizes files into directories. These common prefixes can be retrieved through
     * the {@link ObjectListing#getCommonPrefixes()} method.
     * </p>
     * <p>
     * For example, consider a bucket that contains the following keys:
     * <ul>
     * <li>"foo/bar/baz"</li>
     * <li>"foo/bar/bash"</li>
     * <li>"foo/bar/bang"</li>
     * <li>"foo/boo"</li>
     * </ul>
     * If calling <code>listObjects</code> with a prefix value of "foo/" and a delimiter value of
     * "/" on this bucket, an <code>ObjectListing</code> is returned that contains one key
     * ("foo/boo") and one entry in the common prefixes list ("foo/bar/"). To see deeper into the
     * virtual hierarchy, make another call to <code>listObjects</code> setting the prefix parameter
     * to any interesting common prefix to list the individual keys under that prefix.
     * </p>
     * <p>
     * The total number of keys in a bucket doesn't substantially affect list performance.
     * </p>
     *
     * @param listObjectsRequest The request object containing all options for listing the objects
     *                           in a specified bucket.
     * @return A listing of the objects in the specified bucket, along with any other associated
     * information, such as common prefixes (if a delimiter was specified), the original
     * request parameters, etc.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listObjects(String)
     * @see COS#listObjects(String, String)
     */
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Provides an easy way to continue a truncated object listing and retrieve the next page of
     * results.
     * </p>
     * <p>
     * To continue the object listing and retrieve the next page of results, call the initial
     * {@link ObjectListing} from one of the <code>listObjects</code> methods. If truncated
     * (indicated when {@link ObjectListing#isTruncated()} returns <code>true</code>), pass the
     * <code>ObjectListing</code> back into this method in order to retrieve the next page of
     * results. Continue using this method to retrieve more results until the returned
     * <code>ObjectListing</code> indicates that it is not truncated.
     * </p>
     *
     * @param previousObjectListing The previous truncated <code>ObjectListing</code>. If a
     *                              non-truncated <code>ObjectListing</code> is passed in, an empty
     *                              <code>ObjectListing</code> is returned without ever contacting .
     * @return The next set of <code>ObjectListing</code> results, beginning immediately after the
     * last result in the specified previous <code>ObjectListing</code>.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listObjects(String)
     * @see COS#listObjects(String, String)
     * @see COS#listObjects(ListObjectsRequest)
     * @see COS#listNextBatchOfObjects(ListNextBatchOfObjectsRequest)
     */
    public ObjectListing listNextBatchOfObjects(ObjectListing previousObjectListing)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Provides an easy way to continue a truncated object listing and retrieve the next page of
     * results.
     * </p>
     * <p>
     * To continue the object listing and retrieve the next page of results, call the initial
     * {@link ObjectListing} from one of the <code>listObjects</code> methods. If truncated
     * (indicated when {@link ObjectListing#isTruncated()} returns <code>true</code>), pass the
     * <code>ObjectListing</code> back into this method in order to retrieve the next page of
     * results. Continue using this method to retrieve more results until the returned
     * <code>ObjectListing</code> indicates that it is not truncated.
     * </p>
     *
     * @param listNextBatchOfObjectsRequest The request object for listing next batch of objects
     *                                      using the previous truncated <code>ObjectListing</code>. If a non-truncated
     *                                      <code>ObjectListing</code> is passed in by the request object, an empty
     *                                      <code>ObjectListing</code> is returned without ever contacting .
     * @return The next set of <code>ObjectListing</code> results, beginning immediately after the
     * last result in the specified previous <code>ObjectListing</code>.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listObjects(String)
     * @see COS#listObjects(String, String)
     * @see COS#listObjects(ListObjectsRequest)
     * @see COS#listNextBatchOfObjects(ObjectListing)
     */
    public ObjectListing listNextBatchOfObjects(
            ListNextBatchOfObjectsRequest listNextBatchOfObjectsRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Returns a list of summary information about the versions in the specified bucket.
     * </p>
     * <p>
     * The returned version summaries are ordered first by key and then by version. Keys are sorted
     * lexicographically (alphabetically) while versions are sorted from most recent to least
     * recent. Both versions with data and delete markers are included in the results.
     * </p>
     * <p>
     * Because buckets can contain a virtually unlimited number of versions, the complete results of
     * a list query can be extremely large. To manage large result sets, COS uses pagination to
     * split them into multiple responses. Always check the {@link VersionListing#isTruncated()}
     * method to determine if the returned listing is complete or if additional calls are needed to
     * get more results. Callers are encouraged to use
     * {@link COS#listNextBatchOfVersions(VersionListing)} as an easy way to get the next page of
     * results.
     * </p>
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link #setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     *
     * @param bucketName The name of the COS bucket whose versions are to be listed.
     * @param prefix     An optional parameter restricting the response to keys beginning with the
     *                   specified prefix. Use prefixes to separate a bucket into different sets of keys,
     *                   similar to how a file system organizes files into directories.
     * @return A listing of the versions in the specified bucket, along with any other associated
     * information and original request parameters.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listVersions(ListVersionsRequest)
     * @see COS#listVersions(String, String, String, String, String, Integer)
     */
    public VersionListing listVersions(String bucketName, String prefix)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Returns a list of summary information about the versions in the specified bucket.
     * </p>
     * <p>
     * The returned version summaries are ordered first by key and then by version. Keys are sorted
     * lexicographically (alphabetically) while versions are sorted from most recent to least
     * recent. Both versions with data and delete markers are included in the results.
     * </p>
     * <p>
     * Because buckets can contain a virtually unlimited number of versions, the complete results of
     * a list query can be extremely large. To manage large result sets, COS uses pagination to
     * split them into multiple responses. Always check the {@link VersionListing#isTruncated()}
     * method to determine if the returned listing is complete or if additional calls are needed to
     * get more results. Callers are encouraged to use
     * {@link COS#listNextBatchOfVersions(VersionListing)} as an easy way to get the next page of
     * results.
     * </p>
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link #setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     *
     * @param bucketName      The name of the QCloud COS bucket whose versions are to be listed.
     * @param prefix          An optional parameter restricting the response to keys that begin with the
     *                        specified prefix. Use prefixes to separate a bucket into different sets of keys,
     *                        similar to how a file system organizes files into directories.
     * @param keyMarker       Optional parameter indicating where in the sorted list of all versions in
     *                        the specified bucket to begin returning results. Results are always ordered first
     *                        lexicographically (i.e. alphabetically) and then from most recent version to least
     *                        recent version. If a keyMarker is used without a versionIdMarker, results begin
     *                        immediately after that key's last version. When a keyMarker is used with a
     *                        versionIdMarker, results begin immediately after the version with the specified key
     *                        and version ID.
     *                        <p>
     *                        This enables pagination; to get the next page of results use the next key marker and
     *                        next version ID marker (from {@link VersionListing#getNextKeyMarker()} and
     *                        {@link VersionListing#getNextVersionIdMarker()}) as the markers for the next request
     *                        to list versions, or use the convenience method
     *                        {@link COS#listNextBatchOfVersions(VersionListing)}
     * @param versionIdMarker Optional parameter indicating where in the sorted list of all versions
     *                        in the specified bucket to begin returning results. Results are always ordered first
     *                        lexicographically (i.e. alphabetically) and then from most recent version to least
     *                        recent version. A keyMarker must be specified when specifying a versionIdMarker.
     *                        Results begin immediately after the version with the specified key and version ID.
     *                        <p>
     *                        This enables pagination; to get the next page of results use the next key marker and
     *                        next version ID marker (from {@link VersionListing#getNextKeyMarker()} and
     *                        {@link VersionListing#getNextVersionIdMarker()}) as the markers for the next request
     *                        to list versions, or use the convenience method
     *                        {@link COS#listNextBatchOfVersions(VersionListing)}
     * @param delimiter       Optional parameter that causes keys that contain the same string between the
     *                        prefix and the first occurrence of the delimiter to be rolled up into a single result
     *                        element in the {@link VersionListing#getCommonPrefixes()} list. These rolled-up keys
     *                        are not returned elsewhere in the response. The most commonly used delimiter is "/",
     *                        which simulates a hierarchical organization similar to a file system directory
     *                        structure.
     * @param maxResults      Optional parameter indicating the maximum number of results to include in
     *                        the response. QCloud COS might return fewer than this, but will not return more. Even
     *                        if maxKeys is not specified, QCloud COS will limit the number of results in the
     *                        response.
     * @return A listing of the versions in the specified bucket, along with any other associated
     * information and original request parameters.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listVersions(ListVersionsRequest)
     * @see COS#listVersions(String, String)
     */
    public VersionListing listVersions(String bucketName, String prefix, String keyMarker,
                                       String versionIdMarker, String delimiter, Integer maxResults)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Returns a list of summary information about the versions in the specified bucket.
     * </p>
     * <p>
     * The returned version summaries are ordered first by key and then by version. Keys are sorted
     * lexicographically (alphabetically) while versions are sorted from most recent to least
     * recent. Both versions with data and delete markers are included in the results.
     * </p>
     * <p>
     * Because buckets can contain a virtually unlimited number of versions, the complete results of
     * a list query can be extremely large. To manage large result sets, COS uses pagination to
     * split them into multiple responses. Always check the {@link VersionListing#isTruncated()}
     * method to determine if the returned listing is complete or if additional calls are needed to
     * get more results. Callers are encouraged to use
     * {@link COS#listNextBatchOfVersions(VersionListing)} as an easy way to get the next page of
     * results.
     * </p>
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link #setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     *
     * @param listVersionsRequest The request object containing all options for listing the versions
     *                            in a specified bucket.
     * @return A listing of the versions in the specified bucket, along with any other associated
     * information and original request parameters.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listVersions(String, String, String, String, String, Integer)
     * @see COS#listVersions(String, String)
     */
    public VersionListing listVersions(ListVersionsRequest listVersionsRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Provides an easy way to continue a truncated {@link VersionListing} and retrieve the next
     * page of results.
     * </p>
     * <p>
     * Obtain the initial <code>VersionListing</code> from one of the <code>listVersions</code>
     * methods. If the result is truncated (indicated when {@link VersionListing#isTruncated()}
     * returns <code>true</code>), pass the <code>VersionListing</code> back into this method in
     * order to retrieve the next page of results. From there, continue using this method to
     * retrieve more results until the returned <code>VersionListing</code> indicates that it is not
     * truncated.
     * </p>
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link #setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     *
     * @param previousVersionListing The previous truncated <code>VersionListing</code>. If a
     *                               non-truncated <code>VersionListing</code> is passed in, an empty
     *                               <code>VersionListing</code> is returned without ever contacting COS.
     * @return The next set of <code>VersionListing</code> results, beginning immediately after the
     * last result in the specified previous <code>VersionListing</code>.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listVersions(String, String)
     * @see COS#listVersions(ListVersionsRequest)
     * @see COS#listVersions(String, String, String, String, String, Integer)
     * @see COS#listNextBatchOfVersions(ListNextBatchOfVersionsRequest)
     */
    public VersionListing listNextBatchOfVersions(VersionListing previousVersionListing)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Provides an easy way to continue a truncated {@link VersionListing} and retrieve the next
     * page of results.
     * </p>
     * <p>
     * Obtain the initial <code>VersionListing</code> from one of the <code>listVersions</code>
     * methods. If the result is truncated (indicated when {@link VersionListing#isTruncated()}
     * returns <code>true</code>), pass the <code>VersionListing</code> back into this method in
     * order to retrieve the next page of results. From there, continue using this method to
     * retrieve more results until the returned <code>VersionListing</code> indicates that it is not
     * truncated.
     * </p>
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link #setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     *
     * @param listNextBatchOfVersionsRequest The request object for listing next batch of versions
     *                                       using the previous truncated <code>VersionListing</code>. If a non-truncated
     *                                       <code>VersionListing</code> is passed in by the request object, an empty
     *                                       <code>VersionListing</code> is returned without ever contacting COS.
     * @return The next set of <code>VersionListing</code> results, beginning immediately after the
     * last result in the specified previous <code>VersionListing</code>.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     * @see COS#listVersions(String, String)
     * @see COS#listVersions(ListVersionsRequest)
     * @see COS#listVersions(String, String, String, String, String, Integer)
     * @see COS#listNextBatchOfVersions(VersionListing)
     */
    public VersionListing listNextBatchOfVersions(
            ListNextBatchOfVersionsRequest listNextBatchOfVersionsRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Copy a source object to a new destination in COS.
     * </p>
     * <p>
     * To copy an object, the caller's account must have read access to the source object and write
     * access to the destination bucket. cos support copy a object from a diff account, diff region,
     * diff bucket
     * </p>
     *
     * @param sourceBucketName      The name of the bucket containing the source object to copy.
     * @param sourceKey             The key in the source bucket under which the source object is stored.
     * @param destinationBucketName The name of the bucket in which the new object will be created.
     *                              This can be the same name as the source bucket's.
     * @param destinationKey        The key in the destination bucket under which the new object will be
     *                              created.
     * @return A {@link CopyObjectResult} object containing the information returned by about the
     * newly created object, or <code>null</code> if constraints were specified that weren't
     * met when attempted to copy the object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */

    public CopyObjectResult copyObject(String sourceBucketName, String sourceKey,
                                       String destinationBucketName, String destinationKey)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Copy a source object to a new destination in COS.
     * </p>
     * <p>
     * To copy an object, the caller's account must have read access to the source object and write
     * access to the destination bucket. cos support copy a object from a diff account, diff region,
     * diff bucket
     * </p>
     *
     * @param copyObjectRequest The request object containing all the options for copying an QCloud
     *                          COS object.
     * @return A {@link CopyObjectResult} object containing the information returned by about the
     * newly created object, or <code>null</code> if constraints were specified that weren't
     * met when attempted to copy the object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest)
            throws CosClientException, CosServiceException;

    /**
     * Copies a source object to a part of a multipart upload.
     * <p>
     * To copy an object, the caller's account must have read access to the source object and write
     * access to the destination bucket.
     * </p>
     *
     * @param copyPartRequest The request object containing all the options for copying an object.
     * @return CopyPartResult containing the information returned by COS about the newly created
     * object, or <code>null</code> if constraints were specified that weren't met when COS
     * attempted to copy the object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public CopyPartResult copyPart(CopyPartRequest copyPartRequest)
            throws CosClientException, CosServiceException;


    /**
     * Sets the lifecycle configuration for the specified bucket.
     *
     * @param bucketName                   the bucket name
     * @param bucketLifecycleConfiguration lifecycle config for the bucket
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */

    public void setBucketLifecycleConfiguration(String bucketName,
                                                BucketLifecycleConfiguration bucketLifecycleConfiguration)
            throws CosClientException, CosServiceException;

    /**
     * Sets the lifecycle configuration for the specified bucket.
     *
     * @param setBucketLifecycleConfigurationRequest The request object containing all options for
     *                                               setting the bucket lifecycle configuration.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             `------+++++++++++++++++++++++++++++++++++++++++++++++++* request or handling the
     *                             response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setBucketLifecycleConfiguration(
            SetBucketLifecycleConfigurationRequest setBucketLifecycleConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Gets the lifecycle configuration for the specified bucket, or null if the specified bucket
     * does not exist or if no configuration has been established.
     *
     * @param bucketName the bucket name
     * @return BucketLifecycleConfiguration the bucket lifecycle configuration
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public BucketLifecycleConfiguration getBucketLifecycleConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Gets the lifecycle configuration for the specified bucket, or null if the specified bucket
     * does not exist or if no configuration has been established.
     *
     * @param getBucketLifecycleConfigurationRequest The request object for retrieving the bucket
     *                                               lifecycle configuration.
     * @return BucketLifecycleConfiguration the bucket lifecycle configuration
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public BucketLifecycleConfiguration getBucketLifecycleConfiguration(
            GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Removes the lifecycle configuration for the bucket specified.
     *
     * @param bucketName the bucket name
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void deleteBucketLifecycleConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Removes the lifecycle configuration for the bucket specified.
     *
     * @param deleteBucketLifecycleConfigurationRequest The request object containing all options
     *                                                  for removing the bucket lifecycle configuration.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void deleteBucketLifecycleConfiguration(
            DeleteBucketLifecycleConfigurationRequest deleteBucketLifecycleConfigurationRequest)
            throws CosClientException, CosServiceException;

    ;

    /**
     * <p>
     * Sets the versioning configuration for the specified bucket.
     * </p>
     * <p>
     * A bucket's versioning configuration can be in one of three possible states:
     * <ul>
     * <li>{@link BucketVersioningConfiguration#OFF}
     * <li>{@link BucketVersioningConfiguration#ENABLED}
     * <li>{@link BucketVersioningConfiguration#SUSPENDED}
     * </ul>
     * </p>
     * <p>
     * By default, new buckets are in the {@link BucketVersioningConfiguration#OFF off} state. Once
     * versioning is enabled for a bucket the status can never be reverted to
     * {@link BucketVersioningConfiguration#OFF off}.
     * </p>
     * <p>
     * Objects created before versioning was enabled or when versioning is suspended will be given
     * the default <code>null</code> version ID. Note that
     * the <code>null</code> version ID is a valid version ID and is not the same as not having a
     * version ID.
     * </p>
     * <p>
     * The versioning configuration of a bucket has different implications for each operation
     * performed on that bucket or for objects within that bucket. For example, when versioning is
     * enabled a <code>PutObject</code> operation creates a unique object version-id for the object
     * being uploaded. The The <code>PutObject</code> API guarantees that, if versioning is enabled
     * for a bucket at the time of the request, the new object can only be permanently deleted using
     * a <code>DeleteVersion</code> operation. It can never be overwritten. Additionally, the
     * <code>PutObject</code> API guarantees that, if versioning is enabled for a bucket the
     * request, no other object will be overwritten by that request. Refer to the documentation
     * sections for each API for information on how versioning status affects the semantics of that
     * particular API.
     * </p>
     *
     * @param setBucketVersioningConfigurationRequest The request object containing all options for
     *                                                setting the bucket versioning configuration.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setBucketVersioningConfiguration(
            SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest)
            throws CosClientException, CosServiceException;


    /**
     * <p>
     * Returns the versioning configuration for the specified bucket.
     * </p>
     * <p>
     * A bucket's versioning configuration can be in one of three possible states:
     * <ul>
     * <li>{@link BucketVersioningConfiguration#OFF}
     * <li>{@link BucketVersioningConfiguration#ENABLED}
     * <li>{@link BucketVersioningConfiguration#SUSPENDED}
     * </ul>
     * </p>
     * <p>
     * By default, new buckets are in the {@link BucketVersioningConfiguration#OFF off} state. Once
     * versioning is enabled for a bucket the status can never be reverted to
     * {@link BucketVersioningConfiguration#OFF off}.
     * </p>
     * <p>
     * The versioning configuration of a bucket has different implications for each operation
     * performed on that bucket or for objects within that bucket. For example, when versioning is
     * enabled a <code>PutObject</code> operation creates a unique object version-id for the object
     * being uploaded. The The <code>PutObject</code> API guarantees that, if versioning is enabled
     * for a bucket at the time of the request, the new object can only be permanently deleted using
     * a <code>DeleteVersion</code> operation. It can never be overwritten. Additionally, the
     * <code>PutObject</code> API guarantees that, if versioning is enabled for a bucket the
     * request, no other object will be overwritten by that request.
     * </p>
     *
     * @param bucketName the bucket name
     * @return The bucket versioning configuration for the specified bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public BucketVersioningConfiguration getBucketVersioningConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Returns the versioning configuration for the specified bucket.
     * </p>
     * <p>
     * A bucket's versioning configuration can be in one of three possible states:
     * <ul>
     * <li>{@link BucketVersioningConfiguration#OFF}
     * <li>{@link BucketVersioningConfiguration#ENABLED}
     * <li>{@link BucketVersioningConfiguration#SUSPENDED}
     * </ul>
     * </p>
     * <p>
     * By default, new buckets are in the {@link BucketVersioningConfiguration#OFF off} state. Once
     * versioning is enabled for a bucket the status can never be reverted to
     * {@link BucketVersioningConfiguration#OFF off}.
     * </p>
     * <p>
     * The versioning configuration of a bucket has different implications for each operation
     * performed on that bucket or for objects within that bucket. For example, when versioning is
     * enabled a <code>PutObject</code> operation creates a unique object version-id for the object
     * being uploaded. The The <code>PutObject</code> API guarantees that, if versioning is enabled
     * for a bucket at the time of the request, the new object can only be permanently deleted using
     * a <code>DeleteVersion</code> operation. It can never be overwritten. Additionally, the
     * <code>PutObject</code> API guarantees that, if versioning is enabled for a bucket the
     * request, no other object will be overwritten by that request.
     * </p>
     *
     * @param getBucketVersioningConfigurationRequest The request object for retrieving the bucket
     *                                                versioning configuration.
     * @return The bucket versioning configuration for the specified bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public BucketVersioningConfiguration getBucketVersioningConfiguration(
            GetBucketVersioningConfigurationRequest getBucketVersioningConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Sets the policy associated with the specified bucket. Only the owner of the bucket can set a
     * bucket policy. If a policy already exists for the specified bucket, the new policy replaces
     * the existing policy.
     * </p>
     *
     * @param bucketName the bucket name
     * @param policyText The policy to apply to the specified bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setBucketPolicy(String bucketName, String policyText)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Sets the policy associated with the specified bucket. Only the owner of the bucket can set a
     * bucket policy. If a policy already exists for the specified bucket, the new policy replaces
     * the existing policy.
     * </p>
     *
     * @param setBucketPolicyRequest The request object containing the details of the bucket and
     *                               policy to update.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setBucketPolicy(SetBucketPolicyRequest setBucketPolicyRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the policy for the specified bucket. Only the owner of the bucket can retrieve the
     * policy. If no policy has been set for the bucket, then an empty result object with a
     * <code>null</code> policy text field will be returned.
     * </p>
     *
     * @param bucketName the bucket name
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public BucketPolicy getBucketPolicy(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the policy for the specified bucket. Only the owner of the bucket can retrieve the
     * policy. If no policy has been set for the bucket, then an empty result object with a
     * <code>null</code> policy text field will be returned.
     * </p>
     *
     * @param getBucketPolicyRequest get bucket policy request
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public BucketPolicy getBucketPolicy(GetBucketPolicyRequest getBucketPolicyRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Deletes the policy associated with the specified bucket. Only the owner of the bucket can
     * delete the bucket policy.
     * </p>
     *
     * @param bucketName the bucket name
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void deleteBucketPolicy(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Deletes the policy associated with the specified bucket. Only the owner of the bucket can
     * delete the bucket policy.
     * </p>
     *
     * @param bucketName the bucket name
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void deleteBucketPolicy(DeleteBucketPolicyRequest deleteBucketPolicyRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the {@link AccessControlList} (ACL) for the specified object in Qcloud COS.
     * </p>
     * <p>
     * Each bucket and object in Qcloud COS has an ACL that defines its access control policy. When
     * a request is made, Qcloud COS authenticates the request using its standard authentication
     * procedure and then checks the ACL to verify the sender was granted access to the bucket or
     * object. If the sender is approved, the request proceeds. Otherwise, Qcloud COS returns an
     * error.
     * </p>
     *
     * @param bucketName The name of the bucket containing the object whose ACL is being retrieved.
     * @param key        The key of the object within the specified bucket whose ACL is being retrieved.
     * @return The <code>AccessControlList</code> for the specified Qcloud COS object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public AccessControlList getObjectAcl(String bucketName, String key)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the {@link AccessControlList} (ACL) for the specified object in Qcloud COS.
     * </p>
     * <p>
     * Each bucket and object in Qcloud COS has an ACL that defines its access control policy. When
     * a request is made, Qcloud COS authenticates the request using its standard authentication
     * procedure and then checks the ACL to verify the sender was granted access to the bucket or
     * object. If the sender is approved, the request proceeds. Otherwise, Qcloud COS returns an
     * error.
     * </p>
     *
     * @param getObjectAclRequest the request object containing all the information needed for
     *                            retrieving the object ACL.
     * @return The <code>AccessControlList</code> for the specified Qcloud COS object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public AccessControlList getObjectAcl(GetObjectAclRequest getObjectAclRequest)
            throws CosClientException, CosServiceException;

    /**
     * Sets the {@link CannedAccessControlList} for the specified object.
     * <p>
     * Each bucket and object in has an ACL that defines its access control policy. When a request
     * is made, authenticates the request using its standard authentication procedure and then
     * checks the ACL to verify the sender was granted access to the bucket or object. If the sender
     * is approved, the request proceeds. Otherwise, returns an error.
     * <p>
     *
     * @param bucketName The name of the bucket containing the object whose ACL is being set.
     * @param key        The key of the object within the specified bucket whose ACL is being set.
     * @param acl        The new <code>AccessControlList</code> for the specified object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setObjectAcl(String bucketName, String key, AccessControlList acl)
            throws CosClientException, CosServiceException;

    /**
     * Sets the {@link CannedAccessControlList} for the specified object.
     * <p>
     * Each bucket and object in has an ACL that defines its access control policy. When a request
     * is made, authenticates the request using its standard authentication procedure and then
     * checks the ACL to verify the sender was granted access to the bucket or object. If the sender
     * is approved, the request proceeds. Otherwise, returns an error.
     * <p>
     *
     * @param bucketName The name of the bucket containing the object whose ACL is being set.
     * @param key        The key of the object within the specified bucket whose ACL is being set.
     * @param acl        The new pre-configured <code>CannedAccessControlList</code> for the specified
     *                   object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setObjectAcl(String bucketName, String key, CannedAccessControlList acl)
            throws CosClientException, CosServiceException;


    /**
     * Sets the {@link AccessControlList} for the specified object.
     * <p>
     * Each bucket and object in has an ACL that defines its access control policy. When a request
     * is made, authenticates the request using its standard authentication procedure and then
     * checks the ACL to verify the sender was granted access to the bucket or object. If the sender
     * is approved, the request proceeds. Otherwise, returns an error.
     * <p>
     *
     * @param setObjectAclRequest The request object containing the COS object to modify and the ACL
     *                            to set.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setObjectAcl(SetObjectAclRequest setObjectAclRequest)
            throws CosClientException, CosServiceException;

    /**
     * Sets the {@link AccessControlList} for the specified bucket.
     * <p>
     * Each bucket and object in COS has an ACL that defines its access control policy. When a
     * request is made, COS authenticates the request using its standard authentication procedure
     * and then checks the ACL to verify the sender was granted access to the bucket or object. If
     * the sender is approved, the request proceeds. Otherwise, COS returns an error.
     * <p>
     * When constructing a custom <code>AccessControlList</code>, callers typically retrieve the
     * existing <code>AccessControlList</code> for a bucket .
     *
     * @param bucketName The name of the bucket whose ACL is being set
     * @param acl        The new pre-configured <code>CannedAccessControlList</code> for the specified COS
     *                   bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setBucketAcl(String bucketName, AccessControlList acl)
            throws CosClientException, CosServiceException;

    /**
     * Sets the {@link CannedAccessControlList} for the specified bucket.
     * <p>
     * Each bucket and object in COS has an ACL that defines its access control policy. When a
     * request is made, COS authenticates the request using its standard authentication procedure
     * and then checks the ACL to verify the sender was granted access to the bucket or object. If
     * the sender is approved, the request proceeds. Otherwise, COS returns an error.
     * <p>
     * When constructing a custom <code>AccessControlList</code>, callers typically retrieve the
     * existing <code>AccessControlList</code> for a bucket .
     *
     * @param bucketName The name of the bucket whose ACL is being set
     * @param acl        The <code>AccessControlList</code> for the specified COS bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setBucketAcl(String bucketName, CannedAccessControlList acl)
            throws CosClientException, CosServiceException;

    /**
     * Sets the {@link AccessControlList} for the specified bucket.
     * <p>
     * Each bucket and object in COS has an ACL that defines its access control policy. When a
     * request is made, COS authenticates the request using its standard authentication procedure
     * and then checks the ACL to verify the sender was granted access to the bucket or object. If
     * the sender is approved, the request proceeds. Otherwise, COS returns an error.
     * <p>
     * When constructing a custom <code>AccessControlList</code>, callers typically retrieve the
     * existing <code>AccessControlList</code> for a bucket .
     *
     * @param setBucketAclRequest The request object containing the bucket to modify and the ACL to
     *                            set.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in while processing the request.
     */
    public void setBucketAcl(SetBucketAclRequest setBucketAclRequest)
            throws CosClientException, CosServiceException;

    /**
     * Gets the {@link AccessControlList} (ACL) for the specified bucket.
     * <p>
     * Each bucket and object in COS has an ACL that defines its access control policy. When a
     * request is made, COS authenticates the request using its standard authentication procedure
     * and then checks the ACL to verify the sender was granted access to the bucket or object. If
     * the sender is approved, the request proceeds. Otherwise, COS returns an error.
     *
     * @param bucketName The name of the bucket whose ACL is being retrieved.
     * @return The <code>AccessControlList</code> for the specified COS bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public AccessControlList getBucketAcl(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Gets the {@link AccessControlList} (ACL) for the specified bucket.
     * <p>
     * Each bucket and object in COS has an ACL that defines its access control policy. When a
     * request is made, COS authenticates the request using its standard authentication procedure
     * and then checks the ACL to verify the sender was granted access to the bucket or object. If
     * the sender is approved, the request proceeds. Otherwise, COS returns an error.
     *
     * @param getBucketAclRequest The request containing the name of the bucket whose ACL is being
     *                            retrieved.
     * @return The <code>AccessControlList</code> for the specified COS bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public AccessControlList getBucketAcl(GetBucketAclRequest getBucketAclRequest)
            throws CosClientException, CosServiceException;


    /**
     * Gets the cross origin configuration for the specified bucket, or null if no configuration has
     * been established.
     *
     * @param bucketName the bucket name
     * @return BucketCrossOriginConfiguration bucket cross origin configuration
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Gets the cross origin configuration for the specified bucket, or null if no configuration has
     * been established.
     *
     * @param getBucketCrossOriginConfigurationRequest The request object for retrieving the bucket
     *                                                 cross origin configuration.
     * @return BucketCrossOriginConfiguration bucket cross origin configuration
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(
            GetBucketCrossOriginConfigurationRequest getBucketCrossOriginConfigurationRequest)
            throws CosClientException, CosServiceException;


    /**
     * Sets the cross origin configuration for the specified bucket.
     *
     * @param bucketName                     the bucket name
     * @param BucketCrossOriginConfiguration The bucketCrossOriginConfiguration contains all options
     *                                       for setting the bucket cross origin configuration.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void setBucketCrossOriginConfiguration(String bucketName,
                                                  BucketCrossOriginConfiguration bucketCrossOriginConfiguration)
            throws CosClientException, CosServiceException;

    /**
     * Sets the cross origin configuration for the specified bucket.
     *
     * @param setBucketCrossOriginConfigurationRequest The request object containing all options for
     *                                                 setting the bucket cross origin configuration.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void setBucketCrossOriginConfiguration(
            SetBucketCrossOriginConfigurationRequest setBucketCrossOriginConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Delete the cross origin configuration for the specified bucket.
     *
     * @param bucketName The bucket name
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void deleteBucketCrossOriginConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Delete the cross origin configuration for the specified bucket.
     *
     * @param deleteBucketCrossOriginConfigurationRequest The request object containing all options
     *                                                    for deleting the bucket cross origin configuration.
     */
    public void deleteBucketCrossOriginConfiguration(
            DeleteBucketCrossOriginConfigurationRequest deleteBucketCrossOriginConfigurationRequest)
            throws CosClientException, CosServiceException;


    /**
     * Sets a replication configuration for a bucket.
     *
     * @param bucketName    The bucket name for which the replication configuration is set.
     * @param configuration The replication configuration.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void setBucketReplicationConfiguration(String bucketName,
                                                  BucketReplicationConfiguration configuration)
            throws CosClientException, CosServiceException;

    /**
     * Sets a replication configuration for the QCloud bucket.
     *
     * @param setBucketReplicationConfigurationRequest The request object containing all the options
     *                                                 for setting a replication configuration for QCloud bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void setBucketReplicationConfiguration(
            SetBucketReplicationConfigurationRequest setBucketReplicationConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Retrieves the replication configuration for the given QCloud bucket.
     *
     * @param bucketName The bucket name for which the replication configuration is to be retrieved.
     * @return the replication configuration of the bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public BucketReplicationConfiguration getBucketReplicationConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Retrieves the replication configuration for the given QCloud bucket.
     *
     * @param getBucketReplicationConfigurationRequest The request object for retrieving the bucket
     *                                                 replication configuration.
     * @return the replication configuration of the bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    BucketReplicationConfiguration getBucketReplicationConfiguration(
            GetBucketReplicationConfigurationRequest getBucketReplicationConfigurationRequest)
            throws CosClientException, CosServiceException;

    ;

    /**
     * Deletes the replication configuration for the given QCloud bucket.
     *
     * @param bucketName The bucket name for which the replication configuration is to be deleted.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    void deleteBucketReplicationConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Deletes the replication configuration for the given QCloud bucket.
     *
     * @param deleteBucketReplicationConfigurationRequest The request object for delete bucket
     *                                                    replication configuration.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    void deleteBucketReplicationConfiguration(
            DeleteBucketReplicationConfigurationRequest deleteBucketReplicationConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Returns a pre-signed URL for accessing COS resource. you can specify the expiration time.
     * Defaults, if you didn't set the expiration time, the expired time of ClientConfig will be
     * used.
     * </p>
     * <p>
     * Pre-signed URLs allow clients to form a URL for an COS resource, and then sign it with the
     * current COS security credentials. The pre-signed URL can be shared to other users, allowing
     * access to the resource without providing an account's security credentials.
     * </p>
     * <p>
     * Pre-signed URLs are useful in many situations where COS security credentials aren't available
     * from the client that needs to make the actual request to COS.
     * </p>
     * <p>
     * For example, an application may need remote users to upload files to the application owner's
     * COS bucket, but doesn't need to ship the COS security credentials with the application. A
     * pre-signed URL to PUT an object into the owner's bucket can be generated from a remote
     * location with the owner's COS security credentials, then the pre-signed URL can be passed to
     * the end user's application to use.
     * </p>
     *
     * @param bucketName The name of the bucket containing the desired object.
     * @param key        The key in the specified bucket under which the desired object is stored.
     * @param expiration The time at which the returned pre-signed URL will expire.
     * @return A pre-signed URL that can be used to access an COS resource without requiring the
     * user of the URL to know the account's credentials.
     * @throws CosClientException If any errors are encountered in the client while making the
     *                            request or handling the response.
     * @see COS#generatePresignedUrl(String, String, Date)
     * @see COS#generatePresignedUrl(String, String, Date, HttpMethodName)
     */
     public URL generatePresignedUrl(String bucketName, String key, Date expiration, HttpMethodName method)
          throws CosClientException;


    /**
     * <p>
     * Returns a pre-signed URL for accessing COS resource. you can specify the expiration time.
     * Defaults, if you didn't set the expiration time, the expired time of ClientConfig will be
     * used.
     * </p>
     * <p>
     * Pre-signed URLs allow clients to form a URL for an COS resource, and then sign it with the
     * current COS security credentials. The pre-signed URL can be shared to other users, allowing
     * access to the resource without providing an account's security credentials.
     * </p>
     * <p>
     * Pre-signed URLs are useful in many situations where COS security credentials aren't available
     * from the client that needs to make the actual request to COS.
     * </p>
     * <p>
     * For example, an application may need remote users to upload files to the application owner's
     * COS bucket, but doesn't need to ship the COS security credentials with the application. A
     * pre-signed URL to PUT an object into the owner's bucket can be generated from a remote
     * location with the owner's COS security credentials, then the pre-signed URL can be passed to
     * the end user's application to use.
     * </p>
     *
     * @param bucketName The name of the bucket containing the desired object.
     * @param key        The key in the specified bucket under which the desired object is stored.
     * @param expiration The time at which the returned pre-signed URL will expire.
     * @param method     The HTTP method verb to use for this URL
     * @param headers The HTTP headers to use for sign.
     * @param params The HTTP params to use for sign.
     * @return A pre-signed URL that can be used to access an COS resource without requiring the
     * user of the URL to know the account's credentials.
     * @throws CosClientException If any errors are encountered in the client while making the
     *                            request or handling the response.
     * @see COS#generatePresignedUrl(String, String, Date)
     * @see COS#generatePresignedUrl(String, String, Date, HttpMethodName)
     */
    public URL generatePresignedUrl(String bucketName, String key, Date expiration,
                                    HttpMethodName method, Map<String, String> headers, Map<String, String> params) throws CosClientException;


     /**
     * @param bucketName The name of the bucket containing the desired object.
     * @param key        The key in the specified bucket under which the desired object is stored.
     * @param expiration The time at which the returned pre-signed URL will expire.
     * @param method     The HTTP method verb to use for this URL
     * @param headers The HTTP headers to use for sign.
     * @param params The HTTP params to use for sign.
     * @param signPrefixMode The optional signPrefixMode decide the presigned url whether start with 'sign=' and encode value.
     * @param signHost The optional signHost decide whether to sign with host header, by default it is true.
     * @return A pre-signed URL that can be used to access an COS resource without requiring the
     * user of the URL to know the account's credentials.
     * @throws CosClientException If any errors are encountered in the client while making the
     *                            request or handling the response.
     * @see COS#generatePresignedUrl(String, String, Date)
     * @see COS#generatePresignedUrl(String, String, Date, HttpMethodName)
     */
    public URL generatePresignedUrl(String bucketName, String key, Date expiration,
                                    HttpMethodName method, Map<String, String> headers, Map<String, String> params,
                                    Boolean signPrefixMode, Boolean signHost) throws CosClientException;

    /**
     * <p>
     * Returns a pre-signed URL for accessing COS resource. you can specify the expiration time.
     * Defaults, if you didn't set the expiration time, the expired time of ClientConfig will be
     * used.
     * </p>
     * <p>
     * Pre-signed URLs allow clients to form a URL for an COS resource, and then sign it with the
     * current COS security credentials. The pre-signed URL can be shared to other users, allowing
     * access to the resource without providing an account's security credentials.
     * </p>
     * <p>
     * Pre-signed URLs are useful in many situations where COS security credentials aren't available
     * from the client that needs to make the actual request to COS.
     * </p>
     * <p>
     * For example, an application may need remote users to upload files to the application owner's
     * COS bucket, but doesn't need to ship the COS security credentials with the application. A
     * pre-signed URL to PUT an object into the owner's bucket can be generated from a remote
     * location with the owner's COS security credentials, then the pre-signed URL can be passed to
     * the end user's application to use.
     * </p>
     *
     * @param generatePresignedUrlRequest The request object containing all the options for
     *                                    generating a pre-signed URL (bucket name, key, expiration date, etc).
     * @return A pre-signed URL that can be used to access an COS resource without requiring the
     * user of the URL to know the account's credentials.
     * @throws CosClientException If any errors are encountered in the client while making the
     *                            request or handling the response.
     * @see COS#generatePresignedUrl(String, String, Date)
     * @see COS#generatePresignedUrl(String, String, Date, HttpMethod)
     */
    public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest)
            throws CosClientException;

    /**
     * <p>
     * Returns a pre-signed URL for accessing COS resource. you can specify the expiration time.
     * Defaults, if you didn't set the expiration time, the expired time of ClientConfig will be
     * used.
     * </p>
     * <p>
     * Pre-signed URLs allow clients to form a URL for an COS resource, and then sign it with the
     * current COS security credentials. The pre-signed URL can be shared to other users, allowing
     * access to the resource without providing an account's security credentials.
     * </p>
     * <p>
     * Pre-signed URLs are useful in many situations where COS security credentials aren't available
     * from the client that needs to make the actual request to COS.
     * </p>
     * <p>
     * For example, an application may need remote users to upload files to the application owner's
     * COS bucket, but doesn't need to ship the COS security credentials with the application. A
     * pre-signed URL to PUT an object into the owner's bucket can be generated from a remote
     * location with the owner's COS security credentials, then the pre-signed URL can be passed to
     * the end user's application to use.
     * </p>
     *
     * @param generatePresignedUrlRequest The request object containing all the options for
     *                                    generating a pre-signed URL (bucket name, key, expiration date, etc).
     * @param signHost The optional signHost decide whether to sign with host header, by default it is true.
     * @return A pre-signed URL that can be used to access an COS resource without requiring the
     * user of the URL to know the account's credentials.
     * @throws CosClientException If any errors are encountered in the client while making the
     *                            request or handling the response.
     * @see COS#generatePresignedUrl(String, String, Date)
     * @see COS#generatePresignedUrl(String, String, Date, HttpMethod)
     */
    public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest, Boolean signHost)
            throws CosClientException;

    /**
     * Restore an object, which was transitioned to CAS from COS when it was expired, into COS
     * again. This copy is by nature temporary and is always stored as temporary copy in COS. The
     * customer will be able to set / re-adjust the lifetime of this copy. By re-adjust we mean the
     * customer can call this API to shorten or extend the lifetime of the copy. Note the request
     * will only be accepted when there is no ongoing restore request. One needs to have the new
     * cos:RestoreObject permission to perform this operation.
     *
     * @param request The request object containing all the options for restoring an COS object.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void restoreObject(RestoreObjectRequest request)
            throws CosClientException, CosServiceException;

    /**
     * Restore an object, which was transitioned to CAS from COS when it was expired, into COS
     * again. This copy is by nature temporary and is always stored as temporary copy in COS. The
     * customer will be able to set / re-adjust the lifetime of this copy. By re-adjust we mean the
     * customer can call this API to shorten or extend the lifetime of the copy. Note the request
     * will only be accepted when there is no ongoing restore request. One needs to have the new
     * cos:RestoreObject permission to perform this operation.
     *
     * @param bucketName       The name of an existing bucket.
     * @param key              The key under which to store the specified file.
     * @param expirationInDays The number of days after which the object will expire.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void restoreObject(String bucketName, String key, int expirationInDays)
            throws CosClientException, CosServiceException;


    /**
     * update the object meta.
     *
     * @param bucketName     The name of an existing bucket.
     * @param key            The key under which to store the specified file.
     * @param objectMetadata object new metadata for the specified object
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void updateObjectMetaData(String bucketName, String key, ObjectMetadata objectMetadata)
            throws CosClientException, CosServiceException;

    /**
     * Returns the website configuration for the specified bucket. Bucket
     * website configuration allows you to host your static websites entirely
     * out of COS. To host your website in COS, create a bucket,
     * upload your files, and configure it as a website. Once your bucket has
     * been configured as a website, you can access all your content via the
     * COS website endpoint. To ensure that the existing COS REST
     * API will continue to behave the same, regardless of whether or not your
     * bucket has been configured to host a website, a new HTTP endpoint has
     * been introduced where you can access your content. The bucket content you
     * want to make available via the website must be publicly readable.
     *
     * @param bucketName The name of the bucket whose website configuration is being
     *                   retrieved.
     * @return The bucket website configuration for the specified bucket,
     * otherwise null if there is no website configuration set for the
     * specified bucket.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public BucketWebsiteConfiguration getBucketWebsiteConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Returns the website configuration for the specified bucket. Bucket
     * website configuration allows you to host your static websites entirely
     * out of COS. To host your website in COS, create a bucket,
     * upload your files, and configure it as a website. Once your bucket has
     * been configured as a website, you can access all your content via the
     * COS website endpoint. To ensure that the existing COS REST
     * API will continue to behave the same, regardless of whether or not your
     * bucket has been configured to host a website, a new HTTP endpoint has
     * been introduced where you can access your content. The bucket content you
     * want to make available via the website must be publicly readable.
     *
     * @param getBucketWebsiteConfigurationRequest The request object for retrieving the bucket website configuration.
     * @return The bucket website configuration for the specified bucket,
     * otherwise null if there is no website configuration set for the
     * specified bucket.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public BucketWebsiteConfiguration getBucketWebsiteConfiguration(GetBucketWebsiteConfigurationRequest getBucketWebsiteConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Sets the website configuration for the specified bucket. Bucket
     * website configuration allows you to host your static websites entirely
     * out COS. To host your website in COS, create a bucket,
     * upload your files, and configure it as a website. Once your bucket has
     * been configured as a website, you can access all your content via the
     * COS website endpoint. To ensure that the existing COS REST
     * API will continue to behave the same, regardless of whether or not your
     * bucket has been configured to host a website, a new HTTP endpoint has
     * been introduced where you can access your content. The bucket content you
     * want to make available via the website must be publicly readable.
     *
     * @param bucketName    The name of the bucket whose website configuration is being
     *                      set.
     * @param configuration The configuration describing how the specified bucket will
     *                      serve web requests (i.e. default index page, error page).
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public void setBucketWebsiteConfiguration(String bucketName, BucketWebsiteConfiguration configuration)
            throws CosClientException, CosServiceException;

    /**
     * Sets the website configuration for the specified bucket. Bucket website
     * configuration allows you to host your static websites entirely out of
     * COS. To host your website in COS, create a bucket, upload
     * your files, and configure it as a website. Once your bucket has been
     * configured as a website, you can access all your content via the COS
     * website endpoint. To ensure that the existing COS REST API will
     * continue to behave the same, regardless of whether or not your bucket has
     * been configured to host a website, a new HTTP endpoint has been
     * introduced where you can access your content. The bucket content you want
     * to make available via the website must be publicly readable.
     *
     * @param setBucketWebsiteConfigurationRequest The request object containing the name of the bucket whose
     *                                             website configuration is being updated, and the new website
     *                                             configuration values.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public void setBucketWebsiteConfiguration(SetBucketWebsiteConfigurationRequest setBucketWebsiteConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * This operation removes the website configuration for a bucket. Calling
     * this operation on a bucket with no website configuration does <b>not</b>
     * throw an exception. Calling this operation a bucket that does not exist
     * <b>will</b> throw an exception.
     *
     * @param bucketName The name of the bucket whose website configuration is being
     *                   deleted.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public void deleteBucketWebsiteConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * This operation removes the website configuration for a bucket. Calling
     * this operation on a bucket with no website configuration does <b>not</b>
     * throw an exception. Calling this operation a bucket that does not exist
     * <b>will</b> throw an exception.
     *
     * @param deleteBucketWebsiteConfigurationRequest The request object specifying the name of the bucket whose
     *                                                website configuration is to be deleted.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public void deleteBucketWebsiteConfiguration(DeleteBucketWebsiteConfigurationRequest deleteBucketWebsiteConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * This operation removes the domain configuration for a bucket. Calling
     * this operation on a bucket with no domain configuration does <b>not</b>
     * throw an exception. Calling this operation a bucket that does not exist
     * <b>will</b> throw an exception.
     *
     * @param bucketName The name of the bucket whose domain configuration is being
     *                   deleted.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public void deleteBucketDomainConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * This operation removes the domain configuration for a bucket. Calling
     * this operation on a bucket with no domain configuration does <b>not</b>
     * throw an exception. Calling this operation a bucket that does not exist
     * <b>will</b> throw an exception.
     *
     * @param deleteBucketDomainConfigurationRequest The request object specifying the name of the bucket whose
     *                                               domain configuration is to be deleted.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public void deleteBucketDomainConfiguration(DeleteBucketDomainConfigurationRequest deleteBucketDomainConfigurationReqeuest)
            throws CosClientException, CosServiceException;

    /**
     * Sets the domain configuration for the specified bucket.
     *
     * @param bucketName    The name of the bucket whose domain configuration is being set.
     * @param configuration The configuration describing the specified bucket custom domain
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void setBucketDomainConfiguration(String bucketName, BucketDomainConfiguration configuration)
            throws CosClientException, CosServiceException;

    /**
     * Sets the domain configuration for the specified bucket.
     *
     * @param setBucketDomainConfigurationRequest The request object containing the name of the bucket whose
     *                                            domain configuration is being updated, and the new domain
     *                                            configuration values.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void setBucketDomainConfiguration(SetBucketDomainConfigurationRequest setBucketDomainConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Returns the domain configuration for the specified bucket.
     *
     * @param bucketName The name of the bucket whose domain configuration is being retrieved.
     * @return The bucket domain configuration for the specified bucket,
     * otherwise null if there is no domain configuration set for the
     * specified bucket.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public BucketDomainConfiguration getBucketDomainConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Returns the domain configuration for the specified bucket.
     *
     * @param getBucketDomainConfigurationRequest The request object for retrieving the bucket domain configuration.
     * @return The bucket domain configuration for the specified bucket,
     * otherwise null if there is no domain configuration set for the
     * specified bucket.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public BucketDomainConfiguration getBucketDomainConfiguration(GetBucketDomainConfigurationRequest getBucketDomainConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Sets the referer configuration for the specified bucket.
     *
     * @param bucketName    The name of the bucket whose referer configuration is being set.
     * @param configuration The configuration describing the specified bucket referer
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void setBucketRefererConfiguration(String bucketName, BucketRefererConfiguration configuration)
            throws CosClientException, CosServiceException;

    /**
     * Sets the referer configuration for the specified bucket.
     *
     * @param setBucketRefererConfigurationRequest The request object containing the name of the bucket whose
     *                                            referer configuration is being updated, and the new referer
     *                                            configuration values.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public void setBucketRefererConfiguration(SetBucketRefererConfigurationRequest setBucketRefererConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Returns the referer configuration for the specified bucket.
     *
     * @param bucketName The name of the bucket whose referer configuration is being retrieved.
     * @return The bucket referer configuration for the specified bucket,
     * otherwise null if there is no referer configuration set for the
     * specified bucket.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public BucketRefererConfiguration getBucketRefererConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * Returns the referer configuration for the specified bucket.
     *
     * @param getBucketDomainConfigurationRequest The request object for retrieving the bucket referer configuration.
     * @return The bucket referer configuration for the specified bucket,
     * otherwise null if there is no referer configuration set for the
     * specified bucket.
     * @throws CosClientException  If any errors are encountered on the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the request.
     */
    public BucketRefererConfiguration getBucketRefererConfiguration(GetBucketRefererConfigurationRequest getBucketRefererConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the logging configuration for the specified bucket.
     * The bucket logging configuration object indicates if server access logging is
     * enabled for the specified bucket, the destination bucket
     * where server access logs are delivered, and the optional log file prefix.
     * </p>
     *
     * @param bucketName The name of the bucket whose bucket logging configuration is
     *                   being retrieved.
     * @return The bucket logging configuration for the specified bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public BucketLoggingConfiguration getBucketLoggingConfiguration(String bucketName)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Gets the logging configuration for the specified bucket. The bucket
     * logging configuration object indicates if server access logging is
     * enabled for the specified bucket, the destination bucket where server access
     * logs are delivered, and the optional log file prefix.
     * </p>
     *
     * @param getBucketLoggingConfigurationRequest The request object for retrieving the bucket logging
     *                                             configuration.
     * @return The bucket logging configuration for the specified bucket.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public BucketLoggingConfiguration getBucketLoggingConfiguration(
            GetBucketLoggingConfigurationRequest getBucketLoggingConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * <p>
     * Sets the logging configuration for the specified bucket.
     * The bucket logging configuration object indicates whether server access logging is
     * enabled or not for the specified bucket, the destination bucket
     * where server access logs are delivered, and the optional log file prefix.
     * </p>
     *
     * @param setBucketLoggingConfigurationRequest The request object containing all options for setting the
     *                                             bucket logging configuration.
     * @throws CosClientException  If any errors are encountered in the client while making the
     *                             request or handling the response.
     * @throws CosServiceException If any errors occurred in COS while processing the
     *                             request.
     */
    public void setBucketLoggingConfiguration(SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Deletes an inventory configuration (identified by the inventory ID) from the bucket.
     *
     * @param bucketName The name of the bucket from which the inventory configuration is to be deleted.
     * @param id         The ID of the inventory configuration to delete.
     */
    public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration(
            String bucketName, String id) throws CosClientException, CosServiceException;

    /**
     * Deletes an inventory configuration (identified by the inventory ID) from the bucket.
     *
     * @param deleteBucketInventoryConfigurationRequest The request object for deleting an inventory configuration.
     */
    public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration(
            DeleteBucketInventoryConfigurationRequest deleteBucketInventoryConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Returns an inventory configuration (identified by the inventory ID) from the bucket.
     *
     * @param bucketName The name of the bucket to get the inventory configuration from.
     * @param id         The ID of the inventory configuration to delete.
     * @return An {@link GetBucketInventoryConfigurationResult} object containing the inventory configuration.
     */
    public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(
            String bucketName, String id) throws CosClientException, CosServiceException;

    /**
     * Returns an inventory configuration (identified by the inventory ID) from the bucket.
     *
     * @param getBucketInventoryConfigurationRequest The request object to retreive an inventory configuration.
     * @return An {@link GetBucketInventoryConfigurationResult} object containing the inventory configuration.
     */
    public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(
            GetBucketInventoryConfigurationRequest getBucketInventoryConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Sets an inventory configuration (identified by the inventory ID) to the bucket.
     *
     * @param bucketName             The name of the bucket to set the inventory configuration to.
     * @param inventoryConfiguration The inventory configuration to set.
     */
    public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration(
            String bucketName, InventoryConfiguration inventoryConfiguration)
            throws CosClientException, CosServiceException;

    /**
     * Sets an inventory configuration (identified by the inventory ID) to the bucket.
     *
     * @param setBucketInventoryConfigurationRequest The request object for setting an inventory configuration.
     */
    public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration(
            SetBucketInventoryConfigurationRequest setBucketInventoryConfigurationRequest)
            throws CosClientException, CosServiceException;

    /**
     * Returns the list of inventory configurations for the bucket.
     *
     * @param listBucketInventoryConfigurationsRequest The request object to list the inventory configurations in a bucket.
     * @return An {@link ListBucketInventoryConfigurationsResult} object containing the list of {@link InventoryConfiguration}s.
     */
    public ListBucketInventoryConfigurationsResult listBucketInventoryConfigurations(
            ListBucketInventoryConfigurationsRequest listBucketInventoryConfigurationsRequest)
            throws CosClientException, CosServiceException;

    /**
     * Gets the tagging configuration for the specified bucket, or null if
     * the specified bucket does not exist, or if no configuration has been established.
     *
     * @param bucketName The name of the bucket for which to retrieve tagging
     *                   configuration.
     * @see COSClient#getBucketTaggingConfiguration(GetBucketTaggingConfigurationRequest)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/GetBucketTagging">AWS API Documentation</a>
     */
    public BucketTaggingConfiguration getBucketTaggingConfiguration(String bucketName);

    /**
     * Gets the tagging configuration for the specified bucket, or null if
     * the specified bucket does not exist, or if no configuration has been established.
     *
     * @param getBucketTaggingConfigurationRequest The request object for retrieving the bucket tagging
     *                                             configuration.
     * @see COSClient#getBucketTaggingConfiguration(String)
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/GetBucketTagging">AWS API Documentation</a>
     */
    public BucketTaggingConfiguration getBucketTaggingConfiguration(
            GetBucketTaggingConfigurationRequest getBucketTaggingConfigurationRequest);

    /**
     * Sets the tagging configuration for the specified bucket.
     *
     * @param bucketName                 The name of the bucket for which to set the tagging
     *                                   configuration.
     * @param bucketTaggingConfiguration The new tagging configuration for this bucket, which
     *                                   completely replaces any existing configuration.
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/PutBucketTagging">AWS API Documentation</a>
     */
    public void setBucketTaggingConfiguration(String bucketName, BucketTaggingConfiguration bucketTaggingConfiguration);

    /**
     * Sets the tagging configuration for the specified bucket.
     *
     * @param setBucketTaggingConfigurationRequest The request object containing all options for setting the
     *                                             bucket tagging configuration.
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/PutBucketTagging">AWS API Documentation</a>
     */
    public void setBucketTaggingConfiguration(SetBucketTaggingConfigurationRequest setBucketTaggingConfigurationRequest);

    /**
     * Removes the tagging configuration for the bucket specified.
     *
     * @param bucketName The name of the bucket for which to remove the tagging
     *                   configuration.
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/DeleteBucketTagging">AWS API Documentation</a>
     */
    public void deleteBucketTaggingConfiguration(String bucketName);

    /**
     * Removes the tagging configuration for the bucket specified.
     *
     * @param deleteBucketTaggingConfigurationRequest The request object containing all options for removing the
     *                                                bucket tagging configuration.
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/DeleteBucketTagging">AWS API Documentation</a>
     */
    public void deleteBucketTaggingConfiguration(
            DeleteBucketTaggingConfigurationRequest deleteBucketTaggingConfigurationRequest);

    /**
     * Get the intelligent configuration for the specified bucket.
     *
     * @param getBucketIntelligentTierConfigurationRequest
     * @return
     */
    BucketIntelligentTierConfiguration getBucketIntelligentTierConfiguration(GetBucketIntelligentTierConfigurationRequest getBucketIntelligentTierConfigurationRequest);


    /**
     * Get the intelligent configuration for the specified bucket.
     *
     * @param bucketName
     * @return
     */
    BucketIntelligentTierConfiguration getBucketIntelligentTierConfiguration(String bucketName);

    /**
     * Sets the intelligent configuration for the specified bucket.
     *
     * @param setBucketIntelligentTierConfigurationRequest The request object containing all options for setting the
     *                                                     bucket intelligent configuration.
     */
    void setBucketIntelligentTieringConfiguration(SetBucketIntelligentTierConfigurationRequest setBucketIntelligentTierConfigurationRequest);

    /**
     * append data to an COS object
     *
     * @param appendObjectRequest
     * @return
     * @throws CosServiceException
     * @throws CosClientException
     */
    public AppendObjectResult appendObject(AppendObjectRequest appendObjectRequest)
            throws CosServiceException, CosClientException;

    /**
     * rename object, which contains both file or dir in fs
     *
     * @param renameRequest
     * @return
     * @throws CosServiceException
     * @throws CosClientException
     */
    public void rename(RenameRequest renameRequest)
            throws CosServiceException, CosClientException;

    /**
     * This operation filters the contents of an COS object based on a simple Structured Query Language (SQL) statement.
     * In the request, along with the SQL expression, you must also specify a data serialization format (JSON or CSV) of the
     * object. COS uses this to parse object data into records, and returns only records that match the specified SQL
     * expression. You must also specify the data serialization format for the response.
     *
     * @param selectRequest The request object for selecting object content.
     * @return A {@link SelectObjectContentResult}.
     * @throws CosClientException
     * @throws CosServiceException
     */
    SelectObjectContentResult selectObjectContent(SelectObjectContentRequest selectRequest)
            throws CosClientException, CosServiceException;

    /**
     * Returns the tags for the specified object.
     *
     * @param getObjectTaggingRequest The request object containing all the options on how to
     *                                retrieve the COS object tags.
     * @return The tags for the specified object.
     */
    public GetObjectTaggingResult getObjectTagging(GetObjectTaggingRequest getObjectTaggingRequest);

    /**
     * Set the tags for the specified object.
     *
     * @param setObjectTaggingRequest The request object containing all the options for setting the
     *                                tags for the specified object.
     */
    public SetObjectTaggingResult setObjectTagging(SetObjectTaggingRequest setObjectTaggingRequest);

    /**
     * Remove the tags for the specified object.
     *
     * @param deleteObjectTaggingRequest The request object containing all the options for deleting
     *                                   the tags for the specified object.
     * @return a {@link DeleteObjectTaggingResult} object containing the
     * information returned by COS for the the tag deletion.
     */
    public DeleteObjectTaggingResult deleteObjectTagging(DeleteObjectTaggingRequest deleteObjectTaggingRequest);

    /**
     * =====================================================
     * 数据万象相关接口
     * =====================================================
     */

    /**
     * CreateMediaJobs 接口用于提交一个任务。 https://cloud.tencent.com/document/product/460/38936
     *
     * @param req
     */
    MediaJobResponse createMediaJobs(MediaJobsRequest req) throws UnsupportedEncodingException;

    /**
     * CancelMediaJob 接口用于取消一个任务。  https://cloud.tencent.com/document/product/460/38939
     */
    Boolean cancelMediaJob(MediaJobsRequest req);

    /**
     * DescribeMediaJob 用于查询指定的任务。  https://cloud.tencent.com/document/product/460/38937
     *
     * @return
     */
    MediaJobResponse describeMediaJob(MediaJobsRequest req);

    /**
     * DescribeMediaJobs 用于拉取符合条件的任务。  https://cloud.tencent.com/document/product/460/38938
     */
    MediaListJobResponse describeMediaJobs(MediaJobsRequest cIMediaJobsRequest);

    /**
     * DescribeMediaQueues 接口用于搜索队列。 https://cloud.tencent.com/document/product/460/38913
     */
    MediaListQueueResponse describeMediaQueues(MediaQueueRequest mediaQueueRequest);

    /**
     * UpdateMediaQueue 接口用于更新队列。  https://cloud.tencent.com/document/product/460/42324
     */
    MediaQueueResponse updateMediaQueue(MediaQueueRequest mediaQueueRequest) throws UnsupportedEncodingException;

    /**
     * DescribeMediaBuckets 接口用于查询存储桶是否已开通媒体处理功能。  https://cloud.tencent.com/document/product/460/38914
     */
    MediaBucketResponse describeMediaBuckets(MediaBucketRequest mediaBucketRequest);

    /**
     * CreateMediaTemplate 用于新增模板。。
     * 动图模板 https://cloud.tencent.com/document/product/460/46989
     * 截图模板 https://cloud.tencent.com/document/product/460/46994
     * 转码模板 https://cloud.tencent.com/document/product/460/46999
     */
    MediaTemplateResponse createMediaTemplate(MediaTemplateRequest request) throws UnsupportedEncodingException;

    /**
     * DeleteMediaTemplate 用于删除模板。 https://cloud.tencent.com/document/product/460/46990
     *
     * @return
     */
    Boolean deleteMediaTemplate(MediaTemplateRequest request);

    /**
     * DescribeMediaTemplates 用于查询动图模板。  https://cloud.tencent.com/document/product/460/46991
     */
    MediaListTemplateResponse describeMediaTemplates(MediaTemplateRequest request);

    /**
     * UpdateMediaTemplate 用于更新模板。。  https://cloud.tencent.com/document/product/460/46992
     */
    Boolean updateMediaTemplate(MediaTemplateRequest request) throws UnsupportedEncodingException;

    /**
     * GenerateSnapshot 接口用于获取媒体文件某个时间的截图，输出的截图统一为 jpeg 格式。
     * https://cloud.tencent.com/document/product/460/38934
     */
    SnapshotResponse generateSnapshot(SnapshotRequest request) throws UnsupportedEncodingException;

    /**
     * GenerateMediainfo 接口用于获取媒体文件的信息。 https://cloud.tencent.com/document/product/460/38935
     */
    MediaInfoResponse generateMediainfo(MediaInfoRequest request) throws UnsupportedEncodingException;

    /**
     * DeleteWorkflow 接口用于删除工作流。 https://cloud.tencent.com/document/product/460/45947
     */
    Boolean deleteWorkflow(MediaWorkflowListRequest request);

    /**
     * DescribeWorkflow 接口用于搜索工作流。  https://cloud.tencent.com/document/product/460/45948
     */
    MediaWorkflowListResponse describeWorkflow(MediaWorkflowListRequest request);

    /**
     * DescribeWorkflowExecution 接口用于获取工作流实例详情。 https://cloud.tencent.com/document/product/460/45949
     */
    MediaWorkflowExecutionResponse describeWorkflowExecution(MediaWorkflowListRequest request);

    /**
     * DescribeWorkflowExecutions 接口用于获取工作流实例列表。 https://cloud.tencent.com/document/product/460/45950
     */
    MediaWorkflowExecutionsResponse describeWorkflowExecutions(MediaWorkflowListRequest request);

    /**
     * CreateDocProcessJobs 接口用于提交一个文档预览任务。 https://cloud.tencent.com/document/product/460/46942
     */
    DocJobResponse createDocProcessJobs(DocJobRequest request);

    /**
     * DescribeDocProcessJob 用于查询指定的文档预览任务。 https://cloud.tencent.com/document/product/460/46943
     */
    DocJobResponse describeDocProcessJob(DocJobRequest request);

    /**
     * DescribeDocProcessJobs 用于拉取符合条件的文档预览任务。 https://cloud.tencent.com/document/product/460/46944
     */
    DocJobListResponse describeDocProcessJobs(DocJobListRequest request);

    /**
     * DescribeDocProcessQueues 接口用于查询文档预览队列。 https://cloud.tencent.com/document/product/460/46946
     * @return
     */
    DocListQueueResponse describeDocProcessQueues(DocQueueRequest request);

    /**
     * UpdateDocProcessQueue 接口用于更新文档预览队列。https://cloud.tencent.com/document/product/460/46947
     * @return
     */
    boolean updateDocProcessQueue(DocQueueRequest request);

    /**
     * DescribeDocProcessBuckets 接口用于查询存储桶是否已开通文档预览功能。https://cloud.tencent.com/document/product/460/46945
     */
    DocBucketResponse describeDocProcessBuckets(DocBucketRequest request);

    /**
     * process Image 接口用于对图片进行处理
     */
    CIUploadResult processImage(ImageProcessRequest request);

    /**
     * ImageAuditing图片审核  https://cloud.tencent.com/document/product/460/37318
     */
    ImageAuditingResponse imageAuditing(ImageAuditingRequest request);

    /**
     * CreateVideoAuditingJob 视频审核任务发起接口 https://cloud.tencent.com/document/product/460/46427
     */
    VideoAuditingResponse createVideoAuditingJob(VideoAuditingRequest request);

    /**
     * DescribeAuditingJob 视频审核任务查询接口 https://cloud.tencent.com/document/product/460/46926
     */
    VideoAuditingResponse describeAuditingJob(VideoAuditingRequest request);

    /**
     * CreateAudioAuditingJobs 音频审核任务创建接口 https://cloud.tencent.com/document/product/460/53395
     */
    AudioAuditingResponse createAudioAuditingJobs(AudioAuditingRequest request);

    /**
     * DescribeAudioAuditingJob 音频审核任务查询接口 https://cloud.tencent.com/document/product/460/53396
     */
    AudioAuditingResponse describeAudioAuditingJob(AudioAuditingRequest request);

    /**
     * GetImageLabel 图片标签 https://cloud.tencent.com/document/product/460/39082
     */
    ImageLabelResponse getImageLabel(ImageLabelRequest request);

    /**
     * GetImageLabel 图片标签V2 https://cloud.tencent.com/document/product/460/39082
     */
    ImageLabelV2Response getImageLabelV2(ImageLabelV2Request request);

    /**
     * CreateAuditingTextJobs 音频审核任务查询接口 https://cloud.tencent.com/document/product/460/56289
     */
    TextAuditingResponse createAuditingTextJobs(TextAuditingRequest request);

    /**
     * DescribeAuditingTextJob 查询文本审核任务结果 https://cloud.tencent.com/document/product/436/56288
     */
    TextAuditingResponse describeAuditingTextJob(TextAuditingRequest request);

    /**
     * CreateAuditingDocumentJobs 提交文档审核任务 https://cloud.tencent.com/document/product/460/59380
     */
    DocumentAuditingResponse createAuditingDocumentJobs(DocumentAuditingRequest request);

    /**
     * DescribeAuditingDocumentJobs 提交文档审核任务 https://cloud.tencent.com/document/product/460/59383
     */
    DocumentAuditingResponse describeAuditingDocumentJob(DocumentAuditingRequest request);

    /**
     * BatchImageAuditing 批量提交图片审核任务 https://cloud.tencent.com/document/product/460/59383
     */
    BatchImageAuditingResponse batchImageAuditing(BatchImageAuditingRequest request);

    /**
     * createDocProcessBucket 开通文档预览功能
     */
    Boolean createDocProcessBucket(DocBucketRequest request);

    /**
     * GenerateDocPreviewHtmlUrl  查询账号下已开通文档预览功能的bucket
     */
    String GenerateDocPreviewUrl(DocHtmlRequest docJobRequest) throws URISyntaxException;

    /**
     * createWebpageAuditingJob  提交网页审核任务 https://cloud.tencent.com/document/product/460/63968
     */
    WebpageAuditingResponse createWebpageAuditingJob(WebpageAuditingRequest request);

    /**
     * describeWebpageAuditingJob 查询网页审核任务 https://cloud.tencent.com/document/product/460/63970
     */
    WebpageAuditingResponse describeWebpageAuditingJob(WebpageAuditingRequest request);
}


