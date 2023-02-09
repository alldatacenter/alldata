/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

public class SetObjectAclRequest extends GenericRequest {

    private CannedAccessControlList cannedACL;

    public SetObjectAclRequest(String bucketName, String key) {
    	super(bucketName, key);
    }
    
    public SetObjectAclRequest(String bucketName, String key, String versionId) {
    	super(bucketName, key, versionId);
    }

    public SetObjectAclRequest(String bucketName, String key, CannedAccessControlList cannedACL) {
        super(bucketName, key);
        this.cannedACL = cannedACL;
    }
    
    public SetObjectAclRequest(String bucketName, String key, String veresionId, CannedAccessControlList cannedACL) {
        super(bucketName, key, veresionId);
        this.cannedACL = cannedACL;
    }

    public CannedAccessControlList getCannedACL() {
        return cannedACL;
    }

    public void setCannedACL(CannedAccessControlList cannedACL) {
        this.cannedACL = cannedACL;
    }

    public SetObjectAclRequest withCannedACL(CannedAccessControlList cannedACL) {
        setCannedACL(cannedACL);
        return this;
    }
}
