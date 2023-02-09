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
import com.aliyun.oss.common.auth.InstanceProfileCredentialsFetcher;
import com.aliyun.oss.common.provider.mock.InstanceProfileCredentialsFetcherMock;
import com.aliyun.oss.common.provider.mock.InstanceProfileCredentialsFetcherMock.ResponseCategory;
import com.aliyuncs.exceptions.ClientException;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class InstanceProfileCredentialsFetcherTest extends TestBase {

    @Test
    public void testFetchNormalCredentials() {
        try {
            InstanceProfileCredentialsFetcherMock credentialsFetcher = new InstanceProfileCredentialsFetcherMock()
                    .withResponseCategory(ResponseCategory.Normal);

            InstanceProfileCredentials credentials = (InstanceProfileCredentials) credentialsFetcher.fetch(3);
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertEquals(credentials.getSecurityToken().length(), 536);
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertFalse(credentials.willSoonExpire());

            credentials = (InstanceProfileCredentials) credentialsFetcher.fetch();
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertEquals(credentials.getSecurityToken().length(), 536);
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertFalse(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFetchExpiredCredentials() {
        try {
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcherMock()
                    .withResponseCategory(ResponseCategory.Expired);

            InstanceProfileCredentials credentials = (InstanceProfileCredentials) credentialsFetcher.fetch(3);
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertEquals(credentials.getSecurityToken().length(), 536);
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertTrue(credentials.willSoonExpire());

            credentials = (InstanceProfileCredentials) credentialsFetcher.fetch();
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertEquals(credentials.getSecurityToken().length(), 536);
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertTrue(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFetchInvalidCredentials() {
        try {
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcherMock()
                    .withResponseCategory(ResponseCategory.FormatInvalid);
            credentialsFetcher.fetch(3);
            Assert.fail("EcsInstanceCredentialsFetcher.fetch should not be successful.");
        } catch (ClientException e) {
            Assert.assertEquals(FORMAT_ERROR_MESSAGE, e.getMessage());
        }

        try {
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcherMock()
                    .withResponseCategory(ResponseCategory.FormatInvalid);
            credentialsFetcher.fetch();
            Assert.fail("EcsInstanceCredentialsFetcher.fetch should not be successful.");
        } catch (ClientException e) {
            Assert.assertEquals(FORMAT_ERROR_MESSAGE, e.getMessage());
        }
        
        try {
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcherMock()
                    .withResponseCategory(ResponseCategory.ServerHalt);
            credentialsFetcher.fetch(3);
            Assert.fail("EcsInstanceCredentialsFetcher.fetch should not be successful.");
        } catch (ClientException e) {
        }
    }

    @Test
    public void testFetchExceptionalCredentials() {
        try {
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcherMock()
                    .withResponseCategory(ResponseCategory.Exceptional);
            credentialsFetcher.fetch(3);
            Assert.fail("EcsInstanceCredentialsFetcher.fetch should not be successful.");
        } catch (ClientException e) {
        }

        try {
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcherMock()
                    .withResponseCategory(ResponseCategory.Exceptional);
            credentialsFetcher.fetch();
            Assert.fail("EcsInstanceCredentialsFetcher.fetch should not be successful.");
        } catch (ClientException e) {
        }
    }

    @Test
    public void testSetRoleNameNegative() {
        try {
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcher();
            credentialsFetcher.setRoleName(null);
            Assert.fail("EcsInstanceCredentialsFetcher.setRoleName should not be successful.");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcher();
            credentialsFetcher.setRoleName("");
            Assert.fail("EcsInstanceCredentialsFetcher.setRoleName should not be successful.");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            new InstanceProfileCredentialsFetcher().withRoleName(null);
            Assert.fail("EcsInstanceCredentialsFetcher.setRoleName should not be successful.");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            new InstanceProfileCredentialsFetcher().withRoleName("");
            Assert.fail("EcsInstanceCredentialsFetcher.setRoleName should not be successful.");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetMetadataNegative() {
        try {
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcher()
                    .withRoleName("NotExistRoleName");
            credentialsFetcher.fetch();
            Assert.fail("EcsInstanceCredentialsFetcher.getMetadata should not be successful.");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ClientException);
        }
    }

    /**
     * NOTE: Run this case on ecs.
     */
    @Ignore
    public void testFetchCredentialsOnEcs() {
        try {
            // TODO: Establish a simulated ECS metadata service
            InstanceProfileCredentialsFetcher credentialsFetcher = new InstanceProfileCredentialsFetcher()
                    .withRoleName(TestConfig.ECS_ROLE_NAME);

            InstanceProfileCredentials credentials = (InstanceProfileCredentials) credentialsFetcher.fetch(3);
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertEquals(credentials.getSecurityToken().length(), 536);
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertFalse(credentials.willSoonExpire());

            credentials = (InstanceProfileCredentials) credentialsFetcher.fetch();
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertEquals(credentials.getSecurityToken().length(), 536);
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertFalse(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private static final String FORMAT_ERROR_MESSAGE = "Invalid json got from ECS Metadata service.";
    
}
