package com.obs.test.objects;

import com.obs.services.ObsClient;
import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.ListMultipartUploadsRequest;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.MultipartUpload;
import com.obs.services.model.MultipartUploadListing;
import com.obs.services.model.VersionOrDeleteMarker;
import com.obs.test.TestTools;
import com.obs.test.tools.PropertiesTools;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ListObjectTest {
    private static final File configFile = new File("./app/src/test/resource/test_data.properties");
    private static final ArrayList<String> createdBuckets = new ArrayList<>();

    @BeforeClass
    public static void prepareTestObjects() throws IOException {
        String location = PropertiesTools.getInstance(configFile).getProperties("environment.location");
        String bucketName = PropertiesTools.getInstance(configFile).getProperties("bucketPrefix") + "list-objects";
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        CreateBucketRequest request = new CreateBucketRequest();
        request.setBucketName(bucketName);
        request.setBucketType(BucketTypeEnum.OBJECT);
        request.setLocation(location);
        HeaderResponse response = obsClient.createBucket(bucketName);
        assertEquals(200, response.getStatusCode());
        createdBuckets.add(bucketName);

    }

    @AfterClass
    public static void delete_buckets() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        TestTools.delete_buckets(obsClient, createdBuckets);
    }

    @Test
    public void test_list_objects() {

    }


}
