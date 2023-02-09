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
import java.io.FileInputStream;
import java.io.InputStream;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.PutObjectRequest;

public class TrafficLimitSample {
    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";

    private static String bucketName = "*** Provide bucket name ***";
    private static String key = "*** Provide key ***";

    public static void main(String[] args) throws Throwable {

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Put object form file with traffic limit
            InputStream inputStream = new FileInputStream("<yourlocalFile>");
            PutObjectRequest PutObjectRequest = new PutObjectRequest(bucketName, key, inputStream);
            // Set traffic limit speed 100KB/s
            PutObjectRequest.setTrafficLimit(100 * 1024 * 8);
            ossClient.putObject(PutObjectRequest);

            // Get object to local file with traffic limit
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            // Set traffic limit speed 100KB/s
            getObjectRequest.setTrafficLimit(100 * 1024 * 8);
            File localFile = new File("<yourLocalFile>");
            ossClient.getObject(getObjectRequest, localFile);
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
            ossClient.shutdown();
        }
    }

}