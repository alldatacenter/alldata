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

import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.CreateSymlinkRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.TagSet;
import com.aliyun.oss.model.UploadFileRequest;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.aliyun.oss.model.SetObjectTaggingRequest;

public class ObjectTaggingTest extends TestBase {

    @Test
    public void testNormalSetObjectAcl() {
        String key = "normal-set-tagging-acl";

        try {
            InputStream instream = genFixedLengthInputStream(1024);
            ossClient.putObject(bucketName, key, instream);

            Map<String, String> tags = new HashMap<String, String>(1);
            tags.put("tag1", "balabala");
            tags.put("tag2", "haha");

            ossClient.setObjectTagging(bucketName, key, tags);

            TagSet tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 2);

            ossClient.deleteObjectTagging(bucketName, key);
            tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 0);

            tagSet = new TagSet();
            tagSet.setTag("tag1", "balabala");
            SetObjectTaggingRequest request = new SetObjectTaggingRequest(bucketName, key)
                    .withTagSet(tagSet);
            Assert.assertEquals("balabala", request.getTag("tag1"));
            ossClient.setObjectTagging(request);
            tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 1);

            ossClient.deleteObjectTagging(bucketName, key);
            tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 0);

            tagSet = new TagSet();
            tagSet.setTag("tag1", "balabala");
            tagSet.setTag("tag2", "ala");
            //request = new SetObjectTaggingRequest(bucketName, key, tagSet);
            ossClient.setObjectTagging(bucketName, key, tagSet);
            tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 2);
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalPutObject() {
        String key = "normal-put-object";

        try {
            Map<String, String> tags = new HashMap<String, String>();
            tags.put("tag1 ", "balabala +");
            tags.put("tag2+", "haha -");

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectTagging(tags);

            InputStream instream = genFixedLengthInputStream(1024);
            ossClient.putObject(bucketName, key, instream, metadata);

            TagSet tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 2);

            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalAppendObject() {
        String key = "normal-append-object";

        try {
            Map<String, String> tags = new HashMap<String, String>();
            tags.put("tag1 ", "balabala +");
            tags.put("tag2+", "haha -");

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectTagging(tags);

            InputStream instream = genFixedLengthInputStream(1024);
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, metadata);
            appendObjectRequest.setPosition(0L);
            ossClient.appendObject(appendObjectRequest);

            TagSet tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 2);

            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void testMutilPartUploadObject() {
        String key = "normal-mutil-part-upload";

        try {
            Map<String, String> tags = new HashMap<String, String>(1);
            tags.put("tag1 ", "balabala +");
            tags.put("tag2+", "haha -");

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectTagging(tags);

            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, key, metadata);
            InitiateMultipartUploadResult initResult = ossClient.initiateMultipartUpload(initRequest);
            String uploadId = initResult.getUploadId();
            List<PartETag> partETags = new ArrayList<PartETag>();

            InputStream instream = genFixedLengthInputStream(1024);
            UploadPartRequest request = new UploadPartRequest(bucketName, key, uploadId, 1, instream, 1024);
            UploadPartResult uploadPartResult = ossClient.uploadPart(request);
            partETags.add(uploadPartResult.getPartETag());

            CompleteMultipartUploadRequest completeRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            ossClient.completeMultipartUpload(completeRequest);

            TagSet tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 2);

            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalCopyObject() {
        String key = "normal-copy-object";

        try {
            InputStream instream = genFixedLengthInputStream(1024);
            ossClient.putObject(bucketName, key, instream);

            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, key, bucketName, key);

            Map<String, String> tags = new HashMap<String, String>();
            tags.put("tag1 ", "balabala +");
            tags.put("tag2+", "haha -");
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectTagging(tags);
            copyObjectRequest.setNewObjectMetadata(metadata);

            ossClient.copyObject(copyObjectRequest);

            TagSet tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 2);

            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalSymlinkObject() {
        String key = "normal-symlink-object";

        try {
            InputStream instream = genFixedLengthInputStream(1024);
            ossClient.putObject(bucketName, key, instream);

            Map<String, String> tags = new HashMap<String, String>();
            tags.put("tag1 ", "balabala +");
            tags.put("tag2+", "haha -");

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectTagging(tags);

            CreateSymlinkRequest createSymlinkRequest = new CreateSymlinkRequest(bucketName, key, key);
            createSymlinkRequest.setMetadata(metadata);
            ossClient.createSymlink(createSymlinkRequest);

            TagSet tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 2);

            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalUploadFile() {
        final String key = "normal-upload-object";

        try {
            File file = createSampleFile(key, 1024 * 500);

            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());

            Map<String, String> tags = new HashMap<String, String>();
            tags.put("tag1 ", "balabala +");
            tags.put("tag2+", "haha -");
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectTagging(tags);
            uploadFileRequest.setObjectMetadata(metadata);

            ossClient.uploadFile(uploadFileRequest);

            TagSet tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), 2);

            file.delete();
            ossClient.deleteObject(bucketName, key);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
