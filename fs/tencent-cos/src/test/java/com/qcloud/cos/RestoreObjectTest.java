package com.qcloud.cos;


import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.StorageClass;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.qcloud.cos.model.RestoreObjectRequest;
import org.junit.Test;

public class RestoreObjectTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }
    
    @Test
    public void restoreObject() {
        InputStream input = new ByteArrayInputStream(new byte[10]);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(10);
        String key = "ut/aaa.txt";
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, input, objectMetadata);
        putObjectRequest.setStorageClass(StorageClass.Archive);
        cosclient.putObject(putObjectRequest);
        RestoreObjectRequest restoreObjectRequest = new RestoreObjectRequest(bucket, key, 1);
        cosclient.restoreObject(restoreObjectRequest);
        cosclient.deleteObject(bucket, key);

    }
}
