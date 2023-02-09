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

package samples;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.event.ProgressEvent;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.event.ProgressListener;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.PutObjectRequest;

/**

 * Examples about reporting progress on upload/download.
 *
 */
public class GetProgressSample {
    
    /**
     * The uploading progress listener. Its progressChanged API is called by the SDK when there's an update.
     */
    static class PutObjectProgressListener implements ProgressListener {

        private long bytesWritten = 0;
        private long totalBytes = -1;
        private boolean succeed = false;
        
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
                System.out.println(this.totalBytes + " bytes in total will be uploaded to OSS");
                break;
            
            case REQUEST_BYTE_TRANSFER_EVENT:
                this.bytesWritten += bytes;
                if (this.totalBytes != -1) {
                    int percent = (int)(this.bytesWritten * 100.0 / this.totalBytes);
                    System.out.println(bytes + " bytes have been written at this time, upload progress: " +
                            percent + "%(" + this.bytesWritten + "/" + this.totalBytes + ")");
                } else {
                    System.out.println(bytes + " bytes have been written at this time, upload ratio: unknown" +
                            "(" + this.bytesWritten + "/...)");
                }
                break;
                
            case TRANSFER_COMPLETED_EVENT:
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
    
    /**
     * The downloading progress listener. Its progressChanged API is called by the SDK when there's an update.
     */
    static class GetObjectProgressListener implements ProgressListener {
        
        private long bytesRead = 0;
        private long totalBytes = -1;
        private boolean succeed = false;
        
        @Override
        public void progressChanged(ProgressEvent progressEvent) {
            long bytes = progressEvent.getBytes();
            ProgressEventType eventType = progressEvent.getEventType();
            switch (eventType) {
            case TRANSFER_STARTED_EVENT:
                System.out.println("Start to download......");
                break;
            
            case RESPONSE_CONTENT_LENGTH_EVENT:
                this.totalBytes = bytes;
                System.out.println(this.totalBytes + " bytes in total will be downloaded to a local file");
                break;
            
            case RESPONSE_BYTE_TRANSFER_EVENT:
                this.bytesRead += bytes;
                if (this.totalBytes != -1) {
                    int percent = (int)(this.bytesRead * 100.0 / this.totalBytes);
                    System.out.println(bytes + " bytes have been read at this time, download progress: " +
                            percent + "%(" + this.bytesRead + "/" + this.totalBytes + ")");
                } else {
                    System.out.println(bytes + " bytes have been read at this time, download ratio: unknown" +
                            "(" + this.bytesRead + "/...)");
                }
                break;
                
            case TRANSFER_COMPLETED_EVENT:
                this.succeed = true;
                System.out.println("Succeed to download, " + this.bytesRead + " bytes have been transferred in total");
                break;
                
            case TRANSFER_FAILED_EVENT:
                System.out.println("Failed to download, " + this.bytesRead + " bytes have been transferred");
                break;
                
            default:
                break;
            }
        }
        
        public boolean isSucceed() {
            return succeed;
        }
    }
    
    public static void main(String[] args) { 
        String endpoint = "http://oss-cn-hangzhou.aliyuncs.com";
        String accessKeyId = "<accessKeyId>";
        String accessKeySecret = "<accessKeySecret>";
        String bucketName = "<bucketName>";
        
        String key = "object-get-progress-sample";
        
        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            File fh = createSampleFile();
            
            // 带进度条的上传 
            client.putObject(new PutObjectRequest(bucketName, key, fh).
                    <PutObjectRequest>withProgressListener(new PutObjectProgressListener()));
            
            // 带进度条的下载
            client.getObject(new GetObjectRequest(bucketName, key).
                    <GetObjectRequest>withProgressListener(new GetObjectProgressListener()), fh);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Create a temp file with about 50MB.
     * 
     */
    private static File createSampleFile() throws IOException {
        File file = File.createTempFile("oss-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        
        for (int i = 0; i < 1000; i++) {
            writer.write("abcdefghijklmnopqrstuvwxyz\n");
            writer.write("0123456789011234567890\n");
        }

        writer.close();

        return file;
    }

}

