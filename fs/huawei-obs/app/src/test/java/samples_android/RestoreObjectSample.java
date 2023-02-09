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

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RestoreObjectRequest.RestoreObjectStatus;
import com.obs.services.model.RestoreTierEnum;
import com.obs.services.model.StorageClassEnum;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

/**
 * This sample demonstrates how to download an cold object
 * from OBS using the OBS SDK for Android.
 */
public class RestoreObjectSample extends Activity
{
    
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static String bucketName = "my-obs-cold-bucket-demo";
    
    private static String objectKey = "my-obs-cold-object-key-demo";
    
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
                AsyncTask<Void, Void, String> task = new RestoreObjectTask();
                task.execute();
            }
        });
    }
    
    class RestoreObjectTask extends AsyncTask<Void, Void, String>
    {
        
        @Override
        protected String doInBackground(Void... params)
        {
            try
            {
                
                /*
                 * Create a cold bucket
                 */
                sb.append("Create a new cold bucket for demo\n\n");
                ObsBucket bucket = new ObsBucket();
                bucket.setBucketName(bucketName);
                bucket.setBucketStorageClass(StorageClassEnum.COLD);
                obsClient.createBucket(bucket);
                
                /*
                 * Create a cold object
                 */
                sb.append("Create a new cold object for demo\n\n");
                String content = "Hello OBS";
                obsClient.putObject(bucketName, objectKey, new ByteArrayInputStream(content.getBytes("UTF-8")), null);
                
                /*
                 * Restore the cold object
                 */
                sb.append("Restore the cold object");
                RestoreObjectRequest restoreObjectRequest =
                    new RestoreObjectRequest(bucketName, objectKey, null, 1, RestoreTierEnum.EXPEDITED);
                sb.append("\t" + (obsClient.restoreObject(restoreObjectRequest) == RestoreObjectStatus.INPROGRESS));
                
                /*
                 * Wait 6 minute to get the object
                 */
                Thread.sleep(60 * 6 * 1000);
                
                /*
                 * Get the cold object status
                 */
                sb.append("Get the cold object status");
                restoreObjectRequest = new RestoreObjectRequest(bucketName, objectKey, null, 1, RestoreTierEnum.EXPEDITED);
                sb.append("\t" + (obsClient.restoreObject(restoreObjectRequest) == RestoreObjectStatus.AVALIABLE) + "\n\n");
                
                /*
                 * Get the cold object
                 */
                sb.append("Get the cold object");
                sb.append("\tcontent:" + ServiceUtils.toString(obsClient.getObject(bucketName, objectKey, null).getObjectContent()));
                
                /*
                 * Delete the cold object
                 */
                obsClient.deleteObject(bucketName, objectKey, null);
                
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
