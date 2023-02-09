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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.PartEtag;
import com.obs.services.model.UploadPartResult;


/**
 * This sample demonstrates how to upload multiparts to OBS 
 * using the OBS SDK for Java.
 */
public class SimpleMultipartUploadSample
{
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static ObsClient obsClient;
    
    private static String bucketName = "my-obs-bucket-demo";
    
    private static String objectKey = "my-obs-object-key-demo";
    
    public static void main(String[] args) throws IOException
    {
        ObsConfiguration config = new ObsConfiguration();
        config.setSocketTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setEndPoint(endPoint);
        try
        {
            /*
             * Constructs a obs client instance with your account for accessing OBS
             */
            obsClient = new ObsClient(ak, sk, config);
            
            /*
             * Create bucket 
             */
            obsClient.createBucket(bucketName);
            
            /*
             * Step 1: initiate multipart upload
             */
            System.out.println("Step 1: initiate multipart upload \n");
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest();
            request.setBucketName(bucketName);
            request.setObjectKey(objectKey);
            InitiateMultipartUploadResult result = obsClient.initiateMultipartUpload(request);
            
            /*
             * Step 2: upload a part
             */
            System.out.println("Step 2: upload part \n");
            UploadPartResult uploadPartResult = obsClient.uploadPart(bucketName, objectKey, result.getUploadId(), 1, new FileInputStream(createSampleFile()));
            
            /*
             * Step 3: complete multipart upload
             */
            System.out.println("Step 3: complete multipart upload \n");
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest();
            completeMultipartUploadRequest.setBucketName(bucketName);
            completeMultipartUploadRequest.setObjectKey(objectKey);
            completeMultipartUploadRequest.setUploadId(result.getUploadId());
            PartEtag partEtag = new PartEtag();
            partEtag.setPartNumber(uploadPartResult.getPartNumber());
            partEtag.setEtag(uploadPartResult.getEtag());
            completeMultipartUploadRequest.getPartEtag().add(partEtag);
            obsClient.completeMultipartUpload(completeMultipartUploadRequest);
        }
        catch (ObsException e)
        {
            System.out.println("Response Code: " + e.getResponseCode());
            System.out.println("Error Message: " + e.getErrorMessage());
            System.out.println("Error Code:       " + e.getErrorCode());
            System.out.println("Request ID:      " + e.getErrorRequestId());
            System.out.println("Host ID:           " + e.getErrorHostId());
        }
        finally
        {
            if (obsClient != null)
            {
                try
                {
                    /*
                     * Close obs client 
                     */
                    obsClient.close();
                }
                catch (IOException e)
                {
                }
            }
        }
        
    }
    
    private static File createSampleFile()
        throws IOException
    {
        File file = File.createTempFile("obs-java-sdk-", ".txt");
        file.deleteOnExit();
        
        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        for (int i = 0; i < 1000000; i++)
        {
            writer.write(UUID.randomUUID() + "\n");
            writer.write(UUID.randomUUID() + "\n");
        }
        writer.flush();
        writer.close();
        
        return file;
    }
}
