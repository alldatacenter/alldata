import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;

import com.aliyun.oss.model.GetBucketWormResult;
import com.aliyun.oss.model.InitiateBucketWormRequest;
import com.aliyun.oss.model.InitiateBucketWormResult;


public class BucketWormSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";

    public static void main(String[] args) {
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Create a retention policy
            // Create an InitiateBucketWormRequest object.
            InitiateBucketWormRequest initiateBucketWormRequest = new InitiateBucketWormRequest(bucketName);
            // Set the retention period to one day.
            initiateBucketWormRequest.setRetentionPeriodInDays(1);

            // Create the retention policy.
            InitiateBucketWormResult initiateBucketWormResult = ossClient.initiateBucketWorm(initiateBucketWormRequest);

            // Query the ID of the retention policy.
            String wormId = initiateBucketWormResult.getWormId();
            System.out.println(wormId);

            // Lock the retention policy.
            ossClient.completeBucketWorm(bucketName, wormId);

            // Query the retention policy.
            GetBucketWormResult getBucketWormResult = ossClient.getBucketWorm(bucketName);

            // Query the ID of the retention policy.
            System.out.println(getBucketWormResult.getWormId());
            // Query the state of the retention policy. InProgress indicates that the retention policy is not locked. Locked indicates that the retention policy is locked.
            System.out.println(getBucketWormResult.getWormState());
            // Query the retention period of the retention policy.
            System.out.println(getBucketWormResult.getRetentionPeriodInDays());
            // Query the created time of the retention policy.
            System.out.println(getBucketWormResult.getCreationDate());


            // Extend the retention period of the locked retention policy.
            ossClient.extendBucketWorm(bucketName, wormId, 2);

            // Cancel the retention policy.
            ossClient.abortBucketWorm(bucketName);

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
