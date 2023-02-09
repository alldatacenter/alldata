package com.qcloud.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.qcloud.cos.model.BucketVersioningConfiguration;
import com.qcloud.cos.model.SetBucketVersioningConfigurationRequest;

public class BucketVersioningTest extends AbstractCOSClientTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void testBucketVersioningEnabled() {
        if (!judgeUserInfoValid()) {
            return;
        }
        BucketVersioningConfiguration bucketVersioningEnabled =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
        cosclient.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(bucket, bucketVersioningEnabled));
        
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        BucketVersioningConfiguration bucketVersioningRet =
                cosclient.getBucketVersioningConfiguration(bucket);
        assertEquals(BucketVersioningConfiguration.ENABLED, bucketVersioningRet.getStatus());
    }

    @Test
    public void testBucketVersioningSuspended() {
        if (!judgeUserInfoValid()) {
            return;
        }
        BucketVersioningConfiguration bucketVersioningEnabled =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED);
        cosclient.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(bucket, bucketVersioningEnabled));
        
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            fail(e.toString());
        }

        BucketVersioningConfiguration bucketVersioningRet =
                cosclient.getBucketVersioningConfiguration(bucket);
        assertEquals(BucketVersioningConfiguration.SUSPENDED, bucketVersioningRet.getStatus());
    }

}
