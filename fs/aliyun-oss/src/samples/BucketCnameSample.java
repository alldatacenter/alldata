import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import java.util.List;

public class BucketCnameSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";

    public static void main(String[] args) {
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Create cnametoken required for domain name ownership verification
            CreateBucketCnameTokenRequest request = new CreateBucketCnameTokenRequest(bucketName);
            request.setDomain("www.example.com");
            ossClient.createBucketCnameToken(request);

            // Get the created cnametoken
            GetBucketCnameTokenRequest grequest = new GetBucketCnameTokenRequest(bucketName);
            grequest.setDomain("www.example.com");
            GetBucketCnameTokenResult gresult = ossClient.getBucketCnameToken(grequest);
            System.out.println(gresult.getCname());
            System.out.println(gresult.getToken());

            // Bind a custom domain name to a bucket
            ossClient.addBucketCname(new AddBucketCnameRequest(bucketName).withDomain("www.example.com"));

            // Query the list of all cnames bound under a storage space (bucket)
            List<CnameConfiguration> list = ossClient.getBucketCname(bucketName);
            System.out.println(list.get(0).getDomain());

            // Delete the bound CNAME of a storage space (bucket)
            ossClient.deleteBucketCname(bucketName, "www.example.com");

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
