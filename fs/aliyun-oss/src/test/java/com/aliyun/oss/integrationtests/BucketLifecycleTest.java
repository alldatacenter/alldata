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

package com.aliyun.oss.integrationtests;

import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_LIFECYCLE_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.genRandomString;
import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;
import static com.aliyun.oss.model.SetBucketLifecycleRequest.MAX_LIFECYCLE_RULE_LIMIT;
import static com.aliyun.oss.model.SetBucketLifecycleRequest.MAX_RULE_ID_LENGTH;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.oss.model.*;
import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.model.LifecycleRule.AbortMultipartUpload;
import com.aliyun.oss.model.LifecycleRule.RuleStatus;
import com.aliyun.oss.model.LifecycleRule.StorageTransition;

public class BucketLifecycleTest extends TestBase {

    @Test
    public void testNormalSetBucketLifecycle() throws ParseException {
        final String bucketName = super.bucketName + "normal-set-bucket-lifecycle";
        final String ruleId0 = "delete obsoleted files";
        final String matchPrefix0 = "obsoleted0/";
        final String ruleId1 = "delete temporary files";
        final String matchPrefix1 = "temporary0/";
        final String ruleId2 = "delete obsoleted multipart files";
        final String matchPrefix2 = "obsoleted1/";
        final String ruleId3 = "delete temporary multipart files";
        final String matchPrefix3 = "temporary1/";
        final String ruleId4 = "delete temporary files(2)";
        final String matchPrefix4 = "temporary2/";
        final String ruleId5 = "delete temporary files(3)";
        final String matchPrefix5 = "temporary3/";

        try {
            ossClient.createBucket(bucketName);

            SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
            request.AddLifecycleRule(new LifecycleRule(ruleId0, matchPrefix0, RuleStatus.Enabled, 3));
            request.AddLifecycleRule(new LifecycleRule(ruleId1, matchPrefix1, RuleStatus.Enabled,
                    DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z")));

            LifecycleRule rule = new LifecycleRule(ruleId2, matchPrefix2, RuleStatus.Enabled, 3);
            LifecycleRule.AbortMultipartUpload abortMultipartUpload = new LifecycleRule.AbortMultipartUpload();
            abortMultipartUpload.setExpirationDays(3);
            rule.setAbortMultipartUpload(abortMultipartUpload);
            request.AddLifecycleRule(rule);

            rule = new LifecycleRule(ruleId3, matchPrefix3, RuleStatus.Enabled, 30);
            abortMultipartUpload = new LifecycleRule.AbortMultipartUpload();
            abortMultipartUpload.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            rule.setAbortMultipartUpload(abortMultipartUpload);
            List<StorageTransition> storageTransitions = new ArrayList<StorageTransition>();
            StorageTransition storageTransition = new StorageTransition();
            storageTransition.setStorageClass(StorageClass.IA);
            storageTransition.setExpirationDays(10);
            storageTransitions.add(storageTransition);
            storageTransition = new LifecycleRule.StorageTransition();
            storageTransition.setStorageClass(StorageClass.Archive);
            storageTransition.setExpirationDays(20);
            storageTransitions.add(storageTransition);
            rule.setStorageTransition(storageTransitions);
            request.AddLifecycleRule(rule);

            rule = new LifecycleRule(ruleId4, matchPrefix4, RuleStatus.Enabled);
            rule.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            request.AddLifecycleRule(rule);

            rule = new LifecycleRule(ruleId5, matchPrefix5, RuleStatus.Disabled);
            storageTransition = new LifecycleRule.StorageTransition();
            storageTransition.setStorageClass(StorageClass.Archive);
            storageTransition.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            storageTransitions = new ArrayList<StorageTransition>();
            storageTransitions.add(storageTransition);
            rule.setStorageTransition(storageTransitions);
            request.AddLifecycleRule(rule);

            ossClient.setBucketLifecycle(request);

            List<LifecycleRule> rules = ossClient.getBucketLifecycle(bucketName);
            Assert.assertEquals(rules.size(), 6);

            LifecycleRule r0 = rules.get(0);
            Assert.assertEquals(r0.getId(), ruleId0);
            Assert.assertEquals(r0.getPrefix(), matchPrefix0);
            Assert.assertEquals(r0.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r0.getExpirationDays(), 3);
            Assert.assertTrue(r0.getAbortMultipartUpload() == null);

            LifecycleRule r1 = rules.get(1);
            Assert.assertEquals(r1.getId(), ruleId1);
            Assert.assertEquals(r1.getPrefix(), matchPrefix1);
            Assert.assertEquals(r1.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(DateUtil.formatIso8601Date(r1.getExpirationTime()), "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r1.getAbortMultipartUpload() == null);

            LifecycleRule r2 = rules.get(2);
            Assert.assertEquals(r2.getId(), ruleId2);
            Assert.assertEquals(r2.getPrefix(), matchPrefix2);
            Assert.assertEquals(r2.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r2.getExpirationDays(), 3);
            Assert.assertNotNull(r2.getAbortMultipartUpload());
            Assert.assertEquals(r2.getAbortMultipartUpload().getExpirationDays(), 3);

            LifecycleRule r3 = rules.get(3);
            Assert.assertEquals(r3.getId(), ruleId3);
            Assert.assertEquals(r3.getPrefix(), matchPrefix3);
            Assert.assertEquals(r3.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r3.getExpirationDays(), 30);
            Assert.assertNotNull(r3.getAbortMultipartUpload());
            Assert.assertEquals(DateUtil.formatIso8601Date(r3.getAbortMultipartUpload().getCreatedBeforeDate()),
                    "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r3.hasStorageTransition());
            Assert.assertTrue(r3.getStorageTransition().get(0).getExpirationDays() == 10);
            Assert.assertEquals(r3.getStorageTransition().get(0).getStorageClass(), StorageClass.IA);
            Assert.assertTrue(r3.getStorageTransition().get(1).getExpirationDays() == 20);
            Assert.assertEquals(r3.getStorageTransition().get(1).getStorageClass(), StorageClass.Archive);

            LifecycleRule r4 = rules.get(4);
            Assert.assertEquals(r4.getId(), ruleId4);
            Assert.assertEquals(r4.getPrefix(), matchPrefix4);
            Assert.assertEquals(r4.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(DateUtil.formatIso8601Date(r4.getCreatedBeforeDate()), "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r4.getAbortMultipartUpload() == null);

            LifecycleRule r5 = rules.get(5);
            Assert.assertEquals(r5.getId(), ruleId5);
            Assert.assertEquals(r5.getPrefix(), matchPrefix5);
            Assert.assertEquals(r5.getStatus(), RuleStatus.Disabled);
            Assert.assertFalse(r5.hasCreatedBeforeDate());
            Assert.assertFalse(r5.hasExpirationTime());
            Assert.assertFalse(r5.hasExpirationDays());
            Assert.assertFalse(r5.hasAbortMultipartUpload());
            Assert.assertTrue(r5.hasStorageTransition());
            Assert.assertEquals(DateUtil.formatIso8601Date(r5.getStorageTransition().get(0).getCreatedBeforeDate()),
                    "2022-10-12T00:00:00.000Z");
            Assert.assertEquals(r5.getStorageTransition().get(0).getStorageClass(), StorageClass.Archive);

            // Override existing lifecycle rules
            final String nullRuleId = null;
            request.clearLifecycles();
            request.AddLifecycleRule(new LifecycleRule(nullRuleId, matchPrefix0, RuleStatus.Enabled, 7));
            ossClient.setBucketLifecycle(request);

            waitForCacheExpiration(5);

            rules = ossClient.getBucketLifecycle(bucketName);
            Assert.assertEquals(rules.size(), 1);

            r0 = rules.get(0);
            Assert.assertEquals(matchPrefix0, r0.getPrefix());
            Assert.assertEquals(r0.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r0.getExpirationDays(), 7);

            ossClient.deleteBucketLifecycle(bucketName);

            // Try get bucket lifecycle again
            try {
                ossClient.getBucketLifecycle(bucketName);
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_LIFECYCLE, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_LIFECYCLE_ERR));
            }
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testNormalSetBucketLifecyclWithTagging() throws ParseException {
        final String bucketName = super.bucketName + "normal-set-bucket-lifecycle-tagging";
        final String ruleId0 = "delete obsoleted files";
        final String matchPrefix0 = "obsoleted0/";
        final String ruleId1 = "delete temporary files";
        final String matchPrefix1 = "temporary0/";
        final String ruleId2 = "delete obsoleted multipart files";
        final String matchPrefix2 = "obsoleted1/";
        final String ruleId3 = "delete temporary multipart files";
        final String matchPrefix3 = "temporary1/";
        final String ruleId4 = "delete temporary files(2)";
        final String matchPrefix4 = "temporary2/";
        final String ruleId5 = "delete temporary files(3)";
        final String matchPrefix5 = "temporary3/";

        try {
            ossClient.createBucket(bucketName);

            SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);

            Map<String, String> tags = new HashMap<String, String>(1);
            tags.put("tag1", "balabala");
            tags.put("tag2", "haha");

            // ruleId0
            LifecycleRule rule = new LifecycleRule(ruleId0, matchPrefix0, RuleStatus.Enabled, 3);
            rule.setTags(tags);
            request.AddLifecycleRule(rule);

            // ruleId1, unsupported tag
            rule = new LifecycleRule(ruleId1, matchPrefix1, RuleStatus.Enabled, 
                    DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            request.AddLifecycleRule(rule);

            // ruleId2, unsupported tag
            rule = new LifecycleRule(ruleId2, matchPrefix2, RuleStatus.Enabled, 3);
            LifecycleRule.AbortMultipartUpload abortMultipartUpload = new LifecycleRule.AbortMultipartUpload();
            abortMultipartUpload.setExpirationDays(3);
            rule.setAbortMultipartUpload(abortMultipartUpload);
            request.AddLifecycleRule(rule);

            // ruleId3, unsupported tagging
            rule = new LifecycleRule(ruleId3, matchPrefix3, RuleStatus.Enabled, 30);
            abortMultipartUpload = new LifecycleRule.AbortMultipartUpload();
            abortMultipartUpload.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            rule.setAbortMultipartUpload(abortMultipartUpload);
            List<StorageTransition> storageTransitions = new ArrayList<StorageTransition>();
            StorageTransition storageTransition = new StorageTransition();
            storageTransition.setStorageClass(StorageClass.IA);
            storageTransition.setExpirationDays(10);
            storageTransitions.add(storageTransition);
            storageTransition = new LifecycleRule.StorageTransition();
            storageTransition.setStorageClass(StorageClass.Archive);
            storageTransition.setExpirationDays(20);
            storageTransitions.add(storageTransition);
            rule.setStorageTransition(storageTransitions);
            request.AddLifecycleRule(rule);

            // ruleId4
            rule = new LifecycleRule(ruleId4, matchPrefix4, RuleStatus.Enabled);
            rule.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            rule.setTags(tags);
            request.AddLifecycleRule(rule);

            // ruleId5
            rule = new LifecycleRule(ruleId5, matchPrefix5, RuleStatus.Enabled);
            storageTransition = new LifecycleRule.StorageTransition();
            storageTransition.setStorageClass(StorageClass.Archive);
            storageTransition.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            storageTransitions = new ArrayList<StorageTransition>();
            storageTransitions.add(storageTransition);
            rule.setStorageTransition(storageTransitions);
            rule.setTags(tags);
            request.AddLifecycleRule(rule);

            ossClient.setBucketLifecycle(request);

            List<LifecycleRule> rules = ossClient.getBucketLifecycle(bucketName);
            Assert.assertEquals(rules.size(), 6);

            LifecycleRule r0 = rules.get(0);
            Assert.assertEquals(r0.getId(), ruleId0);
            Assert.assertEquals(r0.getPrefix(), matchPrefix0);
            Assert.assertEquals(r0.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r0.getExpirationDays(), 3);
            Assert.assertTrue(r0.getAbortMultipartUpload() == null);
            Assert.assertEquals(r0.getTags().size(), 2);

            LifecycleRule r1 = rules.get(1);
            Assert.assertEquals(r1.getId(), ruleId1);
            Assert.assertEquals(r1.getPrefix(), matchPrefix1);
            Assert.assertEquals(r1.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(DateUtil.formatIso8601Date(r1.getExpirationTime()), "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r1.getAbortMultipartUpload() == null);

            LifecycleRule r2 = rules.get(2);
            Assert.assertEquals(r2.getId(), ruleId2);
            Assert.assertEquals(r2.getPrefix(), matchPrefix2);
            Assert.assertEquals(r2.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r2.getExpirationDays(), 3);
            Assert.assertNotNull(r2.getAbortMultipartUpload());
            Assert.assertEquals(r2.getAbortMultipartUpload().getExpirationDays(), 3);

            LifecycleRule r3 = rules.get(3);
            Assert.assertEquals(r3.getId(), ruleId3);
            Assert.assertEquals(r3.getPrefix(), matchPrefix3);
            Assert.assertEquals(r3.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r3.getExpirationDays(), 30);
            Assert.assertNotNull(r3.getAbortMultipartUpload());
            Assert.assertEquals(DateUtil.formatIso8601Date(r3.getAbortMultipartUpload().getCreatedBeforeDate()), 
                    "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r3.hasStorageTransition());
            Assert.assertTrue(r3.getStorageTransition().get(0).getExpirationDays() == 10);
            Assert.assertEquals(r3.getStorageTransition().get(0).getStorageClass(), StorageClass.IA);
            Assert.assertTrue(r3.getStorageTransition().get(1).getExpirationDays() == 20);
            Assert.assertEquals(r3.getStorageTransition().get(1).getStorageClass(), StorageClass.Archive);

            LifecycleRule r4 = rules.get(4);
            Assert.assertEquals(r4.getId(), ruleId4);
            Assert.assertEquals(r4.getPrefix(), matchPrefix4);
            Assert.assertEquals(r4.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(DateUtil.formatIso8601Date(r4.getCreatedBeforeDate()), "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r4.getAbortMultipartUpload() == null);
            Assert.assertEquals(r4.getTags().size(), 2);

            LifecycleRule r5 = rules.get(5);
            Assert.assertEquals(r5.getId(), ruleId5);
            Assert.assertEquals(r5.getPrefix(), matchPrefix5);
            Assert.assertEquals(r5.getStatus(), RuleStatus.Enabled);
            Assert.assertFalse(r5.hasCreatedBeforeDate());
            Assert.assertFalse(r5.hasExpirationTime());
            Assert.assertFalse(r5.hasExpirationDays());
            Assert.assertFalse(r5.hasAbortMultipartUpload());
            Assert.assertTrue(r5.hasStorageTransition());
            Assert.assertEquals(DateUtil.formatIso8601Date(r5.getStorageTransition().get(0).getCreatedBeforeDate()), 
                    "2022-10-12T00:00:00.000Z");
            Assert.assertEquals(r5.getStorageTransition().get(0).getStorageClass(), StorageClass.Archive);
            Assert.assertEquals(r5.getTags().size(), 2);
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUnormalSetBucketLifecycle() throws ParseException {
        final String bucketName = super.bucketName + "unormal-set-bucket-lifecycle";
        final String ruleId0 = "delete obsoleted files";
        final String matchPrefix0 = "obsoleted/";

        try {
            ossClient.createBucket(bucketName);

            //set none expiration or transition
            try {
                final LifecycleRule r = new LifecycleRule(ruleId0, matchPrefix0, RuleStatus.Enabled);
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
                request.AddLifecycleRule(r);
                Assert.fail("Rule has none expiration or transition specified, should be failed.");
            } catch (IllegalArgumentException e) {
                // Expected exception.
            }

            // Set non-existent bucket 
            final String nonexistentBucket = "nonexistent-bucket";            
            final LifecycleRule r = new LifecycleRule(ruleId0, matchPrefix0, RuleStatus.Enabled, 3);
            try {                
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(nonexistentBucket);
                request.AddLifecycleRule(r);
                ossClient.setBucketLifecycle(request);
                
                Assert.fail("Set bucket lifecycle should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
            }

            // Set bucket without ownership
            final String bucketWithoutOwnership = "oss";
            try {
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketWithoutOwnership);
                request.AddLifecycleRule(r);
                ossClient.setBucketLifecycle(request);

                Assert.fail("Set bucket lifecycle should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            }

            // Set length of rule id exceeding RULE_ID_MAX_LENGTH(255)
            final String ruleId256 = genRandomString(MAX_RULE_ID_LENGTH + 1);
            try {
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
                request.AddLifecycleRule(new LifecycleRule(ruleId256, matchPrefix0, RuleStatus.Enabled, 3));
                
                Assert.fail("Set bucket lifecycle should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // Set size of lifecycle rules exceeding LIFECYCLE_RULE_MAX_LIMIT(1000)
            try {
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(nonexistentBucket);
                for (int i = 0; i < (MAX_LIFECYCLE_RULE_LIMIT + 1) ; i++) {
                    request.AddLifecycleRule(r);
                }

                Assert.fail("Set bucket lifecycle should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // Set both rule id and prefix null
            final String nullRuleId = null;
            final String nullMatchPrefix = null;
            try {
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
                request.AddLifecycleRule(new LifecycleRule(nullRuleId, nullMatchPrefix, RuleStatus.Enabled, 3));
                ossClient.setBucketLifecycle(request);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }

            // Set both expiration day and expiration time
            try {
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(nonexistentBucket);
                LifecycleRule invalidRule = new LifecycleRule();
                invalidRule.setId(ruleId0);
                invalidRule.setPrefix(matchPrefix0);
                invalidRule.setStatus(RuleStatus.Enabled);
                invalidRule.setExpirationTime(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
                invalidRule.setExpirationDays(3);
                request.AddLifecycleRule(invalidRule);

                Assert.fail("Set bucket lifecycle should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // Set neither expiration day nor expiration time
            try {
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(nonexistentBucket);
                LifecycleRule invalidRule = new LifecycleRule();
                invalidRule.setId(ruleId0);
                invalidRule.setPrefix(matchPrefix0);
                invalidRule.setStatus(RuleStatus.Enabled);
                request.AddLifecycleRule(invalidRule);

                Assert.fail("Set bucket lifecycle should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // With abort multipart upload option
            try {
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(nonexistentBucket);
                LifecycleRule invalidRule = new LifecycleRule();
                invalidRule.setId(ruleId0);
                invalidRule.setPrefix(matchPrefix0);
                invalidRule.setStatus(RuleStatus.Enabled);
                invalidRule.setExpirationDays(3);
                LifecycleRule.AbortMultipartUpload abortMultipartUpload = new AbortMultipartUpload();
                abortMultipartUpload.setExpirationDays(3);
                abortMultipartUpload.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
                invalidRule.setAbortMultipartUpload(abortMultipartUpload);
                request.AddLifecycleRule(invalidRule);

                Assert.fail("Set bucket lifecycle should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // With storage transition option
            try {
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(nonexistentBucket);
                LifecycleRule invalidRule = new LifecycleRule();
                invalidRule.setId(ruleId0);
                invalidRule.setPrefix(matchPrefix0);
                invalidRule.setStatus(RuleStatus.Enabled);
                invalidRule.setExpirationDays(3);
                LifecycleRule.StorageTransition storageTransition = new StorageTransition();
                storageTransition.setExpirationDays(3);
                storageTransition.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
                List<StorageTransition> storageTransitions = new ArrayList<StorageTransition>();
                storageTransitions.add(storageTransition);
                invalidRule.setStorageTransition(storageTransitions);
                request.AddLifecycleRule(invalidRule);

                Assert.fail("Set bucket lifecycle should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUnormalGetBucketLifecycle() {
        // Get non-existent bucket
        final String nonexistentBucket = "unormal-get-bucket-lifecycle";
        try {
            ossClient.getBucketLifecycle(nonexistentBucket);
            Assert.fail("Get bucket lifecycle should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }

        // Get bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            ossClient.getBucketLogging(bucketWithoutOwnership);
            Assert.fail("Get bucket lifecycle should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }

        // Get bucket without setting lifecycle configuration
        final String bucketWithoutLifecycleConfiguration = super.bucketName + "bucket-without-lifecycle-configuration";
        try {
            ossClient.createBucket(bucketWithoutLifecycleConfiguration);

            ossClient.getBucketLifecycle(bucketWithoutLifecycleConfiguration);
            Assert.fail("Get bucket lifecycle should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_LIFECYCLE, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_LIFECYCLE_ERR));
        } finally {
            TestUtils.waitForCacheExpiration(5);
            ossClient.deleteBucket(bucketWithoutLifecycleConfiguration);
        }
    }

    @Test
    public void testUnormalDeleteBucketLifecycle() {
        // Delete non-existent bucket
        final String nonexistentBucket = super.bucketName + "unormal-delete-bucket-lifecycle";
        try {
            ossClient.deleteBucketLifecycle(nonexistentBucket);
            Assert.fail("Delete bucket lifecycle should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }

        // Delete bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            ossClient.deleteBucketLifecycle(bucketWithoutOwnership);
            Assert.fail("Delete bucket lifecycle should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }

        // Delete bucket without setting lifecycle configuration
        final String bucketWithoutLifecycleConfiguration = super.bucketName + "bucket-without-lifecycle-configuration";
        try {
            ossClient.createBucket(bucketWithoutLifecycleConfiguration);
            ossClient.deleteBucketLifecycle(bucketWithoutLifecycleConfiguration);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketWithoutLifecycleConfiguration);
        }
    }

    @Test
    public void testNormalSetBucketLifecycleWithAccessMonitor() throws ParseException {
        final String bucketName = super.bucketName + "normal-set-bucket-lifecycle";
        final String ruleId0 = "delete obsoleted files";
        final String matchPrefix0 = "obsoleted0/";
        final String ruleId1 = "delete temporary files";
        final String matchPrefix1 = "temporary0/";
        final String ruleId2 = "delete obsoleted multipart files";
        final String matchPrefix2 = "obsoleted1/";
        final String ruleId3 = "delete temporary multipart files";
        final String matchPrefix3 = "temporary1/";
        final String ruleId4 = "delete temporary files(2)";
        final String matchPrefix4 = "temporary2/";
        final String ruleId5 = "delete temporary files(3)";
        final String matchPrefix5 = "temporary3/";

        try {
            ossClient.createBucket(bucketName);
            ossClient.putBucketAccessMonitor(bucketName, AccessMonitor.AccessMonitorStatus.Enabled.toString());

            SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
            request.AddLifecycleRule(new LifecycleRule(ruleId0, matchPrefix0, RuleStatus.Enabled, 3));
            request.AddLifecycleRule(new LifecycleRule(ruleId1, matchPrefix1, RuleStatus.Enabled,
                    DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z")));

            LifecycleRule rule = new LifecycleRule(ruleId2, matchPrefix2, RuleStatus.Enabled, 3);
            LifecycleRule.AbortMultipartUpload abortMultipartUpload = new LifecycleRule.AbortMultipartUpload();
            abortMultipartUpload.setExpirationDays(3);
            rule.setAbortMultipartUpload(abortMultipartUpload);
            request.AddLifecycleRule(rule);

            rule = new LifecycleRule(ruleId3, matchPrefix3, RuleStatus.Enabled, 30);
            abortMultipartUpload = new LifecycleRule.AbortMultipartUpload();
            abortMultipartUpload.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            rule.setAbortMultipartUpload(abortMultipartUpload);
            List<StorageTransition> storageTransitions = new ArrayList<StorageTransition>();
            StorageTransition storageTransition = new StorageTransition();
            storageTransition.setStorageClass(StorageClass.IA);
            storageTransition.setExpirationDays(10);
            storageTransition.setIsAccessTime(true);
            storageTransition.setReturnToStdWhenVisit(false);
            storageTransitions.add(storageTransition);
            storageTransition = new LifecycleRule.StorageTransition();
            storageTransition.setStorageClass(StorageClass.Archive);
            storageTransition.setExpirationDays(20);
            storageTransition.setIsAccessTime(false);
            storageTransitions.add(storageTransition);
            rule.setStorageTransition(storageTransitions);
            request.AddLifecycleRule(rule);

            rule = new LifecycleRule(ruleId4, matchPrefix4, RuleStatus.Enabled);
            rule.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            request.AddLifecycleRule(rule);

            rule = new LifecycleRule(ruleId5, matchPrefix5, RuleStatus.Disabled);
            storageTransition = new LifecycleRule.StorageTransition();
            storageTransition.setStorageClass(StorageClass.IA);
            storageTransition.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            storageTransition.setIsAccessTime(false);
            storageTransitions = new ArrayList<StorageTransition>();
            storageTransitions.add(storageTransition);
            rule.setStorageTransition(storageTransitions);
            List<LifecycleRule.NoncurrentVersionStorageTransition> noncurrentVersionStorageTransitions = new ArrayList<LifecycleRule.NoncurrentVersionStorageTransition>();
            LifecycleRule.NoncurrentVersionStorageTransition noncurrentVersionStorageTransition = new LifecycleRule.NoncurrentVersionStorageTransition();
            noncurrentVersionStorageTransition.setStorageClass(StorageClass.IA);
            noncurrentVersionStorageTransition.setNoncurrentDays(10);
            noncurrentVersionStorageTransition.setIsAccessTime(true);
            noncurrentVersionStorageTransition.setReturnToStdWhenVisit(false);
            noncurrentVersionStorageTransitions.add(noncurrentVersionStorageTransition);
            rule.setNoncurrentVersionStorageTransitions(noncurrentVersionStorageTransitions);
            request.AddLifecycleRule(rule);

            ossClient.setBucketLifecycle(request);

            List<LifecycleRule> rules = ossClient.getBucketLifecycle(bucketName);
            Assert.assertEquals(rules.size(), 6);

            LifecycleRule r0 = rules.get(0);
            Assert.assertEquals(r0.getId(), ruleId0);
            Assert.assertEquals(r0.getPrefix(), matchPrefix0);
            Assert.assertEquals(r0.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r0.getExpirationDays(), 3);
            Assert.assertTrue(r0.getAbortMultipartUpload() == null);

            LifecycleRule r1 = rules.get(1);
            Assert.assertEquals(r1.getId(), ruleId1);
            Assert.assertEquals(r1.getPrefix(), matchPrefix1);
            Assert.assertEquals(r1.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(DateUtil.formatIso8601Date(r1.getExpirationTime()), "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r1.getAbortMultipartUpload() == null);

            LifecycleRule r2 = rules.get(2);
            Assert.assertEquals(r2.getId(), ruleId2);
            Assert.assertEquals(r2.getPrefix(), matchPrefix2);
            Assert.assertEquals(r2.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r2.getExpirationDays(), 3);
            Assert.assertNotNull(r2.getAbortMultipartUpload());
            Assert.assertEquals(r2.getAbortMultipartUpload().getExpirationDays(), 3);

            LifecycleRule r3 = rules.get(3);
            Assert.assertEquals(r3.getId(), ruleId3);
            Assert.assertEquals(r3.getPrefix(), matchPrefix3);
            Assert.assertEquals(r3.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r3.getExpirationDays(), 30);
            Assert.assertNotNull(r3.getAbortMultipartUpload());
            Assert.assertEquals(DateUtil.formatIso8601Date(r3.getAbortMultipartUpload().getCreatedBeforeDate()),
                    "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r3.hasStorageTransition());
            Assert.assertTrue(r3.getStorageTransition().get(0).getExpirationDays() == 10);
            Assert.assertEquals(r3.getStorageTransition().get(0).getStorageClass(), StorageClass.IA);
            Assert.assertTrue(r3.getStorageTransition().get(1).getExpirationDays() == 20);
            Assert.assertEquals(r3.getStorageTransition().get(1).getStorageClass(), StorageClass.Archive);
            Assert.assertEquals(r3.getStorageTransition().get(0).getIsAccessTime().toString(), "true");
            Assert.assertEquals(r3.getStorageTransition().get(0).getReturnToStdWhenVisit().toString(), "false");
            Assert.assertEquals(r3.getStorageTransition().get(1).getIsAccessTime().toString(), "false");

            LifecycleRule r4 = rules.get(4);
            Assert.assertEquals(r4.getId(), ruleId4);
            Assert.assertEquals(r4.getPrefix(), matchPrefix4);
            Assert.assertEquals(r4.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(DateUtil.formatIso8601Date(r4.getCreatedBeforeDate()), "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r4.getAbortMultipartUpload() == null);

            LifecycleRule r5 = rules.get(5);
            Assert.assertEquals(r5.getId(), ruleId5);
            Assert.assertEquals(r5.getPrefix(), matchPrefix5);
            Assert.assertEquals(r5.getStatus(), RuleStatus.Disabled);
            Assert.assertFalse(r5.hasCreatedBeforeDate());
            Assert.assertFalse(r5.hasExpirationTime());
            Assert.assertFalse(r5.hasExpirationDays());
            Assert.assertFalse(r5.hasAbortMultipartUpload());
            Assert.assertTrue(r5.hasStorageTransition());
            Assert.assertEquals(DateUtil.formatIso8601Date(r5.getStorageTransition().get(0).getCreatedBeforeDate()),
                    "2022-10-12T00:00:00.000Z");
            Assert.assertEquals(r5.getStorageTransition().get(0).getStorageClass(), StorageClass.IA);
            Assert.assertEquals(r5.getStorageTransition().get(0).getIsAccessTime().toString(), "false");
            Assert.assertEquals(r5.getNoncurrentVersionStorageTransitions().get(0).getIsAccessTime().toString(), "true");
            Assert.assertEquals(r5.getNoncurrentVersionStorageTransitions().get(0).getReturnToStdWhenVisit().toString(), "false");

            // Override existing lifecycle rules
            final String nullRuleId = null;
            request.clearLifecycles();
            request.AddLifecycleRule(new LifecycleRule(nullRuleId, matchPrefix0, RuleStatus.Enabled, 7));
            ossClient.setBucketLifecycle(request);

            waitForCacheExpiration(5);

            rules = ossClient.getBucketLifecycle(bucketName);
            Assert.assertEquals(rules.size(), 1);

            r0 = rules.get(0);
            Assert.assertEquals(matchPrefix0, r0.getPrefix());
            Assert.assertEquals(r0.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r0.getExpirationDays(), 7);

            ossClient.deleteBucketLifecycle(bucketName);

            // Try get bucket lifecycle again
            try {
                ossClient.getBucketLifecycle(bucketName);
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_LIFECYCLE, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_LIFECYCLE_ERR));
            }
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUnormalSetBucketLifecycleWithAccessMonitor() throws ParseException {
        final String bucketName = super.bucketName + "unormal-set-bucket-lifecycle";
        final String ruleId0 = "delete obsoleted files";
        final String matchPrefix0 = "obsoleted/";

        try {
            ossClient.createBucket(bucketName);

            // Adding atime based lifecycle rules is supported only when the status is set to enabled
            try {
                try{
                    ossClient.putBucketAccessMonitor(bucketName, AccessMonitor.AccessMonitorStatus.Disabled.toString());
                } catch (Exception e){
                    System.out.println("Accessmonitor execution failed");
                    e.printStackTrace();
                }
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
                LifecycleRule invalidRule = new LifecycleRule();
                invalidRule.setId(ruleId0);
                invalidRule.setPrefix(matchPrefix0);
                invalidRule.setStatus(RuleStatus.Enabled);
                invalidRule.setExpirationDays(3);
                LifecycleRule.StorageTransition storageTransition = new StorageTransition(1, StorageClass.IA, true, true);
                List<StorageTransition> storageTransitions = new ArrayList<StorageTransition>();
                storageTransitions.add(storageTransition);
                invalidRule.setStorageTransition(storageTransitions);
                request.AddLifecycleRule(invalidRule);
                ossClient.setBucketLifecycle(request);
            } catch (OSSException e) {
                Assert.assertEquals("MalformedXML", e.getErrorCode());
            }

            // Returntostdwhenvisit can exist only when isaccesstime is true
            try {
                try{
                    ossClient.putBucketAccessMonitor(bucketName, AccessMonitor.AccessMonitorStatus.Enabled.toString());
                } catch (Exception e){
                    System.out.println("Accessmonitor execution failed");
                    e.printStackTrace();
                }
                SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
                LifecycleRule invalidRule = new LifecycleRule();
                invalidRule.setId(ruleId0);
                invalidRule.setPrefix(matchPrefix0);
                invalidRule.setStatus(RuleStatus.Enabled);
                invalidRule.setExpirationDays(3);
                LifecycleRule.NoncurrentVersionStorageTransition noncurrentVersionStorageTransition = new LifecycleRule.NoncurrentVersionStorageTransition(3, StorageClass.Archive, false, true);
                List<LifecycleRule.NoncurrentVersionStorageTransition> noncurrentVersionStorageTransitions = new ArrayList<LifecycleRule.NoncurrentVersionStorageTransition>();
                noncurrentVersionStorageTransitions.add(noncurrentVersionStorageTransition);
                invalidRule.setNoncurrentVersionStorageTransitions(noncurrentVersionStorageTransitions);
                request.AddLifecycleRule(invalidRule);
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUnormalSetBucketLifecycleWithNot() throws ParseException {
        final String bucketName = super.bucketName + "unormal-set-bucket-lifecycle";
        final String ruleId0 = "delete obsoleted files";
        final String matchPrefix0 = "obsoleted/";

        try {
            ossClient.createBucket(bucketName);

            SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
            LifecycleRule rule = new LifecycleRule(ruleId0, matchPrefix0, RuleStatus.Enabled, 3);
            LifecycleFilter filter = new LifecycleFilter();
            LifecycleNot not = new LifecycleNot();
            List<LifecycleNot> notList = new ArrayList<LifecycleNot>();
            not.setPrefix(matchPrefix0);
            notList.add(not);
            filter.setNotList(notList);
            rule.setFilter(filter);
            request.AddLifecycleRule(rule);

            ossClient.setBucketLifecycle(request);
        } catch (Exception e){
            Assert.assertTrue(e instanceof IllegalArgumentException);
            Assert.assertEquals(e.getMessage(),"If there is no tag node under the not node, the prefix of the not node cannot be the same as that of the rule.");
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testNormalSetBucketLifecycleWithNot() throws ParseException {
        final String bucketName = super.bucketName + "normal-set-bucket-lifecycle-tagging";
        final String ruleId0 = "delete obsoleted files";
        final String matchPrefix0 = "obsoleted0/";
        final String ruleId1 = "delete temporary files";
        final String matchPrefix1 = "temporary0/";

        try {
            ossClient.createBucket(bucketName);

            SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);

            Map<String, String> tags = new HashMap<String, String>(1);
            tags.put("tag1", "balabala");
            tags.put("tag2", "haha");
            LifecycleFilter filter = new LifecycleFilter();
            LifecycleNot not = new LifecycleNot();
            Tag tag = new Tag("tag-key","tag-value");
            not.setPrefix("not-prefix");
            not.setTag(tag);
            List<LifecycleNot> notList = new ArrayList<LifecycleNot>();
            notList.add(not);
            filter.setNotList(notList);

            // ruleId0
            LifecycleRule rule = new LifecycleRule(ruleId0, matchPrefix0, RuleStatus.Enabled, 3);
            rule.setTags(tags);
            rule.setFilter(filter);
            request.AddLifecycleRule(rule);

            // ruleId1, unsupported tag
            LifecycleNot not1 = new LifecycleNot();
            LifecycleFilter filter1 = new LifecycleFilter();
            rule = new LifecycleRule(ruleId1, matchPrefix1, RuleStatus.Enabled,
                    DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
            not1.setPrefix(matchPrefix1+"not-prefix1");

            List<LifecycleNot> notList1 = new ArrayList<LifecycleNot>();
            notList1.add(not1);
            filter1.setNotList(notList1);
            rule.setFilter(filter1);
            request.AddLifecycleRule(rule);

            ossClient.setBucketLifecycle(request);

            List<LifecycleRule> rules = ossClient.getBucketLifecycle(bucketName);

            LifecycleRule r0 = rules.get(0);
            Assert.assertEquals(r0.getId(), ruleId0);
            Assert.assertEquals(r0.getPrefix(), matchPrefix0);
            Assert.assertEquals(r0.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r0.getExpirationDays(), 3);
            Assert.assertTrue(r0.getAbortMultipartUpload() == null);
            Assert.assertEquals(r0.getTags().size(), 2);
            Assert.assertEquals(r0.getFilter().getNotList().get(0).getPrefix(), matchPrefix0+"not-prefix");
            Assert.assertEquals(r0.getFilter().getNotList().get(0).getTag().getKey(), "tag-key");
            Assert.assertEquals(r0.getFilter().getNotList().get(0).getTag().getValue(), "tag-value");
            LifecycleRule r1 = rules.get(1);
            Assert.assertEquals(r1.getId(), ruleId1);
            Assert.assertEquals(r1.getPrefix(), matchPrefix1);
            Assert.assertEquals(r1.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(DateUtil.formatIso8601Date(r1.getExpirationTime()), "2022-10-12T00:00:00.000Z");
            Assert.assertTrue(r1.getAbortMultipartUpload() == null);
            Assert.assertEquals(r1.getFilter().getNotList().get(0).getPrefix(), matchPrefix1+"not-prefix1");
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
}
