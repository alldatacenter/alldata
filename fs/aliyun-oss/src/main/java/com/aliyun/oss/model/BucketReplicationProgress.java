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

import java.util.Date;

/**
 * The progress of cross region bucket replication.
 * <p>
 * For historical data, it uses the percentage (e.g. 0.85 means 85%) of copied
 * file count as the progress indicator. It's only applicable for buckets
 * enabled with historical data replication. For new coming data, it uses the
 * replicated timestamp as the progress indicator. It means all files which are
 * uploaded before that timestamp have been replicated to the target bucket.
 * </p>
 */
public class BucketReplicationProgress extends GenericResult {
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

    public float getHistoricalObjectProgress() {
        return historicalObjectProgress;
    }

    public void setHistoricalObjectProgress(float historicalObjectProgress) {
        this.historicalObjectProgress = historicalObjectProgress;
    }

    public Date getNewObjectProgress() {
        return newObjectProgress;
    }

    public void setNewObjectProgress(Date newObjectProgress) {
        this.newObjectProgress = newObjectProgress;
    }

    private String replicationRuleID;
    private ReplicationStatus replicationStatus;
    private String targetBucketName;
    private String targetBucketLocation;
    private String targetCloud;
    private String targetCloudLocation;
    private boolean enableHistoricalObjectReplication;

    private float historicalObjectProgress;
    private Date newObjectProgress;
}
