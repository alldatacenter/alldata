/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss;

import java.io.File;
import java.net.URL;

import org.junit.Test;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;

public class OSSTestUtils {
    
    
    @Test
    public void testOss(){
        
    }

    /**
     * Gets test file path.
     * **/
    public static String getTestFilePath(){
        Class<?> clazz = OSSTestUtils.class;

        String name = clazz.getName().replaceAll("[.]", "/");
        URL path = clazz.getResource("/"+name+".class");

        File file = new File(path.getFile());

        return file.getParent() + "/files/";
    }
    
    public static void ensureBucket(OSS client, String bucketName)
            throws OSSException, ClientException{
        assert (client != null && bucketName != null);
        if (!client.doesBucketExist(bucketName)){
            client.createBucket(bucketName);
        }
    }
    
    public static void cleanBucket(OSS client, String bucketName)
            throws OSSException, ClientException{
        assert (client != null && bucketName != null);
        if (client.doesBucketExist(bucketName)){
            // Check if the bucket is empty.
            ObjectListing ObjectListing = client.listObjects(bucketName);
            for (OSSObjectSummary objSummary : ObjectListing
                    .getObjectSummaries()) {
                // If the bucket is not empty, delete the files first.
                client.deleteObject(bucketName, objSummary.getKey());
            }

            client.deleteBucket(bucketName);
        }
    }}
