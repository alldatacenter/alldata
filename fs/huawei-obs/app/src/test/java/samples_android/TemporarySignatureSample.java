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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.ObsConvertor;
import com.obs.services.internal.V2Convertor;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.BucketCors;
import com.obs.services.model.BucketCorsRule;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This sample demonstrates how to do common operations in temporary signature way
 * on OBS using the OBS SDK for Android.
 */
public class TemporarySignatureSample extends Activity
{
    
    private static final String endPoint = "http://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static String bucketName = "my-obs-bucket-demo";
    
    private static String objectKey = "my-obs-object-key-demo";
    
    private static ObsClient obsClient;
    
    private static StringBuffer sb;
    
    private static OkHttpClient httpClient;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sb = new StringBuffer();
        
        httpClient = new OkHttpClient.Builder().followRedirects(false)
            .retryOnConnectionFailure(false)
            .cache(null)
            .build();
        
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(endPoint);
        config.setAuthType(AuthTypeEnum.OBS);
        
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
                AsyncTask<Void, Void, String> task = new TemporarySignatureTask();
                task.execute();
            }
        });
    }
    
    class TemporarySignatureTask extends AsyncTask<Void, Void, String>
    {
        
        @Override
        protected String doInBackground(Void... params)
        {
            try
            {
                
                /*
                * Create bucket
                */
                doCreateBucket();
                
                /*
                 * Set/Get/Delete bucket cors
                 */
                doBucketCorsOperations();
                
                /*
                 * Create object
                 */
                doCreateObject();
                
                /*
                 * Get object
                 */
                doGetObject();
                
                /*
                 * Set/Get object acl
                 */
                doObjectAclOperations();
                
                /*
                 * Delete object
                 */
                doDeleteObject();
                
                /*
                 * Delete bucket
                 */
                doDeleteBucket();
                
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
        
        private Request.Builder getBuilder(TemporarySignatureResponse res) {
    		Request.Builder builder = new Request.Builder();
    		for (Map.Entry<String, String> entry : res.getActualSignedRequestHeaders().entrySet()) {
    			builder.header(entry.getKey(), entry.getValue());
    		}
    		return builder.url(res.getSignedUrl());
    	}
        
        private void doDeleteObject()
            throws IOException, ObsException
        {
            TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.DELETE, 300);
            req.setBucketName(bucketName);
            req.setObjectKey(objectKey);
            TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
            sb.append("Deleting object using temporary signature url:");
            sb.append("\t" + res.getSignedUrl() + "\n");
            getResponse(getBuilder(res).delete().build());
            sb.append("\n\n");
            
        }
        
        private void doObjectAclOperations()
            throws ObsException, IOException
        {
            
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("x-obs-acl", "public-read");
            
            TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.PUT, 300);
            req.setBucketName(bucketName);
            req.setObjectKey(objectKey);
            req.setHeaders(headers);
            req.setSpecialParam(SpecialParamEnum.ACL);
            TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
            
            sb.append("Setting object ACL to public-read using temporary signature url:");
            sb.append("\t" + res.getSignedUrl() + "\n");
            getResponse(getBuilder(res).put(RequestBody.create(null, "".getBytes("UTF-8"))).build());
            sb.append("\n\n");
            
            TemporarySignatureRequest req2 = new TemporarySignatureRequest(HttpMethodEnum.GET, 300);
            req2.setBucketName(bucketName);
            req2.setObjectKey(objectKey);
            req2.setSpecialParam(SpecialParamEnum.ACL);
            TemporarySignatureResponse res2 = obsClient.createTemporarySignature(req2);
            sb.append("Getting object ACL using temporary signature url:");
            sb.append("\t" + res2.getSignedUrl() + "\n");
            
            getResponse(getBuilder(res2).get().build());
            sb.append("\n\n");
        }
        
        private void doGetObject()
            throws ObsException, IOException
        {
            TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.GET, 300);
            req.setBucketName(bucketName);
            req.setObjectKey(objectKey);
            TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
            sb.append("Getting object using temporary signature url:");
            sb.append("\t" + res.getSignedUrl() + "\n");
            getResponse(getBuilder(res).get().build());
            sb.append("\n\n");
        }
        
        private void doCreateObject()
            throws ObsException, IOException
        {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "text/plain");
            
            TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.PUT, 300);
            req.setBucketName(bucketName);
            req.setObjectKey(objectKey);
            req.setHeaders(headers);
            
            TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
            
            sb.append("Createing object using temporary signature url:");
            sb.append("\t" + res.getSignedUrl() + "\n");
            getResponse(getBuilder(res).put(RequestBody.create(MediaType.parse("text/plain"), "Hello OBS".getBytes("UTF-8"))).build());
            sb.append("\n\n");
        }
        
        private void doBucketCorsOperations()
            throws ObsException, IOException, ServiceException
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
            
            String requestXml = ObsConvertor.getInstance().transBucketCors(bucketCors);
            String requestXmlMd5 = ServiceUtils.computeMD5(requestXml);
            
            Map<String, String> headers = new HashMap<String, String>();
            
            headers.put("Content-Type", "application/xml");
            headers.put("Content-MD5", requestXmlMd5);
            
            
            TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.PUT, 300);
            req.setBucketName(bucketName);
            req.setHeaders(headers);
            req.setSpecialParam(SpecialParamEnum.CORS);
            TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
            
            sb.append("Setting bucket CORS using temporary signature url:");
            sb.append("\t" + res.getSignedUrl() + "\n");
            
            getResponse(getBuilder(res).put(RequestBody.create(MediaType.parse("application/xml"), requestXml.getBytes("UTF-8"))).build());
            sb.append("\n\n");
            
            TemporarySignatureRequest req2 = new TemporarySignatureRequest(HttpMethodEnum.GET, 300);
            req2.setBucketName(bucketName);
            req2.setSpecialParam(SpecialParamEnum.CORS);
            TemporarySignatureResponse res2 = obsClient.createTemporarySignature(req2);
            sb.append("Getting bucket CORS using temporary signature url:");
            sb.append("\t" + res2.getSignedUrl() + "\n");
            
            getResponse(getBuilder(res2).get().build());
            sb.append("\n\n");
            
        }
        
        private void doDeleteBucket()
            throws ObsException, IOException
        {
            TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.DELETE, 300);
            req.setBucketName(bucketName);
            TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
            sb.append("Deleting bucket using temporary signature url:");
            sb.append("\t" + res.getSignedUrl() + "\ns");
            getResponse(getBuilder(res).delete().build());
            sb.append("\n\n");
        }
        
        private void doCreateBucket()
            throws ObsException, IOException
        {
            TemporarySignatureRequest req = new TemporarySignatureRequest(HttpMethodEnum.PUT, 300);
            req.setBucketName(bucketName);
            TemporarySignatureResponse res = obsClient.createTemporarySignature(req);
            sb.append("Creating bucket using temporary signature url:");
            sb.append("\t" + res.getSignedUrl() + "\n");
            String location = "your-location";
            Request request = getBuilder(res).put(RequestBody.create(null, "<CreateBucketConfiguration><Location>" +location+ "</Location></CreateBucketConfiguration>".getBytes())).url(res.getSignedUrl()).build();
            getResponse(request);
            sb.append("\n\n");
        }
        
        private void getResponse(Request request)
            throws IOException
        {
            Call c = httpClient.newCall(request);
            Response res = c.execute();
            sb.append("\tStatus:" + res.code());
            if (res.body() != null)
            {
                String content = res.body().string();
                if (content == null || content.trim().equals(""))
                {
                    sb.append("\n");
                }
                else
                {
                    sb.append("\tContent:" + content + "\n\n");
                }
            }
            else
            {
                sb.append("\n");
            }
            res.close();
        }
        
    }
    
}
