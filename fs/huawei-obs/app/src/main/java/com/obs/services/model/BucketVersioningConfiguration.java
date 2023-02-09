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

/**
 * Bucket versioning status
 */
public class BucketVersioningConfiguration extends HeaderResponse {
    /**
     * Versioning is suspended.
     */
    @Deprecated
    public static final String SUSPENDED = "Suspended";

    /**
     * Versioning is enabled.
     */
    @Deprecated
    public static final String ENABLED = "Enabled";

    private VersioningStatusEnum status;

    /**
     * Constructor If versioning is enabled for a bucket, it cannot be disabled
     * or changed to {@link #SUSPENDED}.
     * 
     * @param status
     *            Versioning status
     * @see #ENABLED
     * @see #SUSPENDED
     */
    @Deprecated
    public BucketVersioningConfiguration(String status) {
        this.status = VersioningStatusEnum.getValueFromCode(status);
    }

    /**
     * Constructor If versioning is enabled for a bucket, it cannot be disabled
     * or changed to suspended.
     * 
     * @param status
     *            Versioning status
     */
    public BucketVersioningConfiguration(VersioningStatusEnum status) {
        this.status = status;
    }

    public BucketVersioningConfiguration() {

    }

    /**
     * Obtain the versioning status of a bucket.
     * 
     * @return status Versioning status
     * @see #getVersioningStatus()
     */
    @Deprecated
    public String getStatus() {
        return this.status != null ? this.status.getCode() : null;
    }

    /**
     * Set the versioning status.
     * 
     * @param status
     *            Versioning status
     * @see #setVersioningStatus(VersioningStatusEnum status)
     */
    @Deprecated
    public void setStatus(String status) {
        this.status = VersioningStatusEnum.getValueFromCode(status);
    }

    /**
     * Obtain the versioning status.
     * 
     * @return status Versioning status
     */
    public VersioningStatusEnum getVersioningStatus() {
        return status;
    }

    /**
     * Set the versioning status.
     * 
     * @param status
     *            Versioning status
     */
    public void setVersioningStatus(VersioningStatusEnum status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "BucketVersioningConfiguration [status=" + status + "]";
    }

}
