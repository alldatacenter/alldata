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

/**
 * The request that is to delete cross region's bucket replication.
 */
public class DeleteBucketReplicationRequest extends GenericRequest {
    public DeleteBucketReplicationRequest(String bucketName, String replicationRuleID) {
        super(bucketName);
        this.replicationRuleID = replicationRuleID;
    }

    public DeleteBucketReplicationRequest(String bucketName) {
        super(bucketName);
    }

    public String getReplicationRuleID() {
        return replicationRuleID;
    }

    public void setReplicationRuleID(String replicationRuleID) {
        this.replicationRuleID = replicationRuleID;
    }

    public DeleteBucketReplicationRequest withReplicationRuleID(String replicationRuleID) {
        setReplicationRuleID(replicationRuleID);
        return this;
    }

    private String replicationRuleID;
}
