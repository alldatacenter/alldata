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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.InvalidCredentialsException;
import com.aliyun.oss.common.auth.ProfileConfigFile;
import com.aliyun.oss.common.auth.ProfileConfigLoader;
import com.aliyun.oss.common.utils.AuthUtils;
import junit.framework.Assert;
import org.junit.Test;

public class ProfilesConfigFileTest extends TestBase {

    @Test
    public void testGetCredentialsWithDefaultParameters() {
        try {
            // STS
            Map<String, String> options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            options.put(AuthUtils.OSS_SESSION_TOKEN, TEST_SECURITY_TOKEN);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            ProfileConfigFile configFile = new ProfileConfigFile(AuthUtils.DEFAULT_PROFILE_PATH);
            Credentials credentials = configFile.getCredentials();

            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());
            Assert.assertEquals(TEST_SECURITY_TOKEN, credentials.getSecurityToken());
            Assert.assertTrue(credentials.useSecurityToken());

            new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();

            // Normal
            options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            configFile = new ProfileConfigFile(AuthUtils.DEFAULT_PROFILE_PATH);
            credentials = configFile.getCredentials();

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
    public void testGetCredentialsWithLoader() {
        try {
            // STS
            Map<String, String> options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            options.put(AuthUtils.OSS_SESSION_TOKEN, TEST_SECURITY_TOKEN);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            ProfileConfigLoader profileLoader = new ProfileConfigLoader();
            ProfileConfigFile configFile = new ProfileConfigFile(
                    new File(AuthUtils.DEFAULT_PROFILE_PATH), profileLoader);
            Credentials credentials = configFile.getCredentials();

            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());
            Assert.assertEquals(TEST_SECURITY_TOKEN, credentials.getSecurityToken());
            Assert.assertTrue(credentials.useSecurityToken());

            new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();

            // Normal
            options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            configFile = new ProfileConfigFile(new File(AuthUtils.DEFAULT_PROFILE_PATH), profileLoader);
            credentials = configFile.getCredentials();

            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());
            Assert.assertNull(credentials.getSecurityToken());
            Assert.assertFalse(credentials.useSecurityToken());

            new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();

            // filepath
            options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            configFile = new ProfileConfigFile(AuthUtils.DEFAULT_PROFILE_PATH, profileLoader);
            credentials = configFile.getCredentials();

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
    public void testGetFreshCredentials() {
        try {
        	new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();
        	Thread.sleep(1000);
        	
            Map<String, String> options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            ProfileConfigFile configFile = new ProfileConfigFile(AuthUtils.DEFAULT_PROFILE_PATH);
            Credentials credentials = configFile.getCredentials();

            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());
            Assert.assertNull(credentials.getSecurityToken());
            Assert.assertFalse(credentials.useSecurityToken());
            
            new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();
            Thread.sleep(1000);

            // Fresh
            options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            options.put(AuthUtils.OSS_SESSION_TOKEN, TEST_SECURITY_TOKEN);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            credentials = configFile.getCredentials();

            Assert.assertEquals(TEST_ACCESS_KEY_ID, credentials.getAccessKeyId());
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, credentials.getSecretAccessKey());
            Assert.assertEquals(TEST_SECURITY_TOKEN, credentials.getSecurityToken());
            Assert.assertTrue(credentials.useSecurityToken());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGetCredentialsNegative() {
        try {
            Map<String, String> options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, "");
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            ProfileConfigFile configFile = new ProfileConfigFile(AuthUtils.DEFAULT_PROFILE_PATH);

            try {
                configFile.getCredentials();
                Assert.fail("ProfileConfigFile.getCredentials should not be successful");
            } catch (InvalidCredentialsException e) {

            }

            options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, "");
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);
            try {
                configFile.getCredentials();
                Assert.fail("ProfileConfigFile.getCredentials should not be successful");
            } catch (InvalidCredentialsException e) {

            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            new File(AuthUtils.DEFAULT_PROFILE_PATH).delete();
        }
    }

    @Test
    public void testConfigFileNull() {
        try {
            String filepath = null;
            ProfileConfigFile profileConfigFile = new ProfileConfigFile(filepath);
            Assert.fail("File path error, should be failed here.");
        } catch (IllegalArgumentException e) {
            // expected.
        }
    }

    private static final String TEST_ACCESS_KEY_ID = "AccessKeyId";
    private static final String TEST_ACCESS_KEY_SECRET = "AccessKeySecret";
    private static final String TEST_SECURITY_TOKEN = "SecurityToken";

}
