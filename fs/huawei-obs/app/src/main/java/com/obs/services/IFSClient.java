/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services;

import java.io.IOException;

import com.obs.services.exception.ObsException;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.TaskProgressStatus;
import com.obs.services.model.fs.ListContentSummaryRequest;
import com.obs.services.model.fs.ListContentSummaryResult;
import com.obs.services.model.fs.ObsFSAttribute;
import com.obs.services.model.fs.DropFileRequest;
import com.obs.services.model.fs.DropFileResult;
import com.obs.services.model.fs.DropFolderRequest;
import com.obs.services.model.fs.GetAttributeRequest;
import com.obs.services.model.fs.GetBucketFSStatusRequest;
import com.obs.services.model.fs.GetBucketFSStatusResult;
import com.obs.services.model.fs.NewBucketRequest;
import com.obs.services.model.fs.NewFileRequest;
import com.obs.services.model.fs.NewFolderRequest;
import com.obs.services.model.fs.ObsFSBucket;
import com.obs.services.model.fs.ObsFSFile;
import com.obs.services.model.fs.ObsFSFolder;
import com.obs.services.model.fs.ReadFileRequest;
import com.obs.services.model.fs.ReadFileResult;
import com.obs.services.model.fs.RenameRequest;
import com.obs.services.model.fs.RenameResult;
import com.obs.services.model.fs.SetBucketFSStatusRequest;
import com.obs.services.model.fs.TruncateFileRequest;
import com.obs.services.model.fs.TruncateFileResult;
import com.obs.services.model.fs.WriteFileRequest;
import com.obs.services.model.fs.ContentSummaryFsResult;
import com.obs.services.model.fs.ContentSummaryFsRequest;
import com.obs.services.model.fs.ListContentSummaryFsResult;
import com.obs.services.model.fs.ListContentSummaryFsRequest;
import com.obs.services.model.fs.accesslabel.DeleteAccessLabelRequest;
import com.obs.services.model.fs.accesslabel.DeleteAccessLabelResult;
import com.obs.services.model.fs.accesslabel.GetAccessLabelRequest;
import com.obs.services.model.fs.accesslabel.GetAccessLabelResult;
import com.obs.services.model.fs.accesslabel.SetAccessLabelRequest;
import com.obs.services.model.fs.accesslabel.SetAccessLabelResult;

/**
 * Gateway interface for OBS files
 */
public interface IFSClient {
    /**
     * Disable ObsClient and release connection resources.
     * @throws IOException ioException
     */
    void close() throws IOException;

    /**
     * Create a bucket.
     * @param request Request parameters for creating a bucket
     * @return Bucket supporting the file interface
     * @throws ObsException OBS SDK self-defined exception, 
     * thrown when the interface fails to be called or access to OBS fails
     */
    ObsFSBucket newBucket(NewBucketRequest request) throws ObsException;

    /**
     * Specify whether to enable the file gateway feature for the bucket.
     * @param request Request parameters for specifying whether to enable the file gateway feature for the bucket
     * @return Common response headers
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    HeaderResponse setBucketFSStatus(SetBucketFSStatusRequest request) throws ObsException;

    /**
     * Check whether the file gateway feature is enabled for the bucket.
     * @param request Request parameters for checking whether the file gateway feature is enabled for the bucket
     * @return Response to the check of whether the file gateway feature is enabled for the bucket
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    GetBucketFSStatusResult getBucketFSStatus(GetBucketFSStatusRequest request) throws ObsException;

    /**
     * Create a file.
     * @param request Request parameters for creating a file
     * @return Files in the bucket that supports the file interface
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    ObsFSFile newFile(NewFileRequest request) throws ObsException;

    /**
     * Create a folder.
     * @param request Request parameters for creating a folder
     * @return Folders in the bucket that supports the file interface
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    ObsFSFolder newFolder(NewFolderRequest request) throws ObsException;

    /**
     * Obtain file or folder properties.
     * @param request Request parameters for obtaining filer or folder properties
     * @return File or folder properties
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    ObsFSAttribute getAttribute(GetAttributeRequest request) throws ObsException;

    /**
     * Obtain the file content.
     * @param request Request parameters for obtaining the file content
     * @return Response to the request for obtaining file content
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    ReadFileResult readFile(ReadFileRequest request) throws ObsException;

    /**
     * Write data to a file.
     * @param request Request parameters for writing data to a file
     * @return Files in the bucket that supports the file interface
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    ObsFSFile writeFile(WriteFileRequest request) throws ObsException;

    /**
     * Append data to a file.
     * @param request Request parameters for writing data to a file
     * @return Files in the bucket that supports the file interface
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    ObsFSFile appendFile(WriteFileRequest request) throws ObsException;

    /**
     * Rename a file.
     * @param request Request parameters for renaming a file
     * @return Response to the request for renaming a file
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    RenameResult renameFile(RenameRequest request) throws ObsException;

    /**
     * Rename a folder.
     * @param request Request parameters for renaming a folder
     * @return Response to the request for renaming a folder
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    RenameResult renameFolder(RenameRequest request) throws ObsException;

    /**
     * Truncate a file.
     * @param request Request parameters for truncating a file
     * @return Response to the request for truncating a file
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    TruncateFileResult truncateFile(TruncateFileRequest request) throws ObsException;

    /**
     * Delete a file.
     * @param request Request parameters for deleting a file
     * @return Response to the request for deleting a file
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    DropFileResult dropFile(DropFileRequest request) throws ObsException;

    /**
     * Delete a folder.
     * @param request Request parameters for deleting a folder
     * @return Batch task execution status
     * @throws ObsException OBS SDK self-defined exception, 
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    TaskProgressStatus dropFolder(DropFolderRequest request) throws ObsException;

    /**
     * obtain folder contentSummary
     *
     * @param request Request parameters for obtain folder contentSummary
     * @return Response to the request for obtain folder contentSummary
     * @throws ObsException OBS SDK self-defined exception,
     *                      thrown when the interface fails to be called or access to OBS fails
     * @since 3.20.5
     */
    ListContentSummaryResult listContentSummary(ListContentSummaryRequest request) throws ObsException;

    /**
     * obtain folder contentSummary
     *
     * @param request Request parameters for obtain folder contentSummary
     * @return Response to the request for obtain folder contentSummary
     * @throws ObsException OBS SDK self-defined exception,
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    ListContentSummaryFsResult listContentSummaryFs(ListContentSummaryFsRequest request) throws ObsException;

    /**
     * obtain current folder contentSummary
     * @param request Request parameters for obtain current folder contentSummary
     * @return Response to the request for obtain current folder contentSummary
     * @throws ObsException ObsException OBS SDK self-defined exception,
     *                     thrown when the interface fails to be called or access to OBS fails
     */
    ContentSummaryFsResult getContentSummaryFs(ContentSummaryFsRequest request) throws ObsException;

    /**
     * set access label for a folder
     * s3 protocol is not supported
     *
     * @param request Request parameters for access label setting
     * @return Response to the request for access label setting
     * @throws ObsException ObsException OBS SDK self-defined exception,
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    SetAccessLabelResult setAccessLabelFs(SetAccessLabelRequest request) throws ObsException;

    /**
     * get access label of the folder
     * s3 protocol is not supported
     *
     * @param request Request parameters for getting access label
     * @return Response to the request for getting access label
     * @throws ObsException ObsException OBS SDK self-defined exception,
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    GetAccessLabelResult getAccessLabelFs(GetAccessLabelRequest request) throws ObsException;

    /**
     * delete access label of the folder
     * s3 protocol is not supported
     *
     * @param request Request parameters for deleting access label
     * @return Response to the request for deleting access label
     * @throws ObsException ObsException OBS SDK self-defined exception,
     *                      thrown when the interface fails to be called or access to OBS fails
     */
    DeleteAccessLabelResult deleteAccessLabelFs(DeleteAccessLabelRequest request) throws ObsException;
}
