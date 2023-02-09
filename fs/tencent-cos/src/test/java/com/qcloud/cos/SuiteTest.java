package com.qcloud.cos;

import com.qcloud.cos.utils.CRC64Test;
import com.qcloud.cos.utils.UrlEncoderUtilsTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({AclTest.class, BatchDeleteTest.class, BucketReplicationTest.class,
        BucketVersioningTest.class, CORSTest.class, CreateDeleteHeadBucketTest.class,
        GeneratePresignedUrlTest.class, GetBucketLocationTest.class,
        ListObjectTest.class, ListVersionsTest.class, MultipartUploadTest.class,
        PutGetDelTest.class, PutGetLifeCycleConfigTest.class, PutObjectCopyTest.class, 
        SSECustomerTest.class, TransferManagerTest.class, BucketWebsiteTest.class, AppendObjectTest.class,
        UrlEncoderUtilsTest.class, CRC64Test.class, BucketInventoryTest.class, BucketLoggingTest.class, BucketTaggingTest.class,
        GetSetDelPolicyTest.class, ObjecTaggingTest.class, RestoreObjectTest.class,
	MediaBucketTest.class, MediaJobTest.class, MediaQueueTest.class, MediaTemplateTest.class,MediaWorkflowTest.class
        })
public class SuiteTest {
}
