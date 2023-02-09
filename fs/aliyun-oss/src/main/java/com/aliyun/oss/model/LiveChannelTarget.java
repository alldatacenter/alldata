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
 * The persistent storage information for the Live Channel.
 */
public class LiveChannelTarget {
    public static String LIVE_CHANNEL_DEFAULT_TYPE = "HLS";
    public static int LIVE_CHANNEL_DEFAULT_FRAG_DURATION = 5;
    public static int LIVE_CHANNEL_DEFAULT_FRAG_COUNT = 3;
    public static String LIVE_CHANNEL_DEFAULT_PLAY_LIST_NAME = "playlist.m3u8";

    public LiveChannelTarget() {
        this.type = LIVE_CHANNEL_DEFAULT_TYPE;
        this.fragDuration = LIVE_CHANNEL_DEFAULT_FRAG_DURATION;
        this.fragCount = LIVE_CHANNEL_DEFAULT_FRAG_COUNT;
        this.playlistName = LIVE_CHANNEL_DEFAULT_PLAY_LIST_NAME;
    }

    public LiveChannelTarget(String type, String playlistName) {
        this.type = type;
        this.fragDuration = LIVE_CHANNEL_DEFAULT_FRAG_DURATION;
        this.fragCount = LIVE_CHANNEL_DEFAULT_FRAG_COUNT;
        this.playlistName = playlistName;
    }

    public LiveChannelTarget(String type, int fragDuration, int fragCount, String playlistName) {
        this.type = type;
        this.fragDuration = fragDuration;
        this.fragCount = fragCount;
        this.playlistName = playlistName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getFragDuration() {
        return fragDuration;
    }

    public void setFragDuration(int fragDuration) {
        this.fragDuration = fragDuration;
    }

    public int getFragCount() {
        return fragCount;
    }

    public void setFragCount(int fragCount) {
        this.fragCount = fragCount;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    // The video data's format, only HLS is supported now.
    private String type;
    // The fragment duration in seconds for each ts file, when type is HLS.
    private int fragDuration;
    // The ts file count in the m3u8 file, when type is HLS.
    private int fragCount;
    // The m3u8 file's basename, when the type is HLS.
    private String playlistName;
}
