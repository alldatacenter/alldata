package com.qcloud.cos;

import com.qcloud.cos.model.ciModel.job.MediaJobObject;
import com.qcloud.cos.model.ciModel.job.MediaJobResponse;
import com.qcloud.cos.model.ciModel.job.MediaJobsRequest;
import com.qcloud.cos.model.ciModel.job.MediaListJobResponse;
import com.qcloud.cos.model.ciModel.queue.MediaListQueueResponse;
import com.qcloud.cos.model.ciModel.queue.MediaQueueRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MediaJobTest extends AbstractCOSClientCITest {

    public static final String TAG = "Transcode";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientCITest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientCITest.closeCosClient();
    }

    @Test
    public void describeMediaJobTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        MediaQueueRequest queueRequest = new MediaQueueRequest();
        queueRequest.setBucketName(bucket);
        MediaListQueueResponse queueResponse = cosclient.describeMediaQueues(queueRequest);
        if (queueResponse != null && queueResponse.getQueueList().size() != 0) {
            MediaJobsRequest request = new MediaJobsRequest();
            request.setBucketName(bucket);
            String queueId = queueResponse.getQueueList().get(0).getQueueId();
            request.setQueueId(queueId);
            request.setTag(TAG);
            MediaListJobResponse response = cosclient.describeMediaJobs(request);
            List<MediaJobObject> jobsDetail = response.getJobsDetailList();
            for (MediaJobObject mediaJobObject : jobsDetail) {
                request = new MediaJobsRequest();
                request.setBucketName(bucket);
                request.setJobId(mediaJobObject.getJobId());
                MediaJobResponse jobResponse = cosclient.describeMediaJob(request);
                System.out.println(jobResponse);
                assertEquals(queueId, jobResponse.getJobsDetail().getQueueId());
            }
        }
    }

    @Test
    public void describeMediaJobsTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        MediaQueueRequest queueRequest = new MediaQueueRequest();
        queueRequest.setBucketName(bucket);
        MediaListQueueResponse queueResponse = cosclient.describeMediaQueues(queueRequest);
        if (queueResponse != null && queueResponse.getQueueList().size() != 0) {
            MediaJobsRequest request = new MediaJobsRequest();
            request.setBucketName(bucket);
            String queueId = queueResponse.getQueueList().get(0).getQueueId();
            request.setQueueId(queueId);
            request.setTag(TAG);
            MediaListJobResponse response = cosclient.describeMediaJobs(request);
            List<MediaJobObject> jobsDetail = response.getJobsDetailList();
            for (MediaJobObject mediaJobObject : jobsDetail) {
                assertEquals(TAG, mediaJobObject.getTag());
                assertEquals(queueId, mediaJobObject.getQueueId());
            }
        }
    }

}
