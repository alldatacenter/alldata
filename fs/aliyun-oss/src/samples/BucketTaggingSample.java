import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;

import java.util.Map;

public class BucketTaggingSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";

    public static void main(String[] args) {
        // Creates an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Adds tags to the bucket.
            SetBucketTaggingRequest request = new SetBucketTaggingRequest("<yourBucketName>");
            request.setTag("<yourTagkey1>", "<yourTagValue1>");
            request.setTag("<yourTagkey2>", "<yourTagValue2>");
            ossClient.setBucketTagging(request);

            // Obtains the tags added to the bucket.
            TagSet tagSet = ossClient.getBucketTagging(new GenericRequest(bucketName));
            Map<String, String> tags = tagSet.getAllTags();
            for(Map.Entry tag:tags.entrySet()){
                System.out.println("key:"+tag.getKey()+" value:"+tag.getValue());
            }

            // List buckets with a specified tag
            ListBucketsRequest listBucketsRequest = new ListBucketsRequest();
            listBucketsRequest.setTag("<yourTagKey>", "<yourTagValue>");
            BucketList bucketList = ossClient.listBuckets(listBucketsRequest);
            for (Bucket o : bucketList.getBucketList()) {
                System.out.println("list result bucket: " + o.getName());
            }

            // Deletes the tags added to the bucket.
            ossClient.deleteBucketTagging(new GenericRequest(bucketName));
        } catch (OSSException oe) {
            System.out.println("Error Message: " + oe.getErrorMessage());
            System.out.println("Error Code:       " + oe.getErrorCode());
            System.out.println("Request ID:      " + oe.getRequestId());
            System.out.println("Host ID:           " + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Error Message: " + ce.getMessage());
        } finally {
            /*
             * Do not forget to shut down the client finally to release all allocated resources.
             */
            ossClient.shutdown();
        }
    }
}
