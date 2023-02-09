package com.obs.test.objects;

import com.obs.services.ObsClient;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsObject;
import com.obs.test.TestTools;
import com.obs.test.tools.PropertiesTools;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ListObjectsWithEncodingTypeTest extends ListObjectTest {
    private static final File configFile = new File("./app/src/test/resource/test_data.properties");
    public static final ArrayList<Integer> illegalXml = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 127));

    @BeforeClass
    public static void prepareTestObjects() throws IOException {
        ListObjectTest.prepareTestObjects();
        String bucketName = PropertiesTools.getInstance(configFile).getProperties("bucketPrefix") + "list-objects";
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        for (int c : illegalXml) {
            obsClient.putObject(bucketName, "illegal_" + (char) c, new ByteArrayInputStream("Hello OBS".getBytes()));
        }
    }

    @Test
    public void test_list_objects_with_xml() throws IOException {
        String bucketName = PropertiesTools.getInstance(configFile).getProperties("bucketPrefix") + "list-objects";
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(bucketName);
        request.setEncodingType("url");
        ObjectListing objects = obsClient.listObjects(request);
        ArrayList<String> objectNames = new ArrayList<>();
        for (ObsObject object : objects.getObjects()) {
            objectNames.add(object.getObjectKey());
            objectNames.add(object.getObjectKey());
        }
        assertTrue(objects.getObjects().size() > 32);
        for (int c : illegalXml) {
            assertTrue(objectNames.contains("illegal_" + (char) c));
        }
    }

    @Test
    public void test_list_objects_with_prefix() throws IOException {
        String bucketName = PropertiesTools.getInstance(configFile).getProperties("bucketPrefix") + "list-objects";
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(bucketName);
        request.setEncodingType("url");
        request.setPrefix("illegal_\u0010");
        ObjectListing objects = obsClient.listObjects(request);
        for (ObsObject object : objects.getObjects()) {
            assertEquals("illegal_\u0010", object.getObjectKey());
        }
    }
}
