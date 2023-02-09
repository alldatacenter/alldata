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

package com.aliyun.oss.common.provider;

import com.aliyun.oss.common.auth.InstanceProfileCredentials;
import com.aliyun.oss.common.utils.DateUtil;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Date;

public class InstanceProfileCredentialsTest extends TestBase {

    @Test
    public void testBasicTest() {
        try {
            InstanceProfileCredentials credentials = new InstanceProfileCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, null,
                    "2017-11-03T05:10:02Z");
            Assert.assertEquals(credentials.getAccessKeyId(), ACCESS_KEY_ID);
            Assert.assertEquals(credentials.getSecretAccessKey(), ACCESS_KEY_SECRET);
            Assert.assertEquals(credentials.getSecurityToken(), null);
            Assert.assertFalse(credentials.useSecurityToken());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testWillSoonExpire() {
        try {
            InstanceProfileCredentials credentials = new InstanceProfileCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, null,
                    "2016-11-11T11:11:11Z");
            Assert.assertTrue(credentials.willSoonExpire());
            Assert.assertTrue(credentials.isExpired());
            Assert.assertTrue(credentials.shouldRefresh());

            long currTime = new Date().getTime() + 100 * 1000;
            credentials = new InstanceProfileCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, null, DateUtil.formatAlternativeIso8601Date(new Date(currTime)));
            Assert.assertTrue(credentials.willSoonExpire());
            Assert.assertFalse(credentials.isExpired());
            Assert.assertTrue(credentials.shouldRefresh());

            credentials.setLastFailedRefreshTime();
            Assert.assertFalse(credentials.shouldRefresh());
            Thread.sleep(11000);
            Assert.assertTrue(credentials.shouldRefresh());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testExpiredFactor() {
        try {
            InstanceProfileCredentials credentials = new InstanceProfileCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, null,
                    "2010-11-11T11:11:11Z").withExpiredFactor(10.0);
            Thread.sleep(1000);
            Assert.assertTrue(credentials.willSoonExpire());

            long currTime = new Date().getTime() + (3600*6*8/10 + 100)*1000;
            credentials = new InstanceProfileCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, null, DateUtil.formatAlternativeIso8601Date(new Date(currTime)))
                    .withExpiredFactor(0.8);
            Thread.sleep(1000);
            Assert.assertFalse(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private static final String ACCESS_KEY_ID = "AccessKeyId";
    private static final String ACCESS_KEY_SECRET = "AccessKeySecret";

}
