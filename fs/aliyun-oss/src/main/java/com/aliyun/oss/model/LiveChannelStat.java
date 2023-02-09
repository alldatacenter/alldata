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

public class LiveChannelStat extends GenericResult {

    /**
     * The Live Channel's video and audio information
     * 
     */
    public static class VideoStat {

        public VideoStat() {
        }

        public VideoStat(int width, int height, int frameRate, int bandWidth, String codec) {
            this.width = width;
            this.height = height;
            this.frameRate = frameRate;
            this.bandWidth = bandWidth;
            this.codec = codec;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getFrameRate() {
            return frameRate;
        }

        public void setFrameRate(int frameRate) {
            this.frameRate = frameRate;
        }

        public int getBandWidth() {
            return bandWidth;
        }

        public void setBandWidth(int bandWidth) {
            this.bandWidth = bandWidth;
        }

        public String getCodec() {
            return codec;
        }

        public void setCodec(String codec) {
            this.codec = codec;
        }

        // The width of the video.
        private int width;
        // The height of the video.
        private int height;
        // The frame rate of the video.
        private int frameRate;
        // The bandwidth of the video.
        private int bandWidth;
        // The codec of the video.
        private String codec;
    }

    /**
     * The Live Channel's Audio information
     * 
     */
    public static class AudioStat {

        public AudioStat() {
        }

        public AudioStat(int bandWidth, int sampleRate, String codec) {
            this.bandWidth = bandWidth;
            this.sampleRate = sampleRate;
            this.codec = codec;
        }

        public int getBandWidth() {
            return bandWidth;
        }

        public void setBandWidth(int bandWidth) {
            this.bandWidth = bandWidth;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public String getCodec() {
            return codec;
        }

        public void setCodec(String codec) {
            this.codec = codec;
        }

        // The bandwidth of the audio, in bytes/s.
        private int bandWidth;
        // The sample rate of the audio, in HZ.
        private int sampleRate;
        // The codec of the audio.
        private String codec;
    }

    public PushflowStatus getPushflowStatus() {
        return status;
    }

    public void setPushflowStatus(PushflowStatus status) {
        this.status = status;
    }

    public Date getConnectedDate() {
        return connectedDate;
    }

    public void setConnectedDate(Date connectedDate) {
        this.connectedDate = connectedDate;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public VideoStat getVideoStat() {
        return videoStat;
    }

    public void setVideoStat(VideoStat videoStat) {
        this.videoStat = videoStat;
    }

    public AudioStat getAudioStat() {
        return audioStat;
    }

    public void setAudioStat(AudioStat audioStat) {
        this.audioStat = audioStat;
    }

    // The Live Channel's pusing streaming status.
    private PushflowStatus status;
    // The current pushing streaming's start time of client's connection.
    private Date connectedDate;
    // The current pushing streaming's endpoint (including port), when the
    // status is Live.
    private String remoteAddress;
    // The video information.
    private VideoStat videoStat;
    // The audio information.
    private AudioStat audioStat;

}
