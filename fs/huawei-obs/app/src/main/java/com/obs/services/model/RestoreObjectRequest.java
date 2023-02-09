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

package com.obs.services.model;

/**
 *
 * Parameters in a request for restoring an Archive object
 *
 */
public class RestoreObjectRequest extends BaseObjectRequest {

    {
        httpMethod = HttpMethodEnum.POST;
    }

    private String versionId;

    /**
     * Obtain the object version ID.
     *
     * @return Version ID of the object
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Set the version ID of the object.
     *
     * @param versionId
     *            Version ID of the object
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Expedited restoration, which restores an object in 1 to 5 minutes
     */
    @Deprecated
    public static final String EXPEDITED = "Expedited";

    /**
     * Standard restoration, which restores the object in 3 to 5 hours
     */
    @Deprecated
    public static final String STANDARD = "Standard";

    /**
     * Batch restoration, which restores objects in 5 to 12 hours
     */
    @Deprecated
    public static final String BULK = "Bulk";

    /**
     *
     * Status of the Archive object
     *
     */
    public static class RestoreObjectStatus extends HeaderResponse {

        private int code;
        /**
         * The object has been restored and can be downloaded.
         */
        public static final RestoreObjectStatus AVALIABLE = new RestoreObjectStatus(200);
        /**
         * The object is being restored and cannot be downloaded.
         */
        public static final RestoreObjectStatus INPROGRESS = new RestoreObjectStatus(202);

        private RestoreObjectStatus(int code) {
            this.code = code;
        }

        /**
         * Obtain the status code of the object.
         *
         * @return Status code of the object
         */
        public int getCode() {
            return this.code;
        }

        public static RestoreObjectStatus valueOf(int retCode) {
            return retCode == 200 ? AVALIABLE : retCode == 202 ? INPROGRESS : new RestoreObjectStatus(retCode);
        }
    }

    private int days;

    private RestoreTierEnum tier;

    public RestoreObjectRequest() {

    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param days
     *            Retention period of the restored object
     */
    public RestoreObjectRequest(String bucketName, String objectKey, int days) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.days = days;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Version ID of the object
     * @param days
     *            Retention period of the restored object
     */
    public RestoreObjectRequest(String bucketName, String objectKey, String versionId, int days) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.versionId = versionId;
        this.days = days;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Version ID of the object
     * @param days
     *            Retention period of the restored object
     * @param tier
     *            Restore option
     */
    @Deprecated
    public RestoreObjectRequest(String bucketName, String objectKey, String versionId, int days, String tier) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.versionId = versionId;
        this.days = days;
        this.tier = RestoreTierEnum.getValueFromCode(tier);
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Version ID of the object
     * @param days
     *            Retention period of the restored object
     * @param tier
     *            Restore option
     */
    public RestoreObjectRequest(String bucketName, String objectKey, String versionId, int days, RestoreTierEnum tier) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.versionId = versionId;
        this.days = days;
        this.tier = tier;
    }

    /**
     * Obtain the retention period of the restored object. The value ranges from
     * 1 to 30 (in days).
     *
     * @return Retention period of the restored object
     */
    public int getDays() {
        return days;
    }

    /**
     * Set the retention period of the restored object. The value ranges from 1
     * to 30 (in days).
     *
     * @param days
     *            Retention period of the restored object
     */
    public void setDays(int days) {
        this.days = days;
    }

    /**
     * Obtain the restore option.
     *
     * @see #getRestoreTier()
     * @return Restore option
     */
    @Deprecated
    public String getTier() {
        return this.tier != null ? this.tier.getCode() : null;
    }

    /**
     * Set the restore option.
     *
     * @see #setRestoreTier(RestoreTierEnum tier)
     * @param tier
     *            Restore option
     */
    @Deprecated
    public void setTier(String tier) {
        this.tier = RestoreTierEnum.getValueFromCode(tier);
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

    @Override
    public String toString() {
        return "RestoreObjectRequest [days=" + days + ", tier=" + tier + ", getBucketName()=" + getBucketName()
                + ", getObjectKey()=" + getObjectKey() + ", getVersionId()=" + getVersionId() + ", isRequesterPays()="
                + isRequesterPays() + "]";
    }
}
