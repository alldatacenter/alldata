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

import java.util.HashSet;
import java.util.Set;

/**
 * Bucket info
 */
public class BucketInfo extends GenericResult {
    public Bucket getBucket() {
        return this.bucket;
    }

    public void setBucket(Bucket bucket) {
        this.bucket = bucket;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setDataRedundancyType(DataRedundancyType dataRedundancyType) {
        this.dataRedundancyType = dataRedundancyType;
    }

    public DataRedundancyType getDataRedundancyType() {
        return dataRedundancyType;
    }

    @Deprecated
    public Set<Grant> getGrants() {
        return this.grants;
    }

    public void grantPermission(Grantee grantee, Permission permission) {
        if (grantee == null || permission == null) {
            throw new NullPointerException();
        }

        grants.add(new Grant(grantee, permission));
    }

    public CannedAccessControlList getCannedACL() {
        return cannedACL;
    }

    public void setCannedACL(CannedAccessControlList cannedACL) {
        this.cannedACL = cannedACL;
    }
    
    public ServerSideEncryptionConfiguration getServerSideEncryptionConfiguration() {
        return sseConfig;
    }

    public void setServerSideEncryptionConfiguration(ServerSideEncryptionConfiguration sseConfig) {
        this.sseConfig = sseConfig;
    }

    private Bucket bucket;
    private String comment;
    private DataRedundancyType dataRedundancyType;
    private Set<Grant> grants = new HashSet<Grant>();
    private CannedAccessControlList cannedACL;
    private ServerSideEncryptionConfiguration sseConfig;
}
