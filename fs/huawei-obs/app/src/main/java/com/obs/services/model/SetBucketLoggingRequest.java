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

/**
 * Set logging for a bucket.
 * 
 * @since 3.20.3
 */
public class SetBucketLoggingRequest extends BaseBucketRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private BucketLoggingConfiguration loggingConfiguration;
    private boolean updateTargetACLifRequired = false;
    
    public SetBucketLoggingRequest(String bucketName, BucketLoggingConfiguration loggingConfiguration) {
        this.bucketName = bucketName;
        this.loggingConfiguration = loggingConfiguration;
    }
    
    public SetBucketLoggingRequest(String bucketName, 
            BucketLoggingConfiguration loggingConfiguration, boolean updateTargetACLifRequired) {
        this.bucketName = bucketName;
        this.loggingConfiguration = loggingConfiguration;
        this.updateTargetACLifRequired = updateTargetACLifRequired;
    }

    public BucketLoggingConfiguration getLoggingConfiguration() {
        return loggingConfiguration;
    }

    public void setLoggingConfiguration(BucketLoggingConfiguration loggingConfiguration) {
        this.loggingConfiguration = loggingConfiguration;
    }

    public boolean isUpdateTargetACLifRequired() {
        return updateTargetACLifRequired;
    }

    public void setUpdateTargetACLifRequired(boolean updateTargetACLifRequired) {
        this.updateTargetACLifRequired = updateTargetACLifRequired;
    }

    @Override
    public String toString() {
        return "SetBucketLoggingRequest [loggingConfiguration=" + loggingConfiguration + ", updateTargetACLifRequired="
                + updateTargetACLifRequired + ", getBucketName()=" + getBucketName() + ", isRequesterPays()="
                + isRequesterPays() + "]";
    }
}
