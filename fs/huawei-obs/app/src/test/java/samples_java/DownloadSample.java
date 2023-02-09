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

/**
 * This sample demonstrates how to download an object 
 * from OBS in different ways using the OBS SDK for Java.
 */
public class DownloadSample
{
    private static final String endPoint = "https://your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static ObsClient obsClient;
    
    private static String bucketName = "my-obs-bucket-demo";
    
    private static String objectKey = "my-obs-object-key-demo";
    
    private static String localFilePath = "/temp/" + objectKey;
    
    public static void main(String[] args)
        throws IOException
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
            obsClient = new ObsClient(ak, sk, config);
            
            /*
             * Create bucket 
             */
            System.out.println("Create a new bucket for demo\n");
            obsClient.createBucket(bucketName);
            
            /*
             * Upload an object to your bucket
             */
            System.out.println("Uploading a new object to OBS from a file\n");
            obsClient.putObject(bucketName, objectKey, createSampleFile());
            
            System.out.println("Downloading an object\n");
            
            /*
             * Download the object as an inputstream and display it directly 
             */
            simpleDownload();
            
            File localFile = new File(localFilePath);
            if (!localFile.getParentFile().exists())
            {
                localFile.getParentFile().mkdirs();
            }
            
            System.out.println("Downloading an object to file:" + localFilePath + "\n");
            /*
             * Download the object to a file
             */
            downloadToLocalFile();
            
            System.out.println("Deleting object  " + objectKey + "\n");
            obsClient.deleteObject(bucketName, objectKey, null);
            
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
    
    private static void downloadToLocalFile()
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
    
    private static void simpleDownload()
        throws ObsException, IOException
    {
        ObsObject obsObject = obsClient.getObject(bucketName, objectKey, null);
        displayTextInputStream(obsObject.getObjectContent());
    }
    
    private static void displayTextInputStream(InputStream input)
        throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true)
        {
            String line = reader.readLine();
            if (line == null)
                break;
            
            System.out.println("\t" + line);
        }
        System.out.println();
        
        reader.close();
    }
    
    private static File createSampleFile()
        throws IOException
    {
        File file = File.createTempFile("obs-java-sdk-", ".txt");
        file.deleteOnExit();
        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("0123456789011234567890\n");
        writer.close();
        
        return file;
    }
    
}
