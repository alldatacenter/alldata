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
 * Request for configuring bucket ACL.
 * 
 * @since 3.20.3
 *
 */
public class SetBucketAclRequest extends BaseBucketRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private AccessControlList acl;
    private String cannedACL;
    
    public SetBucketAclRequest() {
        
    }
    
    public SetBucketAclRequest(String bucketName, AccessControlList acl) {
        this.bucketName = bucketName;
        this.acl = acl;
    }

    public AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }
    
    public String getCannedACL() {
        return cannedACL;
    }

    public void setCannedACL(String cannedACL) {
        this.cannedACL = cannedACL;
    }

    @Override
    public String toString() {
        return "SetBucketAclRequest [acl=" + acl + ", cannedACL=" + cannedACL + ", getBucketName()=" + getBucketName()
                + ", isRequesterPays()=" + isRequesterPays() + "]";
    }
}
