package com.qcloud.cos;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.qcloud.cos.model.BucketPolicy;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;

public class GetSetDelPolicyTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void setGetDelPolicyTest() {
        try {
            String policyText = String.format(
                    "{\"Statement\": [  {   \"Action\": [    \"name/cos:*\"   ],   \"Condition\": {    \"ip_equal\": {     \"qcs:ip\": [      \"10.1.1.0/24\"     ]    },    \"string_equal\": {     \"qcs:sourceVpc\": [      \"vpc-123456\"     ]    }   },   \"Effect\": \"deny\",   \"Principal\": {    \"qcs\": [     \"qcs::cam::anyone:anyone\"    ]   },   \"Resource\": [    \"qcs::cos:%s:uid/%s:%s/*\"   ]  } ], \"version\": \"2.0\"}",
                    region, appid, bucket);
            cosclient.setBucketPolicy(bucket, policyText);

            BucketPolicy bucketPolicy = cosclient.getBucketPolicy(bucket);
            assertNotNull(bucketPolicy.getPolicyText());
            assertFalse(bucketPolicy.getPolicyText().isEmpty());

            cosclient.deleteBucketPolicy(bucket);
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
