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
import java.io.InputStream;
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
 * This sample demonstrates how to list objects under specified bucket 
 * from OBS using the OBS SDK for Java.
 */
public class ListObjectsSample
{
    
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static ObsClient obsClient;
    
    private static String bucketName = "my-obs-bucket-demo";
    
    public static void main(String[] args)
        throws UnsupportedEncodingException
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
            ObjectListing objectListing = null;
            ListObjectsRequest listObjectsRequest = null;
            /*
             * First insert 100 objects for demo
             */
            List<String> keys = new ArrayList<String>();
            for (int i = 0; i < 100; i++)
            {
                String key = keyPrefix + i;
                InputStream instream = new ByteArrayInputStream(content.getBytes("UTF-8"));
                obsClient.putObject(bucketName, key, instream, null);
                keys.add(key);
            }
            System.out.println("Put " + keys.size() + " objects completed.");
            
            /*
             * List objects using default parameters, will return up to 1000 objects
             */
            System.out.println("List objects using default parameters:\n");
            objectListing = obsClient.listObjects(bucketName);
            for (ObsObject object : objectListing.getObjects())
            {
                System.out.println("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag() + "]");
            }
            System.out.println();
            
            /*
             * List the first 10 objects 
             */
            System.out.println("List the first 10 objects :\n");
            listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setMaxKeys(10);
            objectListing = obsClient.listObjects(listObjectsRequest);
            for (ObsObject object : objectListing.getObjects())
            {
                System.out.println("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag() + "]");
            }
            System.out.println();
            
            String theSecond10ObjectsMarker = objectListing.getNextMarker();
            
            /*
             * List the second 10 objects using marker 
             */
            System.out.println("List the second 10 objects using marker:\n");
            listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setMaxKeys(10);
            listObjectsRequest.setMarker(theSecond10ObjectsMarker);
            objectListing = obsClient.listObjects(listObjectsRequest);
            for (ObsObject object : objectListing.getObjects())
            {
                System.out.println("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag() + "]");
            }
            System.out.println();
            
            /*
             * List objects with prefix and max keys
             */
            System.out.println("List objects with prefix and max keys:\n");
            listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setPrefix(keyPrefix + "2");
            listObjectsRequest.setMaxKeys(5);
            objectListing = obsClient.listObjects(listObjectsRequest);
            for (ObsObject object : objectListing.getObjects())
            {
                System.out.println("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag() + "]");
            }
            System.out.println();
            
            /*
             * List all the objects in way of pagination
             */
            System.out.println("List all the objects in way of pagination:\n");
            listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setMaxKeys(10);
            String nextMarker = null;
            int index = 1;
            do
            {
                listObjectsRequest.setMarker(nextMarker);
                objectListing = obsClient.listObjects(listObjectsRequest);
                System.out.println("Page:" + index++ + "\n");
                for (ObsObject object : objectListing.getObjects())
                {
                    System.out.println("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag() + "]");
                }
                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
            System.out.println();
            
            /*
             * Delete all the objects created
             */
            DeleteObjectsRequest request = new DeleteObjectsRequest();
            request.setBucketName(bucketName);
            request.setQuiet(false);
            KeyAndVersion[] kvs = new KeyAndVersion[keys.size()];
            index = 0;
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
}
