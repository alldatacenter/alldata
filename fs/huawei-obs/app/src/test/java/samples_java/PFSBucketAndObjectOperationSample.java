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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.BucketMetadataInfoRequest;
import com.obs.services.model.BucketMetadataInfoResult;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.ListBucketsRequest;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.ObsObject;

/**
 * This sample demonstrates how to do object-related operations
 * (such as create/delete/get/copy object, do object ACL/OPTIONS) 
 * on OBS using the OBS SDK for Java.
 */
public class PFSBucketAndObjectOperationSample
{
    private static final String endPoint = "https://your-endpoint";

    private static final String ak = "*** Provide your Access Key ***";

    private static final String sk = "*** Provide your Secret Key ***";

    private static ObsClient obsClient;

    private static String bucketName = "my-obs-bucket-demo";

    private static String objectKey = "my-obs-object-key-demo";

    private static String newObjectKey = "new-my-obs-object-key-demo";

    private static long newLength = 1024L;

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
             * Create PFS bucket
             */
            CreateBucketRequest request = new CreateBucketRequest();
            request.setBucketName(bucketName);
            request.setBucketType(BucketTypeEnum.PFS);
            obsClient.createBucket(request);

            /*
             * Head PFS bucket
             */
            BucketMetadataInfoRequest bucketMetadataInfoRequest = new BucketMetadataInfoRequest();
            bucketMetadataInfoRequest.setBucketName(bucketName);
            BucketMetadataInfoResult bucketMetadataInfoResult =  obsClient.getBucketMetadata(bucketMetadataInfoRequest);
            System.out.println(bucketMetadataInfoResult);

            /*
             * List buckets
             */
            doListBucketOperations();

            /*
             * Create object
             */
            String content = "Hello OBS";
            obsClient.putObject(bucketName, objectKey, new ByteArrayInputStream(content.getBytes("UTF-8")), null);
            System.out.println("Create object:" + objectKey + " successfully!\n");

            /*
             * Get object metadata
             */
            System.out.println("Getting object metadata");
            ObjectMetadata metadata = obsClient.getObjectMetadata(bucketName, objectKey, null);
            System.out.println("\t" + metadata);

            /*
             * Get object
             */
            System.out.println("Getting object content");
            ObsObject obsObject = obsClient.getObject(bucketName, objectKey, null);
            System.out.println("\tobject content:" + ServiceUtils.toString(obsObject.getObjectContent()));

            /*
             * Copy object
             */
            String sourceBucketName = bucketName;
            String destBucketName = bucketName;
            String sourceObjectKey = objectKey;
            String destObjectKey = objectKey + "-back";
            System.out.println("Copying object\n");
            obsClient.copyObject(sourceBucketName, sourceObjectKey, destBucketName, destObjectKey);

            /*
             * Put/Get object acl operations
             */
            doObjectAclOperations();

            /*
             * Modify object operations
             */
            doModifyFileOperations();

            /*
             * Rename object operations
             */
            doRenameFileOperations();

            /*
             * Truncate object operations
             */
            doTruncateFileOperations();

            /*
             * Get object metadata
             */
            System.out.println("Getting object metadata");
            metadata = obsClient.getObjectMetadata(bucketName, objectKey, null);
            System.out.println("\t" + metadata);

            /*
             * Delete object
             */
            System.out.println("Deleting objects\n");
            obsClient.deleteObject(bucketName, objectKey, null);
            obsClient.deleteObject(bucketName, destObjectKey, null);
        }
        catch (ObsException e)
        {
            System.out.println("Response Code: " + e.getResponseCode());
            System.out.println("Error Message: " + e.getErrorMessage());
            System.out.println("Error Code:    " + e.getErrorCode());
            System.out.println("Request ID:    " + e.getErrorRequestId());
            System.out.println("Host ID:       " + e.getErrorHostId());
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

    private static void doListBucketOperations() {
        /*
         * Create OBJCET bucket
         */
        obsClient.createBucket(bucketName + "-object");

        /*
         * List all buckets
         */
        ListBucketsRequest request = new ListBucketsRequest();
        List<ObsBucket> allBuckets = obsClient.listBuckets(request);
        System.out.println("----------allBuckets----------\n");
        for (int i = 0; i < allBuckets.size(); i++) {
            System.out.println(allBuckets.get(i));
        }

        /*
         * List OBJECT buckets
         */
        request.setBucketType(BucketTypeEnum.OBJECT);
        List<ObsBucket> objectBuckets = obsClient.listBuckets(request);
        System.out.println("-----------objectBuckets---------\n");
        for (int i = 0; i < objectBuckets.size(); i++) {
            System.out.println(objectBuckets.get(i));
        }

        /*
         * List PFS buckets
         */
        request.setBucketType(BucketTypeEnum.PFS);
        List<ObsBucket> PFSBuckets = obsClient.listBuckets(request);
        System.out.println("-----------PFSBuckets---------\n");
        for (int i = 0; i < PFSBuckets.size(); i++) {
            System.out.println(PFSBuckets.get(i));
        }
    }

    private static void doObjectAclOperations()
            throws ObsException
    {
        System.out.println("Setting object ACL to public-read \n");

        obsClient.setObjectAcl(bucketName, objectKey, AccessControlList.REST_CANNED_PUBLIC_READ);

        System.out.println("Getting object ACL " + obsClient.getObjectAcl(bucketName, objectKey) + "\n");

        System.out.println("Setting object ACL to private \n");

        obsClient.setObjectAcl(bucketName, objectKey, AccessControlList.REST_CANNED_PRIVATE);

        System.out.println("Getting object ACL " + obsClient.getObjectAcl(bucketName, objectKey) + "\n");
    }

    private static void doRenameFileOperations() throws  ObsException
    {
        System.out.println("rename " + objectKey + " to " +  newObjectKey + "\n");

        obsClient.renameObject(bucketName, objectKey, newObjectKey);

        System.out.println("rename " + newObjectKey + " to " +  objectKey + "\n");

        obsClient.renameObject(bucketName, newObjectKey, objectKey);
    }

    private static void doTruncateFileOperations() throws  ObsException
    {
        System.out.println("truncate " + objectKey + " length to " +  newLength + "\n");

        obsClient.truncateObject(bucketName, objectKey, newLength);
    }

    private static void doModifyFileOperations() throws ObsException, IOException
    {
        System.out.println("modify " + objectKey + " position 0\n");

        String content = "Hello obs";
        obsClient.modifyObject(bucketName, objectKey, 0, new ByteArrayInputStream(content.getBytes("UTF-8")));
        ObsObject obsObject = obsClient.getObject(bucketName, objectKey, null);
        System.out.println("\tobject content:" + ServiceUtils.toString(obsObject.getObjectContent()));

        obsClient.modifyObject(bucketName, objectKey, 9, new ByteArrayInputStream(content.getBytes("UTF-8")));
        obsObject = obsClient.getObject(bucketName, objectKey, null);
        System.out.println("\tobject content:" + ServiceUtils.toString(obsObject.getObjectContent()));
    }
}