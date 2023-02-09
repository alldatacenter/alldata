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

public class LiveChannelListing {

    public List<LiveChannel> getLiveChannels() {
        return liveChannels;
    }

    public void addLiveChannel(LiveChannel liveChannel) {
        this.liveChannels.add(liveChannel);
    }

    public void setObjectSummaries(List<LiveChannel> liveChannels) {
        this.liveChannels.clear();
        if (liveChannels != null && !liveChannels.isEmpty()) {
            this.liveChannels.addAll(liveChannels);
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public int getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public void setTruncated(boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    public String getNextMarker() {
        return nextMarker;
    }

    public void setNextMarker(String nextMarker) {
        this.nextMarker = nextMarker;
    }

    // The list of LiveChannel instances
    private List<LiveChannel> liveChannels = new ArrayList<LiveChannel>();
    // The prefix filter.
    private String prefix;
    // The marker filter
    private String marker;
    // The max Live Channel count.
    private int maxKeys;
    // Flag of if there's remaining Live Channels in OSS server side.
    // True: more object to come; False: no more live channels to return.
    private boolean isTruncated;
    // The next marker filter.
    private String nextMarker;

}
