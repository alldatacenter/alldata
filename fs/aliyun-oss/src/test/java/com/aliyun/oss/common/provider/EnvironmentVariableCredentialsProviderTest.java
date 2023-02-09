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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.utils.AuthUtils;
import junit.framework.Assert;
import org.junit.Test;

public class EnvironmentVariableCredentialsProviderTest extends TestBase {

    @Test
    public void testGetEnvironmentVariableCredentials() {
        try {
            // unset evn
            List<String> envSet = new ArrayList<String>();
            envSet.add(AuthUtils.ACCESS_KEY_ENV_VAR);
            envSet.add(AuthUtils.SECRET_KEY_ENV_VAR);
            envSet.add(AuthUtils.SESSION_TOKEN_ENV_VAR);
            unsetEnv(envSet);

            // set env
            Map<String, String> envMap = new HashMap<String, String>(System.getenv());
            envMap.put(AuthUtils.ACCESS_KEY_ENV_VAR, TestConfig.ROOT_ACCESS_KEY_ID);
            envMap.put(AuthUtils.SECRET_KEY_ENV_VAR, TestConfig.ROOT_ACCESS_KEY_SECRET);
            setEnv(envMap);

            // env provider
            EnvironmentVariableCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId(), TestConfig.ROOT_ACCESS_KEY_ID);
            Assert.assertEquals(credentials.getSecretAccessKey(), TestConfig.ROOT_ACCESS_KEY_SECRET);
            Assert.assertFalse(credentials.useSecurityToken());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetEnvironmentVariableStsCredentials() {
        try {
            // unset evn
            List<String> envSet = new ArrayList<String>();
            envSet.add(AuthUtils.ACCESS_KEY_ENV_VAR);
            envSet.add(AuthUtils.SECRET_KEY_ENV_VAR);
            envSet.add(AuthUtils.SESSION_TOKEN_ENV_VAR);
            unsetEnv(envSet);

            CredentialsProvider assumeRoleCredProvider = CredentialsProviderFactory.newSTSAssumeRoleSessionCredentialsProvider(
                    TestConfig.RAM_REGION_ID, TestConfig.USER_ACCESS_KEY_ID, TestConfig.USER_ACCESS_KEY_SECRET,
                    TestConfig.RAM_ROLE_ARN);

            // set env
            Credentials assumeRoleCred = assumeRoleCredProvider.getCredentials();
            Map<String, String> envMap = new HashMap<String, String>(System.getenv());
            envMap.put(AuthUtils.ACCESS_KEY_ENV_VAR, assumeRoleCred.getAccessKeyId());
            envMap.put(AuthUtils.SECRET_KEY_ENV_VAR, assumeRoleCred.getSecretAccessKey());
            envMap.put(AuthUtils.SESSION_TOKEN_ENV_VAR, assumeRoleCred.getSecurityToken());
            setEnv(envMap);

            // env provider
            EnvironmentVariableCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId(), assumeRoleCred.getAccessKeyId());
            Assert.assertEquals(credentials.getSecretAccessKey(), assumeRoleCred.getSecretAccessKey());
            Assert.assertEquals(credentials.getSecurityToken(), assumeRoleCred.getSecurityToken());
            Assert.assertTrue(credentials.useSecurityToken());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetEnvironmentVariableCredentialsInOss() {
        try {
            // unset evn
            List<String> envSet = new ArrayList<String>();
            envSet.add(AuthUtils.ACCESS_KEY_ENV_VAR);
            envSet.add(AuthUtils.SECRET_KEY_ENV_VAR);
            envSet.add(AuthUtils.SESSION_TOKEN_ENV_VAR);
            unsetEnv(envSet);

            // set env
            Map<String, String> env = new HashMap<String, String>(System.getenv());
            env.put(AuthUtils.ACCESS_KEY_ENV_VAR, TestConfig.ROOT_ACCESS_KEY_ID);
            env.put(AuthUtils.SECRET_KEY_ENV_VAR, TestConfig.ROOT_ACCESS_KEY_SECRET);
            setEnv(env);

            // env provider
            EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newEnvironmentVariableCredentialsProvider();
            String key = "test.txt";
            String content = "HelloOSS";
            String bucketName = getRandomBucketName();

            // oss put
            OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_ENDPOINT, credentialsProvider);
            ossClient.createBucket(bucketName);
            waitForCacheExpiration(2);
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossClient.deleteObject(bucketName, key);
            ossClient.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetEnvironmentVariableStsCredentialsInOss() {
        try {
            // unset evn
            List<String> envSet = new ArrayList<String>();
            envSet.add(AuthUtils.ACCESS_KEY_ENV_VAR);
            envSet.add(AuthUtils.SECRET_KEY_ENV_VAR);
            envSet.add(AuthUtils.SESSION_TOKEN_ENV_VAR);
            unsetEnv(envSet);

            CredentialsProvider assumeRoleCredProvider = CredentialsProviderFactory.newSTSAssumeRoleSessionCredentialsProvider(
                    TestConfig.RAM_REGION_ID, TestConfig.USER_ACCESS_KEY_ID, TestConfig.USER_ACCESS_KEY_SECRET,
                    TestConfig.RAM_ROLE_ARN);

            // set env
            Credentials assumeRoleCred = assumeRoleCredProvider.getCredentials();
            Map<String, String> envMap = new HashMap<String, String>(System.getenv());
            envMap.put(AuthUtils.ACCESS_KEY_ENV_VAR, assumeRoleCred.getAccessKeyId());
            envMap.put(AuthUtils.SECRET_KEY_ENV_VAR, assumeRoleCred.getSecretAccessKey());
            envMap.put(AuthUtils.SESSION_TOKEN_ENV_VAR, assumeRoleCred.getSecurityToken());
            setEnv(envMap);

            // env provider
            EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newEnvironmentVariableCredentialsProvider();
            String key = "test.txt";
            String content = "HelloOSS";
            String bucketName = getRandomBucketName();

            OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_ENDPOINT, credentialsProvider);
            ossClient.createBucket(bucketName);
            waitForCacheExpiration(2);
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossClient.deleteObject(bucketName, key);
            ossClient.deleteBucket(bucketName);
            ossClient.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
