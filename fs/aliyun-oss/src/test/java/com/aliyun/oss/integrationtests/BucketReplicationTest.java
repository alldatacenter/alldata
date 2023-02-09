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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AddBucketReplicationRequest.ReplicationAction;
import com.aliyun.oss.model.BucketList;
import com.aliyun.oss.model.BucketReplicationProgress;
import com.aliyun.oss.model.DeleteBucketReplicationRequest;
import com.aliyun.oss.model.GetBucketReplicationProgressRequest;
import com.aliyun.oss.model.ReplicationRule;
import com.aliyun.oss.model.ReplicationStatus;
import com.aliyun.oss.model.AddBucketReplicationRequest;

import static com.aliyun.oss.integrationtests.TestConfig.*;

public class BucketReplicationTest extends TestBase {
    static String targetBucketName = "java-sdk-test-qd-15";
    final String targetBucketLoc = "oss-cn-qingdao";
    static OSS replicationClient = null;

    public void setUp() throws Exception {
        super.setUp();
        targetBucketName = super.bucketName + "-bucket-replication";
        replicationClient = new OSSClientBuilder().build("oss-cn-qingdao.aliyuncs.com",
                OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET);
        replicationClient.createBucket(targetBucketName);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        replicationClient.shutdown();
    }

    public void testNormalAddBucketReplication() throws ParseException {
        final String bucketName = "test-bucket-set-replication";
        final String ruleId = "bucket-replication-rule-id";

        try {
            ossClient.createBucket(bucketName);
            
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setReplicationRuleID(ruleId);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            
            ossClient.addBucketReplication(request);
                        
            List<ReplicationRule> rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
                                    
            ReplicationRule r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationRuleID(), ruleId);
            Assert.assertEquals(r0.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(r0.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Starting);
            Assert.assertNull(r0.getObjectPrefixList());
            Assert.assertEquals(r0.getReplicationActionList().size(), 1);
            Assert.assertEquals(r0.getReplicationActionList().get(0), ReplicationAction.parse("ALL"));
            
            BucketReplicationProgress progress = ossClient.getBucketReplicationProgress(bucketName, ruleId);
            Assert.assertEquals(progress.getReplicationRuleID(), ruleId);
            Assert.assertEquals(progress.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(progress.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(progress.getReplicationStatus(), ReplicationStatus.Starting);
            Assert.assertEquals(progress.getHistoricalObjectProgress(), Float.valueOf(0));
            Assert.assertEquals(progress.isEnableHistoricalObjectReplication(), true);
            Assert.assertEquals(progress.getNewObjectProgress(), null);
                        
            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName, ruleId));
                        
            rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
            
            r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationRuleID(), ruleId);
            Assert.assertEquals(r0.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(r0.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Closing);
            
            List <String> locations = ossClient.getBucketReplicationLocation(bucketName);
            Assert.assertEquals(locations.size() > 0, true);
            
        } catch (OSSException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    public void testNormalAddBucketReplicationWithDefaultRuleID() throws ParseException {
        final String bucketName = "test-bucket-replication-default-ruleid";

        try {
            ossClient.createBucket(bucketName);
            
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            ossClient.addBucketReplication(request);
                        
            List<ReplicationRule> rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
            
            ReplicationRule r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationRuleID().length(), "d6a8bfe3-56f6-42dd-9e7f-b4301d99b0ed".length());
            Assert.assertEquals(r0.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(r0.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(r0.isEnableHistoricalObjectReplication(), true);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Starting);
            
            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName, r0.getReplicationRuleID()));
                        
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    public void testNormalAddBucketReplicationWithRuleID() throws ParseException {
        final String bucketName = "test-bucket-replication-ruleid-3";
        final String repRuleID = "~`!@#$%^&*()-_+=|\\[]{}<>:;\"',./?";

        try {
            ossClient.createBucket(bucketName);
                        
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setReplicationRuleID(repRuleID);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            request.setEnableHistoricalObjectReplication(false);
            ossClient.addBucketReplication(request);
                                    
            List<ReplicationRule> rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
                        
            ReplicationRule r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationRuleID(), repRuleID);
            Assert.assertEquals(r0.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(r0.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(r0.isEnableHistoricalObjectReplication(), false);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Starting);
            
            BucketReplicationProgress process = ossClient.getBucketReplicationProgress(bucketName, repRuleID);
            Assert.assertEquals(process.getReplicationRuleID(), repRuleID);
            Assert.assertEquals(process.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(process.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(process.getReplicationStatus(), ReplicationStatus.Starting);
            Assert.assertEquals(process.getHistoricalObjectProgress(), Float.valueOf(0));
            
            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName, repRuleID));
                        
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    public void testNormalAddBucketReplicationWithAction() throws ParseException {
        final String bucketName = "test-bucket-replication-action-10";

        try {
            ossClient.createBucket(bucketName);
            
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            
            List<String> prefixes = new ArrayList<String>();
            prefixes.add("image/");
            prefixes.add("video");
            request.setObjectPrefixList(prefixes);
            
            List<ReplicationAction> actions = new ArrayList<ReplicationAction>();
            actions.add(ReplicationAction.PUT);
            actions.add(ReplicationAction.DELETE);
            request.setReplicationActionList(actions);
            
            ossClient.addBucketReplication(request);
                        
            List<ReplicationRule> rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
            
            ReplicationRule r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationRuleID().length(), "d6a8bfe3-56f6-42dd-9e7f-b4301d99b0ed".length());
            Assert.assertEquals(r0.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(r0.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(r0.isEnableHistoricalObjectReplication(), true);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Starting);
            Assert.assertEquals(r0.getObjectPrefixList().size(), 2);
            Assert.assertEquals(r0.getReplicationActionList().size(), 2);
            
            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName, r0.getReplicationRuleID()));
        } catch (OSSException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    public void testNormalDeleteBucketReplication() throws ParseException {
        final String bucketName = "test-bucket-delete-replication";
        final String repRuleID = "test-replication-ruleid";

        try {
            ossClient.createBucket(bucketName);
            
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setReplicationRuleID(repRuleID);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            ossClient.addBucketReplication(request);
            
            List<ReplicationRule> rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
            ReplicationRule r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Starting);
            
            ossClient.deleteBucketReplication(bucketName, repRuleID);
            
            rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
            r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Closing);
            
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    public void testNormalDeleteBucketReplicationWithRuleID() throws ParseException {
        final String bucketName = "test-bucket-delete-replication-ruleid";
        final String repRuleID = "test-replication-ruleid";

        try {
            ossClient.createBucket(bucketName);
            
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setReplicationRuleID(repRuleID);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            ossClient.addBucketReplication(request);
            
            List<ReplicationRule> rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
            ReplicationRule r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Starting);
            
            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName).withReplicationRuleID(repRuleID));
            
            rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
            r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Closing);
            
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    public void testNormalGetBucketReplicationProgress() throws ParseException {
        final String bucketName = "test-bucket-get-replication-progress";
        final String repRuleID = "test-replication-progress-ruleid";
        
        try {
            ossClient.createBucket(bucketName);
            
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setReplicationRuleID(repRuleID);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            ossClient.addBucketReplication(request);
            
            List<ReplicationRule> rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
            ReplicationRule r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationRuleID(), repRuleID);
            Assert.assertEquals(r0.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(r0.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(r0.isEnableHistoricalObjectReplication(), true);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Starting);
            
            BucketReplicationProgress process = ossClient.getBucketReplicationProgress(bucketName, repRuleID);
            Assert.assertEquals(process.getReplicationRuleID(), repRuleID);
            Assert.assertEquals(process.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(process.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(process.getReplicationStatus(), ReplicationStatus.Starting);
            Assert.assertEquals(process.isEnableHistoricalObjectReplication(), true);
            Assert.assertEquals(process.getHistoricalObjectProgress(), Float.valueOf(0));
            Assert.assertEquals(process.getNewObjectProgress(), null);
                        
            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName, repRuleID));
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    public void testNormalGetBucketReplicationProgressWithDisableHistory() throws ParseException {
        final String bucketName = "test-bucket-replication-progress-disable-history";
        final String repRuleID = "test-replication-ruleid";
//        Date now = Calendar.getInstance().getTime();

        try {
            ossClient.createBucket(bucketName);

            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setReplicationRuleID(repRuleID);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            request.setEnableHistoricalObjectReplication(false);
            ossClient.addBucketReplication(request);

            List<ReplicationRule> rules = ossClient.getBucketReplication(bucketName);
            Assert.assertEquals(rules.size(), 1);
            ReplicationRule r0 = rules.get(0);
            Assert.assertEquals(r0.getReplicationRuleID(), repRuleID);
            Assert.assertEquals(r0.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(r0.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(r0.isEnableHistoricalObjectReplication(), false);
            Assert.assertEquals(r0.getReplicationStatus(), ReplicationStatus.Starting);

            BucketReplicationProgress process = ossClient
                    .getBucketReplicationProgress(new GetBucketReplicationProgressRequest(
                            bucketName).withReplicationRuleID(repRuleID));
            Assert.assertEquals(process.getReplicationRuleID(), repRuleID);
            Assert.assertEquals(process.getTargetBucketName(), targetBucketName);
            Assert.assertEquals(process.getTargetBucketLocation(), targetBucketLoc);
            Assert.assertEquals(process.getReplicationStatus(), ReplicationStatus.Starting);
            Assert.assertEquals(process.getHistoricalObjectProgress(), Float.valueOf(0));
            Assert.assertEquals(process.isEnableHistoricalObjectReplication(), false);
            Assert.assertEquals(process.getNewObjectProgress(), null);
            // Assert.assertEquals(diffSecond(process.getNewObjectProgress(), now) < 5, true);

            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(
                            bucketName, repRuleID));
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    public void testNormalGetBucketReplicationLocation() throws ParseException {
        final String bucketName = "test-bucket-replication-location";

        try {
            ossClient.createBucket(bucketName);
            
            List<String> locations = ossClient.getBucketReplicationLocation(bucketName);
            Assert.assertEquals(locations.size() > 0, true);
            
            for (String loc : locations) {
                System.out.println("loc:" + loc);
            }
            
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    // Negative
    @Test
    public void testUnormalSetBucketReplication() throws ParseException {
        final String bucketName = super.bucketName  + "-unormal-bucket-replication";
        final String ruleId = "bucket-replication-rule-id";

        try {
            ossClient.createBucket(bucketName);
            
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            request.setReplicationRuleID(ruleId);
            ossClient.addBucketReplication(request);
            
            try {
                ossClient.addBucketReplication(request);
                Assert.fail("Set bucket replication should not be successful.");
            } catch (OSSException e) {
                //Assert.assertEquals(e.getErrorCode(), "InvalidArgument");
                //Assert.assertEquals(e.getMessage().startsWith("Rule ID is not unique."), true);
            }
            
            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName, ruleId));
                        
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalSetBucketReplicationLocation() throws ParseException {
        final String bucketName = super.bucketName  + "-unormal-bucket-replication-loc";
        final String ruleId = "bucket-replication-rule-id";

        try {
            ossClient.createBucket(bucketName);
            
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation("oss-cn-zhengzhou");
            request.setReplicationRuleID(ruleId);
            
            try {
                ossClient.addBucketReplication(request);
                Assert.fail("Set bucket replication should not be successful.");
            } catch (OSSException e) {
                Assert.assertEquals(e.getErrorCode(), "InvalidTargetLocation");
                Assert.assertEquals(e.getMessage().startsWith("The target bucket you specified does not locate in the target location"), true);
            }
                                    
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalGetBucketReplication() throws ParseException {
        final String bucketName = super.bucketName  + "-unormal-get-bucket-replication";

        try {
            ossClient.createBucket(bucketName);
                     
            try {
                ossClient.getBucketReplication(bucketName);
                Assert.fail("Get bucket replication should not be successful.");
            } catch (OSSException e) {
                Assert.assertEquals(e.getErrorCode(), "NoSuchReplicationConfiguration");
                Assert.assertEquals(e.getMessage().startsWith("The bucket you specified does not have replication configuration"), true);
            }
              
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalGetBucketReplicationProgress() throws ParseException {
        final String bucketName = super.bucketName  + "-unormal-bucket-replication-progress";
        final String repRuleID = "test-replication-progress-ruleid";
        
        try {
            ossClient.createBucket(bucketName);
            
            try {
                ossClient.getBucketReplicationProgress(bucketName, repRuleID);
                Assert.fail("Get bucket replication should not be successful.");
            } catch (OSSException e) {
                Assert.assertEquals(e.getErrorCode(), "NoSuchReplicationRule");
                Assert.assertEquals(e.getMessage().startsWith("The BucketReplicationRule you specified does not exist"), true);
            }
            
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalGetBucketReplicationLocation() throws ParseException {
        final String bucketName = super.bucketName  + "-unormal-bucket-replication-location";

        try {
            ossClient.getBucketReplicationLocation(bucketName);
            Assert.fail("Get bucket replication location should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith("The specified bucket does not exist"));
        }
        
    }
    
    @Test
    public void testUnormalSetBucketReplicationInvalidParam() throws ParseException {
        final String bucketName = super.bucketName  + "-unormal-bucket-delete-replication-param";
        
        try {
            ossClient.createBucket(bucketName);
                     
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);

            try {
                ossClient.addBucketReplication(request);
                Assert.fail("Get bucket replication should not be successful.");
            } catch (NullPointerException e) {
            }
                        
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalDeleteBucketReplicationInvalidParam() throws ParseException {
        final String bucketName = super.bucketName  + "-unormal-bucket-delete-replication-param";
        final String ruleId = "bucket-replication-rule-id";
        
        try {
            ossClient.createBucket(bucketName);
                     
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            request.setReplicationRuleID(ruleId);
            ossClient.addBucketReplication(request);
                
            try {
                ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName));
                Assert.fail("Get bucket replication should not be successful.");
            } catch (NullPointerException e) {
            }
            
            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName, ruleId));
            
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalGetBucketReplicationProgressInvalidParam() throws ParseException {
        final String bucketName = super.bucketName  + "-unormal-bucket-replication-progress";
        final String ruleId = "bucket-replication-rule-id";
        
        try {
            ossClient.createBucket(bucketName);
                     
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setTargetBucketName(targetBucketName);
            request.setTargetBucketLocation(targetBucketLoc);
            request.setReplicationRuleID(ruleId);
            ossClient.addBucketReplication(request);
                
            try {
                ossClient.getBucketReplicationProgress(new GetBucketReplicationProgressRequest(bucketName));
                Assert.fail("Get bucket replication should not be successful.");
            } catch (NullPointerException e) {
            }
            
            ossClient.deleteBucketReplication(new DeleteBucketReplicationRequest(bucketName, ruleId));
            
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testParseStatusWrong() {
        try {
            ReplicationStatus.parse("wrong-status");
        } catch (IllegalArgumentException e) {
            // expected exception.
        }
    }

    @SuppressWarnings("unused")
    private long diffSecond(Date post, Date pre) {
        long diff = post.getTime() - pre.getTime();
        return diff / 1000;
    }
}
