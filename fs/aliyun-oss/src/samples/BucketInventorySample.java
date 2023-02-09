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

package sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;

/**
 * This sample demonstrates how to upload an object by append mode
 * to Aliyun OSS using the OSS SDK for Java.
 */
public class BucketInventorySample {
    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";
    private static String destBucketName ="*** Your destination bucket name ***";
    private static String accountId ="*** Your destination Bucket owner account ***";
    private static String roleArn ="*** Your destination bucket role arn ***";

    public static void main(String[] args) throws IOException {
        /*
         * Constructs a client instance with your account for accessing OSS
         */
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            InventoryConfiguration inventoryConfiguration = new InventoryConfiguration();
            String inventoryId = "testid";
            // fields
            List<String> fields = new ArrayList<String>();
            fields.add(InventoryOptionalFields.Size);
            fields.add(InventoryOptionalFields.LastModifiedDate);
            fields.add(InventoryOptionalFields.IsMultipartUploaded);
            fields.add(InventoryOptionalFields.StorageClass);
            inventoryConfiguration.setOptionalFields(fields);
            // schedule
            inventoryConfiguration.setSchedule(new InventorySchedule().withFrequency(InventoryFrequency.Weekly));
            // id
            inventoryConfiguration.setInventoryId(inventoryId);
            // includedVersions
            inventoryConfiguration.setIncludedObjectVersions(InventoryIncludedObjectVersions.Current);
            // isenable
            inventoryConfiguration.setEnabled(true);
            // prefix
            InventoryFilter inventoryFilter = new InventoryFilter().withPrefix("testPrefix");
            inventoryConfiguration.setInventoryFilter(inventoryFilter);
            // destination
            InventoryDestination destination = new InventoryDestination();
            InventoryOSSBucketDestination ossInvDest = new InventoryOSSBucketDestination();
            ossInvDest.setPrefix("bucket-prefix");
            ossInvDest.setFormat(InventoryFormat.CSV);
            ossInvDest.setAccountId(accountId);
            ossInvDest.setRoleArn(roleArn);
            ossInvDest.setBucket(destBucketName);
            InventoryEncryption inventoryEncryption = new InventoryEncryption();
            InventoryServerSideEncryptionKMS serverSideKmsEncryption = new InventoryServerSideEncryptionKMS().withKeyId("123");
            inventoryEncryption.setServerSideKmsEncryption(serverSideKmsEncryption);
            ossInvDest.setEncryption(inventoryEncryption);
            destination.setOssBucketDestination(ossInvDest);
            inventoryConfiguration.setDestination(destination);

            // set bucket inventory configuration.
            ossClient.setBucketInventoryConfiguration(bucketName, inventoryConfiguration);

            // get bucket inventory configuration.
            GetBucketInventoryConfigurationResult getResult = ossClient.getBucketInventoryConfiguration(
                    new GetBucketInventoryConfigurationRequest(bucketName, inventoryId));

            // print retrieved inventory configuration.
            printInventoryConfig(getResult.getInventoryConfiguration());

            // list bucket inventory configuration.
            String continuationToken = null;
            while (true) {
                ListBucketInventoryConfigurationsRequest listRequest = new ListBucketInventoryConfigurationsRequest(bucketName, continuationToken);
                // at most 100 results can be returned every list request.
                ListBucketInventoryConfigurationsResult result = ossClient.listBucketInventoryConfigurations(listRequest);
                System.out.println("=======List bucket inventory configuration=======");
                System.out.println("istruncated:" + result.isTruncated());
                System.out.println("continuationToken:" + result.getContinuationToken());
                System.out.println("nextContinuationToken:" + result.getNextContinuationToken());
                System.out.println("list size :" + result.getInventoryConfigurationList().size());

                if (result.getInventoryConfigurationList() != null && !result.getInventoryConfigurationList().isEmpty()) {
                    for (InventoryConfiguration config : result.getInventoryConfigurationList()) {
                        printInventoryConfig(config);
                    }
                }

                if (result.isTruncated()) {
                    continuationToken = result.getNextContinuationToken();
                } else {
                   break;
                }
            }

            // delete the specified bucket inventory configuration.
            DeleteBucketInventoryConfigurationRequest delRequest = new DeleteBucketInventoryConfigurationRequest(bucketName, inventoryId);
            ossClient.deleteBucketInventoryConfiguration(delRequest);
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
            ossClient.shutdown();
        }
    }

    private static void printInventoryConfig(InventoryConfiguration config) {
        System.out.println("=====Inventory configuration=====");
        System.out.println("id:" + config.getInventoryId());
        System.out.println("isenabled:" + config.isEnabled());
        System.out.println("includedVersions:" + config.getIncludedObjectVersions());
        System.out.println("schdule:" + config.getSchedule().getFrequency());
        if (config.getInventoryFilter().getPrefix() != null) {
            System.out.println("filter, prefix:" + config.getInventoryFilter().getPrefix());
        }

        List<String> fields = config.getOptionalFields();
        for (String field : fields) {
            System.out.println("field:" + field);
        }

        InventoryOSSBucketDestination destin = config.getDestination().getOssBucketDestination();
        System.out.println("format:" + destin.getFormat());
        System.out.println("bucket:" + destin.getBucket());
        System.out.println("prefix:" + destin.getPrefix());
        System.out.println("accountId:" + destin.getAccountId());
        System.out.println("roleArn:" + destin.getRoleArn());
        if (destin.getEncryption() != null) {
            if (destin.getEncryption().getServerSideKmsEncryption() != null) {
                System.out.println("server-side kms encryption, key id:" + destin.getEncryption().getServerSideKmsEncryption().getKeyId());
            } else if (destin.getEncryption().getServerSideOssEncryption() != null) {
                System.out.println("server-side oss encryption");
            }
        }
    }
}