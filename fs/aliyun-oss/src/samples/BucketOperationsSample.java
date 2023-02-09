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

package samples;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.UUID;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import com.aliyun.oss.model.LifecycleRule.RuleStatus;
import com.aliyun.oss.model.SetBucketCORSRequest.CORSRule;

/**
 * This sample demonstrates how to do bucket-related operations
 * (such as Bucket ACL/CORS/Lifecycle/Logging/Website/Location) 
 * on Aliyun OSS using the OSS SDK for Java.
 */
public class BucketOperationsSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    
    private static OSS client = null;

    private static String bucketName = "my-oss-bucket" + UUID.randomUUID();
    
    public static void main(String[] args) throws IOException {
        
        /*
         * Constructs a client instance with your account for accessing OSS
         */
        client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {
            /*
             * Put Bucket Operation
             */
            doCreateBucketOperation();
            
            /*
             * Get Bucket Location Operation
             */
            doGetBucketLocationOperation();

            /*
             * Put/Get Bucket ACL Operations
             */
            doBucketACLOperations();
            
            /*
             * Put/Get/Delete Bucket CORS Operations
             */
            doBucketCORSOperations();
            
            /*
             * Put/Get/Delete Bucket Lifecycle Operations
             */
            doBucketLifecycleOperations();
            
            /*
             * Put/Get/Delete Bucket Logging Operations
             */
            doBucketLoggingOperations();
            
            /*
             * Put/Get Bucket Referer Operations
             */
            doBucketRefererOperations();
            
            /*
             * Put/Get/Delete Bucket Website Operations
             */
            doBucketWebsiteOperations();

            /*
             * Delete Bucket Operation
             */
            doDeleteBucketOperation();

            /*
             * Determine whether buckets exist
             */
            doesBucketExist();

            /*
             * Query bucket information
             */
            getBucketInfo();

            /*
             * See if hierarchical namespaces are turned on
             */
            getBucketHnsStatus();
            
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message: " + oe.getErrorMessage());
            System.out.println("Error Code:       " + oe.getErrorCode());
            System.out.println("Request ID:      " + oe.getRequestId());
            System.out.println("Host ID:           " + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ce.getMessage());
        } finally {
            /*
             * Do not forget to shut down the client finally to release all allocated resources.
             */
            if (client != null) {
                client.shutdown();
            }
        }
    }
    
    private static void doCreateBucketOperation() {
        System.out.println("Creating bucket " + bucketName + "\n");
        client.createBucket(bucketName);
    }
    
    private static void doGetBucketLocationOperation() {
        String location = client.getBucketLocation(bucketName);
        System.out.println("Getting bucket location " + location + "\n");
    }

    private static void doBucketACLOperations() {
        System.out.println("Setting bucket ACL to " + CannedAccessControlList.PublicRead.toString() + "\n");
        client.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
        
        AccessControlList acl = client.getBucketAcl(bucketName);
        System.out.println("Getting bucket ACL " + acl.toString() + "\n");
    }
    
    private static void doBucketCORSOperations() {
        SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
        
        CORSRule r0 = new CORSRule();
        r0.addAllowdOrigin("http://www.a.com");
        r0.addAllowdOrigin("http://www.b.com");
        r0.addAllowedMethod("GET");
        r0.addAllowedHeader("Authorization");
        r0.addExposeHeader("x-oss-test");
        r0.addExposeHeader("x-oss-test1");
        r0.setMaxAgeSeconds(100);
        request.addCorsRule(r0);
        
        System.out.println("Setting bucket CORS\n");
        client.setBucketCORS(request);
        
        System.out.println("Getting bucket CORS:");
        List<CORSRule> rules = client.getBucketCORSRules(bucketName);
        r0 = rules.get(0);
        System.out.println("\tAllowedOrigins " + r0.getAllowedOrigins());
        System.out.println("\tAllowedMethods " + r0.getAllowedMethods());
        System.out.println("\tAllowedHeaders " + r0.getAllowedHeaders());
        System.out.println("\tExposeHeaders " + r0.getExposeHeaders());
        System.out.println("\tMaxAgeSeconds " + r0.getMaxAgeSeconds());
        System.out.println();
        
        System.out.println("Deleting bucket CORS\n");
        client.deleteBucketCORSRules(bucketName);
    }
    
    private static void doBucketLifecycleOperations() {
        final String ruleId0 = "delete obsoleted files";
        final String matchPrefix0 = "obsoleted/";
        final String ruleId1 = "delete temporary files";
        final String matchPrefix1 = "temporary/";
        
        SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);
        request.AddLifecycleRule(new LifecycleRule(ruleId0, matchPrefix0, RuleStatus.Enabled, 3));
        request.AddLifecycleRule(new LifecycleRule(ruleId1, matchPrefix1, RuleStatus.Enabled, 
                parseISO8601Date("2022-10-12T00:00:00.000Z")));
        
        System.out.println("Setting bucket lifecycle\n");
        client.setBucketLifecycle(request);
        
        System.out.println("Getting bucket lifecycle:");
        List<LifecycleRule> rules = client.getBucketLifecycle(bucketName);
        LifecycleRule r0 = rules.get(0);
        LifecycleRule r1 = rules.get(1);
        System.out.println("\tRule0: Id=" + r0.getId() + ", Prefix=" + r0.getPrefix() +
                ", Status=" + r0.getStatus() + ", ExpirationDays=" + r0.getExpirationDays());
        System.out.println("\tRule1: Id=" + r1.getId() + ", Prefix=" + r1.getPrefix() +
                ", Status=" + r1.getStatus() + ", ExpirationTime=" + formatISO8601Date(r1.getExpirationTime()));
        System.out.println();
        
        System.out.println("Deleting bucket lifecycle\n");
        client.deleteBucketLifecycle(bucketName);
    }
    
    private static void doBucketLoggingOperations() {
        final String targetBucket = bucketName;
        final String targetPrefix = "log-";
        SetBucketLoggingRequest request = new SetBucketLoggingRequest(bucketName);
        request.setTargetBucket(targetBucket);
        request.setTargetPrefix(targetPrefix);
        
        System.out.println("Setting bucket logging\n");
        client.setBucketLogging(request);
        
        System.out.println("Getting bucket logging:");
        BucketLoggingResult result = client.getBucketLogging(bucketName);
        System.out.println("\tTarget bucket=" + result.getTargetBucket() + 
                ", target prefix=" + result.getTargetPrefix() + "\n");
        System.out.println();
        
        System.out.println("Deleting bucket logging\n");
        client.deleteBucketLogging(bucketName);
    }
    
    private static void doBucketRefererOperations() {
        List<String> refererList = new ArrayList<String>();
        refererList.add("http://www.aliyun.com");
        refererList.add("https://www.aliyun.com");
        refererList.add("http://www.*.com");
        refererList.add("https://www.?.aliyuncs.com");
        BucketReferer r = new BucketReferer();
        r.setRefererList(refererList);
        
        System.out.println("Setting bucket referer\n");
        client.setBucketReferer(bucketName, r);
        
        System.out.println("Getting bucket referer:");
        r = client.getBucketReferer(bucketName);
        List<String> returedRefererList = r.getRefererList();
        System.out.println("\tAllow empty referer? " + r.isAllowEmptyReferer() + 
                ", referer list=" + returedRefererList + "\n");
        
        r.clearRefererList();
        System.out.println("Clearing bucket referer\n");
        client.setBucketReferer(bucketName, r);
    }
    
    private static void doBucketWebsiteOperations() {
        SetBucketWebsiteRequest request = new SetBucketWebsiteRequest(bucketName);
        request.setIndexDocument("inde.html");
        request.setErrorDocument("error.html");
        
        System.out.println("Setting bucket website\n");
        client.setBucketWebsite(request);
        
        System.out.println("Getting bucket website:");
        BucketWebsiteResult result = client.getBucketWebsite(bucketName);
        System.out.println("\tIndex document " + result.getIndexDocument() + 
                ", error document=" + result.getErrorDocument() + "\n");
        
        System.out.println("Deleting bucket website\n");
        client.deleteBucketWebsite(bucketName);
    }
    
    private static void doDeleteBucketOperation() {
        System.out.println("Deleting bucket " + bucketName + "\n");
        client.deleteBucket(bucketName);
    }

    private static Date parseISO8601Date(String dateString) {
        final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.US);
        dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
        Date date = null;
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
    
    private static String formatISO8601Date(Date date) {
        final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.US);
        dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return dateFormat.format(date);
    }

    private static void doesBucketExist() {
        // Determine whether the bucket named examplebucket exists. If the value of true is returned, the bucket exists. Otherwise, the bucket does not exist.
        boolean exists = ossClient.doesBucketExist("examplebucket");
        System.out.println(exists);

        // Shut down the OSSClient instance.
        ossClient.shutdown();
    }

    private static void getBucketInfo() {
        // Information about a bucket includes the region (Region or Location), creation date (CreationDate), and owner (Owner) of the bucket.
        // Specify the name of the bucket. Example: examplebucket.
        BucketInfo info = ossClient.getBucketInfo("examplebucket");
        // Query the region.
        info.getBucket().getLocation();
        // Query the creation date of the bucket.
        info.getBucket().getCreationDate();
        // Query the information about the owner of the bucket.
        info.getBucket().getOwner();
        // Query the redundancy option for the bucket.
        info.getDataRedundancyType();

        // Shut down the OSSClient instance.
        ossClient.shutdown();
    }

    private static void getBucketHnsStatus() {
        BucketInfo info =  client.getBucketInfo(bucketName);
        // View whether the value of HnsStatus is Enabled. When the value of HnsStatus is Enabled, the hierarchical namespace feature is enabled for the bucket.
        System.out.println("Hnstatus:" + info.getBucket().getHnsStatus());
    }
}
