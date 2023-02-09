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

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

/**
 * This sample demonstrates how to list versions under specified bucket
 * from OBS using the OBS SDK for Android.
 */
public class ListVersionsSample extends Activity
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
                AsyncTask<Void, Void, String> task = new ListVersionsTask();
                task.execute();
            }
        });
    }
    
    class ListVersionsTask extends AsyncTask<Void, Void, String>
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
                sb.append("Put " + keys.size() + " objects completed.");
                sb.append("\n");
                
                /*
                 * List versions using default parameters, will return up to 1000 objects
                 */
                sb.append("List versions using default parameters:\n\n");
                listVersionResult = obsClient.listVersions(bucketName);
                versionsToDelete = listVersionResult.getVersions();
                for (VersionOrDeleteMarker versionOrDeleteMarker : listVersionResult.getVersions())
                {
                    sb.append("\t" + versionOrDeleteMarker.getKey() + " etag[" + versionOrDeleteMarker.getEtag() + "]" + " versionid["
                        + versionOrDeleteMarker.getVersionId() + "]");
                }
                sb.append("\n");
                
                /*
                 * List all the versions in way of pagination
                 */
                sb.append("List all the versions in way of pagination:\n\n");
                ListVersionsRequest listRequest = new ListVersionsRequest(bucketName);
                listRequest.setMaxKeys(10);
                int index = 1;
                do
                {
                    listVersionResult = obsClient.listVersions(listRequest);
                    sb.append("Page:" + index++ + "\n\n");
                    for (VersionOrDeleteMarker versionOrDeleteMarker : listVersionResult.getVersions())
                    {
                        sb.append("\t" + versionOrDeleteMarker.getKey() + " etag[" + versionOrDeleteMarker.getEtag() + "]" + " versionid["
                            + versionOrDeleteMarker.getVersionId() + "]");
                    }
                    listRequest.setKeyMarker(listVersionResult.getNextKeyMarker());
                    listRequest.setVersionIdMarker(listVersionResult.getNextVersionIdMarker());
                } while (listVersionResult.isTruncated());
                sb.append("\n");
                
                /*
                 * List all versions group by folder
                 */
                sb.append("List all versions group by folder \n\n");
                listRequest = new ListVersionsRequest(bucketName);
                listRequest.setMaxKeys(1000);
                listRequest.setDelimiter("/");
                listVersionResult = obsClient.listVersions(listRequest);
                sb.append("Root path:");
                for (VersionOrDeleteMarker versionOrDeleteMarker : listVersionResult.getVersions())
                {
                    sb.append("\t" + versionOrDeleteMarker.getKey() + " etag[" + versionOrDeleteMarker.getEtag() + "]" + " versionid["
                        + versionOrDeleteMarker.getVersionId() + "]");
                }
                listVersionsByPrefix(listVersionResult);
                
                sb.append("\n");
                
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
        
        private void listVersionsByPrefix(ListVersionsResult listVersionResult)
            throws ObsException
        {
            for (String prefix : listVersionResult.getCommonPrefixes())
            {
                sb.append("Folder " + prefix + ":");
                ListVersionsRequest request = new ListVersionsRequest(bucketName);
                request.setPrefix(prefix);
                request.setDelimiter("/");
                request.setMaxKeys(1000);
                listVersionResult = obsClient.listVersions(request);
                for (VersionOrDeleteMarker versionOrDeleteMarker : listVersionResult.getVersions())
                {
                    sb.append("\t" + versionOrDeleteMarker.getKey() + " etag[" + versionOrDeleteMarker.getEtag() + "]" + " versionid["
                        + versionOrDeleteMarker.getVersionId() + "]");
                }
                listVersionsByPrefix(listVersionResult);
            }
        }
        
    }
    
}
