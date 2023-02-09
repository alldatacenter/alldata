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
import com.obs.services.model.ObsObject;
import com.obs.services.model.PutObjectRequest;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

/**
 * This sample demonstrates how to create an empty folder under
 * specified bucket to OBS using the OBS SDK for Android.
 */
public class CreateFolderSample extends Activity
{
    
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static String bucketName = "my-obs-bucket-demo";
    
    private static ObsClient obsClient;
    
    private static StringBuffer sb = new StringBuffer();
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
                AsyncTask<Void, Void, String> task = new CreateFolderTask();
                task.execute();
            }
        });
    }
    
    class CreateFolderTask extends AsyncTask<Void, Void, String>
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
                 * Way 1:
                 * Create an empty folder without request body, note that the key must be
                 * suffixed with a slash
                 */
                final String keySuffixWithSlash1 = "MyObjectKey1/";
                obsClient.putObject(bucketName, keySuffixWithSlash1, new ByteArrayInputStream(new byte[0]));
                sb.append("Creating an empty folder " + keySuffixWithSlash1 + "\n\n");
                
                /*
                 * Verify whether the size of the empty folder is zero
                 */
                ObsObject object = obsClient.getObject(bucketName, keySuffixWithSlash1);
                sb.append(
                    "Size of the empty folder '" + object.getObjectKey() + "' is " + object.getMetadata().getContentLength() + "\n\n");
                object.getObjectContent().close();
                
                /*
                 * Way 2:
                 * Create an empty folder without request body, note that the key must be
                 * suffixed with a slash
                 */
                final String keySuffixWithSlash2 = "MyObjectKey2/";
                PutObjectRequest request = new PutObjectRequest();
                request.setBucketName(bucketName);
                request.setObjectKey(keySuffixWithSlash2);
                obsClient.putObject(request);
                sb.append("Creating an empty folder " + keySuffixWithSlash2 + "\n\n");
                
                /*
                 * Verify whether the size of the empty folder is zero
                 */
                object = obsClient.getObject(bucketName, keySuffixWithSlash2, null);
                sb.append("Size of the empty folder '" + object.getObjectKey() + "' is " + object.getMetadata().getContentLength());
                object.getObjectContent().close();
                
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
