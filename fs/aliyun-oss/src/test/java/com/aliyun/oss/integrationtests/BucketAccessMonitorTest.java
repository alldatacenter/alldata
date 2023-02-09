package com.aliyun.oss.integrationtests;

import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AccessMonitor;
import com.aliyun.oss.model.LifecycleRule;
import com.aliyun.oss.model.SetBucketLifecycleRequest;
import com.aliyun.oss.model.StorageClass;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BucketAccessMonitorTest extends TestBase {


    @Test
    public void testBucketAccessMonitor() {

        try {
            ossClient.putBucketAccessMonitor(bucketName, "Enabled");
            AccessMonitor config = ossClient.getBucketAccessMonitor(bucketName);
            Assert.assertEquals("Enabled", config.getStatus());

            ossClient.putBucketAccessMonitor(bucketName, "Disabled");
            config = ossClient.getBucketAccessMonitor(bucketName);
            Assert.assertEquals("Disabled", config.getStatus());

        } catch (Exception e1) {
            Assert.fail(e1.getMessage());
        }
    }

    @Test
    public void testBucketAccessMonitorException() {

        try {
            ossClient.putBucketAccessMonitor(bucketName, "Enabled");
            AccessMonitor config = ossClient.getBucketAccessMonitor(bucketName);
            Assert.assertEquals("Enabled", config.getStatus());

            LifecycleRule lifecycleRule = new LifecycleRule();
            List<LifecycleRule> lifecycleRuleList = new ArrayList<LifecycleRule>();
            SetBucketLifecycleRequest setBucketLifecycleRequest = new SetBucketLifecycleRequest(bucketName);

            LifecycleRule.StorageTransition storageTransition = new LifecycleRule.StorageTransition();
            storageTransition.setStorageClass(StorageClass.IA);
            storageTransition.setExpirationDays(30);
            storageTransition.setIsAccessTime(true);
            storageTransition.setReturnToStdWhenVisit(false);
            List<LifecycleRule.StorageTransition> storageTransitionList = new ArrayList<LifecycleRule.StorageTransition>();
            storageTransitionList.add(storageTransition);

            LifecycleRule.NoncurrentVersionStorageTransition noncurrentVersionStorageTransition = new LifecycleRule.NoncurrentVersionStorageTransition();
            noncurrentVersionStorageTransition.setStorageClass(StorageClass.IA);
            noncurrentVersionStorageTransition.setNoncurrentDays(30);
            noncurrentVersionStorageTransition.setIsAccessTime(true);
            noncurrentVersionStorageTransition.setReturnToStdWhenVisit(true);

            List<LifecycleRule.NoncurrentVersionStorageTransition> noncurrentVersionStorageTransitionList = new ArrayList<LifecycleRule.NoncurrentVersionStorageTransition>();
            noncurrentVersionStorageTransitionList.add(noncurrentVersionStorageTransition);

            lifecycleRule.setId("abdddddd");
            lifecycleRule.setPrefix("abc");
            lifecycleRule.setExpiredDeleteMarker(false);
            lifecycleRule.setStorageTransition(storageTransitionList);
            lifecycleRule.setNoncurrentVersionStorageTransitions(noncurrentVersionStorageTransitionList);
            lifecycleRule.setExpirationDays(40);
            lifecycleRuleList.add(lifecycleRule);
            setBucketLifecycleRequest.setLifecycleRules(lifecycleRuleList);

            ossClient.setBucketLifecycle(setBucketLifecycleRequest);

            boolean updateFlag = true;
            List<LifecycleRule>  rules = ossClient.getBucketLifecycle(bucketName);
            for(LifecycleRule rule : rules){
                if (rule.getStorageTransition() != null && !rule.getStorageTransition().isEmpty()) {
                    for(LifecycleRule.StorageTransition trans : rule.getStorageTransition()){
                        if("true".equals(trans.getIsAccessTime().toString())){
                            updateFlag = false;
                        }
                    }
                }
                if (rule.getNoncurrentVersionStorageTransitions() != null && !rule.getNoncurrentVersionStorageTransitions().isEmpty()) {
                    for(LifecycleRule.NoncurrentVersionStorageTransition trans : rule.getNoncurrentVersionStorageTransitions()){
                        if("true".equals(trans.getIsAccessTime())){
                            updateFlag = false;
                        }
                    }
                }
            }
            if(updateFlag){
                ossClient.putBucketAccessMonitor(bucketName, "Disabled");
                config = ossClient.getBucketAccessMonitor(bucketName);
                Assert.assertEquals("Disabled", config.getStatus());
            }else{
                try{
                    ossClient.putBucketAccessMonitor(bucketName, "Disabled");
                } catch (OSSException e){
                    Assert.assertEquals(e.getErrorCode(), "AccessMonitorDisableNotAllowed");
                }
            }
        } catch (Exception e1) {
            Assert.fail(e1.getMessage());
        }
    }
}