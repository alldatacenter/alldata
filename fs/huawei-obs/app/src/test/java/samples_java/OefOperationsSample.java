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

import java.io.IOException;

import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.oef.services.OefClient;
import com.oef.services.model.CompressBean;
import com.oef.services.model.CreateAsyncFetchJobsRequest;
import com.oef.services.model.CreateAsynchFetchJobsResult;
import com.oef.services.model.PutExtensionPolicyRequest;
import com.oef.services.model.QueryExtensionPolicyResult;
import com.oef.services.model.FetchBean;
import com.oef.services.model.QueryAsynchFetchJobsResult;
import com.oef.services.model.TranscodeBean;

public class OefOperationsSample {
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static OefClient oefClient;
    
    private static String bucketName = "my-obs-bucket-demo";
	    
	    public static void main(String[] args)
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
	            oefClient = new OefClient(ak, sk, config);
	            
	            /*
	             * put bucket extension policy
	             */
	            putExtensionPolicy();
	            
	            /*
	             * get bucket extension policy
	             */
	            queryExtensionPolicy();
	            
	            /*
	             * delete bucket extension policy
	             */
	            deleteExtensionPolicy();
	            
	            /*
	             * create asynch fetch job
	             */
	            createFetchJob();

	            /*
	             * query asynch fetch job
	             */
	            queryFetchJob();
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
	            if (oefClient != null)
	            {
	                try
	                {
	                    /*
	                     * Close obs client 
	                     */
	                    oefClient.close();
	                }
	                catch (IOException e)
	                {
	                }
	            }
	        }
	    }
	    
	    private static void putExtensionPolicy() {
	    	 System.out.println("putExtensionPolicy\n");
	    	 PutExtensionPolicyRequest request = new PutExtensionPolicyRequest();
			 request.setFetch(new FetchBean("open","xxx"));
			 request.setTranscode(new TranscodeBean("closed", "xxx"));
			 request.setCompress(new CompressBean("open", "xxx"));
			 System.out.println(request.toString());
			 oefClient.putExtensionPolicy(bucketName, request);
		}
	    
	    private static void queryExtensionPolicy() {
	    	System.out.println("queryExtensionPolicy\n");
	    	QueryExtensionPolicyResult ret = oefClient.queryExtensionPolicy(bucketName);
	    	System.out.println(ret.toString());
		}
	    
	    private static void deleteExtensionPolicy() {
	    	System.out.println("deleteExtensionPolicy\n");
	    	oefClient.deleteExtensionPolicy(bucketName);
		}
	    
	    private static void createFetchJob() {
	    	System.out.println("createFetchJob\n");
	    	CreateAsyncFetchJobsRequest request = new CreateAsyncFetchJobsRequest("https://your-url", bucketName);
	    	System.out.println(request.toString());
	    	CreateAsynchFetchJobsResult ret = oefClient.createFetchJob(request);
			System.out.println(ret.toString());
	    }
	    
	    private static void queryFetchJob() {
	    	System.out.println("queryFetchJob\n");
	    	String jobId = "your-jobId";
	    	QueryAsynchFetchJobsResult ret = oefClient.queryFetchJob(bucketName, jobId);
	    	System.out.println(ret.toString());
	    }

}
