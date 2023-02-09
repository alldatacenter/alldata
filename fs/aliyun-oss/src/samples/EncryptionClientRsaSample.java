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
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSEncryptionClient;
import com.aliyun.oss.OSSEncryptionClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.crypto.EncryptionMaterials;
import com.aliyun.oss.crypto.SimpleRSAEncryptionMaterials;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;

/**
 * This sample demonstrates how to upload an object by append mode 
 * to Aliyun OSS using the OSS SDK for Java.
 */
public class EncryptionClientRsaSample {
    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";

    private static String bucketName = "*** Provide bucket name ***";
    private static String key = "*** Provide key ***";
    private static String content = "jdjfhdus6182042795hlnf12s8yhfs976y2nfoshhnsdfsf235bvsmnhtskbcfd!";

    // Input your rsa private key. e.g.this private key is in pkcs1 codec and pem wraped. 
    private static final String PRIVATE_PKCS1_PEM = 
                    "-----BEGIN RSA PRIVATE KEY-----\n" + 
                    "MIICWwIBAAKBgQCokfiAVXXf5ImFzKDw+XO/UByW6mse2QsIgz3ZwBtMNu59fR5z\n" + 
                    "ttSx+8fB7vR4CN3bTztrP9A6bjoN0FFnhlQ3vNJC5MFO1PByrE/MNd5AAfSVba93\n" + 
                    "I6sx8NSk5MzUCA4NJzAUqYOEWGtGBcom6kEF6MmR1EKib1Id8hpooY5xaQIDAQAB\n" + 
                    "AoGAOPUZgkNeEMinrw31U3b2JS5sepG6oDG2CKpPu8OtdZMaAkzEfVTJiVoJpP2Y\n" + 
                    "nPZiADhFW3e0ZAnak9BPsSsySRaSNmR465cG9tbqpXFKh9Rp/sCPo4Jq2n65yood\n" + 
                    "JBrnGr6/xhYvNa14sQ6xjjfSgRNBSXD1XXNF4kALwgZyCAECQQDV7t4bTx9FbEs5\n" + 
                    "36nAxPsPM6aACXaOkv6d9LXI7A0J8Zf42FeBV6RK0q7QG5iNNd1WJHSXIITUizVF\n" + 
                    "6aX5NnvFAkEAybeXNOwUvYtkgxF4s28s6gn11c5HZw4/a8vZm2tXXK/QfTQrJVXp\n" + 
                    "VwxmSr0FAajWAlcYN/fGkX1pWA041CKFVQJAG08ozzekeEpAuByTIOaEXgZr5MBQ\n" + 
                    "gBbHpgZNBl8Lsw9CJSQI15wGfv6yDiLXsH8FyC9TKs+d5Tv4Cvquk0efOQJAd9OC\n" + 
                    "lCKFs48hdyaiz9yEDsc57PdrvRFepVdj/gpGzD14mVerJbOiOF6aSV19ot27u4on\n" + 
                    "Td/3aifYs0CveHzFPQJAWb4LCDwqLctfzziG7/S7Z74gyq5qZF4FUElOAZkz718E\n" + 
                    "yZvADwuz/4aK0od0lX9c4Jp7Mo5vQ4TvdoBnPuGoyw==\n" + 
                    "-----END RSA PRIVATE KEY-----";
    // Input your rsa public key. e.g.this public key is in x509 codec and pem wraped. 
    private static final String PUBLIC_X509_PEM = 
                    "-----BEGIN PUBLIC KEY-----\n" + 
                    "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCokfiAVXXf5ImFzKDw+XO/UByW\n" + 
                    "6mse2QsIgz3ZwBtMNu59fR5zttSx+8fB7vR4CN3bTztrP9A6bjoN0FFnhlQ3vNJC\n" + 
                    "5MFO1PByrE/MNd5AAfSVba93I6sx8NSk5MzUCA4NJzAUqYOEWGtGBcom6kEF6MmR\n" + 
                    "1EKib1Id8hpooY5xaQIDAQAB\n" + 
                    "-----END PUBLIC KEY-----";

    public static void main(String[] args) throws IOException {  
        OSSEncryptionClient ossEncryptionClient = null;
        try {
            // Create Rsa keyPair.
            RSAPrivateKey privateKey = SimpleRSAEncryptionMaterials.getPrivateKeyFromPemPKCS1(PRIVATE_PKCS1_PEM);
            RSAPublicKey publicKey = SimpleRSAEncryptionMaterials.getPublicKeyFromPemX509(PUBLIC_X509_PEM);
            KeyPair keyPair = new KeyPair(publicKey, privateKey);

            // Create encryption materials description.
            Map<String, String> matDesc = new HashMap<String, String>();
            matDesc.put("desc-key", "desc-value");

            // Create encryption client.
            SimpleRSAEncryptionMaterials encryptionMaterials = new SimpleRSAEncryptionMaterials(keyPair, matDesc);
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
