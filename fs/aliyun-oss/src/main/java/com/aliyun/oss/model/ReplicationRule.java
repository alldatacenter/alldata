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

import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.model.AddBucketReplicationRequest.ReplicationAction;

/**
 * The cross region replication rules on a bucket.
 */
public class ReplicationRule {

    public String getReplicationRuleID() {
        return replicationRuleID;
    }

    public void setReplicationRuleID(String replicationRuleID) {
        this.replicationRuleID = replicationRuleID;
    }

    public ReplicationStatus getReplicationStatus() {
        return replicationStatus;
    }

    public void setReplicationStatus(ReplicationStatus replicationStatus) {
        this.replicationStatus = replicationStatus;
    }

    public String getTargetBucketName() {
        return targetBucketName;
    }

    public void setTargetBucketName(String targetBucketName) {
        this.targetBucketName = targetBucketName;
    }

    public String getTargetBucketLocation() {
        return targetBucketLocation;
    }

    public void setTargetBucketLocation(String targetBucketLocation) {
        this.targetBucketLocation = targetBucketLocation;
    }

    public String getTargetCloud(){
        return this.targetCloud;
    }
    public void setTargetCloud(String targetCloud){
        this.targetCloud = targetCloud;
    }
    public String getTargetCloudLocation(){
        return this.targetCloudLocation;
    }
    public void setTargetCloudLocation(String targetCloudLocation){
        this.targetCloudLocation = targetCloudLocation;
    }

    public boolean isEnableHistoricalObjectReplication() {
        return enableHistoricalObjectReplication;
    }

    public void setEnableHistoricalObjectReplication(boolean enableHistoricalObjectReplication) {
        this.enableHistoricalObjectReplication = enableHistoricalObjectReplication;
    }

    public List<String> getObjectPrefixList() {
        return objectPrefixList;
    }

    public void setObjectPrefixList(List<String> objectPrefixList) {
        this.objectPrefixList = new ArrayList<String>();
        if (objectPrefixList != null && !objectPrefixList.isEmpty()) {
            this.objectPrefixList.addAll(objectPrefixList);
        }
    }

    public List<ReplicationAction> getReplicationActionList() {
        return replicationActionList;
    }

    public void setReplicationActionList(List<ReplicationAction> replicationActionList) {
        this.replicationActionList = new ArrayList<ReplicationAction>();
        if (replicationActionList != null && !replicationActionList.isEmpty()) {
            this.replicationActionList.addAll(replicationActionList);
        }
    }

    public void setSyncRole(String name) {
        this.syncRole = name;
    }

    public String getSyncRole() {
        return this.syncRole;
    }

    public void setReplicaKmsKeyID(String id) {
        this.replicaKmsKeyID = id;
    }

    public String getReplicaKmsKeyID() {
        return this.replicaKmsKeyID;
    }

    public void setSseKmsEncryptedObjectsStatus(String status) {
        this.sseKmsEncryptedObjectsStatus = status;
    }

    public String getSseKmsEncryptedObjectsStatus() {
        return this.sseKmsEncryptedObjectsStatus;
    }

    public String getSourceBucketLocation() {
        return sourceBucketLocation;
    }

    public void setSourceBucketLocation(String sourceBucketLocation) {
        this.sourceBucketLocation = sourceBucketLocation;
    }

    private String replicationRuleID;
    private ReplicationStatus replicationStatus;
    private String targetBucketName;
    private String targetBucketLocation;
    private String targetCloud;
    private String targetCloudLocation;
    private boolean enableHistoricalObjectReplication;
    private List<String> objectPrefixList;
    private List<ReplicationAction> replicationActionList;
    private String syncRole;
    private String replicaKmsKeyID;
    private String sseKmsEncryptedObjectsStatus;
    private String sourceBucketLocation;

}
