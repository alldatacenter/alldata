import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.TransferAcceleration;


public class BucketTransferAccelerationSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";

    public static void main(String[] args) {
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Configure transfer acceleration for the bucket.
            // If enabled is set to true, transfer acceleration is enabled. If enabled is set to false, transfer acceleration is disabled.
            boolean enabled = true;
            ossClient.setBucketTransferAcceleration(bucketName, enabled);

            // Query the transfer acceleration status of the bucket.
            // If the returned value is true, the transfer acceleration feature is enabled for the bucket. If the returned value is false, the transfer acceleration feature is disabled for the bucket.
            TransferAcceleration result = ossClient.getBucketTransferAcceleration(bucketName);
            System.out.println("Is transfer acceleration enabled:"+ result.isEnabled());

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
