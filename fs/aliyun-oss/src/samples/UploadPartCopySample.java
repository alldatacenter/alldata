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
import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.UploadPartCopyRequest;
import com.aliyun.oss.model.UploadPartCopyResult;

/**
 * This sample demonstrates how to upload parts by copy mode 
 * to Aliyun OSS using the OSS SDK for Java.
 */
public class UploadPartCopySample {
    
    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    
    private static OSS client = null;

    private static String sourceBucketName = "*** Provide source bucket name ***";
    private static String sourceKey = "*** Provide source key ***";
    private static String targetBucketName = "*** Provide target bucket name ***";
    private static String targetKey = "*** Provide target key ***";
    
    private static String localFilePath = "*** Provide local file path ***";
    
    public static void main(String[] args) throws IOException {        
        /*
         * Constructs a client instance with your account for accessing OSS
         */
        client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {            
            /*
             * Upload an object to your source bucket
             */
            System.out.println("Uploading a new object to OSS from a file\n");
            client.putObject(new PutObjectRequest(sourceBucketName, sourceKey, createSampleFile()));
            
            /*
             * Claim a new upload id for your target bucket
             */
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(targetBucketName, targetKey);
            InitiateMultipartUploadResult initiateMultipartUploadResult = client.initiateMultipartUpload(initiateMultipartUploadRequest);
            String uploadId = initiateMultipartUploadResult.getUploadId();
            
            /*
             * Calculate how many parts to be divided
             */
            final long partSize = 5 * 1024 * 1024L;   // 5MB
            ObjectMetadata metadata = client.getObjectMetadata(sourceBucketName, sourceKey);
            long objectSize = metadata.getContentLength();
            int partCount = (int) (objectSize / partSize);
            if (objectSize % partSize != 0) {
                partCount++;
            }
            if (partCount > 10000) {
                throw new RuntimeException("Total parts count should not exceed 10000");
            } else {                
                System.out.println("Total parts count " + partCount + "\n");
            }

            /*
             * Upload multiparts by copy mode
             */
            System.out.println("Begin to upload multiparts by copy mode to OSS\n");
            List<PartETag> partETags = new ArrayList<PartETag>();
            for (int i = 0; i < partCount; i++) {
                long startPos = i * partSize;
                long curPartSize = (i + 1 == partCount) ? (objectSize - startPos) : partSize;;

                UploadPartCopyRequest uploadPartCopyRequest = new UploadPartCopyRequest(
                        sourceBucketName, sourceKey, targetBucketName, targetKey);
                uploadPartCopyRequest.setUploadId(uploadId);
                uploadPartCopyRequest.setPartSize(curPartSize);
                uploadPartCopyRequest.setBeginIndex(startPos);
                uploadPartCopyRequest.setPartNumber(i + 1);
                
                UploadPartCopyResult uploadPartCopyResult = client.uploadPartCopy(uploadPartCopyRequest);
                System.out.println("\tPart#" + uploadPartCopyResult.getPartNumber() + " done\n");
                partETags.add(uploadPartCopyResult.getPartETag());
            }
            
            /*
             * Complete to upload multiparts
             */
            System.out.println("Completing to upload multiparts\n");
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
                    targetBucketName, targetKey, uploadId, partETags);
            client.completeMultipartUpload(completeMultipartUploadRequest);
            
            /*
             * Fetch the object that newly created at the step below.
             */
            System.out.println("Fetching an object");
            client.getObject(new GetObjectRequest(targetBucketName, targetKey), new File(localFilePath));
            
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message: " + oe.getErrorMessage());
            System.out.println("Error Code:       " + oe.getErrorCode());
            System.out.println("Request ID:      " + oe.getRequestId());
            System.out.println("Host ID:           " + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ce.getMessage());
        } finally {
            /*
             * Do not forget to shut down the client finally to release all allocated resources.
             */
            client.shutdown();
        }
    }
    
    private static File createSampleFile() throws IOException {
        File file = File.createTempFile("oss-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        for (int i = 0; i < 1000000; i++) {
            writer.write("abcdefghijklmnopqrstuvwxyz\n");
            writer.write("0123456789011234567890\n");
        }
        writer.close();

        return file;
    }
}
