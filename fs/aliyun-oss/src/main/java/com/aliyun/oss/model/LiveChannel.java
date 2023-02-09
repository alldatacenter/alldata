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
import java.util.List;

public class LiveChannel {

    public LiveChannel() {
    }

    public LiveChannel(String name, String description, LiveChannelStatus status, Date lastModified,
            List<String> publishUrls, List<String> playUrls) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.lastModified = lastModified;
        this.publishUrls = publishUrls;
        this.playUrls = playUrls;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LiveChannelStatus getStatus() {
        return status;
    }

    public void setStatus(LiveChannelStatus status) {
        this.status = status;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public List<String> getPublishUrls() {
        return publishUrls;
    }

    public void setPublishUrls(List<String> publishUrls) {
        this.publishUrls = publishUrls;
    }

    public List<String> getPlayUrls() {
        return playUrls;
    }

    public void setPlayUrls(List<String> playUrls) {
        this.playUrls = playUrls;
    }

    @Override
    public String toString() {
        return "LiveChannel [name=" + getName() + ",description=" + getDescription() + ",status=" + getStatus()
                + ",lastModified=" + getLastModified() + ",publishUrls=" + publishUrls.get(0) + ",playUrls="
                + playUrls.get(0) + "]";
    }

    private String name;
    private String description;
    private LiveChannelStatus status;
    private Date lastModified;
    private List<String> publishUrls;
    private List<String> playUrls;

}
