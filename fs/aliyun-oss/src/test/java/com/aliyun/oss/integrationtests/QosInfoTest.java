/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.integrationtests;

import com.aliyun.oss.model.GenericRequest;
import org.junit.Test;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.BucketQosInfo;
import com.aliyun.oss.model.UserQosInfo;
import com.aliyun.oss.model.SetBucketQosInfoRequest;
import junit.framework.Assert;

public class QosInfoTest extends TestBase {

    @Test
    public void testUserQosInfo() {
        try {
            UserQosInfo userQosInfo = ossClient.getUserQosInfo();
            Assert.assertEquals(userQosInfo.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertNotNull(userQosInfo.getRegion());
            Assert.assertNotNull(userQosInfo.getTotalUploadBw());
            Assert.assertNotNull(userQosInfo.getIntranetUploadBw());
            Assert.assertNotNull(userQosInfo.getExtranetUploadBw());
            Assert.assertNotNull(userQosInfo.getTotalDownloadBw());
            Assert.assertNotNull(userQosInfo.getIntranetDownloadBw());
            Assert.assertNotNull(userQosInfo.getExtranetDownloadBw());
            Assert.assertNotNull(userQosInfo.getTotalQps());
            Assert.assertNotNull(userQosInfo.getIntranetQps());
            Assert.assertNotNull(userQosInfo.getExtranetQps());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testBucketQosInfo() {
        try {
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalUploadBw(-1);
            bucketQosInfo.setIntranetUploadBw(2);
            bucketQosInfo.setExtranetUploadBw(2);
            bucketQosInfo.setTotalDownloadBw(-1);
            bucketQosInfo.setIntranetDownloadBw(-1);
            bucketQosInfo.setExtranetDownloadBw(-1);
            bucketQosInfo.setTotalQps(-1);
            bucketQosInfo.setIntranetQps(-1);
            bucketQosInfo.setExtranetQps(-1);

            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);

            BucketQosInfo result = ossClient.getBucketQosInfo(bucketName);
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(result.getTotalUploadBw(), bucketQosInfo.getTotalUploadBw());
            Assert.assertEquals(result.getIntranetUploadBw(), bucketQosInfo.getIntranetUploadBw());
            Assert.assertEquals(result.getExtranetUploadBw(), bucketQosInfo.getExtranetUploadBw());
            Assert.assertEquals(result.getTotalDownloadBw(), bucketQosInfo.getTotalDownloadBw());
            Assert.assertEquals(result.getIntranetDownloadBw(), bucketQosInfo.getIntranetDownloadBw());
            Assert.assertEquals(result.getExtranetDownloadBw(), bucketQosInfo.getExtranetDownloadBw());
            Assert.assertEquals(result.getIntranetQps(), bucketQosInfo.getIntranetQps());
            Assert.assertEquals(result.getExtranetQps(), bucketQosInfo.getExtranetQps());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucketQosInfo(bucketName);
        }
    }

    @Test
    public void testBucketQosInfoWithNoneArgs() {
        try {
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);

            // Should be return default setting -1.
            BucketQosInfo result = ossClient.getBucketQosInfo(new GenericRequest(bucketName));
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(result.getTotalUploadBw().intValue(), -1);
            Assert.assertEquals(result.getIntranetUploadBw().intValue(), -1);
            Assert.assertEquals(result.getExtranetUploadBw().intValue(), -1);
            Assert.assertEquals(result.getTotalDownloadBw().intValue(), -1);
            Assert.assertEquals(result.getIntranetDownloadBw().intValue(), -1);
            Assert.assertEquals(result.getExtranetDownloadBw().intValue(), -1);
            Assert.assertEquals(result.getIntranetQps().intValue(), -1);
            Assert.assertEquals(result.getExtranetQps().intValue(), -1);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucketQosInfo(new GenericRequest(bucketName));
        }
    }

    @Test
    public void testPutBucketQosInfoWithIllegalArgs() {
        UserQosInfo userQosInfo = null;
        try {
            userQosInfo = ossClient.getUserQosInfo();
            Assert.assertEquals(userQosInfo.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        // BucketQosInfo totalUploadBw > UserQosInfo totalUploadBw, should be failed.
        try {
            Integer totalUploadBw = userQosInfo.getTotalUploadBw() + 1;
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalUploadBw(totalUploadBw);
            SetBucketQosInfoRequest request = new SetBucketQosInfoRequest(bucketName);
            request.setBucketQosInfo(bucketQosInfo);
            ossClient.setBucketQosInfo(request);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }

        // intranetUploadBw > totalUploadBw, should be failed.
        try {
            Integer totalUploadBw = userQosInfo.getTotalUploadBw();
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalUploadBw(totalUploadBw);
            bucketQosInfo.setIntranetUploadBw(totalUploadBw + 1);
            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }

        // extranetUploadBw > totalUploadBw, should be failed.
        try {
            Integer totalUploadBw = userQosInfo.getTotalUploadBw();
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalUploadBw(totalUploadBw);
            bucketQosInfo.setExtranetUploadBw(totalUploadBw + 1);
            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }

        // BucketQosInfo totalDownloadBw > UserQosInfo totalDownloadBw, should be failed.
        try {
            Integer totalDownloadBw = userQosInfo.getTotalDownloadBw() + 1;
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalUploadBw(totalDownloadBw);
            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }

        // intranetDownloadBw > totalDownloadBw, should be failed.
        try {
            Integer totalDownloadBw = userQosInfo.getTotalDownloadBw();
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalDownloadBw(totalDownloadBw);
            bucketQosInfo.setIntranetDownloadBw(totalDownloadBw + 1);
            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }

        // extranetDownloadBw > totalDownloadBw, should be failed.
        try {
            Integer totalDownloadBw = userQosInfo.getTotalDownloadBw();
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalDownloadBw(totalDownloadBw);
            bucketQosInfo.setExtranetDownloadBw(totalDownloadBw + 1);
            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }

        // BucketQosInfo totalQps > UserQosInfo totalQps, should be failed.
        try {
            Integer totalQps = userQosInfo.getTotalQps() + 1;
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalUploadBw(totalQps);
            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }

        // intranetQps > totalQps, should be failed.
        try {
            Integer totalQps = userQosInfo.getTotalQps();
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalQps(totalQps);
            bucketQosInfo.setIntranetQps(totalQps + 1);
            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }

        // extranetQps > totalQps, should be failed.
        try {
            Integer totalQps = userQosInfo.getTotalQps();
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalQps(totalQps);
            bucketQosInfo.setExtranetQps(totalQps + 1);
            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
    }

}