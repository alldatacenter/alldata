package com.qcloud.cos;

import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowListRequest;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowListResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MediaWorkflowTest extends AbstractCOSClientCITest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientCITest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientCITest.closeCosClient();
    }

    @Test
    public void mediaWorkflowTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        MediaWorkflowListRequest request = new MediaWorkflowListRequest();
        request.setBucketName(bucket);
        MediaWorkflowListResponse response = cosclient.describeWorkflow(request);
        List<MediaWorkflowObject> mediaWorkflowList = response.getMediaWorkflowList();
        if (mediaWorkflowList.size() != 0) {
            for (MediaWorkflowObject mediaWorkflowObject : mediaWorkflowList) {
                assertEquals(bucket,mediaWorkflowObject.getBucketId());
                assertFalse(mediaWorkflowObject.getName().isEmpty());
                assertFalse(mediaWorkflowObject.getState().isEmpty());
            }
        }

    }

}
