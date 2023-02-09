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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.ObsObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

/**
 * This sample demonstrates how to download an object
 * from OBS in different ways using the OBS SDK for Android.
 */
public class DownloadSample extends Activity
{
    
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static String bucketName = "my-obs-bucket-demo";
    
    private static String objectKey = "my-obs-object-key-demo";
    
    private static String localFilePath = "/storage/sdcard/" + objectKey;
    
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
                AsyncTask<Void, Void, String> task = new DownloadTask();
                task.execute();
            }
        });
    }
    
    class DownloadTask extends AsyncTask<Void, Void, String>
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
                 * Upload an object to your bucket
                 */
                sb.append("Uploading a new object to OBS from a file\n\n");
                obsClient.putObject(bucketName, objectKey, createSampleFile());
                
                sb.append("Downloading an object\n\n");
                
                /*
                 * Download the object as an inputstream and display it directly
                 */
                simpleDownload();
                
                File localFile = new File(localFilePath);
                if (!localFile.getParentFile().exists())
                {
                    localFile.getParentFile().mkdirs();
                }
                
                sb.append("Downloading an object to file:" + localFilePath + "\n\n");
                /*
                 * Download the object to a file
                 */
                downloadToLocalFile();
                
                sb.append("Deleting object  " + objectKey + "\n\n");
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
        
        private void downloadToLocalFile()
            throws ObsException, IOException
        {
            ObsObject obsObject = obsClient.getObject(bucketName, objectKey, null);
            ReadableByteChannel rchannel = Channels.newChannel(obsObject.getObjectContent());
            
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            WritableByteChannel wchannel = Channels.newChannel(new FileOutputStream(new File(localFilePath)));
            
            while (rchannel.read(buffer) != -1)
            {
                buffer.flip();
                wchannel.write(buffer);
                buffer.clear();
            }
            rchannel.close();
            wchannel.close();
        }
        
        private void simpleDownload()
            throws ObsException, IOException
        {
            ObsObject obsObject = obsClient.getObject(bucketName, objectKey, null);
            displayTextInputStream(obsObject.getObjectContent());
        }
        
        private void displayTextInputStream(InputStream input)
            throws IOException
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            while (true)
            {
                String line = reader.readLine();
                if (line == null)
                    break;
                
                sb.append("\t" + line);
            }
            sb.append("\n");
            
            reader.close();
        }
        
        private File createSampleFile()
            throws IOException
        {
            File file = File.createTempFile("obs-android-sdk-", ".txt");
            file.deleteOnExit();
            Writer writer = new OutputStreamWriter(new FileOutputStream(file));
            writer.write("abcdefghijklmnopqrstuvwxyz\n\n");
            writer.write("0123456789011234567890\n\n");
            writer.close();
            
            return file;
        }
        
    }
    
}
