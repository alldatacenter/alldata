package com.qcloud.cos;

import static org.junit.Assert.*;

import com.qcloud.cos.model.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BucketLoggingTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void setGetBucketLoggingTest() {
        BucketLoggingConfiguration bucketLoggingConfiguration = new BucketLoggingConfiguration();
        bucketLoggingConfiguration.setDestinationBucketName(bucket);
        bucketLoggingConfiguration.setLogFilePrefix("logs");
        SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest =
                new SetBucketLoggingConfigurationRequest(bucket, bucketLoggingConfiguration);
        cosclient.setBucketLoggingConfiguration(setBucketLoggingConfigurationRequest);
        BucketLoggingConfiguration bucketLoggingConfiguration1 = cosclient.getBucketLoggingConfiguration(bucket);
        assertEquals(bucketLoggingConfiguration1.getDestinationBucketName(), bucketLoggingConfiguration1.getDestinationBucketName());
        assertEquals(bucketLoggingConfiguration.getLogFilePrefix(), bucketLoggingConfiguration1.getLogFilePrefix());
    }
}
