package com.obs.test.buckets;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.SSEAlgorithmEnum;
import com.obs.services.model.SetBucketEncryptionRequest;
import com.obs.test.TestTools;
import com.obs.test.tools.PrepareTestBucket;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Locale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class BucketEncryptionTest {
    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();

    @Test
    public void test_set_bucket_encryption_with_kmsID_and_delete() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        BucketEncryption encryption = new BucketEncryption();
        encryption.setKmsKeyId(TestTools.getKMSID());
        encryption.setSseAlgorithm(SSEAlgorithmEnum.KMS);
        SetBucketEncryptionRequest request = new SetBucketEncryptionRequest(bucketName, encryption);
        HeaderResponse set_response = obsClient.setBucketEncryption(request);
        assertEquals(200, set_response.getStatusCode());

        BucketEncryption encryption_result = obsClient.getBucketEncryption(bucketName);
        assertEquals(200, encryption_result.getStatusCode());
        assertEquals(encryption.getKmsKeyId(), encryption_result.getKmsKeyId());
        assertEquals(encryption.getSseAlgorithm(), encryption_result.getSseAlgorithm());

        HeaderResponse delete_result = obsClient.deleteBucketEncryption(bucketName);
        assertEquals(204, delete_result.getStatusCode());
        try {
            obsClient.getBucketEncryption(bucketName);
            fail("No exception");
        } catch (ObsException e) {
            assertEquals(404, e.getResponseCode());
        }
        delete_result = obsClient.deleteBucketEncryption(bucketName);
        assertEquals(204, delete_result.getStatusCode());
    }

    @Test
    public void test_set_bucket_encryption_without_kmsID() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        BucketEncryption encryption = new BucketEncryption();
        encryption.setSseAlgorithm(SSEAlgorithmEnum.KMS);
        SetBucketEncryptionRequest request = new SetBucketEncryptionRequest(bucketName, encryption);

        HeaderResponse set_response = obsClient.setBucketEncryption(request);
        assertEquals(200, set_response.getStatusCode());

        BucketEncryption encryption_result = obsClient.getBucketEncryption(bucketName);
        assertEquals(200, encryption_result.getStatusCode());
        assertEquals(encryption.getKmsKeyId(), encryption_result.getKmsKeyId());
        assertEquals(encryption.getSseAlgorithm(), encryption_result.getSseAlgorithm());
    }

    @Test
    public void test_set_bucket_encryption_with_wrong_kmsID() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        BucketEncryption encryption = new BucketEncryption();
        encryption.setKmsKeyId("WrongKMSID");
        encryption.setSseAlgorithm(SSEAlgorithmEnum.KMS);
        SetBucketEncryptionRequest request = new SetBucketEncryptionRequest(bucketName, encryption);
        try {
            obsClient.setBucketEncryption(request);
            fail("No exception");
        } catch (ObsException e) {
            assertEquals(400, e.getResponseCode());
            assertTrue(e.getXmlMessage().contains("The configuration of bucket encryption " +
                    "contains illegal KMSMasterKeyID"));
        }
    }

}
