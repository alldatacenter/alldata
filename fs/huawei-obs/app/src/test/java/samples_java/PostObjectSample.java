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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.PostSignatureRequest;
import com.obs.services.model.PostSignatureResponse;

/**
 * This sample demonstrates how to post object under specified bucket from
 * OBS using the OBS SDK for Java.
 */
public class PostObjectSample
{
    private static final String protocol = "https://";
    
    /*
     * Example: obs.cn-north-1.myhuaweicloud.com
     */
    private static final String endPoint = "your-endpoint";
    
    private static final String ak = "*** Provide your Access Key ***";
    
    private static final String sk = "*** Provide your Secret Key ***";
    
    private static ObsClient obsClient;
    
    private static String bucketName = "my-obs-bucket-demo";
    
    private static String objectKey = "my-obs-object-key-demo";
    
    private static AuthTypeEnum authType = AuthTypeEnum.OBS;
    
    public static void main(String[] args)
        throws IOException
    {
        ObsConfiguration config = new ObsConfiguration();
        config.setEndPoint(endPoint);
        config.setAuthType(authType);
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
             * Create sample file
             */
            File sampleFile = createSampleFile();
            
            /*
             * Claim a post object request
             */
            PostSignatureRequest request = new PostSignatureRequest();
            request.setExpires(3600);
            
            Map<String, Object> formParams = new HashMap<String, Object>();
            
            String contentType = "text/plain";
            if(authType == AuthTypeEnum.OBS) {
                formParams.put("x-obs-acl", "public-read");
            }else {
                formParams.put("acl", "public-read");
            }
            formParams.put("content-type", contentType);
            
            request.setFormParams(formParams);
            
            PostSignatureResponse response = obsClient.createPostSignature(request);
            
            
            formParams.put("key", objectKey);
            formParams.put("policy", response.getPolicy());
            
            if(authType == AuthTypeEnum.OBS) {
                formParams.put("signature", response.getSignature());
                formParams.put("accesskeyid", ak);
            }else {
                formParams.put("signature", response.getSignature());
                formParams.put("AwsAccesskeyid", ak);
            }
            
            String postUrl = protocol + bucketName + "." + endPoint;
            System.out.println("Creating object in browser-based way");
            System.out.println("\tpost url:" + postUrl);
            
           String res =  formUpload(postUrl, formParams, sampleFile, contentType);
           System.out.println("\tresponse:"+ res);
        }
        catch (Exception ex)
        {
            if (ex instanceof ObsException)
            {
                ObsException e = (ObsException)ex;
                System.out.println("Response Code: " + e.getResponseCode());
                System.out.println("Error Message: " + e.getErrorMessage());
                System.out.println("Error Code:       " + e.getErrorCode());
                System.out.println("Request ID:      " + e.getErrorRequestId());
                System.out.println("Host ID:           " + e.getErrorHostId());
            }
            else
            {
                ex.printStackTrace();
            }
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
    
    private static String formUpload(String postUrl, Map<String, Object> formFields, File sampleFile, String contentType)
    {
        String res = "";
        HttpURLConnection conn = null;
        String boundary = "9431149156168";
        BufferedReader reader = null;
        DataInputStream in = null;
        OutputStream out = null;
        try
        {
            URL url = new URL(postUrl);
            conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "OBS/Test");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            out = new DataOutputStream(conn.getOutputStream());
            
            // text
            if (formFields != null)
            {
                StringBuffer strBuf = new StringBuffer();
                Iterator<Entry<String, Object>> iter = formFields.entrySet().iterator();
                int i = 0;
                
                while (iter.hasNext())
                {
                    Entry<String, Object> entry = iter.next();
                    String inputName = entry.getKey();
                    Object inputValue = entry.getValue();
                    
                    if (inputValue == null)
                    {
                        continue;
                    }
                    
                    if (i == 0)
                    {
                        strBuf.append("--").append(boundary).append("\r\n");
                        strBuf.append("Content-Disposition: form-data; name=\"" + inputName + "\"\r\n\r\n");
                        strBuf.append(inputValue);
                    }
                    else
                    {
                        strBuf.append("\r\n").append("--").append(boundary).append("\r\n");
                        strBuf.append("Content-Disposition: form-data; name=\"" + inputName + "\"\r\n\r\n");
                        strBuf.append(inputValue);
                    }
                    
                    i++;
                }
                out.write(strBuf.toString().getBytes());
            }
            
            // file
            String filename = sampleFile.getName();
            if (contentType == null || contentType.equals(""))
            {
                contentType = "application/octet-stream";
            }
            
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("\r\n").append("--").append(boundary).append("\r\n");
            strBuf.append("Content-Disposition: form-data; name=\"file\"; " + "filename=\"" + filename + "\"\r\n");
            strBuf.append("Content-Type: " + contentType + "\r\n\r\n");
            
            out.write(strBuf.toString().getBytes());
            
            in = new DataInputStream(new FileInputStream(sampleFile));
            int bytes = 0;
            byte[] bufferOut = new byte[1024];
            while ((bytes = in.read(bufferOut)) != -1)
            {
                out.write(bufferOut, 0, bytes);
            }
            
            byte[] endData = ("\r\n--" + boundary + "--\r\n").getBytes();
            out.write(endData);
            out.flush();
            
            // Read the returned data.
            strBuf = new StringBuffer();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                strBuf.append(line).append("\n");
            }
            res = strBuf.toString();
        }
        catch (Exception e)
        {
            System.out.println("Send post request exception: " + e);
            e.printStackTrace();
        }
        finally
        {
            if(out != null){
                try
                {
                    out.close();
                }
                catch (IOException e)
                {
                }
            }
            
            if(in != null){
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                }
            }
            if(reader != null){
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                }
            }
            if (conn != null)
            {
                conn.disconnect();
                conn = null;
            }
        }
        
        return res;
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
