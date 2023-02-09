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
 *
 * OBS Bucket extension permission
 *
 */
public enum ExtensionBucketPermissionEnum {
    /**
     * Grant the read permission to all users belonging to the specified
     * "domainId" for listing objects, listing multipart uploads, listing bucket
     * versions, and obtaining bucket metadata.
     */
    GRANT_READ("grantReadHeader"),
    /**
     * Grant the write permission to all users
     * belonging to the specified "domainId" so
     * that the users can create, delete,
     * overwrite objects in buckets, as well as
     * initialize, upload, copy, and combine
     * parts and cancel multipart uploads.
     */
    GRANT_WRITE("grantWriteHeader"),
    /**
     * Grant the "READ_ACP" permission to all
     * users belonging to the specified
     * "domainId" to obtain ACLs of objects.
     */
    GRANT_READ_ACP("grantReadAcpHeader"),
    /**
     * Grant the "WRITE_ACP" permission to all
     * users belonging to the specified
     * "domainId" to modify bucket ACLs.
     */
    GRANT_WRITE_ACP("grantWriteAcpHeader"),
    /**
     * Grant full control permissions to users
     * in the domain of the specified ID.
     */
    GRANT_FULL_CONTROL("grantFullControlHeader"),
    /**
     * Grant the read permission to all users
     * belonging to the specified "domainId".
     * By default, these users have the read
     * permission on all objects in the
     * bucket.
     */
    GRANT_READ_DELIVERED("grantReadDeliveredHeader"),
    /**
     * Grant the full control permission to
     * all users belonging to the specified
     * "domainId". By default, these users
     * have the full control permission on
     * all objects in the bucket.
     */
    GRANT_FULL_CONTROL_DELIVERED("grantFullControlDeliveredHeader");

    private String code;

    private ExtensionBucketPermissionEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
