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

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.StorageClassEnum;
import com.obs.services.model.TaskProgressStatus;

/**
 * Folders in a bucket that supports the file interface
 *
 */
public class ObsFSFolder extends PutObjectResult {

    protected ObsClient innerClient;

    public ObsFSFolder(String bucketName, String objectKey, String etag, String versionId,
            StorageClassEnum storageClass, String objectUrl) {
        super(bucketName, objectKey, etag, versionId, storageClass, objectUrl);
    }

    protected void setInnerClient(ObsClient innerClient) {
        this.innerClient = innerClient;
    }
    
    /**
     * Obtain folder properties.
     * 
     * @return Folder properties
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public ObsFSAttribute attribute() throws ObsException {
        this.checkInternalClient();
        return (ObsFSAttribute) this.innerClient.getObjectMetadata(this.getBucketName(), this.getObjectKey());
    }

    /**
     * Rename a folder.
     * 
     * @param newName
     *            New folder name
     * @return Response to the request for renaming a folder
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public RenameResult rename(String newName) throws ObsException {
        this.checkInternalClient();
        RenameRequest request = new RenameRequest(this.getBucketName(), this.getObjectKey(), newName);
        return this.innerClient.renameFolder(request);
    }

    /**
     * Delete a folder.
     * 
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public TaskProgressStatus dropFolder() throws ObsException {
        this.checkInternalClient();
        DropFolderRequest request = new DropFolderRequest(this.getBucketName(), this.getObjectKey());
        return this.innerClient.dropFolder(request);
    }

    protected void checkInternalClient() {
        ServiceUtils.assertParameterNotNull(this.innerClient, "ObsClient is null");
    }
}
