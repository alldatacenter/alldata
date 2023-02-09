package com.aliyun.oss.integrationtests;

import com.aliyun.oss.model.HeadObjectRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;

public class HeadObjectTest extends TestBase {

    @Test
    public void testHeadObject() {
        final String key = "head-object";
        final long inputStreanLength = 1024;

        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key,
                    genFixedLengthInputStream(inputStreanLength), null);
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);

            HeadObjectRequest headObjectRequest = new HeadObjectRequest(bucketName, key);
            List<String> matchingETags = new LinkedList<String>();
            matchingETags.add(putObjectResult.getETag());
            headObjectRequest.setMatchingETagConstraints(matchingETags);
            ObjectMetadata o = ossClient.headObject(headObjectRequest);
            Assert.assertEquals(o.getETag(), putObjectResult.getETag());

            headObjectRequest = new HeadObjectRequest(bucketName, key);
            List<String> nonmatchingEtags = new LinkedList<String>();
            nonmatchingEtags.add("nonmatching");
            headObjectRequest.setNonmatchingETagConstraints(nonmatchingEtags);
            o = ossClient.headObject(headObjectRequest);
            Assert.assertEquals(o.getETag(), putObjectResult.getETag());

            headObjectRequest = new HeadObjectRequest(bucketName, key);
            headObjectRequest.setModifiedSinceConstraint(new Date(System.currentTimeMillis() - 3600 * 1000));
            o = ossClient.headObject(headObjectRequest);
            Assert.assertEquals(o.getETag(), putObjectResult.getETag());

            headObjectRequest = new HeadObjectRequest(bucketName, key);
            headObjectRequest.setUnmodifiedSinceConstraint(new Date(System.currentTimeMillis() + 3600 * 1000));
            o = ossClient.headObject(headObjectRequest);
            Assert.assertEquals(o.getETag(), putObjectResult.getETag());

            o = ossClient.headObject(bucketName, key);
            Assert.assertEquals(o.getETag(), putObjectResult.getETag());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
