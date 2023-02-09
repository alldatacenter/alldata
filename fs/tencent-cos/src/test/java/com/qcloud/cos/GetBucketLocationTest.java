package com.qcloud.cos;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.qcloud.cos.exception.CosServiceException;

public class GetBucketLocationTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }
    
    @Test
    public void getbucketLocationTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        String location = cosclient.getBucketLocation(bucket);
        assertEquals(clientConfig.getRegion().getRegionName(), location);
    }
    
    @Test
    public void getNotExistedbucketLocationTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        String bucketName = "not-exist-" + bucket;
        try {
            cosclient.getBucketLocation(bucketName);
        } catch (CosServiceException cse) {
            assertEquals(404, cse.getStatusCode());
        }
    }
    
}
