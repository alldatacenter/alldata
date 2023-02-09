package com.obs.test.objects;

import com.obs.services.ObsClient;
import com.obs.services.model.BaseObjectRequest;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RestoreTierEnum;
import com.obs.services.model.StorageClassEnum;
import com.obs.test.TestTools;
import com.obs.test.tools.PrepareTestBucket;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class RestoreObjectTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();

    @Test
    public void test_restore_object_001() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_restore_object_001";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setObjectStorageClass(StorageClassEnum.COLD);

        PutObjectRequest putRequest = new PutObjectRequest();
        putRequest.setObjectKey(objectKey);
        putRequest.setBucketName(bucketName);
        putRequest.setMetadata(metadata);
        putRequest.setInput(new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        PutObjectResult putResult = obsClient.putObject(putRequest);
        assertEquals(200, putResult.getStatusCode());

        RestoreObjectRequest request = new RestoreObjectRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        request.setRestoreTier(RestoreTierEnum.EXPEDITED);
        request.setDays(10);
        RestoreObjectRequest.RestoreObjectStatus result = obsClient.restoreObject(request);
        assertEquals(202, result.getStatusCode());
    }
}
