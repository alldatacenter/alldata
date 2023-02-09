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

import com.obs.services.exception.ObsException;
import com.obs.services.model.StorageClassEnum;

/**
 * Files in a bucket that supports the file interface
 *
 */
public class ObsFSFile extends ObsFSFolder {

    public ObsFSFile(String bucketName, String objectKey, String etag, String versionId, StorageClassEnum storageClass,
            String objectUrl) {
        super(bucketName, objectKey, etag, versionId, storageClass, objectUrl);
    }

    /**
     * Obtains file properties.
     * 
     * @return File properties
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSAttribute attribute() throws ObsException {
        return super.attribute();
    }

    /**
     * Obtain the file content.
     * 
     * @return Response to the request for obtaining file content
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ReadFileResult read() throws ObsException {
        this.checkInternalClient();
        ReadFileRequest request = new ReadFileRequest(this.getBucketName(), this.getObjectKey());
        return this.innerClient.readFile(request);
    }

    /**
     * Obtain the file content.
     * 
     * @param rangeStart
     *            Start position for reading file content
     * @param rangeEnd
     *            End position for reading file content
     * @return ObsException OBS SDK self-defined exception, thrown when the
     *         interface fails to be called or access to OBS fails
     */
    public ReadFileResult read(long rangeStart, long rangeEnd) throws ObsException {
        this.checkInternalClient();
        ReadFileRequest request = new ReadFileRequest(this.getBucketName(), this.getObjectKey());
        request.setRangeStart(rangeStart);
        request.setRangeEnd(rangeEnd);
        return this.innerClient.readFile(request);
    }

    /**
     * Write data to a file.
     * 
     * @param file
     *            Local path to the file
     * @param position
     *            Start position for writing data to a file
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile write(File file, long position) throws ObsException {
        this.checkInternalClient();
        WriteFileRequest request = new WriteFileRequest(this.getBucketName(), this.getObjectKey(), file, position);
        return this.innerClient.writeFile(request);
    }

    /**
     * Write data to a file.
     * 
     * @param file
     *            Local path to the file
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile write(File file) throws ObsException {
        return this.write(file, 0);
    }

    /**
     * Write data to a file.
     * 
     * @param input
     *            Data stream to be uploaded
     * @param position
     *            Start position for writing data to a file
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile write(InputStream input, long position) throws ObsException {
        this.checkInternalClient();
        WriteFileRequest request = new WriteFileRequest(this.getBucketName(), this.getObjectKey(), input, position);
        return this.innerClient.writeFile(request);
    }

    /**
     * Append data to a file.
     * 
     * @param file
     *            Local path to the file
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile append(File file) throws ObsException {
        this.checkInternalClient();
        WriteFileRequest request = new WriteFileRequest(this.getBucketName(), this.getObjectKey(), file);
        return this.innerClient.appendFile(request);
    }

    /**
     * Append data to a file.
     * 
     * @param input
     *            Data stream to be uploaded
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile append(InputStream input) throws ObsException {
        this.checkInternalClient();
        WriteFileRequest request = new WriteFileRequest(this.getBucketName(), this.getObjectKey(), input);
        return this.innerClient.appendFile(request);
    }

    /**
     * Write data to a file.
     * 
     * @param input
     *            Data stream to be uploaded
     * @return Files in the bucket that supports the file interface
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSFile write(InputStream input) throws ObsException {
        return this.write(input, 0);
    }

    /**
     * Rename a file.
     * 
     * @param newName
     *            New file name
     * @return Response to the request for renaming a file
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public RenameResult rename(String newName) throws ObsException {
        this.checkInternalClient();
        RenameRequest request = new RenameRequest(this.getBucketName(), this.getObjectKey(), newName);
        return this.innerClient.renameFile(request);
    }

    /**
     * Truncate a file.
     * 
     * @param newLength
     *            File size after the truncation
     * @return Response to the request for truncating a file
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public TruncateFileResult truncate(long newLength) throws ObsException {
        this.checkInternalClient();
        TruncateFileRequest request = new TruncateFileRequest(this.getBucketName(), this.getObjectKey(), newLength);
        return this.innerClient.truncateFile(request);
    }

    /**
     * Delete a file.
     * 
     * @return Response to the request for deleting a file
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public DropFileResult drop() throws ObsException {
        this.checkInternalClient();
        DropFileRequest request = new DropFileRequest(this.getBucketName(), this.getObjectKey(), this.getVersionId());
        return this.innerClient.dropFile(request);
    }

}
