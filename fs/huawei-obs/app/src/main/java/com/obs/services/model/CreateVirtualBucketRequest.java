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
 * Parameters in a virtual bucket creation request
 */
public class CreateVirtualBucketRequest {
    private String regionId;
    private String token;
    private String bucketName1;
    private String bucketName2;
    private String bucketAlias;
    private String agencyId;

    /***
     * Constructor
     * @param regionId region id
     * @param token token
     * @param bucketName1 bucket name1
     * @param bucketName2 bucket name2
     * @param bucketAlias bucket alias
     * @param agencyId agency id
     */
    public CreateVirtualBucketRequest(String regionId, String token, String bucketName1, String bucketName2,
                                      String bucketAlias, String agencyId) {
        this.regionId = regionId;
        this.token = token;
        this.bucketName1 = bucketName1;
        this.bucketName2 = bucketName2;
        this.bucketAlias = bucketAlias;
        this.agencyId = agencyId;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getBucketAlias() {
        return bucketAlias;
    }

    public void setBucketAlias(String bucketAlias) {
        this.bucketAlias = bucketAlias;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    @Override
    public String toString() {
        return "CreateVirtualBucketRequest{" +
                "regionId='" + regionId + '\'' +
                ", token='" + token + '\'' +
                ", bucketName1='" + bucketName1 + '\'' +
                ", bucketName2='" + bucketName2 + '\'' +
                ", bucketAlias='" + bucketAlias + '\'' +
                ", agencyId='" + agencyId + '\'' +
                '}';
    }
}
