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
import java.io.IOException;
import java.io.InputStream;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.InconsistentException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.internal.OSSUtils;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.AppendObjectResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.UploadFileRequest;
import junit.framework.Assert;

/**
 * Examples about how to enable and check CRC for uploading and downloading data.
 *
 */
public class CRCSample {
    
    private static String endpoint = "<endpoint, http://oss-cn-hangzhou.aliyuncs.com>";
    private static String accessKeyId = "<accessKeyId>";
    private static String accessKeySecret = "<accessKeySecret>";
    private static String bucketName = "<bucketName>";
    private static String uploadFile = "<uploadFile>";
    private static String key = "crc-sample.txt";    

    
    public static void main(String[] args) throws IOException { 
    	
    	String content = "Hello OSS, Hi OSS, OSS OK.";

    	// CRC check is enabled by default for upload or download. To turn it off, use the commented code below.
    	// ClientConfiguration config = new ClientConfiguration();
    	// config.setCrcCheckEnabled(false);
    	// OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {
        	
        	// putObject/uploadPart/uploadFile are automatically enabled with CRC.
        	// However, appendObject needs to call AppendObjectRequest.setInitCRC to enable CRC.
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossClient.deleteObject(bucketName, key);
            
            // First data appending
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, 
                    new ByteArrayInputStream(content.getBytes())).withPosition(0L);
            
            appendObjectRequest.setInitCRC(0L); // because it's first append, the previous data is empty and thus CRC is 0.
            AppendObjectResult appendObjectResult = ossClient.appendObject(appendObjectRequest);
            
            // Second data appending
            appendObjectRequest = new AppendObjectRequest(bucketName, key, 
            		new ByteArrayInputStream(content.getBytes()));
            appendObjectRequest.setPosition(appendObjectResult.getNextPosition());
            appendObjectRequest.setInitCRC(appendObjectResult.getClientCRC());// the initCRC is the first's CRC.
            appendObjectResult = ossClient.appendObject(appendObjectRequest);
            
            ossClient.deleteObject(bucketName, key);

            // Upload with checkpoint, it supports CRC as well.
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            // Sets the upload file with a local file.
            uploadFileRequest.setUploadFile(uploadFile);
            // Sets the concurrent task number to 5 (default is 1).
            uploadFileRequest.setTaskNum(5);
            // Sets the part size to 1MB (default is 100K)
            uploadFileRequest.setPartSize(1024 * 1024 * 1);
            // Enable checkpoint (by default is off for checkpoint enabled upload)
            uploadFileRequest.setEnableCheckpoint(true);
            
            ossClient.uploadFile(uploadFileRequest);
            
            // Download with CRC. Note that range download does not support CRC.
            OSSObject ossObject = ossClient.getObject(bucketName, key);
            Assert.assertNull(ossObject.getClientCRC());
            Assert.assertNotNull(ossObject.getServerCRC());
            
            InputStream stream = ossObject.getObjectContent();
            while (stream.read() != -1) {
            }
            stream.close();
            
            // Check if CRC is consistent.
            OSSUtils.checkChecksum(IOUtils.getCRCValue(stream), ossObject.getServerCRC(), ossObject.getRequestId());
            
            ossClient.deleteObject(bucketName, key);
                        
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
        } catch (InconsistentException ie) {
        	System.out.println("Caught an OSSException");
        	System.out.println("Request ID:      " + ie.getRequestId());
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            ossClient.shutdown();
        }
    }
}
