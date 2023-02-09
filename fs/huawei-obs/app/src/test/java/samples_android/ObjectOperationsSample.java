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
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.BucketCorsRule;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.OptionsInfoRequest;
import com.obs.services.model.BucketCors;
import com.obs.services.model.ObsObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * This sample demonstrates how to do object-related operations
 * (such as create/delete/get/copy object, do object ACL/OPTIONS)
 * on OBS using the OBS SDK for Android.
 */
public class ObjectOperationsSample extends Activity
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
                AsyncTask<Void, Void, String> task = new ObjectOperationsTask();
                task.execute();
            }
        });
    }
    
    class ObjectOperationsTask extends AsyncTask<Void, Void, String>
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
                 * Create object
                 */
                String content = "Hello OBS";
                obsClient.putObject(bucketName, objectKey, new ByteArrayInputStream(content.getBytes("UTF-8")), null);
                sb.append("Create object:" + objectKey + " successfully!\n\n");
                
                /*
                 * Get object metadata
                 */
                sb.append("Getting object metadata");
                ObjectMetadata metadata = obsClient.getObjectMetadata(bucketName, objectKey, null);
                sb.append("\t" + metadata);
                
                /*
                 * Get object
                 */
                sb.append("\n\nGetting object content");
                ObsObject obsObject = obsClient.getObject(bucketName, objectKey, null);
                sb.append("\tobject content:" + ServiceUtils.toString(obsObject.getObjectContent()));
                
                /*
                 * Copy object
                 */
                String sourceBucketName = bucketName;
                String destBucketName = bucketName;
                String sourceObjectKey = objectKey;
                String destObjectKey = objectKey + "-back";
                sb.append("Copying object\n\n");
                obsClient.copyObject(sourceBucketName, sourceObjectKey, destBucketName, destObjectKey);
                
                /*
                 * Options object
                 */
                doObjectOptions();
                
                /*
                 * Put/Get object acl operations
                 */
                doObjectAclOperations();
                
                /*
                 * Delete object
                 */
                sb.append("Deleting objects\n\n");
                obsClient.deleteObject(bucketName, objectKey, null);
                obsClient.deleteObject(bucketName, destObjectKey, null);
                
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
        
        private void doObjectOptions()
            throws ObsException
        {
            
            BucketCors bucketCors = new BucketCors();
            BucketCorsRule rule = new BucketCorsRule();
            rule.getAllowedHeader().add("Authorization");
            rule.getAllowedOrigin().add("http://www.a.com");
            rule.getAllowedOrigin().add("http://www.b.com");
            rule.getExposeHeader().add("x-obs-test1");
            rule.getExposeHeader().add("x-obs-test2");
            rule.setMaxAgeSecond(100);
            rule.getAllowedMethod().add("HEAD");
            rule.getAllowedMethod().add("GET");
            rule.getAllowedMethod().add("PUT");
            bucketCors.getRules().add(rule);
            obsClient.setBucketCors(bucketName, bucketCors);
            
            sb.append("Options object\n\n");
            OptionsInfoRequest optionInfo = new OptionsInfoRequest();
            optionInfo.setOrigin("http://www.a.com");
            optionInfo.getRequestHeaders().add("Authorization");
            optionInfo.getRequestMethod().add("PUT");
            sb.append(obsClient.optionsObject(bucketName, objectKey, optionInfo));
        }
        
        private void doObjectAclOperations()
            throws ObsException
        {
            sb.append("Setting object ACL to public-read \n\n");
            
            obsClient.setObjectAcl(bucketName, objectKey, AccessControlList.REST_CANNED_PUBLIC_READ);
            
            sb.append("Getting object ACL " + obsClient.getObjectAcl(bucketName, objectKey) + "\n\n");
            
            sb.append("Setting object ACL to private \n\n");
            
            obsClient.setObjectAcl(bucketName, objectKey, AccessControlList.REST_CANNED_PRIVATE);
            
            sb.append("Getting object ACL " + obsClient.getObjectAcl(bucketName, objectKey) + "\n\n");
        }
        
    }
    
}
