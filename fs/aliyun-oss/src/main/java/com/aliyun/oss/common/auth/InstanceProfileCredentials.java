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

package com.aliyun.oss.common.auth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.aliyun.oss.common.utils.AuthUtils;

public class InstanceProfileCredentials extends BasicCredentials {

    public InstanceProfileCredentials(String accessKeyId, String accessKeySecret, String sessionToken, String expiration) {
        super(accessKeyId, accessKeySecret, sessionToken, AuthUtils.DEFAULT_ECS_SESSION_TOKEN_DURATION_SECONDS);

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        parser.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            Date date = parser.parse(expiration.replace('T', ' ').replace('Z', ' '));
            this.expirationInMilliseconds = date.getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to get valid expiration time from ECS Metadata service.");
        }
    }
    
    public InstanceProfileCredentials withExpiredFactor(double expiredFactor) {
        this.expiredFactor = expiredFactor;
        return this;
    }

    public InstanceProfileCredentials withExpiredDuration(long expiredDurationSeconds) {
        this.expiredDurationSeconds = expiredDurationSeconds;
        return this;
    }

    @Override
    public boolean willSoonExpire() {        
        long now = System.currentTimeMillis();
        return expiredDurationSeconds * (1.0 - expiredFactor) > (expirationInMilliseconds - now) / 1000.0;
    }

    public boolean isExpired() {
        long now = System.currentTimeMillis();
        return now >= expirationInMilliseconds - refreshIntervalInMillSeconds;
    }

    public boolean shouldRefresh() {
        long now = System.currentTimeMillis();
        if (now - lastFailedRefreshTime > refreshIntervalInMillSeconds) {
            return true;
        } else {
            return false;
        }
    }

    public void setLastFailedRefreshTime() {
        lastFailedRefreshTime = System.currentTimeMillis();
    }

    private final long expirationInMilliseconds;
    private final long refreshIntervalInMillSeconds = 10000;
    private long lastFailedRefreshTime = 0;
}
