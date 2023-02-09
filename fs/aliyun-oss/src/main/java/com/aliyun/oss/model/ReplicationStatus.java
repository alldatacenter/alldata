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
 * The status of cross region replication.
 * <p>
 * Currently we have starting，doing，closing three status. After
 * PutBucketReplication is sent, OSS start preparing the replication and at this
 * point the status is 'starting'. And when the replication actually happens,
 * the status is "doing". And once DeleteBucketReplication is called, the OSS
 * will do the cleanup work for the replication and the status will be
 * "closing".
 * </p>
 *
 */
public enum ReplicationStatus {

    /**
     * Preparing the replication.
     */
    Starting("starting"),

    /**
     * Doing the replication.
     */
    Doing("doing"),

    /**
     * Cleaning up the replication.
     */
    Closing("closing");

    private String statusString;

    private ReplicationStatus(String replicationStatusString) {
        this.statusString = replicationStatusString;
    }

    @Override
    public String toString() {
        return this.statusString;
    }

    public static ReplicationStatus parse(String replicationStatus) {
        for (ReplicationStatus status : ReplicationStatus.values()) {
            if (status.toString().equals(replicationStatus)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unable to parse the provided replication status " + replicationStatus);
    }
}
