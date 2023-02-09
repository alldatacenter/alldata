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
import com.obs.services.model.BucketVersioningConfiguration;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.DeleteObjectsResult;
import com.obs.services.model.DeleteObjectsResult.DeleteObjectResult;
import com.obs.services.model.KeyAndVersion;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsObject;
import com.obs.services.model.VersionOrDeleteMarker;
import com.obs.services.model.VersioningStatusEnum;


/**
 * This sample demonstrates how to list versions under specified bucket 
 * from OBS using the OBS SDK for Java.
 */
public class ListVersionsSample
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
            
            /*
             * Enable bucket versioning
             */
            obsClient.setBucketVersioning(bucketName, new BucketVersioningConfiguration(VersioningStatusEnum.ENABLED));
            
            final String content = "Hello OBS";
            final String keyPrefix = "MyObjectKey";
            final String folderPrefix = "src";
            final String subFolderPrefix = "test";
            ObjectListing objectListing = null;
            ListVersionsResult listVersionResult = null;
            List<String> keys = new ArrayList<String>();
            VersionOrDeleteMarker[] versionsToDelete;
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
            objectListing = obsClient.listObjects(bucketName);
            for (ObsObject object : objectListing.getObjects())
            {
                for (int i = 0; i < 2; i++)
                {
                    String objectKey = object.getObjectKey() + keyPrefix + i;
                    obsClient.putObject(bucketName, objectKey, new ByteArrayInputStream(content.getBytes("UTF-8")), null);
                    keys.add(objectKey);
                }
            }
            /*
            * Insert 2 objects in root path
            */
            obsClient.putObject(bucketName, keyPrefix + 0, new ByteArrayInputStream(content.getBytes("UTF-8")), null);
            obsClient.putObject(bucketName, keyPrefix + 1, new ByteArrayInputStream(content.getBytes("UTF-8")), null);
            keys.add(keyPrefix + 0);
            keys.add(keyPrefix + 1);
            System.out.println("Put " + keys.size() + " objects completed.");
            System.out.println();
            
            /*
             * List versions using default parameters, will return up to 1000 objects
             */
            System.out.println("List versions using default parameters:\n");
            listVersionResult = obsClient.listVersions(bucketName);
            versionsToDelete = listVersionResult.getVersions();
            for (VersionOrDeleteMarker versionOrDeleteMarker : listVersionResult.getVersions())
            {
                System.out.println("\t" + versionOrDeleteMarker.getKey() + " etag[" + versionOrDeleteMarker.getEtag() + "]" + " versionid["
                    + versionOrDeleteMarker.getVersionId() + "]");
            }
            System.out.println();
            
            /*
             * List all the versions in way of pagination
             */
            System.out.println("List all the versions in way of pagination:\n");
            int index = 1;
            ListVersionsRequest listRequest = new ListVersionsRequest(bucketName);
            listRequest.setMaxKeys(10);
            do
            {
                listVersionResult = obsClient.listVersions(listRequest);
                System.out.println("Page:" + index++ + "\n");
                for (VersionOrDeleteMarker versionOrDeleteMarker : listVersionResult.getVersions())
                {
                    System.out.println("\t" + versionOrDeleteMarker.getKey() + " etag[" + versionOrDeleteMarker.getEtag() + "]"
                        + " versionid[" + versionOrDeleteMarker.getVersionId() + "]");
                }
                listRequest.setKeyMarker(listVersionResult.getNextKeyMarker());
                listRequest.setVersionIdMarker(listVersionResult.getNextVersionIdMarker());
            } while (listVersionResult.isTruncated());
            System.out.println();
            
            /*
             * List all versions group by folder
             */
            System.out.println("List all versions group by folder \n");
            listRequest = new ListVersionsRequest(bucketName);
            listRequest.setDelimiter("/");
            listRequest.setMaxKeys(1000);
            listVersionResult = obsClient.listVersions(listRequest);
            System.out.println("Root path:");
            for (VersionOrDeleteMarker versionOrDeleteMarker : listVersionResult.getVersions())
            {
                System.out.println("\t" + versionOrDeleteMarker.getKey() + " etag[" + versionOrDeleteMarker.getEtag() + "]" + " versionid["
                    + versionOrDeleteMarker.getVersionId() + "]");
            }
            listVersionsByPrefix(listVersionResult);
            
            System.out.println();
            
            /*
             * Delete all the objects created
             */
            DeleteObjectsRequest request = new DeleteObjectsRequest();
            request.setBucketName(bucketName);
            request.setQuiet(false);
            index = 0;
            KeyAndVersion[] kvs = new KeyAndVersion[versionsToDelete.length];
            for (VersionOrDeleteMarker versionOrDeleteMarker : versionsToDelete)
            {
                kvs[index++] = new KeyAndVersion(versionOrDeleteMarker.getKey(), versionOrDeleteMarker.getVersionId());
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
    
    private static void listVersionsByPrefix(ListVersionsResult listVersionResult)
        throws ObsException
    {
        for (String prefix : listVersionResult.getCommonPrefixes())
        {
            System.out.println("Folder " + prefix + ":");
            ListVersionsRequest request = new ListVersionsRequest(bucketName);
            request.setPrefix(prefix);
            request.setDelimiter("/");
            request.setMaxKeys(1000);
            listVersionResult = obsClient.listVersions(request);
            for (VersionOrDeleteMarker versionOrDeleteMarker : listVersionResult.getVersions())
            {
                System.out.println("\t" + versionOrDeleteMarker.getKey() + " etag[" + versionOrDeleteMarker.getEtag() + "]"
                    + " versionid[" + versionOrDeleteMarker.getVersionId() + "]");
            }
            listVersionsByPrefix(listVersionResult);
        }
    }
}
