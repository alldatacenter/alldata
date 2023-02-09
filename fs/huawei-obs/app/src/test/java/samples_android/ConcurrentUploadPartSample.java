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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.Multipart;
import com.obs.services.model.PartEtag;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

/**
 * This sample demonstrates how to multipart upload an object concurrently
 * from OBS using the OBS SDK for Android.
 */
public class ConcurrentUploadPartSample extends Activity
{
    
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static String bucketName = "my-obs-bucket-demo";
    
    private static String objectKey = "my-obs-object-key-demo";
    
    private static ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    private static List<PartEtag> partETags = Collections.synchronizedList(new ArrayList<PartEtag>());
    
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
                AsyncTask<Void, Void, String> task = new ConcurrentUploadPartTask();
                task.execute();
            }
        });
    }
    
    private static class PartUploader implements Runnable
    {
        
        private File sampleFile;
        
        private long offset;
        
        private long partSize;
        
        private int partNumber;
        
        private String uploadId;
        
        public PartUploader(File sampleFile, long offset, long partSize, int partNumber, String uploadId)
        {
            this.sampleFile = sampleFile;
            this.offset = offset;
            this.partSize = partSize;
            this.partNumber = partNumber;
            this.uploadId = uploadId;
        }
        
        @Override
        public void run()
        {
            try
            {
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setObjectKey(objectKey);
                uploadPartRequest.setUploadId(this.uploadId);
                uploadPartRequest.setFile(this.sampleFile);
                uploadPartRequest.setPartSize(this.partSize);
                uploadPartRequest.setOffset(this.offset);
                uploadPartRequest.setPartNumber(this.partNumber);
                
                UploadPartResult uploadPartResult = obsClient.uploadPart(uploadPartRequest);
                sb.append("Part#" + this.partNumber + " done\n\n");
                partETags.add(new PartEtag(uploadPartResult.getEtag(), uploadPartResult.getPartNumber()));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    class ConcurrentUploadPartTask extends AsyncTask<Void, Void, String>
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
                 * Claim a upload id firstly
                 */
                String uploadId = claimUploadId();
                sb.append("Claiming a new upload id " + uploadId + "\n\n");
                
                long partSize = 5 * 1024 * 1024l;// 5MB
                File sampleFile = createSampleFile();
                long fileLength = sampleFile.length();
                
                long partCount = fileLength % partSize == 0 ? fileLength / partSize : fileLength / partSize + 1;
                
                if (partCount > 10000)
                {
                    throw new RuntimeException("Total parts count should not exceed 10000");
                }
                else
                {
                    sb.append("Total parts count " + partCount + "\n\n");
                }
                
                /*
                 * Upload multiparts to your bucket
                 */
                sb.append("Begin to upload multiparts to OBS from a file\n\n");
                for (int i = 0; i < partCount; i++)
                {
                    long offset = i * partSize;
                    long currPartSize = (i + 1 == partCount) ? fileLength - offset : partSize;
                    executorService.execute(new PartUploader(sampleFile, offset, currPartSize, i + 1, uploadId));
                }
                
                /*
                 * Wait for all tasks to finish
                 */
                executorService.shutdown();
                while (!executorService.isTerminated())
                {
                    try
                    {
                        executorService.awaitTermination(5, TimeUnit.SECONDS);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
                
                /*
                 * Verify whether all tasks are finished
                 */
                if (partETags.size() != partCount)
                {
                    throw new IllegalStateException("Some parts are not finished");
                }
                else
                {
                    sb.append("Succeed to complete multiparts into an object named " + objectKey + "\n\n");
                }
                
                /*
                 * View all parts uploaded recently
                 */
                listAllParts(uploadId);
                
                /*
                 * Complete to upload multiparts
                 */
                completeMultipartUpload(uploadId);
                
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
        
        private String claimUploadId()
            throws ObsException
        {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey);
            InitiateMultipartUploadResult result = obsClient.initiateMultipartUpload(request);
            return result.getUploadId();
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
        
        private void completeMultipartUpload(String uploadId)
            throws ObsException
        {
            // Make part numbers in ascending order
            Collections.sort(partETags, new Comparator<PartEtag>()
            {
                
                @Override
                public int compare(PartEtag o1, PartEtag o2)
                {
                    return o1.getPartNumber() - o2.getPartNumber();
                }
            });
            
            sb.append("Completing to upload multiparts\n\n");
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, partETags);
            obsClient.completeMultipartUpload(completeMultipartUploadRequest);
        }
        
        private void listAllParts(String uploadId)
            throws ObsException
        {
            sb.append("Listing all parts......");
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, objectKey, uploadId);
            ListPartsResult partListing = obsClient.listParts(listPartsRequest);
            
            for (Multipart part : partListing.getMultipartList())
            {
                sb.append("\tPart#" + part.getPartNumber() + ", ETag=" + part.getEtag());
            }
            sb.append("\n");
        }
        
    }
    
}
