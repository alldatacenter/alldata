package com.qcloud.cos;

import com.qcloud.cos.model.BucketIntelligentTierConfiguration;
import com.qcloud.cos.model.SetBucketIntelligentTierConfigurationRequest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;



public class BucketIntelligentTest extends AbstractCOSClientTest{

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void setGetBucketIntelligentTest() {
        BucketIntelligentTierConfiguration bucketIntelligentTierConfiguration = new BucketIntelligentTierConfiguration();
        bucketIntelligentTierConfiguration.setStatus(BucketIntelligentTierConfiguration.ENABLED);
        bucketIntelligentTierConfiguration.setTransition(new BucketIntelligentTierConfiguration.Transition(30));
        SetBucketIntelligentTierConfigurationRequest setBucketIntelligentTierConfigurationRequest = new SetBucketIntelligentTierConfigurationRequest();
        setBucketIntelligentTierConfigurationRequest.setBucketName(bucket);
        setBucketIntelligentTierConfigurationRequest.setIntelligentTierConfiguration(bucketIntelligentTierConfiguration);
        cosclient.setBucketIntelligentTieringConfiguration(setBucketIntelligentTierConfigurationRequest);
        BucketIntelligentTierConfiguration bucketIntelligentTierConfiguration1 = cosclient.getBucketIntelligentTierConfiguration(bucket);
        Assert.assertEquals(bucketIntelligentTierConfiguration1.getStatus(), BucketIntelligentTierConfiguration.ENABLED);
        Assert.assertEquals(bucketIntelligentTierConfiguration1.getTransition().getDays(), 30);
    }
}
