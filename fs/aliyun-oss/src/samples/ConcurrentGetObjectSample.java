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
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;

/**
 * This sample demonstrates how to download an object concurrently 
 * from Aliyun OSS using the OSS SDK for Java.
 */
public class ConcurrentGetObjectSample {
  
    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";

    private static OSS client = null;
    
    private static String bucketName = "*** Provide bucket name ***";
    private static String key = "*** Provide key ***";
    private static String localFilePath = "*** Provide local file path ***";
    
    private static ExecutorService executorService = Executors.newFixedThreadPool(5);
    private static AtomicInteger completedBlocks = new AtomicInteger(0);
    
    public static void main(String[] args) throws IOException {
        /*
         * Constructs a client instance with your account for accessing OSS
         */
        client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {            
            /*
             * Upload an object to your bucket
             */
            System.out.println("Uploading a new object to OSS from a file\n");
            client.putObject(new PutObjectRequest(bucketName, key, createSampleFile()));
            
            /*
             * Get size of the object and pre-create a random access file to hold object data
             */
            ObjectMetadata metadata = client.getObjectMetadata(bucketName, key);
            long objectSize = metadata.getContentLength();
            RandomAccessFile raf = new RandomAccessFile(localFilePath, "rw");
            raf.setLength(objectSize);
            raf.close();
            
            /*
             * Calculate how many blocks to be divided
             */
            final long blockSize = 5 * 1024 * 1024L;   // 5MB
            int blockCount = (int) (objectSize / blockSize);
            if (objectSize % blockSize != 0) {
                blockCount++;
            }
            System.out.println("Total blocks count " + blockCount + "\n");
            
            /*
             * Download the object concurrently
             */
            System.out.println("Start to download " + key + "\n");
            for (int i = 0; i < blockCount; i++) {
                long startPos = i * blockSize;
                long endPos = (i + 1 == blockCount) ? objectSize - 1 : (i + 1) * blockSize;
                executorService.execute(new BlockFetcher(startPos, endPos, i + 1));
            }
            
            /*
             * Waiting for all blocks finished
             */
            executorService.shutdown();
            while (!executorService.isTerminated()) {
                try {
                    executorService.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            /*
             * Verify whether all blocks are finished
             */
            if (completedBlocks.intValue() != blockCount) {
                throw new IllegalStateException("Download fails due to some blocks are not finished yet");
            } else {
                System.out.println("Succeed to download object " + key);
            }
            
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
            if (client != null) {
                client.shutdown();
            }
        }
    }
    
    private static class BlockFetcher implements Runnable {
        
        private long startPos;
        private long endPos;
        
        private int blockNumber;
        
        public BlockFetcher(long startPos, long endPos, int blockNumber) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.blockNumber = blockNumber;
        }
        
        @Override
        public void run() {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(localFilePath, "rw");
                raf.seek(startPos);

                GetObjectRequest request = new GetObjectRequest(bucketName, key).withRange(startPos, endPos);
                request.addHeader("x-oss-range-behavior", "standard");
                OSSObject object = client.getObject(request);
                InputStream objectContent = object.getObjectContent();
                try {
                    byte[] buf = new byte[4096];
                    int bytesRead = 0;
                    while ((bytesRead = objectContent.read(buf)) != -1) {
                        raf.write(buf, 0, bytesRead);
                    }
                    
                    completedBlocks.incrementAndGet();
                    System.out.println("Block#" + blockNumber + " done\n");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    objectContent.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
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
