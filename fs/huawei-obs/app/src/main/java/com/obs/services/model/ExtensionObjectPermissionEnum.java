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
public enum ExtensionObjectPermissionEnum {
    /**
     * Grant the read permission on all users belonging to the specified
     * "domainId" to read objects and obtain object metadata.
     */
    GRANT_READ("grantReadHeader"),
    /**
     * Grant the "READ_ACP" permission to all
     * users belonging to the specified
     * "domainId" to obtain ACLs of objects.
     */
    GRANT_READ_ACP("grantReadAcpHeader"),
    /**
     * Grant the "WRITE_ACP" permission to all
     * users belonging to the specified
     * "domainId" to modify ACLs of objects.
     */
    GRANT_WRITE_ACP("grantWriteAcpHeader"),
    /**
     * Grant the full control permission to all
     * users belonging to the specified
     * "domainId" to read objects, obtain object
     * metadata, as well as obtain and write
     * object ACLs.
     */
    GRANT_FULL_CONTROL("grantFullControlHeader");

    private String code;

    private ExtensionObjectPermissionEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
