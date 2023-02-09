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

import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.ProfileConfigFile;
import com.aliyun.oss.common.auth.ProfileConfigLoader;
import com.aliyun.oss.common.auth.ProfileCredentialsProvider;
import com.aliyun.oss.common.utils.AuthUtils;
import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ProfileCredentialsProviderTest extends TestBase {

    @Test
    public void testConstructProvider() {
        // construct provider without ProfileConfigFile object.
        try {
            Map<String, String> options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            ProfileCredentialsProvider provider = new ProfileCredentialsProvider();

            Credentials credentials = provider.getCredentials();

            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());
            Assert.assertNull(credentials.getSecurityToken());
            Assert.assertFalse(credentials.useSecurityToken());

            new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // construct provider with ProfileConfigFile object.
        try {
            Map<String, String> options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            ProfileConfigLoader profileLoader = new ProfileConfigLoader();
            ProfileConfigFile configFile = new ProfileConfigFile(
                    new File(AuthUtils.DEFAULT_PROFILE_PATH), profileLoader);

            ProfileCredentialsProvider provider = new ProfileCredentialsProvider(configFile, null);

            Credentials credentials = provider.getCredentials();
            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());
            Assert.assertNull(credentials.getSecurityToken());
            Assert.assertFalse(credentials.useSecurityToken());

            new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // construct provider with ProfileConfigFile and profile name.
        try {
            Map<String, String> options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            ProfileConfigLoader profileLoader = new ProfileConfigLoader();
            ProfileConfigFile configFile = new ProfileConfigFile(
                    new File(AuthUtils.DEFAULT_PROFILE_PATH), profileLoader);

            ProfileCredentialsProvider provider = new ProfileCredentialsProvider(configFile, "test-name");

            Credentials credentials = provider.getCredentials();
            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());
            Assert.assertNull(credentials.getSecurityToken());
            Assert.assertFalse(credentials.useSecurityToken());

            new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void testRefresh() {
        try {
            Map<String, String> options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            ProfileConfigLoader profileLoader = new ProfileConfigLoader();
            ProfileConfigFile configFile = new ProfileConfigFile(
                    new File(AuthUtils.DEFAULT_PROFILE_PATH), profileLoader);

            ProfileCredentialsProvider provider = new ProfileCredentialsProvider(configFile, "test-name");

            Credentials credentials = provider.getCredentials();
            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());
            Assert.assertNull(credentials.getSecurityToken());
            Assert.assertFalse(credentials.useSecurityToken());


            provider.refresh();
            credentials = provider.getCredentials();
            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());

            // age > force-interval
            Thread.sleep(1200);
            try {
                options = new HashMap<String, String>();
                options.put(AuthUtils.OSS_ACCESS_KEY_ID, "new_key_id_1");
                options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, "new_secret_key_1");
                generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                        AuthUtils.DEFAULT_SECTION_NAME, options);

                provider.setRefreshForceIntervalMillis(1000);

                credentials = provider.getCredentials();
                Assert.assertEquals("new_key_id_1", credentials.getAccessKeyId());
                Assert.assertEquals("new_secret_key_1", credentials.getSecretAccessKey());

            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }

            // age > interval && age < force-interval
            Thread.sleep(1200);
            try {
                options = new HashMap<String, String>();
                options.put(AuthUtils.OSS_ACCESS_KEY_ID, "new_key_id_2");
                options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, "new_secret_key_2");
                generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                        AuthUtils.DEFAULT_SECTION_NAME, options);

                provider.setRefreshForceIntervalMillis(10 * 60 * 1000);
                provider.setRefreshIntervalNanos(1000);

                Thread.sleep(2200);

                credentials = provider.getCredentials();
                Assert.assertEquals("new_key_id_2", credentials.getAccessKeyId());
                Assert.assertEquals("new_secret_key_2", credentials.getSecretAccessKey());
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }

            new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }

    private static final String TEST_ACCESS_KEY_ID = "AccessKeyId";
    private static final String TEST_ACCESS_KEY_SECRET = "AccessKeySecret";
}
