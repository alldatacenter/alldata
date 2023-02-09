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

package com.obs.services.model.fs;

import com.obs.services.model.AbstractBulkRequest;
import com.obs.services.model.DeleteObjectResult;
import com.obs.services.model.TaskCallback;

/**
 * Request parameters for deleting folders. Deleting a folder will delete all
 * sub-folders and files in the folder. This function does not support buckets
 * with versioning enabled.
 */
public class DropFolderRequest extends AbstractBulkRequest {

    private String folderName;

    private String encodingType;

    private TaskCallback<DeleteObjectResult, String> callback;

    public DropFolderRequest() {
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     */
    public DropFolderRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    public DropFolderRequest(String bucketName, String folderName) {
        this.bucketName = bucketName;
        this.folderName = folderName;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param encodingType
     *            The encoding type use for encode objectKey.
     */
    public DropFolderRequest(String bucketName, String folderName, String encodingType) {
        this.bucketName = bucketName;
        this.folderName = folderName;
        this.encodingType = encodingType;
    }

    /**
     * Obtain the folder name.
     * 
     * @return Folder name
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * Set the folder name.
     * 
     * @param folderName
     *            Folder name
     */
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    /**
     * Obtain the callback object of a batch task.
     * 
     * @return Callback object
     */
    public TaskCallback<DeleteObjectResult, String> getCallback() {
        return callback;
    }

    /**
     * Set the callback object of a batch task.
     * 
     * @param callback
     *            Callback object
     */
    public void setCallback(TaskCallback<DeleteObjectResult, String> callback) {
        this.callback = callback;
    }

    /**
     * Set the encoding type that used for encode objectkey
     *
     * @param encodingType
     *            could chose url.
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     * Obtain the list of to-be-deleted objects.
     *
     * @return List of to-be-deleted objects
     */
    public String getEncodingType() {
        return encodingType;
    }

    @Override
    public String toString() {
        return "DropFolderRequest [bucketName=" + bucketName + ", folderName=" + folderName
                + ", encodingType=" + encodingType + "]";
    }
}
