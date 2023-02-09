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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSEncryptionClient;
import com.aliyun.oss.OSSEncryptionClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.crypto.KmsEncryptionMaterials;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;

/**
 * This sample demonstrates how to upload an object by append mode 
 * to Aliyun OSS using the OSS SDK for Java.
 */
public class EncryptionClientKmsSample {
    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";

    private static String bucketName = "*** Provide bucket name ***";
    private static String key = "*** Provide key ***";
    private static String content = "jdjfhdus6182042795hlnf12s8yhfs976y2nfoshhnsdfsf235bvsmnhtskbcfd!";
    private static String cmk = "<yourCmkId>";
    private static String region = "<yourKmsRegion>";

    public static void main(String[] args) throws IOException {  
        OSSEncryptionClient ossEncryptionClient = null;
        try {            
            // Create encryption materials description.
            Map<String, String> matDesc = new HashMap<String, String>();
            matDesc.put("desc-key", "desc-value");

            // Create encryption materials.
            KmsEncryptionMaterials encryptionMaterials = new KmsEncryptionMaterials(region, cmk, matDesc);        

            // Create encryption client.
            ossEncryptionClient = new OSSEncryptionClientBuilder().
                                build(endpoint, accessKeyId, accessKeySecret, encryptionMaterials);
            // Put Object
            ossEncryptionClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()));

            // Get Object
            OSSObject ossObject = ossEncryptionClient.getObject(bucketName, key);
            BufferedReader reader = new BufferedReader(new InputStreamReader(ossObject.getObjectContent()));
            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            reader.close();

            System.out.println("Put plain text: " + content);
            System.out.println("Get and decrypted text: " + buffer.toString());

            // range-get
            int start = 17;
            int end = 35;
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            getObjectRequest.setRange(start, end);
            ossObject = ossEncryptionClient.getObject(getObjectRequest);
            reader = new BufferedReader(new InputStreamReader(ossObject.getObjectContent()));
            buffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            reader.close();

            System.out.println("Range-Get plain text:" + content.substring(start, end + 1));
            System.out.println("Range-Get decrypted text: " + buffer.toString());
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
            ossEncryptionClient.shutdown();
        }
    }
}
