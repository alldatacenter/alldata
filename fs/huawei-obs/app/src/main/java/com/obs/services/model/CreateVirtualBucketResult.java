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
 **/

package com.obs.services.model;

/**
 * Response to a virtual bucket creation request
 */
public class CreateVirtualBucketResult extends HeaderResponse {
    private  String message;
    private  String virtualBucketName;
    private String bucketName1;
    private String bucketName2;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getVirtualBucketName() {
        return virtualBucketName;
    }

    public void setVirtualBucketName(String virtualBucketName) {
        this.virtualBucketName = virtualBucketName;
    }

    public String getBucketName1() {
        return bucketName1;
    }

    public void setBucketName1(String bucketName1) {
        this.bucketName1 = bucketName1;
    }

    public String getBucketName2() {
        return bucketName2;
    }

    public void setBucketName2(String bucketName2) {
        this.bucketName2 = bucketName2;
    }

}
