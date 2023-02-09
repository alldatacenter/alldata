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

public class CreateLiveChannelRequest extends LiveChannelGenericRequest {

    public CreateLiveChannelRequest(String bucketName, String liveChannelName) {
        this(bucketName, liveChannelName, "", LiveChannelStatus.Enabled, new LiveChannelTarget());
    }

    public CreateLiveChannelRequest(String bucketName, String liveChannelName, String liveChannelDescription) {
        this(bucketName, liveChannelName, liveChannelDescription, LiveChannelStatus.Enabled, new LiveChannelTarget());
    }

    public CreateLiveChannelRequest(String bucketName, String liveChannelName, String liveChannelDescription,
            LiveChannelTarget target) {
        this(bucketName, liveChannelName, liveChannelDescription, LiveChannelStatus.Enabled, target);
    }

    public CreateLiveChannelRequest(String bucketName, String liveChannelName, String liveChannelDescription,
            LiveChannelStatus status) {
        this(bucketName, liveChannelName, liveChannelDescription, status, new LiveChannelTarget());
    }

    public CreateLiveChannelRequest(String bucketName, String liveChannelName, String liveChannelDescription,
            LiveChannelStatus status, LiveChannelTarget target) {
        super(bucketName, liveChannelName);
        this.liveChannelDescription = liveChannelDescription;
        this.status = status;
        this.target = target;
    }

    public String getLiveChannelDescription() {
        return liveChannelDescription;
    }

    public void setLiveChannelDescription(String liveChannelDescription) {
        this.liveChannelDescription = liveChannelDescription;
    }

    public LiveChannelStatus getLiveChannelStatus() {
        return status;
    }

    public void setLiveChannelStatus(LiveChannelStatus status) {
        this.status = status;
    }

    public LiveChannelTarget getLiveChannelTarget() {
        return target;
    }

    public void setLiveChannelTarget(LiveChannelTarget target) {
        this.target = target;
    }

    private String liveChannelDescription;
    private LiveChannelStatus status;
    private LiveChannelTarget target;
}
