package com.qcloud.cos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.exception.MultiObjectDeleteException;
import com.qcloud.cos.exception.MultiObjectDeleteException.DeleteError;
import com.qcloud.cos.model.BucketVersioningConfiguration;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.DeleteObjectsRequest.KeyVersion;
import com.qcloud.cos.model.DeleteObjectsResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.SetBucketVersioningConfigurationRequest;
import com.qcloud.cos.model.DeleteObjectsResult.DeletedObject;

import static org.junit.Assert.assertEquals;

public class BatchDeleteTest extends AbstractCOSClientTest {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BatchDeleteTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void batchDeleteAllExistFileForVersionEnabled() throws IOException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        BucketVersioningConfiguration bucketVersioningEnabled =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
        cosclient.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(bucket, bucketVersioningEnabled));

        Thread.sleep(5000L);
        
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket);
        ArrayList<KeyVersion> keyList = new ArrayList<>();

        long deleteFileCount = 30;
        for (long fileIndex = 0; fileIndex < deleteFileCount; ++fileIndex) {
            File localFile = buildTestFile(fileIndex * 1024);
            String key = "ut/" + localFile.getName();
            PutObjectResult putObjectResult = putObjectFromLocalFile(localFile, key);
            keyList.add(new KeyVersion(key, putObjectResult.getVersionId()));
        }
        deleteObjectsRequest.setKeys(keyList);

        DeleteObjectsResult deleteObjectsResult = cosclient.deleteObjects(deleteObjectsRequest);
        assertEquals(deleteFileCount, deleteObjectsResult.getDeletedObjects().size());
    }
    
    @Test
    public void batchDeletePartExistFileForVersionSuspended() throws IOException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        BucketVersioningConfiguration bucketVersioningEnabled =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED);
        cosclient.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(bucket, bucketVersioningEnabled));

        Thread.sleep(5000L);
        
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket);
        ArrayList<KeyVersion> keyList = new ArrayList<>();

        long deleteFileCount = 5;
        for (long fileIndex = 0; fileIndex < deleteFileCount; ++fileIndex) {
            File localFile = buildTestFile(fileIndex * 1024);
            String key = "ut/" + localFile.getName();
            PutObjectResult putObjectResult = putObjectFromLocalFile(localFile, key);
            keyList.add(new KeyVersion(key, "null"));
        }
        keyList.add(new KeyVersion("ut/not_exist_key.txt", "null"));
        deleteObjectsRequest.setKeys(keyList);

        try {
            DeleteObjectsResult deleteObjectsResult = cosclient.deleteObjects(deleteObjectsRequest);
            List<DeletedObject> deleteObjectResultArray = deleteObjectsResult.getDeletedObjects();
        } catch (MultiObjectDeleteException mde) {
            List<DeletedObject> deleteObjects = mde.getDeletedObjects();
            List<DeleteError> deleteErrors = mde.getErrors();
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }

    }
}
