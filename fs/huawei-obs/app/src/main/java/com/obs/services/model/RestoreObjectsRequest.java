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

package com.obs.services.model;

import java.util.List;

/**
 * Request parameters of restoring Archive objects in a batch. The "prefix" and
 * "keyAndVersions" parameters cannot be set in the same request. If both
 * parameters are empty, all Archive objects in the bucket are restored.
 */
public class RestoreObjectsRequest extends AbstractBulkRequest {

    private int days;

    private RestoreTierEnum tier;

    private String prefix;

    private boolean versionRestored;

    private String encodingType;

    private List<KeyAndVersion> keyAndVersions;

    private TaskCallback<RestoreObjectResult, RestoreObjectRequest> callback;

    public RestoreObjectsRequest() {

    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     */
    public RestoreObjectsRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param days
     *            Retention period of the restored objects
     * @param tier
     *            Restore option
     */
    public RestoreObjectsRequest(String bucketName, int days, RestoreTierEnum tier) {
        this.bucketName = bucketName;
        this.days = days;
        this.tier = tier;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param days
     *            Retention period of the restored objects
     * @param tier
     *            Restore option
     * @param encodingType
     *            The encoding type use for encode objectKey.
     */
    public RestoreObjectsRequest(String bucketName, int days, RestoreTierEnum tier, String encodingType) {
        this.bucketName = bucketName;
        this.days = days;
        this.tier = tier;
        this.encodingType = encodingType;
    }

    /**
     * Obtain the retention period of the restored objects. The value ranges
     * from 1 to 30 (in days).
     * 
     * @return Retention period of the restored objects
     */
    public int getDays() {
        return days;
    }

    /**
     * Set the retention period of the restored objects. The value ranges from 1
     * to 30 (in days).
     * 
     * @param days
     *            Retention period of the restored objects
     */
    public void setDays(int days) {
        this.days = days;
    }

    /**
     * Obtain the restore option.
     * 
     * @return Restore option
     */
    public RestoreTierEnum getRestoreTier() {
        return tier;
    }

    /**
     * Set the restore option.
     * 
     * @param tier
     *            Restore option
     */
    public void setRestoreTier(RestoreTierEnum tier) {
        this.tier = tier;
    }

    /**
     * Set the name prefix of the objects to be restored in a batch.
     * 
     * @param prefix
     *            Object name prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Obtain the name prefix of the objects to be restored in a batch.
     * 
     * @return Object name prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Obtain whether to restore all versions of Archive objects. The default
     * value is "false", indicating that only latest versions of Archive objects
     * are restored.
     * 
     * @return Identifier of version restore
     */
    public boolean isVersionRestored() {
        return versionRestored;
    }

    /**
     * Set whether to restore all versions of Archive objects.
     *
     * @param versionRestored
     *            Identifier of version restore
     */
    public void setVersionRestored(boolean versionRestored) {
        this.versionRestored = versionRestored;
    }

    /**
     * Set the list of objects to be restored.
     * 
     * @param keyAndVersions
     *            List of objects to be restored
     */
    public void setKeyAndVersions(List<KeyAndVersion> keyAndVersions) {
        this.keyAndVersions = keyAndVersions;
    }

    /**
     * Obtain the list of objects to be restored.
     * 
     * @return List of objects to be restored
     */
    public List<KeyAndVersion> getKeyAndVersions() {
        return this.keyAndVersions;
    }

    /**
     * Add an object to be restored.
     * 
     * @param objectKey
     *            Object name
     * @param versionId
     *            Object version
     * @return Object that has been added to be restored
     */
    public KeyAndVersion addKeyAndVersion(String objectKey, String versionId) {
        KeyAndVersion kv = new KeyAndVersion(objectKey, versionId);
        this.getKeyAndVersions().add(kv);
        return kv;
    }

    /**
     * Add an object to be restored.
     * 
     * @param objectKey
     *            Object name
     * @return Object that has been added to be restored
     */
    public KeyAndVersion addKeyAndVersion(String objectKey) {
        return this.addKeyAndVersion(objectKey, null);
    }

    /**
     * Obtain the callback object of a batch task.
     * 
     * @return Callback object
     */
    public TaskCallback<RestoreObjectResult, RestoreObjectRequest> getCallback() {
        return callback;
    }

    /**
     * Set the callback object of a batch task.
     * 
     * @param callback
     *            Callback object
     */
    public void setCallback(TaskCallback<RestoreObjectResult, RestoreObjectRequest> callback) {
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
        return "RestoreObjectsRequest [bucketName=" + bucketName + ", days=" + days + ", tier=" + tier
                + ", encodingType=" + encodingType + "]";
    }

}
