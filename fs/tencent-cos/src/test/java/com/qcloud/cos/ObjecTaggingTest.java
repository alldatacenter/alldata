package com.qcloud.cos;

import com.qcloud.cos.model.*;
import com.qcloud.cos.model.Tag.Tag;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import static org.junit.Assert.assertEquals;

public class ObjecTaggingTest extends AbstractCOSClientTest  {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }


    @Test
    public void testSetGetDelObjectTagging() throws IOException {
        String key = "tagging.txt";
        try {
            cosclient.putObject(bucket, key, "data");
            List<Tag> tags = new LinkedList<>();
            tags.add(new Tag("key", "value"));
            tags.add(new Tag("key-1", "value-1"));
            ObjectTagging objectTagging = new ObjectTagging(tags);
            SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(bucket, key, objectTagging);
            cosclient.setObjectTagging(setObjectTaggingRequest);
            GetObjectTaggingResult getObjectTaggingResult = cosclient.getObjectTagging(
                    new GetObjectTaggingRequest(bucket, key));
            assertEquals(getObjectTaggingResult.getTagSet(), tags);;
            cosclient.deleteObjectTagging(new DeleteObjectTaggingRequest(bucket, key));
            GetObjectTaggingResult getObjectTaggingResultSecond = cosclient.getObjectTagging(
                    new GetObjectTaggingRequest(bucket, key));
            List<Tag> resultTagSetSecond = getObjectTaggingResultSecond.getTagSet();
            assertEquals(resultTagSetSecond.size(), 0);
        } finally {
            cosclient.deleteObject(bucket, key);
        }
    }
}
