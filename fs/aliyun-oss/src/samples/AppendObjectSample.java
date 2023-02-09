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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.AppendObjectResult;
import com.aliyun.oss.model.OSSObject;

/**
 * This sample demonstrates how to upload an object by append mode 
 * to Aliyun OSS using the OSS SDK for Java.
 */
public class AppendObjectSample {
    
    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    
    private static String bucketName = "*** Provide bucket name ***";
    private static String key = "*** Provide key ***";
    
    public static void main(String[] args) throws IOException {        
        /*
         * Constructs a client instance with your account for accessing OSS
         */
        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {            
            /*
             * Append an object from specfied input stream, keep in mind that
             * position should be set to zero at first time.
             */
            String content = "Thank you for using Aliyun Object Storage Service";
            InputStream instream = new ByteArrayInputStream(content.getBytes());
            Long firstPosition = 0L;
            System.out.println("Begin to append object at position(" + firstPosition + ")");
            AppendObjectResult appendObjectResult = client.appendObject(
                    new AppendObjectRequest(bucketName, key, instream).withPosition(0L));
            System.out.println("\tNext position=" + appendObjectResult.getNextPosition() + 
                    ", CRC64=" + appendObjectResult.getObjectCRC() + "\n");
            
            /*
             * Continue to append the object from specfied file descriptor at last position
             */
            Long nextPosition = appendObjectResult.getNextPosition();
            System.out.println("Continue to append object at last position(" + nextPosition + "):");
            appendObjectResult = client.appendObject(
                    new AppendObjectRequest(bucketName, key, createTempFile())
                    .withPosition(nextPosition));
            System.out.println("\tNext position=" + appendObjectResult.getNextPosition() + 
                    ", CRC64=" + appendObjectResult.getObjectCRC());
            
            /*
             * View object type of the appendable object
             */
            OSSObject object = client.getObject(bucketName, key);
            System.out.println("\tObject type=" + object.getObjectMetadata().getObjectType() + "\n");
            // Do not forget to close object input stream if not use it any more
            object.getObjectContent().close();
            
            /*
             * Delete the appendable object
             */
            System.out.println("Deleting an appendable object");
            client.deleteObject(bucketName, key);
            
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
    
    private static File createTempFile() throws IOException {
        File file = File.createTempFile("oss-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("0123456789011234567890\n");
        writer.close();

        return file;
    }
}
