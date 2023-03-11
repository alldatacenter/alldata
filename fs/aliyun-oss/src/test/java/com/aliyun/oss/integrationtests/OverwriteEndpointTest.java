package com.aliyun.oss.integrationtests;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class OverwriteEndpointTest  extends TestBase {

    private String secondEndpoint;
    private OSS ossClient2;
    private String key;

    public void setUp() throws Exception {
        super.setUp();
        if (TestConfig.OSS_TEST_ENDPOINT.indexOf("oss-cn-hangzhou.aliyuncs.com") > 0) {
            secondEndpoint = "oss-cn-shanghai.aliyuncs.com";
        } else {
            secondEndpoint = "oss-cn-hangzhou.aliyuncs.com";
        }
        ossClient2 = new OSSClientBuilder().build(secondEndpoint, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        key = "test-key";
        ossClient.putObject(bucketName, key, new ByteArrayInputStream("".getBytes()));
    }

    @Test
    public void testOverwriteEndpoint() {
        try {
            ObjectAcl acl = ossClient.getObjectAcl(bucketName,key);
            Assert.assertEquals(ObjectPermission.Default, acl.getPermission());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            ObjectAcl acl = ossClient2.getObjectAcl(bucketName,key);
            Assert.fail("should not here");
        } catch (Exception e) {
            Assert.assertTrue( e instanceof OSSException);
            OSSException e1 = (OSSException)e;
            Assert.assertEquals(bucketName + "." + secondEndpoint, e1.getHostId());
        }

        try {
            GenericRequest request = new GenericRequest(bucketName, key);
            Assert.assertEquals(null, request.getEndpoint());
            request.setEndpoint(TestConfig.OSS_TEST_ENDPOINT);
            ObjectAcl acl = ossClient2.getObjectAcl(request);
            Assert.assertEquals(ObjectPermission.Default, acl.getPermission());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testInvalidEndpoint() {
        try {
            GenericRequest request = new GenericRequest(bucketName, key);
            request.setEndpoint("?invalid");
            ObjectAcl acl = ossClient2.getObjectAcl(request);
        } catch (Exception e) {
            Assert.assertTrue( e instanceof IllegalArgumentException);
        }
    }
}
