import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AddBucketReplicationRequest;
import com.aliyun.oss.model.BucketReplicationProgress;
import com.aliyun.oss.model.ReplicationRule;

import java.util.List;


public class BucketReplicationSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";

    public static void main(String[] args) {
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            AddBucketReplicationRequest request = new AddBucketReplicationRequest(bucketName);
            request.setReplicationRuleID("<yourRuleId>");
            request.setTargetBucketName("<yourTargetBucketName>");
            // This example uses the target endpoint China (Beijing).
            request.setTargetBucketLocation("oss-cn-beijing");
            // Disable historical data replication. Historical data replication is enabled by default.
            request.setEnableHistoricalObjectReplication(false);
            ossClient.addBucketReplication(request);

            // View cross-region replication configurations
            List<ReplicationRule> rules = ossClient.getBucketReplication(bucketName);
            for (ReplicationRule rule : rules) {
                System.out.println(rule.getReplicationRuleID());
                System.out.println(rule.getTargetBucketLocation());
                System.out.println(rule.getTargetBucketName());
            }

            // View cross-region replication progress
            BucketReplicationProgress process = ossClient.getBucketReplicationProgress(bucketName, "<yourRuleId>");
            System.out.println(process.getReplicationRuleID());
            // Check whether historical data synchronization is enabled.
            System.out.println(process.isEnableHistoricalObjectReplication());
            // Print historical data synchronization progress.
            System.out.println(process.getHistoricalObjectProgress());
            // Print real-time data synchronization progress.
            System.out.println(process.getNewObjectProgress());

            // View the target regions for synchronization
            List<String> locations = ossClient.getBucketReplicationLocation(bucketName);
            for (String loc : locations) {
                System.out.println(loc);
            }

            // Disable cross-region replication. After this function is disabled, objects in the target bucket exist and all changes in the source objects cannot be synchronized.
            ossClient.deleteBucketReplication(bucketName, "<yourRuleId>");


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
