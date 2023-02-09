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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.DeleteObjectsResult;
import com.obs.services.model.DeleteObjectsResult.DeleteObjectResult;
import com.obs.services.model.KeyAndVersion;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsObject;

/**
 * This sample demonstrates how to list objects under a specified folder of a bucket 
 * from OBS using the OBS SDK for Java.
 */
public class ListObjectsInFolderSample
{
    
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static ObsClient obsClient;
    
    private static String bucketName = "my-obs-bucket-demo";
    
    public static void main(String[] args) throws UnsupportedEncodingException
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
            
            final String content = "Hello OBS";
            final String keyPrefix = "MyObjectKey";
            final String folderPrefix = "src";
            final String subFolderPrefix = "test";
            ObjectListing objectListing = null;
            ListObjectsRequest listObjectsRequest = null;
            List<String> keys = new ArrayList<String>();
            /*
             * First prepare folders and sub folders
             */

            for (int i = 0; i < 5; i++)
            {
                String key = folderPrefix + i + "/";
                obsClient.putObject(bucketName, key, new ByteArrayInputStream(new byte[0]), null);
                keys.add(key);
                
                for (int j = 0; j < 3; j++)
                {
                    String subKey = key + subFolderPrefix + j + "/";
                    obsClient.putObject(bucketName, subKey, new ByteArrayInputStream(new byte[0]));
                    keys.add(subKey);
                }
            }
            
            /*
             * Insert 2 objects in each folder
             */
            objectListing  = obsClient.listObjects(bucketName);
            for(ObsObject object : objectListing.getObjects()){
                for(int i=0;i<2;i++){
                    String objectKey = object.getObjectKey() + keyPrefix + i;
                    obsClient.putObject(bucketName, objectKey, new ByteArrayInputStream(content.getBytes("UTF-8")), null);
                    keys.add(objectKey);
                }
            }
          /*
          * Insert 2 objects in root path
          */
            obsClient.putObject(bucketName, keyPrefix+0, new ByteArrayInputStream(content.getBytes("UTF-8")), null);
            obsClient.putObject(bucketName, keyPrefix+1, new ByteArrayInputStream(content.getBytes("UTF-8")), null);
            keys.add(keyPrefix+0);
            keys.add(keyPrefix+1);
            System.out.println("Put " + keys.size() + " objects completed.");
            System.out.println();
           
            /*
             * List all objects in folder src0/
             */
            System.out.println("List all objects in folder src0/ \n");
            listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setPrefix("src0/");
            objectListing  = obsClient.listObjects(listObjectsRequest);
            for(ObsObject object : objectListing.getObjects()){
                System.out.println("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag()+ "]");
            }
            System.out.println();
            
            /*
             * List all objects in sub folder src0/test0/
             */
            System.out.println("List all objects in folder src0/test0/ \n");
            listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setPrefix("src0/test0/");
            objectListing  = obsClient.listObjects(listObjectsRequest);
            for(ObsObject object : objectListing.getObjects()){
                System.out.println("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag()+ "]");
            }
            System.out.println();
            
            /*
             * List all objects group by folder
             */
            System.out.println("List all objects group by folder \n");
            listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setDelimiter("/");
            
            objectListing  = obsClient.listObjects(listObjectsRequest);
            System.out.println("Root path:");
            for(ObsObject object : objectListing.getObjects()){
                System.out.println("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag()+ "]");
            }
            listObjectsByPrefix(listObjectsRequest, objectListing);
            
            System.out.println();
            
            
            /*
             * Delete all the objects created
             */
            DeleteObjectsRequest request = new DeleteObjectsRequest();
            request.setBucketName(bucketName);
            request.setQuiet(false);
            KeyAndVersion[] kvs = new KeyAndVersion[keys.size()];
            int index = 0;
            for (String key : keys)
            {
                kvs[index++] = new KeyAndVersion(key);
            }
            
            request.setKeyAndVersions(kvs);
            
            System.out.println("Delete results:");
            
            DeleteObjectsResult deleteObjectsResult = obsClient.deleteObjects(request);
            for (DeleteObjectResult object : deleteObjectsResult.getDeletedObjectResults())
            {
                System.out.println("\t" + object);
            }
            
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

    private static void listObjectsByPrefix(ListObjectsRequest listObjectsRequest, ObjectListing objectListing) throws ObsException
    {
        for(String prefix : objectListing.getCommonPrefixes()){
            System.out.println("Folder " + prefix + ":");
            listObjectsRequest.setPrefix(prefix);
            objectListing  = obsClient.listObjects(listObjectsRequest);
            for(ObsObject object : objectListing.getObjects()){
                System.out.println("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag()+ "]");
            }
            listObjectsByPrefix(listObjectsRequest, objectListing);
        }
    }
}
