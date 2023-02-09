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

package samples_java;

import java.io.IOException;
import java.util.Date;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.DownloadFileRequest;
import com.obs.services.model.MonitorableProgressListener;
import com.obs.services.model.ProgressStatus;

public class DownloadFileSample {
    private static final String endPoint = "https://your-endpoint";

    private static final String ak = "*** Provide your Access Key ***";

    private static final String sk = "*** Provide your Secret Key ***";
    
    private static ObsClient obsClient;

    private static String bucketName = "my-obs-bucket-demo";

    private static String objectKey = "my-obs-object-key-demo";

    private static String localSavePath = "local save path";
    
    public static void main(String[] args) {
        
        ObsConfiguration config = new ObsConfiguration();
        config.setSocketTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setEndPoint(endPoint);

        try {
            /*
             * Constructs a obs client instance with your account for accessing OBS
             */
            obsClient = new ObsClient(ak, sk, config);

            DownloadFileRequest request = new DownloadFileRequest(bucketName, objectKey);
            // Set the local path to which the object is downloaded.
            request.setDownloadFile(localSavePath);
            // Set the maximum number of parts that can be concurrently downloaded.
            request.setTaskNum(5);
            // Set the part size to 1 MB.
            request.setPartSize(1 * 1024 * 1024);
            // Enable resumable upload.
            request.setEnableCheckpoint(true);
            // Trigger the listener callback every 100 KB.
            request.setProgressInterval(100 * 1024L);

            MonitorableProgressListener progressListener = new MonitorableProgressListener() {
                @Override
                public void progressChanged(ProgressStatus status) {
                    System.out.println(new Date() + "  TransferPercentage:" + status.getTransferPercentage());
                }
            };
            // Set a data transmission listener that can monitor the running status of subprocesses.
            request.setProgressListener(progressListener);

            // Start a thread to download an object.
            DownloadFileManager downloadManager = new DownloadFileManager(obsClient, request, progressListener);
            downloadManager.download();

            // Wait for 3 seconds.
            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Interrupt the thread to simulate the suspension of the download task.
            // Note: After this method is called, the download task does not stop immediately. You are advised to call the progressListener.waitingFinish method to wait until the task is complete.
            // In addition, after this method is called, "java.lang.RuntimeException: Abort io due to thread interrupted” is displayed in the log. This is a normal phenomenon after the thread is interrupted. You can ignore this exception.
            downloadManager.pause();
            System.out.println(new Date() + "  download thread is stop. \n");
            
            // Wait for 3 seconds.
            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            System.out.println(new Date() + "  restart request. \n");
            downloadManager.download();

            // Wait until the download task is complete.
            downloadManager.waitingFinish();
        } catch (ObsException e) {
            System.out.println("Response Code: " + e.getResponseCode());
            System.out.println("Error Message: " + e.getErrorMessage());
            System.out.println("Error Code:       " + e.getErrorCode());
            System.out.println("Request ID:      " + e.getErrorRequestId());
            System.out.println("Host ID:           " + e.getErrorHostId());
        } catch (InterruptedException e) {

        } finally {
            if (obsClient != null) {
                try {
                    /*
                     * Close obs client
                     */
                    obsClient.close();
                } catch (IOException e) {
                }
            }
        }
    }
}

class DownloadFileManager {
    private DownloadFileRequest request;
    private ObsClient obsClient;
    private MonitorableProgressListener progressListener;
    private Thread currentThread;
    
    public DownloadFileManager(ObsClient obsClient, DownloadFileRequest request, MonitorableProgressListener progressListener) {
        this.obsClient = obsClient;
        this.request = request;
        this.progressListener = progressListener;
        request.setProgressListener(progressListener);
    }

    /**
     * Start a download task.
     */
    public void download() {
        this.progressListener.reset();
        this.currentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Use resumable download.
                    obsClient.downloadFile(request);
                } catch (ObsException e) {
                    // When an exception occurs, you can call the interface for resumable download to download the file again.
                    if (null != e.getCause()
                            && e.getCause() instanceof InterruptedException) {
                        System.out.println(new Date() + "  current thread is interrupted. \n");
                    } else {
                        e.printStackTrace();
                    }
                } 
            }
        });
        
        this.currentThread.start();
    }
    
    /**
     * Pause a task.
     * @throws InterruptedException
     */
    public void pause() throws InterruptedException {
        // Interrupt the thread to simulate the suspension of the download task.
        // Note: After this method is called, the download task does not stop immediately. You are advised to call the progressListener.waitingFinish method to wait until the task is complete.
        // In addition, after this method is called, "java.lang.RuntimeException: Abort io due to thread interrupted” is displayed in the log. This is a normal phenomenon after the thread is interrupted. You can ignore this exception.
        this.currentThread.interrupt();
        
        // Wait until the download task is complete.
        this.progressListener.waitingFinish();
    }
    
    /**
     * Wait until the download task is complete.
     * @throws InterruptedException
     */
    public void waitingFinish() throws InterruptedException {
        this.currentThread.join();
    }
}
