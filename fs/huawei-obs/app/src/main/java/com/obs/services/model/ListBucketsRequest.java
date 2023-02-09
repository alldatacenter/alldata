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
 * Parameters in a request for listing buckets
 *
 */
public class ListBucketsRequest {
    private boolean queryLocation = true;

    private BucketTypeEnum bucketType;

    public boolean isQueryLocation() {
        return queryLocation;
    }

    /**
     * Specify whether to list the region information of all buckets.
     * 
     * @param queryLocation
     *            Whether to list the region information of all buckets
     */
    public void setQueryLocation(boolean queryLocation) {
        this.queryLocation = queryLocation;
    }

    @Override
    public String toString() {
        return "ListBucketsRequest [queryLocation=" + queryLocation + "]";
    }

    public BucketTypeEnum getBucketType() {
        return bucketType;
    }

    public void setBucketType(BucketTypeEnum bucketType) {
        this.bucketType = bucketType;
    }
}
