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
import java.io.IOException;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;

/**
 * Image process examples.
 *
 */
public class ImageSample {
    
    private static String endpoint = "<endpoint, http://oss-cn-hangzhou.aliyuncs.com>";
    private static String accessKeyId = "<accessKeyId>";
    private static String accessKeySecret = "<accessKeySecret>";
    private static String bucketName = "<bucketName>";
    private static String key = "example.jpg";
    
    
    public static void main(String[] args) throws IOException {        

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {
            // resize
            String style = "image/resize,m_fixed,w_100,h_100";  
            GetObjectRequest request = new GetObjectRequest(bucketName, key);
            request.setProcess(style);
            
            ossClient.getObject(request, new File("example-resize.jpg"));
            
            // crop
            style = "image/crop,w_100,h_100,x_100,y_100,r_1"; 
            request = new GetObjectRequest(bucketName, key);
            request.setProcess(style);
            
            ossClient.getObject(request, new File("example-crop.jpg"));
            
            // rotate
            style = "image/rotate,90"; 
            request = new GetObjectRequest(bucketName, key);
            request.setProcess(style);
            
            ossClient.getObject(request, new File("example-rotate.jpg"));
            
            // sharpen
            style = "image/sharpen,100"; 
            request = new GetObjectRequest(bucketName, key);
            request.setProcess(style);
            
            ossClient.getObject(request, new File("example-sharpen.jpg"));
            
            // add watermark into the image
            style = "image/watermark,text_SGVsbG8g5Zu-54mH5pyN5YqhIQ"; 
            request = new GetObjectRequest(bucketName, key);
            request.setProcess(style);
            
            ossClient.getObject(request, new File("example-watermark.jpg"));
            
            // convert format
            style = "image/format,png"; 
            request = new GetObjectRequest(bucketName, key);
            request.setProcess(style);
            
            ossClient.getObject(request, new File("example-format.png"));
            
            // image information
            style = "image/info"; 
            request = new GetObjectRequest(bucketName, key);
            request.setProcess(style);
            
            ossClient.getObject(request, new File("example-info.txt"));
            
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
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            ossClient.shutdown();
        }
    }
}
