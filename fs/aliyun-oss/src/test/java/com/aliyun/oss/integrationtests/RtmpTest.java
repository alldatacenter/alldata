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

package com.aliyun.oss.integrationtests;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.SetLiveChannelRequest;
import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CreateLiveChannelRequest;
import com.aliyun.oss.model.CreateLiveChannelResult;
import com.aliyun.oss.model.ListLiveChannelsRequest;
import com.aliyun.oss.model.LiveChannel;
import com.aliyun.oss.model.LiveChannelInfo;
import com.aliyun.oss.model.LiveChannelListing;
import com.aliyun.oss.model.LiveChannelStat;
import com.aliyun.oss.model.LiveChannelStatus;
import com.aliyun.oss.model.LiveChannelTarget;
import com.aliyun.oss.model.LiveRecord;
import com.aliyun.oss.model.PushflowStatus;
import com.aliyun.oss.model.OSSObject;

/**
 * Test rtmp
 */
public class RtmpTest extends TestBase {
        
    @Test
    public void testCreateLiveChannelDefault() {
        final String liveChannel = "normal-create-live-channel-default";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            CreateLiveChannelResult createLiveChannelResult = ossClient.createLiveChannel(createLiveChannelRequest);
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicReadWrite);
            Assert.assertEquals(createLiveChannelResult.getPublishUrls().size(), 1);
            Assert.assertTrue(createLiveChannelResult.getPublishUrls().get(0).startsWith("rtmp://"));
            Assert.assertTrue(createLiveChannelResult.getPublishUrls().get(0).endsWith("live/" + liveChannel));
            Assert.assertEquals(createLiveChannelResult.getPlayUrls().size(), 1);
            Assert.assertTrue(createLiveChannelResult.getPlayUrls().get(0).startsWith("http://"));
            Assert.assertTrue(createLiveChannelResult.getPlayUrls().get(0).endsWith(liveChannel + "/playlist.m3u8"));
            Assert.assertEquals(createLiveChannelResult.getRequestId().length(), REQUEST_ID_LEN);
            
            LiveChannelInfo liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getDescription(), "");
            Assert.assertEquals(liveChannelInfo.getStatus(), LiveChannelStatus.Enabled);
            Assert.assertEquals(liveChannelInfo.getTarget().getType(), "HLS");
            Assert.assertEquals(liveChannelInfo.getTarget().getFragDuration(), 5);
            Assert.assertEquals(liveChannelInfo.getTarget().getFragCount(), 3);
            Assert.assertEquals(liveChannelInfo.getTarget().getPlaylistName(), "playlist.m3u8");
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testCreateLiveChannel() {
        final String liveChannel = "normal-create-live-channel";
        final String liveChannelDesc = "my test live channel";

        try {
            LiveChannelTarget target = new LiveChannelTarget("HLS", 100, 99, "myplaylist.m3u8");
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel, liveChannelDesc, LiveChannelStatus.Disabled, target);
            
            CreateLiveChannelResult createLiveChannelResult = ossClient.createLiveChannel(createLiveChannelRequest);
            Assert.assertEquals(createLiveChannelResult.getPublishUrls().size(), 1);
            Assert.assertTrue(createLiveChannelResult.getPublishUrls().get(0).startsWith("rtmp://"));
            Assert.assertTrue(createLiveChannelResult.getPublishUrls().get(0).endsWith("live/" + liveChannel));
            Assert.assertEquals(createLiveChannelResult.getPlayUrls().size(), 1);
            Assert.assertTrue(createLiveChannelResult.getPlayUrls().get(0).startsWith("http://"));
            Assert.assertTrue(createLiveChannelResult.getPlayUrls().get(0).endsWith(liveChannel + "/myplaylist.m3u8"));
            Assert.assertEquals(createLiveChannelResult.getRequestId().length(), REQUEST_ID_LEN);
            
            LiveChannelInfo liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getDescription(), liveChannelDesc);
            Assert.assertEquals(liveChannelInfo.getStatus(), LiveChannelStatus.Disabled);
            Assert.assertEquals(liveChannelInfo.getTarget().getType(), "HLS");
            Assert.assertEquals(liveChannelInfo.getTarget().getFragDuration(), 100);
            Assert.assertEquals(liveChannelInfo.getTarget().getFragCount(), 99);
            Assert.assertEquals(liveChannelInfo.getTarget().getPlaylistName(), "myplaylist.m3u8");
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
	    
    @Test
    public void testUnormalCreateLiveChannel() {
        final String liveChannel = "unnormal-create-live-channel";

        try {
            LiveChannelTarget target = new LiveChannelTarget("RTMP", "myplaylist.m3u8");
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel, "", LiveChannelStatus.Enabled, target);
            ossClient.createLiveChannel(createLiveChannelRequest);
            Assert.fail("Get live channel should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(e.getErrorCode(), OSSErrorCode.INVALID_ARGUMENT);
        }
        
        try {
            LiveChannelTarget target = new LiveChannelTarget("HLS", 200, 99, "myplaylist.m3u8");
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel, "", LiveChannelStatus.Enabled, target);
            ossClient.createLiveChannel(createLiveChannelRequest);
            Assert.fail("Get live channel should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(e.getErrorCode(), OSSErrorCode.INVALID_ARGUMENT);
        }
        
        try {
            LiveChannelTarget target = new LiveChannelTarget("HLS", 100, 0, "myplaylist.m3u8");
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel, "", LiveChannelStatus.Enabled, target);
            ossClient.createLiveChannel(createLiveChannelRequest);
            Assert.fail("Get live channel should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(e.getErrorCode(), OSSErrorCode.INVALID_ARGUMENT);
        }
        
        try {
            LiveChannelTarget target = new LiveChannelTarget("HLS", 100, 199, "myplaylist.m3u8");
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel, "", LiveChannelStatus.Enabled, target);
            ossClient.createLiveChannel(createLiveChannelRequest);
            Assert.fail("Get live channel should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(e.getErrorCode(), OSSErrorCode.INVALID_ARGUMENT);
        }
    }
    
    @Test
    public void testSetLiveChannelStatus() {
        final String liveChannel = "normal-set-live-channel-status";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);

            // set disable
            SetLiveChannelRequest setLiveChannelRequest = new SetLiveChannelRequest(bucketName, liveChannel,  LiveChannelStatus.Enabled);
            setLiveChannelRequest.setLiveChannelStatus(LiveChannelStatus.Disabled);
            ossClient.setLiveChannelStatus(setLiveChannelRequest);
            
            LiveChannelInfo liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getStatus(), LiveChannelStatus.Disabled);
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);
            
            // set enable
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Enabled);
            
            liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getStatus(), LiveChannelStatus.Enabled);
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testSetLiveChannelStatusRepeated() {
        final String liveChannel = "normal-set-live-channel-status-repeated";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);

            // set disabled
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Disabled);
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Disabled);
            
            LiveChannelInfo liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getStatus(), LiveChannelStatus.Disabled);
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);
            
            // set enabled
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Enabled);
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Enabled);
            
            liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getStatus(), LiveChannelStatus.Enabled);
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);
            
            // set disabled
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Disabled);
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Disabled);
            
            liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getStatus(), LiveChannelStatus.Disabled);
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);
            
            // set enabled
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Enabled);
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Enabled);
            
            liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getStatus(), LiveChannelStatus.Enabled);
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGetLiveChannelInfo() {
        final String liveChannel = "normal-get-live-channel-info";
        final String liveChannelDesc = "my test live channel";

        try {
            LiveChannelTarget target = new LiveChannelTarget("HLS", 100, 99, "myplaylist.m3u8");
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel, liveChannelDesc, LiveChannelStatus.Enabled, target);
            
            ossClient.createLiveChannel(createLiveChannelRequest);

            LiveChannelInfo liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getDescription(), liveChannelDesc);
            Assert.assertEquals(liveChannelInfo.getStatus(), LiveChannelStatus.Enabled);
            Assert.assertEquals(liveChannelInfo.getTarget().getType(), "HLS");
            Assert.assertEquals(liveChannelInfo.getTarget().getFragDuration(), 100);
            Assert.assertEquals(liveChannelInfo.getTarget().getFragCount(), 99);
            Assert.assertEquals(liveChannelInfo.getTarget().getPlaylistName(), "myplaylist.m3u8");
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGetLiveChannelStatWithoutPushflow() {
        final String liveChannel = "normal-get-live-channel-stat-without-pushflow";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);
            
            LiveChannelStat liveChannelStat = ossClient.getLiveChannelStat(bucketName, liveChannel);
            Assert.assertEquals(liveChannelStat.getPushflowStatus(), PushflowStatus.Idle);
            Assert.assertNull(liveChannelStat.getConnectedDate());
            Assert.assertNull(liveChannelStat.getRemoteAddress());
            Assert.assertNull(liveChannelStat.getVideoStat());
            Assert.assertNull(liveChannelStat.getAudioStat());
            Assert.assertEquals(liveChannelStat.getRequestId().length(), REQUEST_ID_LEN);
            
            ossClient.setLiveChannelStatus(bucketName, liveChannel, LiveChannelStatus.Disabled);
            liveChannelStat = ossClient.getLiveChannelStat(bucketName, liveChannel);
            Assert.assertEquals(liveChannelStat.getPushflowStatus(), PushflowStatus.Disabled);
            Assert.assertNull(liveChannelStat.getConnectedDate());
            Assert.assertNull(liveChannelStat.getRemoteAddress());
            Assert.assertNull(liveChannelStat.getVideoStat());
            Assert.assertNull(liveChannelStat.getAudioStat());
            Assert.assertEquals(liveChannelStat.getRequestId().length(), REQUEST_ID_LEN);
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    @Ignore
    public void testGetLiveChannelStat() {
        final String liveChannel = "normal-get-live-channel-stat";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicReadWrite);

            // Manually enable pusing streaming with following cmd.
            // ./ffmpeg \-re \-i allstar.flv \-c copy \-f flv "rtmp://oss-live-channel-2.demo-oss-cn-shenzhen.aliyuncs.com/live/normal-get-live-channel-stat?playlistName=playlist.m3u8"
            Thread.sleep(5 * 1000);
            
            LiveChannelStat liveChannelStat = ossClient.getLiveChannelStat(bucketName, liveChannel);
            Assert.assertEquals(liveChannelStat.getPushflowStatus(), PushflowStatus.Live);
            Assert.assertNotNull(liveChannelStat.getConnectedDate());
            Assert.assertTrue(liveChannelStat.getRemoteAddress().length() >= new String("0.0.0.0:0").length());
            Assert.assertEquals(liveChannelStat.getVideoStat().getWidth(), 672);
            Assert.assertEquals(liveChannelStat.getVideoStat().getHeight(), 378);
            Assert.assertEquals(liveChannelStat.getVideoStat().getFrameRate(), 29);
            Assert.assertTrue(liveChannelStat.getVideoStat().getBandWidth() > 50000);
            Assert.assertEquals(liveChannelStat.getVideoStat().getCodec(), "H264");
            Assert.assertTrue(liveChannelStat.getAudioStat().getBandWidth() > 4000);
            Assert.assertEquals(liveChannelStat.getAudioStat().getSampleRate(), 22050);
            Assert.assertEquals(liveChannelStat.getAudioStat().getCodec(), "AAC");
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testDeleteLiveChannel() {
        final String liveChannel = "normal-delete-live-channel";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);
            
            ossClient.getLiveChannelInfo(bucketName, liveChannel);
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
            
            try {
                ossClient.getLiveChannelInfo(bucketName, liveChannel);
            } catch (OSSException e) {
                Assert.assertEquals(e.getErrorCode(), OSSErrorCode.NO_SUCH_LIVE_CHANNEL);
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testListLiveChannel() {
        final String liveChannelPrefix = "normal-list-live-channel";

        try {
            // create live channels
            for (int i = 0; i < 10; i++) {
                CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                        bucketName, liveChannelPrefix + i);
                ossClient.createLiveChannel(createLiveChannelRequest);
            }
            
            // default
            ListLiveChannelsRequest listLiveChannelsRequest = new ListLiveChannelsRequest(bucketName);
            LiveChannelListing liveChannelListing = ossClient.listLiveChannels(listLiveChannelsRequest);
            
            Assert.assertTrue(liveChannelListing.getLiveChannels().size() >= 10);
            Assert.assertNull(liveChannelListing.getPrefix());
            Assert.assertNull(liveChannelListing.getMarker());
            Assert.assertNull(liveChannelListing.getNextMarker());
            Assert.assertEquals(liveChannelListing.getMaxKeys(), 100);
            Assert.assertFalse(liveChannelListing.isTruncated());
            
            // prefix
            listLiveChannelsRequest = new ListLiveChannelsRequest(bucketName);
            listLiveChannelsRequest.setPrefix(liveChannelPrefix);
            
            liveChannelListing = ossClient.listLiveChannels(listLiveChannelsRequest);
            Assert.assertTrue(liveChannelListing.getLiveChannels().size() == 10);
            Assert.assertEquals(liveChannelListing.getPrefix(), liveChannelPrefix);
            Assert.assertNull(liveChannelListing.getMarker());
            Assert.assertNull(liveChannelListing.getNextMarker());
            Assert.assertEquals(liveChannelListing.getMaxKeys(), 100);
            Assert.assertFalse(liveChannelListing.isTruncated());
            
            for (LiveChannel liveChannel : liveChannelListing.getLiveChannels()) {
                Assert.assertTrue(liveChannel.getName().startsWith(liveChannelPrefix));
                Assert.assertEquals(liveChannel.getDescription(), "");
                Assert.assertEquals(liveChannel.getStatus(), LiveChannelStatus.Enabled);
                Assert.assertTrue(dateAfterValidator(liveChannel.getLastModified()));
  
                Assert.assertEquals(liveChannel.getPublishUrls().size(), 1);
                Assert.assertTrue(liveChannel.getPublishUrls().get(0).startsWith("rtmp://"));
                Assert.assertTrue(liveChannel.getPublishUrls().get(0).indexOf("live/" + liveChannelPrefix) > -1);
                Assert.assertEquals(liveChannel.getPlayUrls().size(), 1);
                Assert.assertTrue(liveChannel.getPlayUrls().get(0).startsWith("http://"));
                Assert.assertTrue(liveChannel.getPlayUrls().get(0).endsWith("/playlist.m3u8"));
            }
            
            // marker
            listLiveChannelsRequest = new ListLiveChannelsRequest(bucketName);
            listLiveChannelsRequest.setPrefix(liveChannelPrefix);
            listLiveChannelsRequest.setMarker(liveChannelPrefix + 5);
            
            liveChannelListing = ossClient.listLiveChannels(listLiveChannelsRequest);
            Assert.assertTrue(liveChannelListing.getLiveChannels().size() == 4);
            Assert.assertEquals(liveChannelListing.getPrefix(), liveChannelPrefix);
            Assert.assertEquals(liveChannelListing.getMarker(), liveChannelPrefix + 5);
            Assert.assertNull(liveChannelListing.getNextMarker());
            Assert.assertEquals(liveChannelListing.getMaxKeys(), 100);
            Assert.assertFalse(liveChannelListing.isTruncated());
            
            for (LiveChannel liveChannel : liveChannelListing.getLiveChannels()) {
                Assert.assertTrue(liveChannel.getName().startsWith(liveChannelPrefix));
                Assert.assertEquals(liveChannel.getDescription(), "");
                Assert.assertEquals(liveChannel.getStatus(), LiveChannelStatus.Enabled);
                Assert.assertTrue(dateAfterValidator(liveChannel.getLastModified()));
  
                Assert.assertEquals(liveChannel.getPublishUrls().size(), 1);
                Assert.assertTrue(liveChannel.getPublishUrls().get(0).startsWith("rtmp://"));
                Assert.assertTrue(liveChannel.getPublishUrls().get(0).indexOf("live/" + liveChannelPrefix) > -1);
                Assert.assertEquals(liveChannel.getPlayUrls().size(), 1);
                Assert.assertTrue(liveChannel.getPlayUrls().get(0).startsWith("http://"));
                Assert.assertTrue(liveChannel.getPlayUrls().get(0).endsWith("/playlist.m3u8"));
            }
            
            // marker
            listLiveChannelsRequest = new ListLiveChannelsRequest(bucketName);
            listLiveChannelsRequest.setPrefix(liveChannelPrefix);
            listLiveChannelsRequest.setMaxKeys(5);
            
            liveChannelListing = ossClient.listLiveChannels(listLiveChannelsRequest);
            Assert.assertTrue(liveChannelListing.getLiveChannels().size() == 5);
            Assert.assertEquals(liveChannelListing.getPrefix(), liveChannelPrefix);
            Assert.assertNull(liveChannelListing.getMarker());
            Assert.assertNotNull(liveChannelListing.getNextMarker());
            Assert.assertEquals(liveChannelListing.getMaxKeys(), 5);
            Assert.assertTrue(liveChannelListing.isTruncated());
            
            for (LiveChannel liveChannel : liveChannelListing.getLiveChannels()) {
                Assert.assertTrue(liveChannel.getName().startsWith(liveChannelPrefix));
                Assert.assertEquals(liveChannel.getDescription(), "");
                Assert.assertEquals(liveChannel.getStatus(), LiveChannelStatus.Enabled);
                Assert.assertTrue(dateAfterValidator(liveChannel.getLastModified()));
  
                Assert.assertEquals(liveChannel.getPublishUrls().size(), 1);
                Assert.assertTrue(liveChannel.getPublishUrls().get(0).startsWith("rtmp://"));
                Assert.assertTrue(liveChannel.getPublishUrls().get(0).indexOf("live/" + liveChannelPrefix) > -1);
                Assert.assertEquals(liveChannel.getPlayUrls().size(), 1);
                Assert.assertTrue(liveChannel.getPlayUrls().get(0).startsWith("http://"));
                Assert.assertTrue(liveChannel.getPlayUrls().get(0).endsWith("/playlist.m3u8"));
            }
            
            // page
            listLiveChannelsRequest = new ListLiveChannelsRequest(bucketName);
            listLiveChannelsRequest.setPrefix(liveChannelPrefix);
            listLiveChannelsRequest.setMaxKeys(5);
            
            do {
                liveChannelListing = ossClient.listLiveChannels(listLiveChannelsRequest);
                Assert.assertTrue(liveChannelListing.getLiveChannels().size() == 5);
                
                for (LiveChannel liveChannel : liveChannelListing.getLiveChannels()) {
                    Assert.assertTrue(liveChannel.getName().startsWith(liveChannelPrefix));
                }
                
                listLiveChannelsRequest.setMarker(liveChannelListing.getNextMarker());
                
            } while (liveChannelListing.isTruncated());
            
            // list all
            List<LiveChannel> liveChannels = ossClient.listLiveChannels(bucketName);
            Assert.assertTrue(liveChannels.size() >= 10);
                                    
            // delete live channels
            for (int i = 0; i < 10; i++) {
                ossClient.deleteLiveChannel(bucketName, liveChannelPrefix + i);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGetLiveChannelHistoryWithoutPushflow() {
        final String liveChannel = "normal-get-live-channel-history-without-pushflow";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);
            
            List<LiveRecord> liveRecords = ossClient.getLiveChannelHistory(bucketName, liveChannel);
            Assert.assertEquals(liveRecords.size(), 0);
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Ignore
    public void testGetLiveChannelHistory() {
        final String liveChannel = "normal-get-live-channel-history";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicReadWrite);

            // Manually enable pusing streaming with following cmd:
            // ./ffmpeg \-re \-i allstar.flv \-c copy \-f flv "rtmp://oss-live-channel-2.demo-oss-cn-shenzhen.aliyuncs.com/live/normal-get-live-channel-history?playlistName=playlist.m3u8"
            Thread.sleep(5 * 1000);
            
            List<LiveRecord> liveRecords = ossClient.getLiveChannelHistory(bucketName, liveChannel);
            Assert.assertTrue(liveRecords.size() >= 1);
            for (LiveRecord liveRecord : liveRecords) {
                Assert.assertTrue(dateAfterValidator(liveRecord.getStartDate()));
                Assert.assertTrue(dateAfterValidator(liveRecord.getEndDate()));
                Assert.assertTrue(liveRecord.getRemoteAddress().length() >= new String("0.0.0.0:0").length());
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGenerateVodPlaylist() {
        final String liveChannel = "normal-generate-vod-playlist";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicReadWrite);
            
            long startTime = System.currentTimeMillis() / 1000 - 3600;
            long endTime = System.currentTimeMillis() / 1000 + 3600;
            try {
                ossClient.generateVodPlaylist(bucketName, liveChannel, "playlist.m3u8", startTime, endTime);
            } catch (OSSException e) {
                Assert.assertEquals(e.getErrorCode(), OSSErrorCode.INVALID_ARGUMENT);
                Assert.assertTrue(e.getMessage().indexOf("No ts file found in specified time span.") > -1);
            }
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetVodPlaylist() {
        final String liveChannel = "normal-get-vod-playlist";

        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicReadWrite);

            long startTime = System.currentTimeMillis() / 1000 - 3600;
            long endTime = System.currentTimeMillis() / 1000 + 3600;
            try {
                OSSObject o = ossClient.getVodPlaylist(bucketName, liveChannel, startTime, endTime);
                Assert.assertEquals(bucketName, o.getBucketName());
                Assert.assertEquals(liveChannel, o.getKey());
            } catch (OSSException e) {
                Assert.assertEquals(e.getErrorCode(), OSSErrorCode.INVALID_ARGUMENT);
                Assert.assertTrue(e.getMessage().indexOf("No ts file found in specified time span.") > -1);
            }

            ossClient.deleteLiveChannel(bucketName, liveChannel);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGeneratePushflowUri() {
        final String liveChannel = "normal-generate-pushflow-uri";
        
        try {
            CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                    bucketName, liveChannel);
            ossClient.createLiveChannel(createLiveChannelRequest);
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.Private);
            
            LiveChannelInfo liveChannelInfo = ossClient.getLiveChannelInfo(bucketName, liveChannel);
            Assert.assertEquals(liveChannelInfo.getRequestId().length(), REQUEST_ID_LEN);

            // generate rtmp url
            long expires = System.currentTimeMillis() / 1000 + 3600;
            String uri = ossClient.generateRtmpUri(bucketName, liveChannel, 
                    liveChannelInfo.getTarget().getPlaylistName(), expires);
            
            //System.out.println("uri:" + uri);
            
            Assert.assertTrue(uri.startsWith("rtmp://" + bucketName));
            Assert.assertTrue(uri.endsWith("playlistName=" + liveChannelInfo.getTarget().getPlaylistName()));

            // Manually verify by url, with following cmd.
            // ./ffmpeg \-re \-i allstar.flv \-c copy \-f flv "<RTMP_URI>"
            
            // generate without parameters
            String uri2 = ossClient.generateRtmpUri(bucketName, liveChannel, 
                    liveChannelInfo.getTarget().getPlaylistName(), expires);
            
            Assert.assertEquals(uri, uri2);
            
            ossClient.deleteLiveChannel(bucketName, liveChannel);

            OSS client = new OSSClientBuilder().build("https://endpoint/", "ak", "sk", "sts");
            uri = client.generateRtmpUri("bucket", "live", "play.m3u8", 1000 );
            Assert.assertTrue(uri.startsWith("rtmp://bucket"));

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testListAllLiveChannel() {
        final String liveChannelPrefix = "normal-list-all-live-channel";
        final int testCnt = 102;

        try {
            // create live channels
            for (int i = 0; i < testCnt; i++) {
                CreateLiveChannelRequest createLiveChannelRequest = new CreateLiveChannelRequest(
                        bucketName, liveChannelPrefix + i);
                ossClient.createLiveChannel(createLiveChannelRequest);
            }

            // list all
            List<LiveChannel> liveChannels = ossClient.listLiveChannels(bucketName);
            Assert.assertTrue(liveChannels.size() >= testCnt);

            // delete live channels
            for (int i = 0; i < testCnt; i++) {
                ossClient.deleteLiveChannel(bucketName, liveChannelPrefix + i);
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testParseLiveStatusWrong() {
        try {
            LiveChannelStatus.parse("wrong-status");
            Assert.fail("should be failed here.");
        } catch (IllegalArgumentException e) {
            // expected exception.
        }
    }

    private static boolean dateAfterValidator(Date date) throws ParseException {
        if (date == null) {
            return false;
        }
        
        Date pastEra = DateUtil.parseIso8601Date("2015-05-20T09:33:28.000Z");
        return date.after(pastEra);
    }
    
}
