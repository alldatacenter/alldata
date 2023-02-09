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
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.PartEtag;
import com.obs.services.model.UploadPartResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

/**
 * This sample demonstrates how to upload multiparts to OBS
 * using the OBS SDK for Android.
 */
public class SimpleMultipartUploadSample extends Activity
{
    
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static String bucketName = "my-obs-bucket-demo";
    
    private static String objectKey = "my-obs-object-key-demo";
    
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
                AsyncTask<Void, Void, String> task = new SimpleMultipartUploadTask();
                task.execute();
            }
        });
    }
    
    class SimpleMultipartUploadTask extends AsyncTask<Void, Void, String>
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
                 * Step 1: initiate multipart upload
                 */
                sb.append("Step 1: initiate multipart upload \n\n");
                InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest();
                request.setBucketName(bucketName);
                request.setObjectKey(objectKey);
                InitiateMultipartUploadResult result = obsClient.initiateMultipartUpload(request);
                
                /*
                 * Step 2: upload a part
                 */
                sb.append("Step 2: upload part \n\n");
                UploadPartResult uploadPartResult =
                    obsClient.uploadPart(bucketName, objectKey, result.getUploadId(), 1, new FileInputStream(createSampleFile()));
                
                /*
                 * Step 3: complete multipart upload
                 */
                sb.append("Step 3: complete multipart upload \n\n");
                CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest();
                completeMultipartUploadRequest.setBucketName(bucketName);
                completeMultipartUploadRequest.setObjectKey(objectKey);
                completeMultipartUploadRequest.setUploadId(result.getUploadId());
                PartEtag partEtag = new PartEtag();
                partEtag.setPartNumber(uploadPartResult.getPartNumber());
                partEtag.seteTag(uploadPartResult.getEtag());
                completeMultipartUploadRequest.getPartEtag().add(partEtag);
                obsClient.completeMultipartUpload(completeMultipartUploadRequest);
                
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
        
        private File createSampleFile()
            throws IOException
        {
            File file = File.createTempFile("obs-android-sdk-", ".txt");
            file.deleteOnExit();
            
            Writer writer = new OutputStreamWriter(new FileOutputStream(file));
            for (int i = 0; i < 1000000; i++)
            {
                writer.write(UUID.randomUUID() + "\n\n");
                writer.write(UUID.randomUUID() + "\n\n");
            }
            writer.flush();
            writer.close();
            
            return file;
        }
    }
    
}
