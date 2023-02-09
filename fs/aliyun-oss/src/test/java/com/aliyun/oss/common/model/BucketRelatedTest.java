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

package com.aliyun.oss.common.model;

import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.junit.Test;

import java.util.*;


import com.aliyun.oss.model.SetBucketCORSRequest.CORSRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BucketRelatedTest {
    final private String bucketName = "test-bucket-name";

    @Test
    public void testAddBucketCnameRequest() {
        AddBucketCnameRequest request = new AddBucketCnameRequest(bucketName).withDomain("domain");
        assertEquals("domain", request.getDomain());

        request.setDomain("new domain");
        assertEquals("new domain", request.getDomain());
    }

    @Test
    public void testDeleteBucketCnameRequest() {
        DeleteBucketCnameRequest request = new DeleteBucketCnameRequest(bucketName).withDomain("domain");
        assertEquals("domain", request.getDomain());

        request.setDomain("new domain");
        assertEquals("new domain", request.getDomain());

        request = new DeleteBucketCnameRequest(bucketName);
        assertEquals(null, request.getDomain());
    }

    @Test
    public void testAddBucketReplicationRequest() {
        AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);

        request.setReplicationRuleID("id");
        assertEquals("id", request.getReplicationRuleID());

        request.setEnableHistoricalObjectReplication(false);
        assertEquals(false, request.isEnableHistoricalObjectReplication());
        request.setEnableHistoricalObjectReplication(true);
        assertEquals(true, request.isEnableHistoricalObjectReplication());

        List<String> prefixes = new ArrayList<String>();
        prefixes.add("prefix 1");
        prefixes.add("prefix 2");
        assertEquals(0, request.getObjectPrefixList().size());
        request.setObjectPrefixList(prefixes);
        assertEquals(prefixes, request.getObjectPrefixList());
        request.setObjectPrefixList(null);
        assertEquals(0, request.getObjectPrefixList().size());
        request.setObjectPrefixList(prefixes);
        prefixes = new ArrayList<String>();
        request.setObjectPrefixList(prefixes);
        assertEquals(0, request.getObjectPrefixList().size());

        List<AddBucketReplicationRequest.ReplicationAction> replicationActionList =
                new ArrayList<AddBucketReplicationRequest.ReplicationAction>();
        assertEquals(0, request.getReplicationActionList().size());
        replicationActionList.add(AddBucketReplicationRequest.ReplicationAction.ALL);
        request.setReplicationActionList(replicationActionList);
        assertEquals(replicationActionList, request.getReplicationActionList());
        request.setReplicationActionList(null);
        assertEquals(0, request.getReplicationActionList().size());
        replicationActionList = new ArrayList<AddBucketReplicationRequest.ReplicationAction>();
        request.setReplicationActionList(replicationActionList);
        assertEquals(0, request.getObjectPrefixList().size());

        assertEquals(AddBucketReplicationRequest.ReplicationAction.ALL,
                AddBucketReplicationRequest.ReplicationAction.parse("ALL"));
        assertEquals(AddBucketReplicationRequest.ReplicationAction.ALL.toString(), "ALL");
        assertEquals(AddBucketReplicationRequest.ReplicationAction.PUT,
                AddBucketReplicationRequest.ReplicationAction.parse("PUT"));
        assertEquals(AddBucketReplicationRequest.ReplicationAction.DELETE,
                AddBucketReplicationRequest.ReplicationAction.parse("DELETE"));
        assertEquals(AddBucketReplicationRequest.ReplicationAction.ABORT,
                AddBucketReplicationRequest.ReplicationAction.parse("ABORT"));

        try {
            AddBucketReplicationRequest.ReplicationAction  value = AddBucketReplicationRequest.ReplicationAction.parse("UN");
            assertTrue(false);
        }catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testDeleteBucketReplicationRequest() {
        DeleteBucketReplicationRequest request = new DeleteBucketReplicationRequest(bucketName).withReplicationRuleID("id");
        assertEquals("id", request.getReplicationRuleID());

        request.setReplicationRuleID("new id");
        assertEquals("new id", request.getReplicationRuleID());

        request = new DeleteBucketReplicationRequest(bucketName, "id1");
        assertEquals("id1", request.getReplicationRuleID());
    }

    @Test
    public void testPutBucketImageRequest() {
        PutBucketImageRequest request = new PutBucketImageRequest("bucket");
        request.setBucketName("bucket");
        request.SetIsForbidOrigPicAccess(false);
        request.SetIsUseStyleOnly(false);
        request.SetIsAutoSetContentType(false);
        request.SetIsUseSrcFormat(false);
        request.SetIsSetAttachName(false);
        request.SetDefault404Pic("404Pic");
        request.SetStyleDelimiters("del");

        assertEquals("bucket", request.getBucketName());
        assertEquals(false, request.GetIsForbidOrigPicAccess());
        assertEquals(false, request.GetIsUseStyleOnly());
        assertEquals(false, request.GetIsAutoSetContentType());
        assertEquals(false, request.GetIsUseSrcFormat());
        assertEquals(false, request.GetIsSetAttachName());
        assertEquals("404Pic", request.GetDefault404Pic());
        assertEquals("del", request.GetStyleDelimiters());
    }

    @Test
    public void testPutImageStyleRequest() {
        PutImageStyleRequest request = new PutImageStyleRequest();
        request.SetBucketName("bucket");
        request.SetStyleName("stylename");
        request.SetStyle("style");

        assertEquals("bucket", request.GetBucketName());
        assertEquals("stylename", request.GetStyleName());
        assertEquals("style", request.GetStyle());
    }

    @Test
    public void testSetBucketCORSRequest() {
        SetBucketCORSRequest.CORSRule rule = new SetBucketCORSRequest.CORSRule();
        rule.addAllowdOrigin(null);
        rule.addAllowdOrigin("");

        List<String> allowedOrigins = new ArrayList<String>();
        rule.setAllowedOrigins(null);
        rule.setAllowedOrigins(allowedOrigins);
        allowedOrigins.add("origin");
        rule.setAllowedOrigins(allowedOrigins);
        rule.addAllowdOrigin("here");
        rule.addAllowdOrigin("");
        rule.addAllowdOrigin(null);

        List<String> allowedMethods = new ArrayList<String>();
        rule.setAllowedMethods(null);
        rule.setAllowedMethods(allowedMethods);
        allowedMethods.add("PUT");
        rule.setAllowedMethods(allowedMethods);
        rule.addAllowedMethod("GET");
        rule.addAllowedMethod("");
        rule.addAllowedMethod(null);

        List<String> allowedHeaders = new ArrayList<String>();
        rule.setAllowedHeaders(null);
        rule.setAllowedHeaders(allowedHeaders);
        allowedHeaders.add("header");
        rule.setAllowedHeaders(allowedHeaders);
        rule.addAllowedHeader("header1");
        rule.addAllowedHeader("");
        rule.addAllowedHeader(null);

        List<String> exposeHeader = new ArrayList<String>();
        rule.setExposeHeaders(null);
        rule.setExposeHeaders(exposeHeader);
        exposeHeader.add("header");
        rule.setExposeHeaders(exposeHeader);
        rule.addExposeHeader("header1");
        rule.addExposeHeader("");
        rule.addExposeHeader(null);

        SetBucketCORSRequest request = new SetBucketCORSRequest("bucket");

        try {
            request.addCorsRule(null);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            rule = new SetBucketCORSRequest.CORSRule();
            rule.clearAllowedOrigins();
            rule.clearAllowedMethods();
            rule.clearAllowedHeaders();
            rule.clearExposeHeaders();
            rule.addAllowdOrigin("*");
            rule.addAllowedMethod("PUT");
            for (int i = 0; i < 11; i++) {
                request.addCorsRule(rule);
            }
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        //one addAllowdOrigin *
        try {
            rule = new SetBucketCORSRequest.CORSRule();
            rule.addAllowdOrigin("*");
            rule.addAllowdOrigin("*");
            rule.addAllowedMethod("PUT");
            request.addCorsRule(rule);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            rule = new SetBucketCORSRequest.CORSRule();
            rule.addAllowdOrigin("*");
            rule.addAllowedMethod("PUT");
            rule.addAllowedHeader("*");
            rule.addAllowedHeader("*");
            request.addCorsRule(rule);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            rule = new SetBucketCORSRequest.CORSRule();
            rule.addAllowdOrigin("*");
            rule.addAllowedMethod("");
            rule.addAllowedHeader("*");
            request.addCorsRule(rule);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            rule = new SetBucketCORSRequest.CORSRule();
            rule.addAllowdOrigin("*");
            rule.addAllowedMethod(null);
            rule.addAllowedHeader("*");
            request.addCorsRule(rule);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            request.setCorsRules(null);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            List<CORSRule> corsRules = new ArrayList<CORSRule>();
            request.setCorsRules(corsRules);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            List<CORSRule> corsRules = new ArrayList<CORSRule>();
            rule = new SetBucketCORSRequest.CORSRule();
            rule.addAllowdOrigin("*");
            rule.addAllowedMethod("PUT");
            corsRules.add(rule);
            request.setCorsRules(corsRules);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
        }

        try {
            List<CORSRule> corsRules = new ArrayList<CORSRule>();
            rule = new SetBucketCORSRequest.CORSRule();
            rule.addAllowdOrigin("*");
            rule.addAllowedMethod("PUT");

            for (int i = 0; i < 11; i++)
                corsRules.add(rule);
            request.setCorsRules(corsRules);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            SetBucketCORSRequest corsRequest = new SetBucketCORSRequest("bucketName");
            CORSRule corsRule = new CORSRule();
            corsRule.addAllowdOrigin("*");
            corsRule.addAllowedMethod("GET");
            corsRule.addAllowedHeader("**");
            corsRequest.addCorsRule(corsRule);
            Assert.fail("no more than one '*' allowed, should be failed here.");
        } catch (IllegalArgumentException e) {
        }

        try {
            SetBucketCORSRequest corsRequest = new SetBucketCORSRequest("bucketName");
            CORSRule corsRule = new CORSRule();
            corsRule.addAllowedMethod("GET");
            corsRule.addAllowedHeader("*");
            corsRule.setAllowedOrigins(new ArrayList<String>(Arrays.asList("*", "", null)));
            corsRequest.addCorsRule(corsRule);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            SetBucketCORSRequest corsRequest = new SetBucketCORSRequest("bucketName");
            CORSRule corsRule = new CORSRule();
            corsRule.setAllowedMethods(new ArrayList<String>(Arrays.asList("GET", "")));
            corsRule.addAllowedHeader("*");
            corsRule.setAllowedOrigins(new ArrayList<String>(Arrays.asList("*")));
            corsRequest.addCorsRule(corsRule);
            Assert.fail("not allowed method, should be failed here.");
        } catch (IllegalArgumentException e) {
            // expected exception.
        }

        try {
            SetBucketCORSRequest corsRequest = new SetBucketCORSRequest("bucketName");
            CORSRule corsRule = new CORSRule();
            corsRule.setAllowedMethods(new ArrayList<String>(Arrays.asList("GET", null)));
            corsRule.addAllowedHeader("*");
            corsRule.setAllowedOrigins(new ArrayList<String>(Arrays.asList("*")));
            corsRequest.addCorsRule(corsRule);
            Assert.fail("not allowed method, should be failed here.");
        } catch (IllegalArgumentException e) {
            // expected exception.
        }

    }

    @Test
    public void testSSEAlgorithm() {
        assertEquals(SSEAlgorithm.AES256, SSEAlgorithm.getDefault());
        assertEquals("AES256", SSEAlgorithm.AES256.getAlgorithm());
        assertEquals(null, SSEAlgorithm.fromString(null));
        assertEquals(SSEAlgorithm.KMS, SSEAlgorithm.fromString("KMS"));

        try {
            SSEAlgorithm sse = SSEAlgorithm.fromString("UN");
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testServerSideEncryptionByDefault() {
        ServerSideEncryptionByDefault sseDefault = new ServerSideEncryptionByDefault()
                .withSSEAlgorithm(SSEAlgorithm.KMS)
                .withKMSMasterKeyID("id");

        assertEquals("KMS", sseDefault.getSSEAlgorithm());
        assertEquals("id", sseDefault.getKMSMasterKeyID());

        sseDefault.setSSEAlgorithm(SSEAlgorithm.AES256);
        assertEquals("AES256", sseDefault.getSSEAlgorithm());

        sseDefault = new ServerSideEncryptionByDefault(SSEAlgorithm.KMS);
        assertEquals("KMS", sseDefault.getSSEAlgorithm());
    }

    @Test
    public void testBucket() {
        Bucket bucket = new Bucket();
        bucket.setName("bucket-name");
        assertEquals("bucket-name", bucket.getName());

        bucket.setStorageClass(null);
        assertFalse(bucket.toString().contains("storageClass="));

        bucket.setStorageClass(StorageClass.IA);
        assertTrue(bucket.toString().contains("storageClass="));

        try {
            StorageClass.parse("UN");
        } catch (Exception e) {}
    }

    @Test
    public void testDataRedundancyType() {
        assertEquals(DataRedundancyType.LRS, DataRedundancyType.parse("LRS"));
        assertEquals(DataRedundancyType.ZRS, DataRedundancyType.parse("ZRS"));

        try {
            DataRedundancyType type = DataRedundancyType.parse("ERROR");
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testOwner() {
        Owner owner = new Owner();
        assertEquals(0, owner.hashCode());

        owner.setId("id");
        owner.setDisplayName("name");
        assertFalse(owner.toString().isEmpty());
        assertEquals("id".hashCode(), owner.hashCode());

        Owner owner1 = new Owner("id1", "name");
        assertFalse(owner.equals(owner1));
        assertFalse(owner.equals(""));

        owner.setDisplayName(null);
        owner.setId(null);
        owner1.setDisplayName(null);
        owner1.setId(null);
        assertEquals(true, owner.equals(owner1));

        owner.setDisplayName("name");
        owner.setId(null);
        owner1.setDisplayName(null);
        owner1.setId(null);
        assertEquals(false, owner.equals(owner1));

        owner.setDisplayName("name");
        owner.setId("id");
        owner1.setDisplayName(null);
        owner1.setId(null);
        assertEquals(false, owner.equals(owner1));

        owner.setDisplayName("name");
        owner.setId("id");
        owner1.setDisplayName("name");
        owner1.setId(null);
        assertEquals(false, owner.equals(owner1));
    }
    @Test
    public void testLifecycleRule() {
        LifecycleRule.AbortMultipartUpload abortMultipartUpload = new LifecycleRule.AbortMultipartUpload(10);
        assertEquals(10, abortMultipartUpload.getExpirationDays());
        Date date = new Date();
        abortMultipartUpload = new LifecycleRule.AbortMultipartUpload(date);
        assertEquals(date, abortMultipartUpload.getCreatedBeforeDate());
        abortMultipartUpload = new LifecycleRule.AbortMultipartUpload().withExpirationDays(11).withCreatedBeforeDate(date);
        assertEquals(11, abortMultipartUpload.getExpirationDays());
        assertEquals(date, abortMultipartUpload.getCreatedBeforeDate());

        Integer expirationDays = new Integer(10);
        LifecycleRule.StorageTransition storageTransition = new LifecycleRule.StorageTransition(expirationDays, StorageClass.IA);
        assertEquals(expirationDays, storageTransition.getExpirationDays());
        storageTransition = new LifecycleRule.StorageTransition(date, StorageClass.IA);
        assertEquals(date, storageTransition.getCreatedBeforeDate());
        storageTransition = new LifecycleRule.StorageTransition().withExpirationDays(expirationDays)
                .withCreatedBeforeDate(date).withStrorageClass(StorageClass.IA);
        assertEquals(date, storageTransition.getCreatedBeforeDate());
        assertEquals(expirationDays, storageTransition.getExpirationDays());
        assertEquals(StorageClass.IA, storageTransition.getStorageClass());

        LifecycleRule rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled,
                10, abortMultipartUpload);
        assertEquals(abortMultipartUpload, rule.getAbortMultipartUpload());
        assertEquals(10, rule.getExpirationDays());

        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled,
                date, abortMultipartUpload);
        assertEquals(abortMultipartUpload, rule.getAbortMultipartUpload());
        assertEquals(date, rule.getExpirationTime());

        List<LifecycleRule.StorageTransition> storageTransitions = null;
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, 10, storageTransitions);
        assertEquals(0, rule.getStorageTransition().size());
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, date, storageTransitions);
        assertEquals(0, rule.getStorageTransition().size());
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, 10, abortMultipartUpload, storageTransitions);
        assertEquals(0, rule.getStorageTransition().size());
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, date, abortMultipartUpload, storageTransitions);
        assertEquals(0, rule.getStorageTransition().size());
        assertEquals(false, rule.hasStorageTransition());

        storageTransitions = new ArrayList<LifecycleRule.StorageTransition>();
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, 10, storageTransitions);
        assertEquals(0, rule.getStorageTransition().size());
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, date, storageTransitions);
        assertEquals(0, rule.getStorageTransition().size());
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, 10, abortMultipartUpload, storageTransitions);
        assertEquals(0, rule.getStorageTransition().size());
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, date, abortMultipartUpload, storageTransitions);
        assertEquals(0, rule.getStorageTransition().size());
        assertEquals(false, rule.hasStorageTransition());

        storageTransitions.add(storageTransition);
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, 10, storageTransitions);
        assertEquals(storageTransitions, rule.getStorageTransition());
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, date, storageTransitions);
        assertEquals(storageTransitions, rule.getStorageTransition());
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, 10, abortMultipartUpload, storageTransitions);
        assertEquals(storageTransitions, rule.getStorageTransition());
        rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled, date, abortMultipartUpload, storageTransitions);
        assertEquals(storageTransitions, rule.getStorageTransition());
        assertEquals(true, rule.hasStorageTransition());

        rule.setStorageTransition(null);
        assertEquals(false, rule.hasStorageTransition());

        Map<String, String> tags = new HashMap<String, String>();
        rule.setTags(null);
        assertEquals(false, rule.hasTags());
        rule.setTags(tags);
        assertEquals(false, rule.hasTags());
        rule.addTag("key", "value");
        assertEquals(true, rule.hasTags());
    }

    @Test
    public void testSetBucketLifecycleRequest() {
        SetBucketLifecycleRequest request = new SetBucketLifecycleRequest("bucket");

        try {
            request.setLifecycleRules(null);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            List<LifecycleRule> lifecycleRules = new ArrayList<LifecycleRule>();
            request.setLifecycleRules(lifecycleRules);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            List<LifecycleRule> lifecycleRules = new ArrayList<LifecycleRule>();
            for (int i = 0; i < 1001; i++)
                lifecycleRules.add(new LifecycleRule());
            request.setLifecycleRules(lifecycleRules);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            List<LifecycleRule> lifecycleRules = new ArrayList<LifecycleRule>();
            for (int i = 0; i < 3; i++)
                lifecycleRules.add(new LifecycleRule());
            request.setLifecycleRules(lifecycleRules);
            Assert.assertEquals(3, request.getLifecycleRules().size());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            Date date = new Date();
            LifecycleRule.AbortMultipartUpload abortMultipartUpload = new LifecycleRule.AbortMultipartUpload(date);
            LifecycleRule rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Unknown,
                    10, abortMultipartUpload);
            request.AddLifecycleRule(rule);
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            LifecycleRule.NoncurrentVersionExpiration expiration = new LifecycleRule.NoncurrentVersionExpiration();
            expiration.setNoncurrentDays(1);
            LifecycleRule rule = new LifecycleRule("id", "prefix", LifecycleRule.RuleStatus.Enabled);
            rule.setNoncurrentVersionExpiration(expiration);
            request.AddLifecycleRule(rule);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            request.AddLifecycleRule(null);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testBucketProcess() {
        BucketProcess process = new BucketProcess(null);
        assertEquals(null, process.getImageProcess());

        ImageProcess imageProcess = new ImageProcess("compliedHost", false, "", "");
        process.setImageProcess(imageProcess);
        assertEquals(imageProcess, process.getImageProcess());
    }


    @SuppressWarnings("deprecation")
    @Test
    public void testBucketReferer() {
        BucketReferer refer = new BucketReferer();
        assertEquals(true, refer.allowEmpty());

        List<String> refererList = new ArrayList<String>();
        refererList.add("1");
        refererList.add("2");

        assertEquals(0, refer.getRefererList().size());
        refer.setRefererList(refererList);
        assertEquals(refererList, refer.getRefererList());

        refer.setRefererList(null);
        assertEquals(0, refer.getRefererList().size());

        refer.setRefererList(refererList);
        assertEquals(2, refer.getRefererList().size());
        refererList.clear();
        refer.setRefererList(refererList);
        assertEquals(0, refer.getRefererList().size());
    }

    @Test
    public void testBucketVersioningConfiguration() {
        BucketVersioningConfiguration conf = new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED);
        assertTrue(conf.getStatus().equals(BucketVersioningConfiguration.SUSPENDED));

        conf = new BucketVersioningConfiguration().withStatus(BucketVersioningConfiguration.ENABLED);
        assertTrue(conf.getStatus().equals(BucketVersioningConfiguration.ENABLED));
    }


    @Test
    public void testSetBucketWebsiteRequest() {
        SetBucketWebsiteRequest request = new SetBucketWebsiteRequest("bucket");

        try {
            request.setRoutingRules(null);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            List<RoutingRule> routingRules = new ArrayList<RoutingRule>();
            request.setRoutingRules(routingRules);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            List<RoutingRule> routingRules = new ArrayList<RoutingRule>();

            request = new SetBucketWebsiteRequest(bucketName);
            RoutingRule rule = new RoutingRule();
            rule.setNumber(2);
            rule.getCondition().setKeyPrefixEquals("~!@#$%^&*()-_=+|\\[]{}<>,./?`~");
            rule.getCondition().setHttpErrorCodeReturnedEquals(404);
            rule.getRedirect().setRedirectType(RoutingRule.RedirectType.Mirror);
            rule.getRedirect().setMirrorURL("http://oss-test.aliyun-inc.com/mirror-test/");
            rule.getRedirect().setMirrorSecondaryURL(null);
            rule.getRedirect().setMirrorProbeURL(null);
            rule.getRedirect().setPassQueryString(null);
            rule.getRedirect().setPassOriginalSlashes(null);

            routingRules.add(rule);
            request.setRoutingRules(routingRules);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
        }
    }

    @Test
    public void testRoutingRule() {
        RoutingRule.Condition condition = new RoutingRule.Condition();
        Integer codeReturned = new Integer(200);

        condition.setHttpErrorCodeReturnedEquals(codeReturned);
        condition.setHttpErrorCodeReturnedEquals(null);
        assertEquals(codeReturned, condition.getHttpErrorCodeReturnedEquals());

        try {
            condition.setHttpErrorCodeReturnedEquals(-1);
            assertTrue(false);
        } catch (Exception e) {
            assertEquals(codeReturned, condition.getHttpErrorCodeReturnedEquals());
        }

        try {
            RoutingRule.RedirectType.parse("UN");
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            RoutingRule.Protocol.parse("UN");
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        RoutingRule.Redirect redirect = new RoutingRule.Redirect();
        Integer httpRedirectCode = new Integer(303);
        redirect.setHttpRedirectCode(httpRedirectCode);
        redirect.setHttpRedirectCode(null);
        assertEquals(httpRedirectCode, redirect.getHttpRedirectCode());

        try {
            redirect.setHttpRedirectCode(200);
            assertTrue(false);
        } catch (Exception e) {
            assertEquals(httpRedirectCode, redirect.getHttpRedirectCode());
        }

        try {
            redirect.setHttpRedirectCode(400);
            assertTrue(false);
        } catch (Exception e) {
            assertEquals(httpRedirectCode, redirect.getHttpRedirectCode());
        }

        redirect = new RoutingRule.Redirect();
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}

        redirect.setHostName("hostname");
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}
        redirect.setProtocol(RoutingRule.Protocol.Https);
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}
        redirect.setReplaceKeyWith("key");
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}
        redirect.setReplaceKeyPrefixWith("prefix");
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}
        redirect.setHttpRedirectCode(303);
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}
        redirect.setMirrorURL("url");
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}

        redirect.setRedirectType(RoutingRule.RedirectType.External);
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}

        redirect.setMirrorURL(null);
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}

        redirect.setRedirectType(RoutingRule.RedirectType.Mirror);
        redirect.setMirrorURL(null);
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}

        redirect.setRedirectType(RoutingRule.RedirectType.Mirror);
        redirect.setMirrorURL("url");
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}

        redirect.setRedirectType(RoutingRule.RedirectType.Mirror);
        redirect.setMirrorURL("http://adbcd");
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}

        redirect.setRedirectType(RoutingRule.RedirectType.Mirror);
        redirect.setMirrorURL("http://adbcd/");
        try {
            redirect.ensureRedirectValid();
        } catch (Exception e) {}


        RoutingRule rule = new RoutingRule();
        rule.setRedirect(null);
        rule.setNumber(null);
        try {
            rule.ensureRoutingRuleValid();
        } catch (Exception e) {}

        rule.setNumber(new Integer(-1));
        try {
            rule.ensureRoutingRuleValid();
        } catch (Exception e) {}
    }

    @Test
    public void testRoutingRuleMoreEnsureBranch() {
        try {
            RoutingRule.Redirect redirect = new RoutingRule.Redirect();
            redirect.setProtocol(RoutingRule.Protocol.Https);
            redirect.ensureRedirectValid();
        } catch (IllegalArgumentException e) {}

        try {
            RoutingRule.Redirect redirect = new RoutingRule.Redirect();
            redirect.setReplaceKeyWith("key");
            redirect.ensureRedirectValid();
        }catch (IllegalArgumentException e) {}


        try {
            RoutingRule.Redirect redirect = new RoutingRule.Redirect();
            redirect.setReplaceKeyPrefixWith("prefix");
            redirect.ensureRedirectValid();
        }catch (IllegalArgumentException e) {}



        try {
            RoutingRule.Redirect redirect = new RoutingRule.Redirect();
            redirect.setMirrorURL("url");
            redirect.ensureRedirectValid();
        }catch (IllegalArgumentException e) {}


        try {
            RoutingRule.Redirect redirect = new RoutingRule.Redirect();
            redirect.setHttpRedirectCode(303);
            redirect.ensureRedirectValid();
        }catch (IllegalArgumentException e) {}


        try {
            RoutingRule.Redirect redirect = new RoutingRule.Redirect();
            redirect.setHostName("test-host");
            redirect.setRedirectType(RoutingRule.RedirectType.Mirror);
            redirect.ensureRedirectValid();
        } catch (IllegalArgumentException e) {}

        try {
            RoutingRule.Redirect redirect = new RoutingRule.Redirect();
            redirect.setHostName("test-host");
            redirect.setRedirectType(RoutingRule.RedirectType.Mirror);
            redirect.setMirrorURL("https://123");
            redirect.ensureRedirectValid();
            Assert.fail("should be failed here.");
        } catch (IllegalArgumentException e) {}

        try {
            RoutingRule.Redirect redirect = new RoutingRule.Redirect();
            redirect.setHostName("test-host");
            redirect.setRedirectType(RoutingRule.RedirectType.Mirror);
            redirect.setMirrorURL("https://123/");
            redirect.ensureRedirectValid();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAccessControlList() {
        Grant grant = new Grant(GroupGrantee.AllUsers, Permission.Read);
        AccessControlList acl = new AccessControlList();

        try {
            acl.grantPermission(null, null);
            assertTrue(false);
        }catch (Exception e) {
            assertTrue(true);
        }

        try {
            acl.grantPermission(GroupGrantee.AllUsers, null);
            assertTrue(false);
        }catch (Exception e) {
            assertTrue(true);
        }

        acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
        assertEquals(1, acl.getGrants().size());
        assertEquals(true, acl.getGrants().contains(grant));

        try {
            acl.revokeAllPermissions(null);
            assertTrue(false);
        }catch (Exception e) {
            assertTrue(true);
        }

        acl.revokeAllPermissions(GroupGrantee.AllUsers);
    }

    @Test
    public void testBucketInfo() {
        BucketInfo info = new BucketInfo();

        try {
            info.grantPermission(null, null);
            assertTrue(false);
        }catch (Exception e) {
            assertTrue(true);
        }

        try {
            info.grantPermission(GroupGrantee.AllUsers, null);
            assertTrue(false);
        }catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testGrant() {
        Grant grant = null;
        Grant grant1 = null;
        try {
            grant = new Grant(null, null);
        } catch (Exception e) {
        }
        assertEquals(null, grant);

        try {
            grant = new Grant(GroupGrantee.AllUsers, null);
        } catch (Exception e) {
        }
        assertEquals(null, grant);

        grant = new Grant(GroupGrantee.AllUsers, Permission.Read);
        grant1 = new Grant(GroupGrantee.AllUsers, Permission.FullControl);

        assertEquals(false, grant.equals(""));
        assertEquals(false, grant.equals(grant1));
        assertEquals(true, grant.equals(new Grant(GroupGrantee.AllUsers, Permission.Read)));

        assertEquals(false, grant.toString().isEmpty());
    }

    @Test
    public void testReplicationRule() {
        ReplicationRule replicationRule = new ReplicationRule();
        replicationRule.setReplicationStatus(ReplicationStatus.Doing);
        assertEquals(ReplicationStatus.Doing, replicationRule.getReplicationStatus());

        List<String> objectPrefixList = new ArrayList<String>();
        replicationRule.setObjectPrefixList(null);
        assertEquals(0, replicationRule.getObjectPrefixList().size());

        replicationRule.setObjectPrefixList(objectPrefixList);
        assertEquals(0, replicationRule.getObjectPrefixList().size());

        objectPrefixList.add("prefix");
        replicationRule.setObjectPrefixList(objectPrefixList);
        assertEquals(objectPrefixList, replicationRule.getObjectPrefixList());

        List<AddBucketReplicationRequest.ReplicationAction> replicationActionList = new ArrayList<AddBucketReplicationRequest.ReplicationAction>();
        replicationRule.setReplicationActionList(null);
        assertEquals(0, replicationRule.getReplicationActionList().size());

        replicationRule.setReplicationActionList(replicationActionList);
        assertEquals(0, replicationRule.getReplicationActionList().size());

        replicationActionList.add(AddBucketReplicationRequest.ReplicationAction.ALL);
        replicationRule.setReplicationActionList(replicationActionList);
        assertEquals(replicationActionList, replicationRule.getReplicationActionList());
    }

    @Test
    public void testSetBucketVersioningRequest() {
        BucketVersioningConfiguration configuration = new BucketVersioningConfiguration();
        SetBucketVersioningRequest request = new SetBucketVersioningRequest("bucket", configuration);
        assertEquals(configuration, request.getVersioningConfiguration());

        request = new SetBucketVersioningRequest("bucket", null);
        assertEquals(null, request.getVersioningConfiguration());

        request.setVersioningConfiguration(configuration);
        assertEquals(configuration, request.getVersioningConfiguration());

        request = new SetBucketVersioningRequest("bucket", null).withVersioningConfiguration(configuration);
        assertEquals(configuration, request.getVersioningConfiguration());
    }

    @Test
    public void testRevokeAllPermissions() {
        AccessControlList accessControlList = new AccessControlList();
        Assert.assertEquals(0, accessControlList.getGrants().size());

        accessControlList.grantPermission(GroupGrantee.AllUsers, Permission.Read);
        Assert.assertEquals(1, accessControlList.getGrants().size());

        accessControlList.revokeAllPermissions(GroupGrantee.AllUsers);
        Assert.assertEquals(0, accessControlList.getGrants().size());
    }

    @Test
    public void testParseAsyncFetchTaskStateWrong() {
        try {
            AsyncFetchTaskState.parse("wrong-value");
        } catch (IllegalArgumentException e) {
            // expected exception.
        }
    }

    @Test
    public void testBucketConstruction() {
        Bucket bucket = new Bucket("abc");
        Assert.assertEquals("abc", bucket.getName());
    }

}
