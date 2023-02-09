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
import java.util.ArrayList;
import java.util.List;
import com.aliyun.oss.event.ProgressEvent;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.event.ProgressListener;
import junit.framework.Assert;
import org.junit.Test;
import com.aliyun.oss.model.DownloadFileRequest;
import com.aliyun.oss.model.DownloadFileResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.UploadFileRequest;
import com.aliyun.oss.model.UploadFileResult;

public class DownloadFileTest extends TestBase {

    @Test
    public void testUploadFileWithoutCheckpoint() {
        final String key = "obj-download-file-wcp";
        
        try {            
            File file = createSampleFile(key, 1024 * 500);
                        
            // upload file
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.addUserMetadata("prop", "propval");
            uploadFileRequest.setObjectMetadata(objMetadata);
            
            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);
            
            // download file
            String filePathNew = key + "-new.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(filePathNew);
            downloadFileRequest.setTaskNum(10);
            
            DownloadFileResult downloadRes = ossClient.downloadFile(downloadFileRequest);
            
            ObjectMetadata objMeta = downloadRes.getObjectMetadata();
            Assert.assertEquals(objMeta.getContentLength(), 102400);
            Assert.assertEquals(objMeta.getObjectType(), "Multipart");
            Assert.assertEquals(objMeta.getUserMetadata().get("prop"), "propval");

            File fileNew = new File(filePathNew);
            Assert.assertTrue("comparte file", compareFile(file.getAbsolutePath(), fileNew.getAbsolutePath()));
            
            ossClient.deleteObject(bucketName, key);
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUploadFileWithCheckpoint() {
        final String key = "obj-download-file-cp";
        
        try {            
            File file = createSampleFile(key, 1024 * 500);
                        
            // upload file
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setEnableCheckpoint(true);
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.addUserMetadata("prop", "propval");
            uploadFileRequest.setObjectMetadata(objMetadata);
            
            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);            
            
            // download file
            String filePathNew = key + "-new.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(filePathNew);
            downloadFileRequest.setTaskNum(10);
            downloadFileRequest.setEnableCheckpoint(true);
            
            DownloadFileResult downloadRes = ossClient.downloadFile(downloadFileRequest);
            
            ObjectMetadata objMeta = downloadRes.getObjectMetadata();
            Assert.assertEquals(objMeta.getContentLength(), 102400);
            Assert.assertEquals(objMeta.getObjectType(), "Multipart");
            Assert.assertEquals(objMeta.getUserMetadata().get("prop"), "propval");

            File fileNew = new File(filePathNew);
            Assert.assertTrue("comparte file", compareFile(file.getAbsolutePath(), fileNew.getAbsolutePath()));
            
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
        final String key = "obj-download-file-cpf";
        
        try {            
            File file = createSampleFile(key, 1024 * 500);
                        
            // upload file
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setEnableCheckpoint(true);
            uploadFileRequest.setCheckpointFile("BingWallpaper.ucp");
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.addUserMetadata("prop", "propval");
            uploadFileRequest.setObjectMetadata(objMetadata);
            
            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);
            
            // download file
            String filePathNew = key + "-new.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(filePathNew);
            downloadFileRequest.setTaskNum(10);
            downloadFileRequest.setEnableCheckpoint(true);
            downloadFileRequest.setCheckpointFile("BingWallpaper.dcp");

            DownloadFileResult downloadRes = ossClient.downloadFile(downloadFileRequest);
            
            ObjectMetadata objMeta = downloadRes.getObjectMetadata();
            Assert.assertEquals(objMeta.getContentLength(), 102400);
            Assert.assertEquals(objMeta.getObjectType(), "Multipart");
            Assert.assertEquals(objMeta.getUserMetadata().get("prop"), "propval");

            File fileNew = new File(filePathNew);
            Assert.assertTrue("comparte file", compareFile(file.getAbsolutePath(), fileNew.getAbsolutePath()));
            
            ossClient.deleteObject(bucketName, key);
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            ossClient.deleteBucket(bucketName);
        }
    }

    static class RangeInfo {
        RangeInfo(long start, long end) {
            this.start = start;
            this.end = end;
        }
        public long start; // start index;
        public long end; // end index;
    }

    @Test
    public void testDownloadFileWithRange() {
        final String key = "obj-download-file-range-cp";

        try {
            File file = createSampleFile(key, 1024 * 500);

            // upload file
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setEnableCheckpoint(true);
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.addUserMetadata("prop", "propval");
            uploadFileRequest.setObjectMetadata(objMetadata);

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            //range
            List<RangeInfo> rangeInfo = new ArrayList<RangeInfo>();
            List<RangeInfo> expectRangeInfo = new ArrayList<RangeInfo>();
            // case normal range
            rangeInfo.add(new RangeInfo(20, 200*1024 + 100));
            expectRangeInfo.add(new RangeInfo(20, 200*1024 + 100));

            rangeInfo.add(new RangeInfo(100, 101));
            expectRangeInfo.add(new RangeInfo(100, 101));

            rangeInfo.add(new RangeInfo(200, 200));
            expectRangeInfo.add(new RangeInfo(200, 200));

            //to end
            rangeInfo.add(new RangeInfo(102402, -1));
            expectRangeInfo.add(new RangeInfo(102402, file.length() -1));

            //end > size
            rangeInfo.add(new RangeInfo(102402, file.length() + 20));
            expectRangeInfo.add(new RangeInfo(102402, file.length() -1));

            //start < 0, end > 0 &&  end < size
            rangeInfo.add(new RangeInfo(-1, 300*1024 + 100));
            expectRangeInfo.add(new RangeInfo(0, 300*1024 + 100));

            //start < 0, end > size
            rangeInfo.add(new RangeInfo(-1, file.length()));
            expectRangeInfo.add(new RangeInfo(0, file.length() -1));

            //start < 0, end < 0
            rangeInfo.add(new RangeInfo(-1, -1));
            expectRangeInfo.add(new RangeInfo(0, file.length() -1));

            //start >= size
            rangeInfo.add(new RangeInfo(file.length(), file.length() + 20));
            expectRangeInfo.add(new RangeInfo(0, file.length() -1));

            //start > end
            rangeInfo.add(new RangeInfo(1024, 100));
            expectRangeInfo.add(new RangeInfo(0, file.length() -1));

            Assert.assertEquals(rangeInfo.size(), expectRangeInfo.size());
            Assert.assertEquals(true, rangeInfo.size() > 0);

            for (int i = 0; i < rangeInfo.size(); i++) {
                // download file
                String filePathNew = key + "-" + i +"-new.txt";
                long start = rangeInfo.get(i).start;
                long end = rangeInfo.get(i).end;
                DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
                downloadFileRequest.setDownloadFile(filePathNew);
                downloadFileRequest.setTaskNum(10);
                downloadFileRequest.setEnableCheckpoint(true);
                downloadFileRequest.setRange(start, end);

                DownloadFileResult downloadRes = ossClient.downloadFile(downloadFileRequest);

                ObjectMetadata objMeta = downloadRes.getObjectMetadata();
                //Assert.assertEquals(objMeta.getContentLength(), downloadFileRequest.getPartSize());
                Assert.assertEquals(objMeta.getObjectType(), "Multipart");
                Assert.assertEquals(objMeta.getUserMetadata().get("prop"), "propval");

                File fileNew = new File(filePathNew);
                long expectStart = expectRangeInfo.get(i).start;
                long expectEnd = expectRangeInfo.get(i).end;
                Assert.assertTrue("comparte file", compareFileWithRange(file.getAbsolutePath(), expectStart, expectEnd, fileNew.getAbsolutePath()));
                fileNew.delete();
            }
            ossClient.deleteObject(bucketName, key);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testDownloadEmptyFileWithoutCheckpoint() {
        final String key = "obj-download-empty-file-wcp";

        try {
            File file = createSampleFile(key, 0);
            Assert.assertEquals(0, file.length());

            // upload file
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.addUserMetadata("prop", "propval");
            uploadFileRequest.setObjectMetadata(objMetadata);

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            // download file
            String filePathNew = key + "-new.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(filePathNew);
            downloadFileRequest.setTaskNum(10);

            DownloadFileResult downloadRes = ossClient.downloadFile(downloadFileRequest);

            ObjectMetadata objMeta = downloadRes.getObjectMetadata();
            Assert.assertEquals(objMeta.getContentLength(), 0);
            Assert.assertEquals(objMeta.getObjectType(), "Multipart");
            Assert.assertEquals(objMeta.getUserMetadata().get("prop"), "propval");

            File fileNew = new File(filePathNew);
            Assert.assertEquals(0, fileNew.length());
            fileNew.delete();

            //download file with range
            filePathNew = key + "range-new.txt";
            downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(filePathNew);
            downloadFileRequest.setTaskNum(10);
            downloadFileRequest.setRange(0, 0);
            downloadRes = ossClient.downloadFile(downloadFileRequest);

            objMeta = downloadRes.getObjectMetadata();
            Assert.assertEquals(objMeta.getContentLength(), 0);
            Assert.assertEquals(objMeta.getObjectType(), "Multipart");
            Assert.assertEquals(objMeta.getUserMetadata().get("prop"), "propval");

            fileNew = new File(filePathNew);
            Assert.assertEquals(0, fileNew.length());
            fileNew.delete();

            ossClient.deleteObject(bucketName, key);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            ossClient.deleteBucket(bucketName);
        }
    }


    @Test
    public void testDownloadFileRetryWithProgress() {
        final String key = "obj-download-file-retry-progress";
        final String downFilePath = key + ".down";
        String newDownFilePath = downFilePath + ".new";
        final String dcpName = downFilePath + ".dcp";
        final String newDcpName = newDownFilePath + ".new";
        File downFile = new File(downFilePath);
        File newDownFile = new File(newDownFilePath);
        File tmpDownFile = null;
        File newTempDownFile = null;

        if (downFile.exists()) {
            downFile.delete();
            Assert.assertFalse(downFile.exists());
        }

        if (newDownFile.exists()) {
            newDownFile.delete();
            Assert.assertFalse(newDownFile.exists());
        }

        File dcpFile = new File(dcpName);
        if (dcpFile.exists()) {
            dcpFile.delete();
            Assert.assertFalse(dcpFile.exists());
        }

        File newDcpFile = new File(newDcpName);
        if (newDcpFile.exists()) {
            newDcpFile.delete();
            Assert.assertFalse(newDcpFile.exists());
        }

        final DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
        try {
            // upload file
            final File uploadFile = createSampleFile(key, 10 * 1024 * 1024);
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(uploadFile.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setEnableCheckpoint(true);
            ossClient.uploadFile(uploadFileRequest);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        downloadFileRequest.setDownloadFile(downFilePath);
                        downloadFileRequest.setPartSize(200 * 1024);
                        downloadFileRequest.setTaskNum(1);
                        downloadFileRequest.setEnableCheckpoint(true);
                        downloadFileRequest.setCheckpointFile(dcpName);
                        downloadFileRequest.setTrafficLimit(8 * 2 * 1024 * 1024);
                        downloadFileRequest.setProgressListener(new DownloadObjectProgressListener(downFilePath, uploadFile.length(), false, 200 * 1024));
                        ossClient.downloadFile(downloadFileRequest);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }, "down-with-progress");

            thread.start();
            Thread.sleep(2000);
            thread.interrupt();

            // cp checkpoint file
            byte[] buffer = new byte[1024];
            int length;
            InputStream is = new FileInputStream(dcpFile);
            OutputStream os = new FileOutputStream(newDcpFile);
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();

            // cp tmp file
            tmpDownFile = new File(downloadFileRequest.getTempDownloadFile());
            DownloadFileRequest downloadFileRequest2 = new DownloadFileRequest(bucketName, key);
            downloadFileRequest2.setDownloadFile(newDownFilePath);
            newTempDownFile = new File(downloadFileRequest2.getTempDownloadFile());
            InputStream is2 = new FileInputStream(tmpDownFile);
            OutputStream os2 = new FileOutputStream(newTempDownFile);
            while ((length = is2.read(buffer)) > 0) {
                os2.write(buffer, 0, length);
            }
            is2.close();
            os2.close();

            // download file twice.
            downloadFileRequest2.setPartSize(200 * 1024);
            downloadFileRequest2.setTaskNum(1);
            downloadFileRequest2.setEnableCheckpoint(true);
            downloadFileRequest2.setCheckpointFile(newDcpName);
            downloadFileRequest2.setProgressListener(new DownloadObjectProgressListener(newDownFilePath, uploadFile.length(), true, 200 * 1024));
            ossClient.downloadFile(downloadFileRequest2);
            Assert.assertTrue("compare file", compareFile(newDownFilePath, uploadFile.getAbsolutePath()));

            // wait for the first download task to finish
            Thread.sleep(10000);
        } catch (Throwable e) {
            Assert.fail(e.getMessage());
        } finally {
            downFile.delete();
            tmpDownFile.delete();
            newDownFile.delete();
            dcpFile.delete();
            newDcpFile.delete();
        }
    }


    @Test
    public void testDownloadNot4KAlignment() {
        final String key = "obj-download-not-4k-aligment";

        try {
            File file = createSampleFile(key, 1024 * 100);

            // upload file
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            // download file
            String filePathNew = key + "-new.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(filePathNew);
            downloadFileRequest.setTaskNum(10);
            downloadFileRequest.setPartSize(1024 * 6);
            downloadFileRequest.setEnableCheckpoint(true);
            ossClient.downloadFile(downloadFileRequest);

        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testDownload4KAlignment() {
        final String key = "obj-download-4k-aligment";

        try {

            File file = createSampleFile(key, 1024 * 100);

            // upload file
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getBucketName(), bucketName);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getKey(), key);

            // download file
            String filePathNew = key + "-new.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(filePathNew);
            downloadFileRequest.setTaskNum(10);
            downloadFileRequest.setPartSize(1024 * 4);
            downloadFileRequest.setEnableCheckpoint(true);
            ossClient.downloadFile(downloadFileRequest);

        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            ossClient.deleteBucket(bucketName);
        }
    }


    class DownloadObjectProgressListener implements ProgressListener {
        private long bytesRead = 0;
        private long totalBytes = -1;
        private boolean succeed = false;
        private String fileName = null;
        private long expectedTotal = -1;
        private boolean reDownFlag = false;
        private long partSize = -1;

        DownloadObjectProgressListener (String fileName, long expectedTotal, boolean reDownFlag, long partSize) {
            this.fileName = fileName;
            this.expectedTotal = expectedTotal;
            this.reDownFlag = reDownFlag;
            this.partSize = partSize;
        }

        @Override
        public void progressChanged(ProgressEvent progressEvent) {
            long bytes = progressEvent.getBytes();
            ProgressEventType eventType = progressEvent.getEventType();
            switch (eventType) {
                case TRANSFER_STARTED_EVENT:
                    System.out.println("Start to download " + this.fileName + "...");
                    break;
                case RESPONSE_CONTENT_LENGTH_EVENT:
                    this.totalBytes = bytes;
                    Assert.assertEquals(this.expectedTotal, this.totalBytes);
                    System.out.println(this.fileName + " " + this.totalBytes + " bytes in total will be downloaded to a local file");
                    break;
                case RESPONSE_BYTE_TRANSFER_EVENT:
                    if (this.reDownFlag) {
                        Assert.assertTrue(bytes > this.partSize);
                        this.reDownFlag = false;
                    }
                    this.bytesRead += bytes;
                    if (this.totalBytes != -1) {
                        int percent = (int)(this.bytesRead * 100.0 / this.totalBytes);
                        System.out.println(this.fileName + " " + bytes + " bytes have been read at this time, download progress: " +
                                percent + "%(" + this.bytesRead + "/" + this.totalBytes + ")");
                    } else {
                        System.out.println(this.fileName + bytes + " bytes have been read at this time, download ratio: unknown" +
                                "(" + this.bytesRead + "/...)");
                    }
                    break;
                case TRANSFER_COMPLETED_EVENT:
                    this.succeed = true;
                    System.out.println(this.fileName + " succeed to download, " + this.bytesRead + " bytes have been transferred in total");
                    break;
                case TRANSFER_FAILED_EVENT:
                    System.out.println(this.fileName + " failed to download, " + this.bytesRead + " bytes have been transferred");
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
