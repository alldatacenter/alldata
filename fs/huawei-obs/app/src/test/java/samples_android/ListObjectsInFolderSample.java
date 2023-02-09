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
package samples_android;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

/**
 * This sample demonstrates how to list objects under a specified folder of a bucket
 * from OBS using the OBS SDK for Android.
 */
public class ListObjectsInFolderSample extends Activity
{
    
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static String bucketName = "my-obs-bucket-demo";
    
    private static ObsClient obsClient;
    
    private static StringBuffer sb;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sb = new StringBuffer();
        
        ObsConfiguration config = new ObsConfiguration();
        config.setSocketTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setEndPoint(endPoint);
        
        /*
        * Constructs a obs client instance with your account for accessing OBS
        */
        obsClient = new ObsClient(ak, sk, config);
        final TextView tv = (TextView)findViewById(R.id.tv);
        tv.setText("Click to start test");
        
        tv.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                tv.setClickable(false);
                AsyncTask<Void, Void, String> task = new ListObjectsInFolderTask();
                task.execute();
            }
        });
    }
    
    class ListObjectsInFolderTask extends AsyncTask<Void, Void, String>
    {
        
        @Override
        protected String doInBackground(Void... params)
        {
            try
            {
                
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
                sb.append("Put " + keys.size() + " objects completed.");
                sb.append("\n");
                
                /*
                 * List all objects in folder src0/
                 */
                sb.append("List all objects in folder src0/ \n\n");
                listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setPrefix("src0/");
                objectListing = obsClient.listObjects(listObjectsRequest);
                for (ObsObject object : objectListing.getObjects())
                {
                    sb.append("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag() + "]");
                }
                sb.append("\n");
                
                /*
                 * List all objects in sub folder src0/test0/
                 */
                sb.append("List all objects in folder src0/test0/ \n\n");
                listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setPrefix("src0/test0/");
                objectListing = obsClient.listObjects(listObjectsRequest);
                for (ObsObject object : objectListing.getObjects())
                {
                    sb.append("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag() + "]");
                }
                sb.append("\n");
                
                /*
                 * List all objects group by folder
                 */
                sb.append("List all objects group by folder \n\n");
                listObjectsRequest = new ListObjectsRequest(bucketName);
                listObjectsRequest.setDelimiter("/");
                
                objectListing = obsClient.listObjects(listObjectsRequest);
                sb.append("Root path:");
                for (ObsObject object : objectListing.getObjects())
                {
                    sb.append("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag() + "]");
                }
                listObjectsByPrefix(listObjectsRequest, objectListing);
                
                sb.append("\n");
                
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
                
                sb.append("\n\nDelete results:");
                
                DeleteObjectsResult deleteObjectsResult = obsClient.deleteObjects(request);
                for (DeleteObjectResult object : deleteObjectsResult.getDeletedObjectResults())
                {
                    sb.append("\t" + object);
                }
                
                return sb.toString();
            }
            catch (ObsException e)
            {
                sb.append("\n\n");
                sb.append("Response Code:" + e.getResponseCode())
                    .append("\n\n")
                    .append("Error Message:" + e.getErrorMessage())
                    .append("\n\n")
                    .append("Error Code:" + e.getErrorCode())
                    .append("\n\n")
                    .append("Request ID:" + e.getErrorRequestId())
                    .append("\n\n")
                    .append("Host ID:" + e.getErrorHostId());
                return sb.toString();
            }
            catch (Exception e)
            {
                sb.append("\n\n");
                sb.append(e.getMessage());
                return sb.toString();
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
        
        @Override
        protected void onPostExecute(String result)
        {
            TextView tv = (TextView)findViewById(R.id.tv);
            tv.setText(result);
            tv.setOnClickListener(null);
            tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        }
        
        private void listObjectsByPrefix(ListObjectsRequest listObjectsRequest, ObjectListing objectListing)
            throws ObsException
        {
            for (String prefix : objectListing.getCommonPrefixes())
            {
                sb.append("Folder " + prefix + ":");
                listObjectsRequest.setPrefix(prefix);
                objectListing = obsClient.listObjects(listObjectsRequest);
                for (ObsObject object : objectListing.getObjects())
                {
                    sb.append("\t" + object.getObjectKey() + " etag[" + object.getMetadata().getEtag() + "]");
                }
                listObjectsByPrefix(listObjectsRequest, objectListing);
            }
        }
        
    }
    
}
