package com.qcloud.cos;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.BucketCrossOriginConfiguration;
import com.qcloud.cos.model.CORSRule;
import com.qcloud.cos.model.CORSRule.AllowedMethods;

import static org.junit.Assert.assertEquals;

public class CORSTest extends AbstractCOSClientTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void setBucketCORSTest() throws InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        BucketCrossOriginConfiguration bucketCORS = new BucketCrossOriginConfiguration();
        List<CORSRule> corsRules = new ArrayList<>();
        CORSRule corsRule = new CORSRule();
        corsRule.setId("set-bucket-cors-test");
        corsRule.setAllowedMethods(AllowedMethods.PUT, AllowedMethods.GET, AllowedMethods.HEAD);
        corsRule.setAllowedHeaders("x-cos-grant-full-control");
        corsRule.setAllowedOrigins("http://mail.qq.com", "http://www.qq.com",
                "http://video.qq.com");
        corsRule.setExposedHeaders("x-cos-request-id");
        corsRule.setMaxAgeSeconds(60);
        corsRules.add(corsRule);
        bucketCORS.setRules(corsRules);
        cosclient.setBucketCrossOriginConfiguration(bucket, bucketCORS);
        
        Thread.sleep(5000L);

        BucketCrossOriginConfiguration corsGet =
                cosclient.getBucketCrossOriginConfiguration(bucket);
        assertEquals(1, corsGet.getRules().size());
        CORSRule corsRuleGet = corsGet.getRules().get(0);
        assertEquals(3, corsRuleGet.getAllowedMethods().size());
        assertEquals(1, corsRuleGet.getAllowedHeaders().size());
        assertEquals(3, corsRuleGet.getAllowedOrigins().size());
        assertEquals(1, corsRuleGet.getExposedHeaders().size());
        assertEquals(60, corsRuleGet.getMaxAgeSeconds());
        
        cosclient.deleteBucketCrossOriginConfiguration(bucket);
    }
    
    @Test
    public void putAndGetNotExistBucketCORSTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        String bucketName = "not-exist-" + bucket;
        BucketCrossOriginConfiguration bucketCORS = new BucketCrossOriginConfiguration();
        List<CORSRule> corsRules = new ArrayList<>();
        bucketCORS.setRules(corsRules);
        try {
            cosclient.setBucketCrossOriginConfiguration(bucketName, bucketCORS);
        } catch (CosServiceException cse) {
            assertEquals(4, cse.getStatusCode() / 100);
        }
        try {
            cosclient.getBucketCrossOriginConfiguration(bucketName);
        } catch (CosServiceException cse) {
            assertEquals(404, cse.getStatusCode() / 100);
        }
    }
}
