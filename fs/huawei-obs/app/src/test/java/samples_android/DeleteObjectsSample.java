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

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.DeleteObjectsResult;
import com.obs.services.model.KeyAndVersion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This sample demonstrates how to delete objects under specified bucket
 * from OBS using the OBS SDK for Android.
 */
public class DeleteObjectsSample extends Activity
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
                AsyncTask<Void, Void, String> task = new DeleteObjectsTask();
                task.execute();
            }
        });
    }
    
    class DeleteObjectsTask extends AsyncTask<Void, Void, String>
    {
        
        @Override
        protected String doInBackground(Void... params)
        {
            try
            {
                /*
                 * Create bucket
                 */
                sb.append("Create a new bucket for demo\n\n");
                obsClient.createBucket(bucketName);
                
                /*
                 * Batch put objects into the bucket
                 */
                final String content = "Thank you for using Object Storage Service";
                final String keyPrefix = "MyObjectKey";
                List<String> keys = new ArrayList<String>();
                for (int i = 0; i < 100; i++)
                {
                    String key = keyPrefix + i;
                    InputStream instream = new ByteArrayInputStream(content.getBytes());
                    obsClient.putObject(bucketName, key, instream, null);
                    sb.append("Succeed to put object " + key);
                    keys.add(key);
                }
                sb.append("\n");
                
                /*
                 * Delete all objects uploaded recently under the bucket
                 */
                sb.append("\n\nDeleting all objects\n\n");
                
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
                
                sb.append("Delete results:");
                
                DeleteObjectsResult deleteObjectsResult = obsClient.deleteObjects(request);
                for (DeleteObjectsResult.DeleteObjectResult object : deleteObjectsResult.getDeletedObjectResults())
                {
                    sb.append("\t" + object);
                }
                
                sb.append("\n\nError results:");
                
                for (DeleteObjectsResult.ErrorResult error : deleteObjectsResult.getErrorResults())
                {
                    sb.append("\t" + error);
                }
                
                sb.append("\n");
                
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
        
    }
    
}
