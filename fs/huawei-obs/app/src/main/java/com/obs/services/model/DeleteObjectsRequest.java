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
**/

package com.obs.services.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parameters in an object batch deletion request
 */
public class DeleteObjectsRequest extends GenericRequest {

    {
        httpMethod = HttpMethodEnum.POST;
    }

    private boolean quiet;

    private List<KeyAndVersion> keyAndVersions;

    private String encodingType;

    public DeleteObjectsRequest() {
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     */
    public DeleteObjectsRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param quiet
     *            Deletion response mode. "false" indicates that the "verbose"
     *            mode is used and "true" indicates that the "quiet" mode is
     *            used.
     * @param keyAndVersions
     *            To-be-deleted object array
     */
    public DeleteObjectsRequest(String bucketName, boolean quiet, KeyAndVersion[] keyAndVersions) {
        this.bucketName = bucketName;
        this.quiet = quiet;
        this.setKeyAndVersions(keyAndVersions);
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param quiet
     *            Deletion response mode. "false" indicates that the "verbose"
     *            mode is used and "true" indicates that the "quiet" mode is
     *            used.
     * @param keyAndVersions
     *            To-be-deleted object array
     * @param encodingType
     *            The encoding type use for encode objectKey.
     */
    public DeleteObjectsRequest(String bucketName, boolean quiet, KeyAndVersion[] keyAndVersions, String encodingType) {
        this.bucketName = bucketName;
        this.quiet = quiet;
        this.setKeyAndVersions(keyAndVersions);
        this.encodingType = encodingType;
    }

    /**
     * Obtain the response mode of the batch deletion. "false" indicates that
     * the "verbose" mode is used and "true" indicates that the "quiet" mode is
     * used.
     * 
     * @return Response mode of the object batch deletion request
     */
    public boolean isQuiet() {
        return quiet;
    }

    /**
     * Set the response mode for the batch deletion. "false" indicates that the
     * "verbose" mode is used and "true" indicates that the "quiet" mode is
     * used.
     * 
     * @param quiet
     *            Response mode of the object batch deletion request
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
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
     * Get the encoding type that used for encode objectkey
     * @return encodingType
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Obtain the list of to-be-deleted objects.
     * 
     * @return List of to-be-deleted objects
     */
    public List<KeyAndVersion> getKeyAndVersionsList() {
        if (this.keyAndVersions == null) {
            this.keyAndVersions = new ArrayList<>();
        }
        return this.keyAndVersions;
    }

    /**
     * Add an object to be deleted.
     * 
     * @param objectKey
     *            Object name
     * @param versionId
     *            Version ID of the object
     * @return Object newly added to the deletion list
     */
    public KeyAndVersion addKeyAndVersion(String objectKey, String versionId) {
        KeyAndVersion kv = new KeyAndVersion(objectKey, versionId);
        this.getKeyAndVersionsList().add(kv);
        return kv;
    }

    /**
     * Add an object to be deleted.
     * 
     * @param objectKey
     *            Object name
     * @return Object newly added to the deletion list
     */
    public KeyAndVersion addKeyAndVersion(String objectKey) {
        return this.addKeyAndVersion(objectKey, null);
    }

    /**
     * Obtain the to-be-deleted object array.
     * 
     * @return To-be-deleted object array
     */
    public KeyAndVersion[] getKeyAndVersions() {
        return this.getKeyAndVersionsList().toArray(new KeyAndVersion[this.getKeyAndVersionsList().size()]);
    }

    /**
     * Specify the to-be-deleted object array.
     * 
     * @param keyAndVersions
     *            To-be-deleted object array
     */
    public void setKeyAndVersions(KeyAndVersion[] keyAndVersions) {
        if (keyAndVersions != null && keyAndVersions.length > 0) {
            this.keyAndVersions = new ArrayList<>(Arrays.asList(keyAndVersions));
        }
    }

    @Override
    public String toString() {
        return "DeleteObjectsRequest [bucketName=" + bucketName + ", quiet=" + quiet + ", encodingType=" + encodingType
                + ", keyAndVersions=" + this.keyAndVersions + "]";
    }

}
