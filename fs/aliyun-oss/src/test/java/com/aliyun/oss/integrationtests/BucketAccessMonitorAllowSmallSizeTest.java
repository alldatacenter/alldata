package com.aliyun.oss.integrationtests;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.LifecycleRule;
import com.aliyun.oss.model.SetBucketLifecycleRequest;
import com.aliyun.oss.model.StorageClass;

import java.util.ArrayList;
import java.util.List;

public class BucketAccessMonitorAllowSmallSizeTest {
    public static OSS initOssClient() {
        String endpoint = "http://oss-cn-qingdao.aliyuncs.com";
        String accessKeyId = "";
        String accessKeySecret = "";
        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        return client;
    }

    public static void destroyOssClient(OSS client) {
        client.shutdown();
    }

    public static void main(String[] args) {
        String bucketName = "qingdao-kkdas-cm";

        OSS client = initOssClient();

        client.putBucketAccessMonitor(bucketName, "Enabled");

        SetBucketLifecycleRequest setBucketLifecycleRequest = new SetBucketLifecycleRequest(bucketName);

        List<LifecycleRule> lifecycleRuleList = new ArrayList<LifecycleRule>();
        LifecycleRule lifecycleRule = new LifecycleRule();
        LifecycleRule.StorageTransition storageTransition = new LifecycleRule.StorageTransition();
        storageTransition.setStorageClass(StorageClass.IA);
        storageTransition.setExpirationDays(30);
        storageTransition.setIsAccessTime(true);
        storageTransition.setReturnToStdWhenVisit(false);
        storageTransition.setAllowSmallFile(true);
        List<LifecycleRule.StorageTransition> storageTransitionList = new ArrayList<LifecycleRule.StorageTransition>();
        storageTransitionList.add(storageTransition);

        LifecycleRule.NoncurrentVersionStorageTransition noncurrentVersionStorageTransition = new LifecycleRule.NoncurrentVersionStorageTransition();
        noncurrentVersionStorageTransition.setStorageClass(StorageClass.IA);
        noncurrentVersionStorageTransition.setNoncurrentDays(30);
        noncurrentVersionStorageTransition.setIsAccessTime(true);
        noncurrentVersionStorageTransition.setReturnToStdWhenVisit(true);
        noncurrentVersionStorageTransition.setAllowSmallFile(false);
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
        client.setBucketLifecycle(setBucketLifecycleRequest);

        List<LifecycleRule> lifecycleRules = client.getBucketLifecycle(bucketName);
    }
}
