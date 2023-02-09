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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.event.ProgressEvent;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.event.ProgressListener;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import static com.aliyun.oss.OSSErrorCode.PART_NOT_SEQUENTIAL;
import org.junit.Test;

public class UploadFileTest extends TestBase {

    @Test
    public void testUploadFileWithoutCheckpoint() {
        final String key = "obj-upload-file-wcp";

        try {
            File file = createSampleFile(key, 1024 * 500);

            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);

            uploadFileRequest = new UploadFileRequest(bucketName, key, file.getAbsolutePath(), (1024 * 100),10);

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            ObjectListing objects = ossClient.listObjects(bucketName, key);
            Assert.assertEquals(objects.getObjectSummaries().size(), 1);
            Assert.assertEquals(objects.getObjectSummaries().get(0).getKey(), key);
            Assert.assertEquals(objects.getObjectSummaries().get(0).getSize(), file.length());
            Assert.assertEquals(objects.getRequestId().length(), REQUEST_ID_LEN);

            ObjectMetadata meta = ossClient.getObjectMetadata(bucketName, key);
            Assert.assertEquals(meta.getContentLength(), file.length());
            Assert.assertEquals(meta.getContentType(), "text/plain");

            File fileNew = new File(key + "-new.txt");
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ossClient.getObject(getObjectRequest, fileNew);
            Assert.assertEquals(file.length(), fileNew.length());

            ossClient.deleteObject(bucketName, key);
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUploadFileSequential() throws Throwable {
        final String key = "obj-upload-file-Sequential";

        String testBucketName = super.bucketName + "-upload-sequential";
        String endpoint = "http://oss-cn-shanghai.aliyuncs.com";

        //create client
        ClientConfiguration conf = new ClientConfiguration().setSupportCname(false);
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        OSS testOssClient = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);
        testOssClient.createBucket(testBucketName);

        // default is none sequential mode.
        try {
            File file = createSampleFile(key, 600 * 1024);

            UploadFileRequest uploadFileRequest = new UploadFileRequest(testBucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(5);
            uploadFileRequest.setPartSize(100*1024);
            testOssClient.uploadFile(uploadFileRequest);

            GetObjectRequest getObjectRequest = new GetObjectRequest(testBucketName, key);
            OSSObject ossObject = testOssClient.getObject(getObjectRequest);
            Assert.assertNull(ossObject.getResponse().getHeaders().get("Content-MD5"));
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // sequential mode not support multi thread.
        try {
            String objectName = key + "-multi-thread";
            File file = createSampleFile(objectName, 600 * 1024);

            UploadFileRequest uploadFileRequest = new UploadFileRequest(testBucketName, objectName);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setSequentialMode(true);
            uploadFileRequest.setTaskNum(5);
            uploadFileRequest.setPartSize(100*1024);

            testOssClient.uploadFile(uploadFileRequest);
            Assert.fail("Not support multi thread upload in sequentialMode, Should be failed here.");
        } catch (OSSException e) {
            Assert.assertEquals(PART_NOT_SEQUENTIAL, e.getErrorCode());
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // single thread in sequential mode.
        try {
            String objectName = key + "-single-thread";
            File file = createSampleFile(objectName, 600 * 1024);
            UploadFileRequest uploadFileRequest = new UploadFileRequest(testBucketName, objectName);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setSequentialMode(true);
            uploadFileRequest.setTaskNum(1);
            uploadFileRequest.setPartSize(100*1024);

            testOssClient.uploadFile(uploadFileRequest);

            GetObjectRequest getObjectRequest = new GetObjectRequest(testBucketName, objectName);
            OSSObject ossObject = testOssClient.getObject(getObjectRequest);

            Assert.assertNotNull(ossObject.getResponse().getHeaders().get("Content-MD5"));
            testOssClient.deleteObject(testBucketName, objectName);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            testOssClient.deleteBucket(testBucketName);
        }
    }

    @Test
    public void testUploadFileWithCheckpoint() {
        final String key = "obj-upload-file-cp";

        try {
            File file = createSampleFile(key, 1024 * 500);

            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setEnableCheckpoint(true);
            uploadFileRequest.setPartSize(1024);

            uploadFileRequest = new UploadFileRequest(bucketName, key, file.getAbsolutePath(), (1024 * 100),10, true);
            uploadFileRequest.setTaskNum(0);
            Assert.assertEquals(1, uploadFileRequest.getTaskNum());
            uploadFileRequest.setTaskNum(1001);
            Assert.assertEquals(1000, uploadFileRequest.getTaskNum());
            uploadFileRequest.setTaskNum(10);

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            ObjectListing objects = ossClient.listObjects(bucketName, key);
            Assert.assertEquals(objects.getObjectSummaries().size(), 1);
            Assert.assertEquals(objects.getObjectSummaries().get(0).getKey(), key);
            Assert.assertEquals(objects.getObjectSummaries().get(0).getSize(), file.length());
            Assert.assertEquals(objects.getRequestId().length(), REQUEST_ID_LEN);

            ObjectMetadata meta = ossClient.getObjectMetadata(bucketName, key);
            Assert.assertEquals(meta.getContentLength(), file.length());
            Assert.assertEquals(meta.getContentType(), "text/plain");

            File fileNew = new File(key + "-new.txt");
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ossClient.getObject(getObjectRequest, fileNew);
            Assert.assertEquals(file.length(), fileNew.length());

            ossClient.deleteObject(bucketName, key);
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUploadFileWithCheckpointFile() {
        final String key = "obj-upload-file-cpf";

        try {
            File file = createSampleFile(key, 1024 * 500);

            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setEnableCheckpoint(true);
            uploadFileRequest.setCheckpointFile("BingWallpaper.ucp");

            uploadFileRequest = new UploadFileRequest(bucketName, key, file.getAbsolutePath(),
                    (1024 * 100),10,
                    true, "BingWallpaper.ucp");

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            ObjectListing objects = ossClient.listObjects(bucketName, key);
            Assert.assertEquals(objects.getObjectSummaries().size(), 1);
            Assert.assertEquals(objects.getObjectSummaries().get(0).getKey(), key);
            Assert.assertEquals(objects.getObjectSummaries().get(0).getSize(), file.length());
            Assert.assertEquals(objects.getRequestId().length(), REQUEST_ID_LEN);

            ObjectMetadata meta = ossClient.getObjectMetadata(bucketName, key);
            Assert.assertEquals(meta.getContentLength(), file.length());
            Assert.assertEquals(meta.getContentType(), "text/plain");

            File fileNew = new File(key + "-new.txt");
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ossClient.getObject(getObjectRequest, fileNew);
            Assert.assertEquals(file.length(), fileNew.length());

            ossClient.deleteObject(bucketName, key);
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void loadErrorCpf() {
        try {
            String key = "test-up-with-error-cpf";
            String cpf = "upload-err.ucp";
            File cpfFile = createSampleFile(cpf, 1024 * 1);
            File file = createSampleFile(key, 1024 * 500);

            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setEnableCheckpoint(true);
            uploadFileRequest.setCheckpointFile(cpfFile.getAbsolutePath());

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            File fileNew = new File(key + "-new.txt");
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ossClient.getObject(getObjectRequest, fileNew);
            Assert.assertEquals(file.length(), fileNew.length());

            ossClient.deleteObject(bucketName, key);
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void loadEffectiveCpf() {
        final String key = "test-up-with-effective-cpf";

        final String cpf = "effective.ucp";
        final File cpfFile = new File(cpf);

        final String newCpf = cpf + "-new.cpf";
        final File newCpfFile = new File(newCpf);

        try {
            final File file = createSampleFile(key, 10 * 1024 * 1024);

            if (cpfFile.exists()) {
                cpfFile.delete();
            }

            Assert.assertFalse(cpfFile.exists());

            // create a effective checkpoint file.
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
                        uploadFileRequest.setUploadFile(file.getAbsolutePath());
                        uploadFileRequest.setTaskNum(1);
                        uploadFileRequest.setPartSize(200 * 1024);
                        uploadFileRequest.setEnableCheckpoint(true);
                        uploadFileRequest.setCheckpointFile(cpf);
                        uploadFileRequest.setTrafficLimit(8 * 3 * 1024 * 1024);

                        UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }, "upload effective cpf thread");

            thread.start();
            Thread.sleep(1500);
            thread.interrupt();

            Assert.assertTrue(cpfFile.exists());

            if (newCpfFile.exists()) {
                newCpfFile.delete();
            }

            // cp checkpoint file to a new checkpoint file.
            InputStream is = new FileInputStream(cpfFile);
            OutputStream os = new FileOutputStream(newCpfFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();

            // upload with effective checkpoint file.
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setPartSize(1024 * 1024);
            uploadFileRequest.setEnableCheckpoint(true);
            uploadFileRequest.setCheckpointFile(newCpf);

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            File fileNew = new File(key + "-new.txt");
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ossClient.getObject(getObjectRequest, fileNew);
            Assert.assertEquals(file.length(), fileNew.length());
            Assert.assertTrue("compare file", compareFile(fileNew.getAbsolutePath(), file.getAbsolutePath()));

            ossClient.deleteObject(bucketName, key);
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            cpfFile.delete();
            newCpfFile.delete();
        }
    }

    @Test
    public void testAcl() {
        String key = "test-upload-file-with-acl";

        // Upload file without acl setting, returned acl will be DEFAULT.
        try {
            File file = createSampleFile(key, 1024 * 500);
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key, file.getAbsolutePath(), (1024 * 100), 10);

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            ObjectAcl acl = ossClient.getObjectAcl(bucketName, key);
            Assert.assertEquals(acl.getPermission(), ObjectPermission.Default);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // Upload With PRIVATE acl setting, returned acl will be PRIVATE.
        try {
            File file = createSampleFile(key, 1024 * 500);
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key, file.getAbsolutePath(), (1024 * 100), 10);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectAcl(CannedAccessControlList.Private);
            uploadFileRequest.setObjectMetadata(metadata);

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            ObjectAcl acl = ossClient.getObjectAcl(bucketName, key);
            Assert.assertEquals(acl.getPermission(), ObjectPermission.Private);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // Test set acl null.
        try {
            File file = createSampleFile(key, 1024 * 500);
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key, file.getAbsolutePath(), (1024 * 100), 10);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectAcl(null);
            uploadFileRequest.setObjectMetadata(metadata);

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            ObjectAcl acl = ossClient.getObjectAcl(bucketName, key);
            Assert.assertEquals(acl.getPermission(), ObjectPermission.Default);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUploadRetryWithProgress() {
        final String objectName = "test-upload-with-progress";
        final String ucpName = objectName + ".ucp";
        final String newUcpName = ucpName + ".new";
        File file = null;
        File ucpFile = null;
        File newUcpFile = new File(newUcpName);
        try {
            file = createSampleFile(objectName, 10 * 1024 * 1024);
            ucpFile = new File(ucpName);

            if (ucpFile.exists()) {
                ucpFile.delete();
            }

            Assert.assertFalse(ucpFile.exists());

            // create a effective checkpoint file.
            final File finalFile = file;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, objectName);
                        uploadFileRequest.setUploadFile(finalFile.getAbsolutePath());
                        uploadFileRequest.setTaskNum(1);
                        uploadFileRequest.setPartSize(200 * 1024);
                        uploadFileRequest.setEnableCheckpoint(true);
                        uploadFileRequest.setCheckpointFile(ucpName);
                        uploadFileRequest.setTrafficLimit(8 * 2 * 1024 * 1024);
                        uploadFileRequest.withProgressListener(new UploadFileProgressListener(finalFile.length(), false, 200 * 1024));
                        ossClient.uploadFile(uploadFileRequest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }, "upload-with-progress");

            thread.start();
            Thread.sleep(2000);
            thread.interrupt();

            if (newUcpFile.exists()) {
                newUcpFile.delete();
            }

            // cp checkpoint file to a new checkpoint file.
            InputStream is = new FileInputStream(ucpFile);
            OutputStream os = new FileOutputStream(newUcpFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // use new ucp file and new progress listener.
        try {
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, objectName);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(1);
            uploadFileRequest.setPartSize(200 * 1024);
            uploadFileRequest.setEnableCheckpoint(true);
            uploadFileRequest.setCheckpointFile(newUcpName);
            uploadFileRequest.withProgressListener(new UploadFileProgressListener(file.length(), true, 200 * 1024));
            ossClient.uploadFile(uploadFileRequest);
        }  catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            ucpFile.delete();
            newUcpFile.delete();
        }

    }

    public class UploadFileProgressListener implements ProgressListener {
        private long bytesWritten = 0;
        private long totalBytes = -1;
        private boolean succeed = false;
        private long expectedTotalBytes = -1;
        private boolean reUploadFlag = false;
        private long partSize = -1;

        UploadFileProgressListener(long expectedTotalBytes, boolean reUploadFlag, long partSize) {
            this.expectedTotalBytes = expectedTotalBytes;
            this.reUploadFlag = reUploadFlag;
            this.partSize = partSize;
        }

        @Override
        public void progressChanged(ProgressEvent progressEvent) {
            long bytes = progressEvent.getBytes();
            ProgressEventType eventType = progressEvent.getEventType();
            switch (eventType) {
                case TRANSFER_STARTED_EVENT:
                    System.out.println("Start to upload......");
                    break;
                case REQUEST_CONTENT_LENGTH_EVENT:
                    this.totalBytes = bytes;
                    Assert.assertEquals(this.expectedTotalBytes, this.totalBytes);
                    System.out.println(this.totalBytes + " bytes in total will be uploaded to OSS");
                    break;
                case REQUEST_BYTE_TRANSFER_EVENT:
                    if (this.reUploadFlag) {
                        Assert.assertTrue( bytes > this.partSize);
                        this.reUploadFlag = false;
                    }
                    this.bytesWritten += bytes;
                    if (this.totalBytes != -1) {
                        int percent = (int) (this.bytesWritten * 100.0 / this.totalBytes);
                        System.out.println(bytes + " bytes have been written at this time, upload progress: " + percent + "%(" + this.bytesWritten + "/" + this.totalBytes + ")");
                    } else {
                        System.out.println(bytes + " bytes have been written at this time, upload ratio: unknown" + "(" + this.bytesWritten + "/...)");
                    }
                    break;
                case TRANSFER_COMPLETED_EVENT:
                    Assert.assertEquals(this.expectedTotalBytes, this.bytesWritten);
                    this.succeed = true;
                    System.out.println("Succeed to upload, " + this.bytesWritten + " bytes have been transferred in total");
                    break;
                case TRANSFER_FAILED_EVENT:
                    System.out.println("Failed to upload, " + this.bytesWritten + " bytes have been transferred");
                    break;
                default:
                    break;
            }
        }

        public boolean isSucceed() {
            return succeed;
        }
    }

}