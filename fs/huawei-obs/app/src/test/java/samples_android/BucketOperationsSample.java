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
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.BucketLoggingConfiguration;
import com.obs.services.model.BucketMetadataInfoRequest;
import com.obs.services.model.BucketMetadataInfoResult;
import com.obs.services.model.BucketQuota;
import com.obs.services.model.BucketStorageInfo;
import com.obs.services.model.BucketTagInfo;
import com.obs.services.model.BucketVersioningConfiguration;
import com.obs.services.model.LifecycleConfiguration;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.OptionsInfoRequest;
import com.obs.services.model.SSEAlgorithmEnum;
import com.obs.services.model.VersioningStatusEnum;
import com.obs.services.model.BucketCors;
import com.obs.services.model.WebsiteConfiguration;

import java.io.IOException;
import java.text.ParseException;

/**
 * This sample demonstrates how to do bucket-related operations
 * (such as do bucket ACL/CORS/Lifecycle/Logging/Website/Location/Tagging/OPTIONS)
 * on OBS using the OBS SDK for Android.
 */
public class BucketOperationsSample extends Activity
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
                AsyncTask<Void, Void, String> task = new BucketOperationsTask();
                task.execute();
            }
        });
    }

	class BucketOperationsTask extends AsyncTask<Void, Void, String>
    {
        
        @Override
        protected String doInBackground(Void... params)
        {
            try
            {
                /*
                * Put bucket operation
                */
                createBucket();
                
                /*
                 * Get bucket location operation
                 */
                getBucketLocation();
                
                /*
                 * Get bucket storageInfo operation
                 */
                getBucketStorageInfo();
                
                /*
                 * Put/Get bucket quota operations
                 */
                doBucketQuotaOperation();
                
                /*
                 * Put/Get bucket versioning operations
                 */
                doBucketVersioningOperation();
                
                /*
                 * Put/Get bucket acl operations
                 */
                doBucketAclOperation();
                
                /*
                 * Put/Get/Delete bucket cors operations
                 */
                doBucketCorsOperation();
                
                /*
                 * Options bucket operation
                 */
                optionsBucket();
                
                /*
                 * Get bucket metadata operation
                 */
                getBucketMetadata();
                
                /*
                 * Put/Get/Delete bucket lifecycle operations
                 */
                doBucketLifecycleOperation();
                
                /*
                 * Put/Get/Delete bucket logging operations
                 */
                doBucketLoggingOperation();
                
                /*
                 * Put/Get/Delete bucket website operations
                 */
                doBucketWebsiteOperation();
                
                /*
                 * Put/Get/Delete bucket tagging operations
                 */
                doBucketTaggingOperation();
                
                /*
                 * Put/Get/Delete bucket encryption operations
                 */
                doBucketEncryptionOperation();
                
                /*
                 * Delete bucket operation
                 */
                deleteBucket();
                
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
        
        private void optionsBucket()
            throws ObsException
        {
            sb.append("Options bucket\n\n");
            OptionsInfoRequest optionInfo = new OptionsInfoRequest();
            optionInfo.setOrigin("http://www.a.com");
            optionInfo.getRequestHeaders().add("Authorization");
            optionInfo.getRequestMethod().add("PUT");
            sb.append(obsClient.optionsBucket(bucketName, optionInfo));
        }
        
        private void getBucketMetadata()
            throws ObsException
        {
            sb.append("Getting bucket metadata\n\n");
            BucketMetadataInfoRequest request = new BucketMetadataInfoRequest(bucketName);
            request.setOrigin("http://www.a.com");
            request.getRequestHeaders().add("Authorization");
            BucketMetadataInfoResult result = obsClient.getBucketMetadata(request);
            sb.append("StorageClass:" + result.getBucketStorageClass());
            sb.append("\tAllowedOrigins " + result.getAllowOrigin());
            sb.append("\tAllowedMethods " + result.getAllowMethods());
            sb.append("\tAllowedHeaders " + result.getAllowHeaders());
            sb.append("\tExposeHeaders " + result.getExposeHeaders());
            sb.append("\tMaxAgeSeconds " + result.getMaxAge() + "\n\n");
            
            sb.append("Deleting bucket CORS\n\n");
            obsClient.deleteBucketCors(bucketName);
        }
        
        private void doBucketTaggingOperation()
            throws ObsException
        {
            sb.append("Setting bucket tagging\n\n");
            
            BucketTagInfo tagInfo = new BucketTagInfo();
            BucketTagInfo.TagSet tagSet = new BucketTagInfo.TagSet();
            tagSet.addTag("key1", "value1");
            tagSet.addTag("key2", "value2");
            tagInfo.setTagSet(tagSet);
            
            obsClient.setBucketTagging(bucketName, tagInfo);
            
            sb.append("Getting bucket tagging\n\n");
            
            sb.append(obsClient.getBucketTagging(bucketName));
            
            sb.append("Deleting bucket tagging\n\n");
            obsClient.deleteBucketTagging(bucketName);
        }
        
        private void doBucketEncryptionOperation() 
            throws ObsException 
        {
            System.out.println("Setting bucket encryption\n");
            
            BucketEncryption encryption = new BucketEncryption(SSEAlgorithmEnum.KMS);
//                encryption.setKmsKeyId("your kmsKeyId");
            obsClient.setBucketEncryption(bucketName, encryption);

            System.out.println("Gettting bucket encryption\n");
            System.out.println(obsClient.getBucketEncryption(bucketName));
            
            System.out.println("Deleting bucket encryption\n");
            obsClient.deleteBucketEncryption(bucketName);
                
        }
        
        private void doBucketVersioningOperation()
            throws ObsException
        {
            sb.append("Getting bucket versioning config " + obsClient.getBucketVersioning(bucketName) + "\n\n");
            //Enable bucket versioning
            obsClient.setBucketVersioning(bucketName, new BucketVersioningConfiguration(VersioningStatusEnum.ENABLED));
            sb.append("Current bucket versioning config " + obsClient.getBucketVersioning(bucketName) + "\n\n");
            //Suspend bucket versioning
            BucketVersioningConfiguration suspended = new BucketVersioningConfiguration(VersioningStatusEnum.SUSPENDED);
            obsClient.setBucketVersioning(bucketName, suspended);
            sb.append("Current bucket versioning config " + obsClient.getBucketVersioning(bucketName) + "\n\n");
        }
        
        private void doBucketQuotaOperation()
            throws ObsException
        {
            BucketQuota quota = new BucketQuota();
            //Set bucket quota to 1GB
            quota.setBucketQuota(1024 * 1024 * 1024);
            obsClient.setBucketQuota(bucketName, quota);
            sb.append("Getting bucket quota " + obsClient.getBucketQuota(bucketName) + "\n\n");
        }
        
        private void getBucketStorageInfo()
            throws ObsException
        {
            BucketStorageInfo storageInfo = obsClient.getBucketStorageInfo(bucketName);
            sb.append("Getting bucket storageInfo " + storageInfo + "\n\n");
        }
        
        private void doBucketAclOperation()
            throws ObsException
        {
            sb.append("Setting bucket ACL to public-read \n\n");
            
            obsClient.setBucketAcl(bucketName, AccessControlList.REST_CANNED_PUBLIC_READ);
            
            sb.append("Getting bucket ACL " + obsClient.getBucketAcl(bucketName) + "\n\n");
            
            sb.append("Setting bucket ACL to private \n\n");
            
            obsClient.setBucketAcl(bucketName, AccessControlList.REST_CANNED_PRIVATE);
            
            sb.append("Getting bucket ACL " + obsClient.getBucketAcl(bucketName) + "\n\n");
        }
        
        private void doBucketCorsOperation()
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
            
            sb.append("Setting bucket CORS\n\n");
            obsClient.setBucketCors(bucketName, bucketCors);
            
            sb.append("Getting bucket CORS:" + obsClient.getBucketCors(bucketName) + "\n\n");
            
        }
        
        private void doBucketLifecycleOperation()
            throws ObsException
        {
            final String ruleId0 = "delete obsoleted files";
            final String matchPrefix0 = "obsoleted/";
            final String ruleId1 = "delete temporary files";
            final String matchPrefix1 = "temporary/";
            final String ruleId2 = "delete temp files";
            final String matchPrefix2 = "temp/";
            
            LifecycleConfiguration lifecycleConfig = new LifecycleConfiguration();
            LifecycleConfiguration.Rule rule0 = lifecycleConfig.new Rule();
            rule0.setEnabled(true);
            rule0.setId(ruleId0);
            rule0.setPrefix(matchPrefix0);
            LifecycleConfiguration.Expiration expiration0 = lifecycleConfig.new Expiration();
            expiration0.setDays(10);
            
            rule0.setExpiration(expiration0);
            lifecycleConfig.addRule(rule0);
            
            LifecycleConfiguration.Rule rule1 = lifecycleConfig.new Rule();
            rule1.setEnabled(true);
            rule1.setId(ruleId1);
            rule1.setPrefix(matchPrefix1);
            LifecycleConfiguration.Expiration expiration1 = lifecycleConfig.new Expiration();
            try
            {
                expiration1.setDate(ServiceUtils.parseIso8601Date("2018-12-31T00:00:00"));
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
            rule1.setExpiration(expiration1);
            lifecycleConfig.addRule(rule1);
            
            LifecycleConfiguration.Rule rule2 = lifecycleConfig.new Rule();
            rule2.setEnabled(true);
            rule2.setId(ruleId2);
            rule2.setPrefix(matchPrefix2);
            LifecycleConfiguration.NoncurrentVersionExpiration noncurrentVersionExpiration =
                lifecycleConfig.new NoncurrentVersionExpiration();
            noncurrentVersionExpiration.setDays(10);
            rule2.setNoncurrentVersionExpiration(noncurrentVersionExpiration);
            lifecycleConfig.addRule(rule2);
            
            sb.append("Setting bucket lifecycle\n\n");
            obsClient.setBucketLifecycleConfiguration(bucketName, lifecycleConfig);
            
            sb.append("Getting bucket lifecycle:");
            LifecycleConfiguration result = obsClient.getBucketLifecycleConfiguration(bucketName);
            LifecycleConfiguration.Rule r0 = result.getRules().get(0);
            LifecycleConfiguration.Rule r1 = result.getRules().get(1);
            LifecycleConfiguration.Rule r2 = result.getRules().get(2);
            sb.append("\tRule0: Id=" + r0.getId() + ", Prefix=" + r0.getPrefix() + ", Status=" + r0.getEnabled() + ", ExpirationDays="
                + r0.getExpiration().getDays());
            sb.append("\tRule1: Id=" + r1.getId() + ", Prefix=" + r1.getPrefix() + ", Status=" + r1.getEnabled() + ", ExpirationTime="
                + r1.getExpiration().getDate());
            
            sb.append("\tRule1: Id=" + r2.getId() + ", Prefix=" + r2.getPrefix() + ", Status=" + r2.getEnabled()
                + ", NocurrentExpirationDays=" + r2.getNoncurrentVersionExpiration().getDays());
            sb.append("\n\n");
            
            sb.append("Deleting bucket lifecycle\n\n");
            obsClient.deleteBucketLifecycleConfiguration(bucketName);
        }
        
        private void doBucketLoggingOperation()
            throws ObsException
        {
            final String targetBucket = bucketName;
            final String targetPrefix = "log-";
            BucketLoggingConfiguration configuration = new BucketLoggingConfiguration();
            configuration.setTargetBucketName(targetBucket);
            configuration.setLogfilePrefix(targetPrefix);
            configuration.setAgency("test");
            
            sb.append("Setting bucket logging\n\n");
            obsClient.setBucketLoggingConfiguration(bucketName, configuration, true);
            
            sb.append("Getting bucket logging:");
            BucketLoggingConfiguration result = obsClient.getBucketLoggingConfiguration(bucketName);
            sb.append("\tTarget bucket=" + result.getTargetBucketName() + ", target prefix=" + result.getLogfilePrefix() + "\n\n");
            sb.append("\n\n");
            
            sb.append("Deleting bucket logging\n\n");
            obsClient.setBucketLoggingConfiguration(targetBucket, new BucketLoggingConfiguration());
        }
        
        private void doBucketWebsiteOperation()
            throws ObsException
        {
            WebsiteConfiguration websiteConfig = new WebsiteConfiguration();
            websiteConfig.setSuffix("index.html");
            websiteConfig.setKey("error.html");
            
            sb.append("Setting bucket website\n\n");
            obsClient.setBucketWebsiteConfiguration(bucketName, websiteConfig);
            
            sb.append("Getting bucket website:");
            WebsiteConfiguration result = obsClient.getBucketWebsiteConfiguration(bucketName);
            sb.append("\tIndex document=" + result.getKey() + ", error document=" + result.getSuffix() + "\n\n");
            
            sb.append("Deleting bucket website\n\n");
            obsClient.deleteBucketWebsiteConfiguration(bucketName);
        }
        
        private void deleteBucket()
            throws ObsException
        {
            sb.append("Deleting bucket " + bucketName + "\n\n");
            obsClient.deleteBucket(bucketName);
        }
        
        private void getBucketLocation()
            throws ObsException
        {
            String location = obsClient.getBucketLocation(bucketName);
            sb.append("Getting bucket location " + location + "\n\n");
        }
        
        private void createBucket()
            throws ObsException
        {
            ObsBucket obsBucket = new ObsBucket();
            obsBucket.setBucketName(bucketName);
            obsClient.createBucket(obsBucket);
            sb.append("Create bucket:" + bucketName + " successfully!\n\n");
        }
        
    }
    
}
