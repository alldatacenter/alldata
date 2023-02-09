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

public class GenerateVodPlaylistRequest extends LiveChannelGenericRequest {

    public GenerateVodPlaylistRequest(String bucketName, String liveChannelName, String playlistName) {
        super(bucketName, liveChannelName);
        this.playlistName = playlistName;
    }

    public GenerateVodPlaylistRequest(String bucketName, String liveChannelName, String playlistName, long startTime,
            long endTime) {
        super(bucketName, liveChannelName);
        this.playlistName = playlistName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    private String playlistName;
    private Long startTime;
    private Long endTime;
}
