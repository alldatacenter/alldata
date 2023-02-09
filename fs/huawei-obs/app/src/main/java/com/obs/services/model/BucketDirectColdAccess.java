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
 * Direct reading policy for Archive objects in a bucket
 *
 */
public class BucketDirectColdAccess extends HeaderResponse {

    private RuleStatusEnum status;

    /**
     * Constructor
     * 
     * @param status
     *            Direct reading status of Archive objects in a bucket
     */
    public BucketDirectColdAccess(RuleStatusEnum status) {
        this.status = status;
    }

    /**
     * Constructor
     */
    public BucketDirectColdAccess() {
    }

    /**
     * Obtain the direct reading status of Archive objects in a bucket.
     * 
     * @return Direct reading status of Archive objects in a bucket
     */
    public RuleStatusEnum getStatus() {
        return status;
    }

    /**
     * Set the direct reading status of Archive objects in a bucket.
     * 
     * @param status
     *            Direct reading status of Archive objects in a bucket
     */
    public void setStatus(RuleStatusEnum status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "BucketDirectColdAccess [Status=" + status.getCode() + "]";
    }

}
