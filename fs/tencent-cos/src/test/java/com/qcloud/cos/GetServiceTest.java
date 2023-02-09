package com.qcloud.cos;

import java.util.List;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.region.Region;

public class GetServiceTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void testGetService() {
        if (!judgeUserInfoValid()) {
            return;
        }
        
        List<Bucket> buckets = cosclient.listBuckets();
        for (Bucket bucketElement : buckets) {
            String bucketName = bucketElement.getName();
            String bucketLocation = bucketElement.getLocation();
            if (bucketName.equals(bucket)) {
                assertEquals(clientConfig.getRegion().getRegionName(), bucketLocation);
                return;
            }
        }
        fail("GetService result not contain bucket: " + bucket);
    }
    
    @Test
    public void testGetServiceForNullRegion() {
        if (!judgeUserInfoValid()) {
            return;
        }
        
        Region oldRegion = clientConfig.getRegion();
        clientConfig.setRegion(null);
        
        try {
            List<Bucket> buckets = cosclient.listBuckets();
            for (Bucket bucketElement : buckets) {
                if (bucketElement.getName().equals(bucket)) {
                    assertEquals(oldRegion.getRegionName(), bucketElement.getLocation());
                    return;
                }
            }
            fail("GetService result not contain bucket: " + bucket);            
        } finally {
            clientConfig.setRegion(oldRegion);
        }

    }
}
