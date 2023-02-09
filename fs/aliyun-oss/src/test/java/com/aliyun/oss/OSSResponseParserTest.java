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

package com.aliyun.oss;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.internal.ResponseParsers;
import com.aliyun.oss.model.AccessControlList;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.BucketList;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.CopyObjectResult;
import com.aliyun.oss.model.Grant;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.MultipartUploadListing;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PartListing;
import com.aliyun.oss.model.PartSummary;
import com.aliyun.oss.model.Permission;
import com.aliyun.oss.utils.ResourceUtils;

/**
 * Testing OSSResponseParser class
 */
public class OSSResponseParserTest {

    // Test file name.
    private static final String FILE_FOLDER = "oss"; 

    /**
     * Gets test file stream.
     * **/
    private InputStream getInputStream(String filename) throws Exception {
        return ResourceUtils.getTestInputStream(FILE_FOLDER + "/" + filename);
    }
    
    @Test 
    public void testParserGetBucketLocation() throws Exception {

        String filename = "getBucketLocation.xml";
        InputStream in = getInputStream(filename);
        String location = ResponseParsers.parseGetBucketLocation(in);
        Assert.assertEquals("oss-cn-qingdao-a", location);
    }

    @Test
    public void testParseListObjects() throws Exception {

        String filename = "listObjects.xml";
        InputStream in = getInputStream(filename);

        ObjectListing objectListing = ResponseParsers.parseListObjects(in);

        Assert.assertFalse(objectListing.isTruncated());
        Assert.assertEquals("pacjux7y1b86pmtu7g8d6b7z-test-bucket",
                objectListing.getBucketName());
        Assert.assertNull(objectListing.getPrefix());
        Assert.assertNull(objectListing.getMarker());
        Assert.assertEquals(100, objectListing.getMaxKeys());
        Assert.assertNull(objectListing.getDelimiter());

        for (OSSObjectSummary summary : objectListing.getObjectSummaries()) {

            Assert.assertEquals("pacjux7y1b86pmtu7g8d6b7z-test-object",
                    summary.getKey());

            Assert.assertEquals(
                    DateUtil.parseIso8601Date("2012-02-09T01:49:38.000Z"),
                    summary.getLastModified());

            Assert.assertEquals(10, summary.getSize());
            Assert.assertEquals("9BF156C2C16BB90B9EC81C96FE37EF1B",
                    summary.getETag());
            Assert.assertEquals("Standard", summary.getStorageClass());

            Assert.assertEquals("51744", summary.getOwner().getId());
            Assert.assertEquals("51744", summary.getOwner().getDisplayName());
        }

        in.close();

    }

    @SuppressWarnings("deprecation")
	@Test
    public void testParseGetBucketAcl() throws Exception {

        String filename = "getBucketAcl.xml";
        InputStream in = getInputStream(filename);
        AccessControlList accessControlList = ResponseParsers.parseGetBucketAcl(in);

        Assert.assertEquals("51744", accessControlList.getOwner().getId());
        Assert.assertEquals("51744", accessControlList.getOwner().getDisplayName());
        Assert.assertEquals(Permission.FullControl, 
                ((Grant)(accessControlList.getGrants().toArray()[0])).getPermission());

        in.close();
    }

    @Test
    public void testParseListBucket() throws Exception {

        String filename = "listBucket.xml";
        InputStream in = getInputStream(filename);

        BucketList bucketList = ResponseParsers.parseListBucket(in);
        assertEquals(null, bucketList.getPrefix());
        assertEquals(null, bucketList.getMarker());
        assertEquals(null, bucketList.getMaxKeys());
        assertEquals(false, bucketList.isTruncated());
        assertEquals(null, bucketList.getNextMarker());
        List<Bucket> buckets = bucketList.getBucketList();
        
        Bucket bucket1 = buckets.get(0);
        Assert.assertEquals("51744", bucket1.getOwner().getId());
        Assert.assertEquals("51744", bucket1.getOwner().getDisplayName());
        Assert.assertEquals("pacjux7y1b86pmtu7g8d6b7z-test-bucket",
                bucket1.getName());
        Assert.assertEquals(DateUtil.parseIso8601Date("2012-02-09T01:49:38.000Z"),
                bucket1.getCreationDate());

        Bucket bucket2 = buckets.get(1);
        Assert.assertEquals("51744", bucket2.getOwner().getId());
        Assert.assertEquals("51744", bucket2.getOwner().getDisplayName());
        Assert.assertEquals("ganshumantest", bucket2.getName());
        Assert.assertEquals(DateUtil.parseIso8601Date("2012-02-09T06:38:47.000Z"),
                bucket2.getCreationDate());

        in.close();
 
        filename = "listBucketTruncated.xml";
        in = getInputStream(filename);
        bucketList = ResponseParsers.parseListBucket(in);
        assertEquals("asdasdasdasd", bucketList.getPrefix());
        assertEquals("asdasdasd", bucketList.getMarker());
        assertEquals(Integer.valueOf(1), bucketList.getMaxKeys());
        assertEquals(true, bucketList.isTruncated());
        assertEquals("asdasdasdasdasd", bucketList.getNextMarker());
        buckets = bucketList.getBucketList();
        bucket1 = buckets.get(0);
        Assert.assertEquals("51744", bucket1.getOwner().getId());
        Assert.assertEquals("51744", bucket1.getOwner().getDisplayName());
        Assert.assertEquals("asdasdasdasd", bucket1.getName());
        Assert.assertEquals("osslocation", bucket1.getLocation());
        Assert.assertEquals(DateUtil.parseIso8601Date("2014-05-15T11:18:32.000Z"),
           bucket1.getCreationDate());
        
        in.close();
    }

    @Test
    public void testCopyObjectResult() throws Exception {

        String filename = "copyObject.xml";
        InputStream in = getInputStream(filename);
        
        CopyObjectResult result = ResponseParsers.parseCopyObjectResult(in);
        Assert.assertEquals("4F62D1D6EF439E057D4BD20F43DC2C84", result.getETag());
        Assert.assertEquals("Wed, 27 Jun 2012 07:28:49 GMT",
                DateUtil.formatRfc822Date(result.getLastModified()));
    }

    @Test
    public void testParseInitiateMultipartUpload() throws Exception {

        String filename = "initiateMultipartUpload.xml";
        InputStream in = getInputStream(filename);

        InitiateMultipartUploadResult initiateMultipartUploadResult = ResponseParsers
                .parseInitiateMultipartUpload(in);

        Assert.assertEquals("dp7d8j2xfec1m984em9xmkgc_gan",
                initiateMultipartUploadResult.getBucketName());
        Assert.assertEquals("test.rar", initiateMultipartUploadResult.getKey());

        Assert.assertEquals("0004B9847F209E446A8BA24F84C6881D",
                initiateMultipartUploadResult.getUploadId());

        in.close();

    }

    @Test
    public void testParseListMultipartUploads() throws Exception {

        String filename = "listMultipartUploads.xml";
        InputStream in = getInputStream(filename);

        MultipartUploadListing multipartUploadListing = ResponseParsers
                .parseListMultipartUploads(in);

        Assert.assertEquals("dp7d8j2xfec1m984em9xmkgc_gan",
                multipartUploadListing.getBucketName());
        Assert.assertNull(multipartUploadListing.getKeyMarker());
        Assert.assertNull(multipartUploadListing.getUploadIdMarker());
        Assert.assertEquals("test.rar",
                multipartUploadListing.getNextKeyMarker());
        Assert.assertEquals("0004B984CE74CF65B555A38F98CCFF96",
                multipartUploadListing.getNextUploadIdMarker());
        Assert.assertNull(multipartUploadListing.getDelimiter());
        Assert.assertNull(multipartUploadListing.getPrefix());
        Assert.assertEquals(1000, multipartUploadListing.getMaxUploads());
        Assert.assertEquals(false, multipartUploadListing.isTruncated());

        Assert.assertEquals("test.rar", multipartUploadListing
                .getMultipartUploads().get(0).getKey());
        Assert.assertEquals("0004B9846579745A77D988FFFDDEAFC3",
                multipartUploadListing.getMultipartUploads().get(0)
                        .getUploadId());
        Assert.assertEquals("Standard", multipartUploadListing
                .getMultipartUploads().get(0).getStorageClass());
        Assert.assertEquals(DateUtil.parseIso8601Date("2012-02-22T02:36:36.000Z"),
                multipartUploadListing.getMultipartUploads().get(0)
                        .getInitiated());

        in.close();
    }

    @Test
    public void testParseListParts() throws Exception {

        String filename = "listParts.xml";
        InputStream in = getInputStream(filename);

        PartListing partListing = ResponseParsers.parseListParts(in);

        Assert.assertEquals("dp7d8j2xfec1m984em9xmkgc_gan",
                partListing.getBucketName());
        Assert.assertEquals("test.rar", partListing.getKey());
        Assert.assertEquals("0004B98692BB2A28C897B642CFAC1DCE",
                partListing.getUploadId());
        Assert.assertEquals("Standard", partListing.getStorageClass());
        Assert.assertEquals(3, partListing.getNextPartNumberMarker().intValue());
        Assert.assertEquals(1000, partListing.getMaxParts().intValue());
        Assert.assertEquals(false, partListing.isTruncated());
        
        List<PartSummary> parts = partListing.getParts();
        Assert.assertNotNull(parts);
        Assert.assertTrue(parts.size() == 1);
        PartSummary part = parts.get(0);
        Assert.assertEquals(3, part.getPartNumber());
        Assert.assertEquals(
                DateUtil.parseIso8601Date("2012-02-22T05:12:29.000Z"),
                part.getLastModified());
        Assert.assertEquals("4B4BEAF5BC622FC89D29BF0E3B70B730", part.getETag());
        Assert.assertEquals(3996796L, part.getSize());

        in.close();
    }

    @Test
    public void testParseCompleteMultipartUpload() throws Exception {

        String filename = "completeMultipartUpload.xml";
        InputStream in = getInputStream(filename);

        CompleteMultipartUploadResult completeMultipartUploadResult = ResponseParsers
                .parseCompleteMultipartUpload(in);

        Assert.assertEquals(
                "http://oss-test.aliyun-inc.com/dp7d8j2xfec1m984em9xmkgc_gan/test.rar",
                completeMultipartUploadResult.getLocation());
        Assert.assertEquals("dp7d8j2xfec1m984em9xmkgc_gan",
                completeMultipartUploadResult.getBucketName());
        Assert.assertEquals("test.rar", completeMultipartUploadResult.getKey());
        Assert.assertEquals("894061A784C63D244E6E18B80E36076A-1",
                completeMultipartUploadResult.getETag());

        in.close();
    }
}

