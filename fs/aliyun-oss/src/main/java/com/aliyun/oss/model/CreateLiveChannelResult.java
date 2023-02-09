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

import java.util.List;

public class CreateLiveChannelResult extends GenericResult {

    public List<String> getPublishUrls() {
        return publishUrls;
    }

    public void setPublishUrls(List<String> publishUrls) {
        this.publishUrls = publishUrls;
    }

    /**
     * Gets the pushing streaming Urls.
     * 
     * @return The list of pushing streaming urls.
     */
    public List<String> getPlayUrls() {
        return playUrls;
    }

    /**
     * Gets the playback urls.
     *
     * @param playUrls
     *            playback urls.
     */
    public void setPlayUrls(List<String> playUrls) {
        this.playUrls = playUrls;
    }

    // The pushing streaming urls.
    private List<String> publishUrls;
    // The playback urls.
    private List<String> playUrls;

}
