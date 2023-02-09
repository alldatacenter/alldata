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
 * This class wraps all the information needed to generate the signed pushing
 * streaming url.
 */
public class GenerateRtmpUriRequest {

    public GenerateRtmpUriRequest(String bucketName, String liveChannelName, String playlistName, long expires) {
        this.bucketName = bucketName;
        this.liveChannelName = liveChannelName;
        this.playlistName = playlistName;
        this.expires = expires;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getLiveChannelName() {
        return liveChannelName;
    }

    public void setLiveChannelName(String liveChannelName) {
        this.liveChannelName = liveChannelName;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public Long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    private String bucketName;
    private String liveChannelName;
    private String playlistName;
    private Long expires;

}
