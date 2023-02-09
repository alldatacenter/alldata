/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.test;

import java.io.IOException;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.DownloadFileRequest;
import com.obs.services.model.DownloadFileResult;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.MonitorableProgressListener;
import com.obs.services.model.ProgressListener;
import com.obs.services.model.ProgressStatus;
import com.obs.services.model.UploadPartRequest;
import com.obs.test.objects.BaseObjectTest;
import com.obs.test.tools.BucketTools;

public class DownloadFileRequestTest extends BaseObjectTest {

    private String bucketName;
    private String objectKey;
    private ObsClient obsClient = null;

    @Before
    public void init() {
        bucketName = DownloadFileRequestTest.class.getName().replaceAll("\\.", "-").toLowerCase() + "-obs";
        objectKey = "for_download_2";

        obsClient = TestTools.getEnvironment_User3();

         BucketTools.createBucket(obsClient, bucketName, BucketTypeEnum.OBJECT);
    }

    @Test
    public void test_upload_big_file() throws IOException {
        InitiateMultipartUploadRequest initrequest = generateInitiateMultipartUploadRequest(bucketName, objectKey);
        InitiateMultipartUploadResult initResult = obsClient.initiateMultipartUpload(initrequest);

        for (int i = 1; i < 20; i++) {
            UploadPartRequest uploadParerequest = generateUploadPartRequest(bucketName, objectKey,
                    initResult.getUploadId(), i, (byte) i);
            obsClient.uploadPart(uploadParerequest);

            System.out.println("upload part : " + i);
        }

        ListPartsRequest listRequest = generateListPartsRequest(bucketName, objectKey, initResult.getUploadId());
        ListPartsResult listResult = obsClient.listParts(listRequest);

        CompleteMultipartUploadRequest completeRequest = generateCompleteMultipartUploadRequest(listResult);
        CompleteMultipartUploadResult completeResult = obsClient.completeMultipartUpload(completeRequest);
    }

    @Test
    public void test_pause() {
        DownloadFileRequest request = new DownloadFileRequest(bucketName, objectKey);
        // 设置下载对象的本地文件路径
        request.setDownloadFile("D:\\test\\test_bigfile_download_1");
        // 设置分段下载时的最大并发数
        request.setTaskNum(5);
        // 设置分段大小为10MB
        request.setPartSize(10 * 1024 * 1024);
        // 开启断点续传模式
        request.setEnableCheckpoint(true);

        PauseAbleProgressListener pauseAbleProgressListener = new PauseAbleProgressListener();

        request.setProgressListener(pauseAbleProgressListener);

        // new Thread(new Runnable() {
        // @Override
        // public void run() {
        //
        // try {
        // // 等10秒钟，尝试暂停
        // Thread.sleep(5 * 1000L);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        //
        // // 暂停
        // pauseAbleProgressListener.setPause(true);
        // try {
        // obsClient.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }
        // }).start();;

        try {
            // 进行断点续传下载
            DownloadFileResult result = obsClient.downloadFile(request);
        } catch (ObsException e) {
            // 发生异常时可再次调用断点续传下载接口进行重新下载
            e.printStackTrace();
        }

    }

    class PauseAbleProgressListener implements ProgressListener {

        private boolean isPause = false;

        public void setPause(boolean isPause) {
            this.isPause = isPause;
        }

        @Override
        public void progressChanged(ProgressStatus status) {
            System.out.println("TransferPercentage:" + status.getTransferPercentage());
            if (isPause) {
                System.out.println("isPause : " + isPause + "TransferPercentage:" + status.getTransferPercentage());
                // throw new PauseException();
            }
        }
    }

    class PauseException extends RuntimeException {
        public PauseException() {
            super();
        }

        @Override
        public String toString() {
            return "now is pause";
        }
    }

    @Test
    public void test_pause_2() {
        DownloadFileRequest request = new DownloadFileRequest(this.bucketName, this.objectKey);
        // 设置下载对象的本地文件路径
        request.setDownloadFile("D:\\test\\test_bigfile_download_5");
        // 设置分段下载时的最大并发数
        request.setTaskNum(5);
        // 设置分段大小为10MB
        request.setPartSize(2 * 1024 * 1024);
        // 开启断点续传模式
        request.setEnableCheckpoint(true);

        request.setProgressInterval(ObsConstraint.DEFAULT_PROGRESS_INTERVAL * 50);
        
        MonitorableProgressListener progressListener = new MonitorableProgressListener() {
            @Override
            public void progressChanged(ProgressStatus status) {
                System.out.println("TransferPercentage:" + status.getTransferPercentage());
            }
        };
        
        request.setProgressListener(progressListener);
        
        progressListener.reset();
        DownloadThread upload = new DownloadThread(obsClient, request);
        
        upload.start();

        
        try {
            int s = 3;
            System.out.println(new Date() + "  Start Wait " + s + " seconds");
            Thread.sleep(s * 1000);
            System.out.println(new Date() + "  End wait " + s + " seconds");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        upload.interrupt();
        System.out.println(new Date() + "  end interrupt thread");
        
//        while(request.isRunning()) {
//            System.out.println(new Date() + "   request is running ");
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        
        try {
            progressListener.waitingFinish(Long.MAX_VALUE);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        System.out.println(new Date() + "   request is not running ");
        
        System.out.println("==================================== Restart ====================================");
        upload = new DownloadThread(obsClient, request);
        progressListener.reset();
        upload.start();
//        try {
//            upload.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        
        try {
            progressListener.waitingFinish(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("==== end");
    }

    class DownloadThread extends Thread {
        private DownloadFileRequest request;
        private ObsClient obsClient;
        
        public DownloadThread(ObsClient obsClient, DownloadFileRequest request) {
            this.obsClient = obsClient;
            this.request = request;
        }
        
        @Override
        public void run() {
            try {
                // 进行断点续传下载
                DownloadFileResult result = this.obsClient.downloadFile(this.request);
            } catch (ObsException e) {
                // 发生异常时可再次调用断点续传下载接口进行重新下载
                e.printStackTrace();
                if (null != e.getCause()
                        && e.getCause() instanceof InterruptedException) {
                    System.out.println("==========================   pause  ==========================");
                } else {
                }
            } 
        }
    }
}
