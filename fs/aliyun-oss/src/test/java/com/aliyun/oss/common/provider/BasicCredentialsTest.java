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

import com.aliyun.oss.common.auth.BasicCredentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import junit.framework.Assert;
import org.junit.Test;

public class BasicCredentialsTest extends TestBase {

    @Test
    public void testBasicTest() {
        try {
            BasicCredentials credentials = new BasicCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, SECURITY_TOKEN)
                    .withExpiredFactor(0.8).withExpiredDuration(900);
            Assert.assertEquals(credentials.getAccessKeyId(), ACCESS_KEY_ID);
            Assert.assertEquals(credentials.getSecretAccessKey(), ACCESS_KEY_SECRET);
            Assert.assertEquals(credentials.getSecurityToken(), SECURITY_TOKEN);
            Assert.assertTrue(credentials.useSecurityToken());

            credentials = new BasicCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, null).withExpiredFactor(0.8)
                    .withExpiredDuration(900);
            Assert.assertEquals(credentials.getAccessKeyId(), ACCESS_KEY_ID);
            Assert.assertEquals(credentials.getSecretAccessKey(), ACCESS_KEY_SECRET);
            Assert.assertNull(credentials.getSecurityToken());
            Assert.assertFalse(credentials.useSecurityToken());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testWillSoonExpire() {
        try {
            BasicCredentials credentials = new BasicCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, SECURITY_TOKEN)
                    .withExpiredFactor(1.0).withExpiredDuration(1);
            Thread.sleep(2000);
            Assert.assertTrue(credentials.willSoonExpire());

            credentials = new BasicCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, SECURITY_TOKEN).withExpiredFactor(1.0)
                    .withExpiredDuration(100);
            Thread.sleep(2000);
            Assert.assertFalse(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testExpiredFactor() {
        try {
            BasicCredentials credentials = new BasicCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, SECURITY_TOKEN)
                    .withExpiredFactor(1.0).withExpiredDuration(3);
            Thread.sleep(2000);
            Assert.assertFalse(credentials.willSoonExpire());

            credentials = new BasicCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, SECURITY_TOKEN).withExpiredFactor(0.1)
                    .withExpiredDuration(3);
            Thread.sleep(1000);
            Assert.assertTrue(credentials.willSoonExpire());

            credentials = new BasicCredentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET, SECURITY_TOKEN).withExpiredFactor(1.0)
                    .withExpiredDuration(1);
            Thread.sleep(1500);
            Assert.assertTrue(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDefaultCredentialProvider() {
        DefaultCredentialProvider provider;
        try {
            provider = new DefaultCredentialProvider(null, "");
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            provider = new DefaultCredentialProvider("", "");
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            provider = new DefaultCredentialProvider(ACCESS_KEY_ID, null);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            provider = new DefaultCredentialProvider(ACCESS_KEY_ID, "");
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        try {
            provider = new DefaultCredentialProvider(ACCESS_KEY_ID, ACCESS_KEY_SECRET);
            Assert.assertTrue(true);
            provider.setCredentials(null);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    private static final String ACCESS_KEY_ID = "AccessKeyId";
    private static final String ACCESS_KEY_SECRET = "AccessKeySecret";
    private static final String SECURITY_TOKEN = "SecurityToken";

}
