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

import java.io.File;
import java.io.InputStream;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.ObjectMetadata;

/**
 * Buckets that support the file interface
 *
 */
public class ObsFSBucket {

    protected ObsClient innerClient;

    private String bucketName;

    private String location;

    public ObsFSBucket(String bucketName, String location) {
        super();
        this.bucketName = bucketName;
        this.location = location;
    }

    protected void setInnerClient(ObsClient innerClient) {
        this.innerClient = innerClient;
    }

    /**
     * Set status of the file gateway feature for a bucket.
     * 
     * @param status
     *            Status of the file gateway feature
     * @return Response to the request for setting status of the file gateway
     *         feature for the bucket
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public HeaderResponse setFSStatus(FSStatusEnum status) throws ObsException {
        this.checkInternalClient();
        return this.innerClient.setBucketFSStatus(new SetBucketFSStatusRequest(this.bucketName, status));
    }

    /**
     * Create a folder.
     * 
     * @param folderName
     *            Folder name
     * @return Folders in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFolder newFolder(String folderName) throws ObsException {
        this.checkInternalClient();
        return this.innerClient.newFolder(new NewFolderRequest(this.bucketName, folderName));
    }

    /**
     * Create a file.
     * 
     * @param fileName
     *            File name
     * @param input
     *            File input stream
     * @param metadata
     *            File properties
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile newFile(String fileName, InputStream input, ObjectMetadata metadata) throws ObsException {
        this.checkInternalClient();
        NewFileRequest request = new NewFileRequest(this.bucketName, fileName);
        request.setInput(input);
        return this.innerClient.newFile(request);
    }

    /**
     * Create a file.
     * 
     * @param fileName
     *            File name
     * @param input
     *            File input stream
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile newFile(String fileName, InputStream input) throws ObsException {
        return this.newFile(fileName, input, null);
    }

    /**
     * Create a file.
     * 
     * @param fileName
     *            File name
     * @param file
     *            Local path to the file
     * @param metadata
     *            File properties
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile newFile(String fileName, File file, ObjectMetadata metadata) throws ObsException {
        this.checkInternalClient();
        NewFileRequest request = new NewFileRequest(this.bucketName, fileName);
        request.setFile(file);
        return this.innerClient.newFile(request);
    }

    /**
     * Create a file.
     * 
     * @param fileName
     *            File name
     * @param file
     *            Local path to the file
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile newFile(String fileName, File file) throws ObsException {
        return this.newFile(fileName, file, null);
    }

    /**
     * Obtain the bucket name.
     * 
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Obtain the bucket location.
     * 
     * @return Bucket location
     */
    public String getLocation() {
        return location;
    }

    protected void checkInternalClient() {
        ServiceUtils.assertParameterNotNull(this.innerClient, "ObsClient is null");
    }
}
