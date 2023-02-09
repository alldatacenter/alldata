package samples;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.BucketQosInfo;
import com.aliyun.oss.model.UserQosInfo;

public class QosInfoSample {
    
    public static void main(String[] args) {
        private static String endpoint = "<endpoint, http://oss-cn-hangzhou.aliyuncs.com>";
        private static String accessKeyId = "<accessKeyId>";
        private static String accessKeySecret = "<accessKeySecret>";
        private static String bucketName = "<bucketName>";
        /*
         * Constructs a client instance with your account for accessing OSS
         */
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // Get user qos info
            UserQosInfo userQosInfo = ossClient.getUserQosInfo();
            System.out.println("===Get user qos info:===");
            System.out.println("region:" + userQosInfo.getRegion());
            System.out.println("total upload bw:" + userQosInfo.getTotalUploadBw());
            System.out.println("intranet upload bw:" + userQosInfo.getIntranetUploadBw());
            System.out.println("extranet upload bw:" + userQosInfo.getExtranetUploadBw());
            System.out.println("total download bw:" + userQosInfo.getTotalDownloadBw());
            System.out.println("intranet download bw:" + userQosInfo.getIntranetDownloadBw());
            System.out.println("extranet download bw:" + userQosInfo.getExtranetDownloadBw());
            System.out.println("total qps:" + userQosInfo.getTotalQps());
            System.out.println("intranet qps:" + userQosInfo.getIntranetQps());
            System.out.println("extranet qps:" + userQosInfo.getExtranetDownloadBw());
            
            // Set bucket qos info
            BucketQosInfo bucketQosInfo = new BucketQosInfo();
            bucketQosInfo.setTotalUploadBw(-1);
            bucketQosInfo.setIntranetUploadBw(-1);
            bucketQosInfo.setExtranetUploadBw(-1);
            bucketQosInfo.setTotalDownloadBw(-1);
            bucketQosInfo.setIntranetDownloadBw(-1);
            bucketQosInfo.setExtranetDownloadBw(-1);
            bucketQosInfo.setTotalQps(-1);
            bucketQosInfo.setIntranetQps(-1);
            bucketQosInfo.setExtranetQps(-1);

            ossClient.setBucketQosInfo(bucketName, bucketQosInfo);

            // Get bucket qos info
            BucketQosInfo result = ossClient.getBucketQosInfo(bucketName);
            System.out.println("=== Get bucket qos info:===");
            System.out.println("total upload bw:" + result.getTotalUploadBw());
            System.out.println("intranet upload bw:" + result.getIntranetUploadBw());
            System.out.println("extranet upload bw:" + result.getExtranetUploadBw());
            System.out.println("total download bw:" + result.getTotalDownloadBw());
            System.out.println("intranet download bw:" + result.getIntranetDownloadBw());
            System.out.println("extranet download bw:" + result.getExtranetDownloadBw());
            System.out.println("total qps:" + result.getTotalQps());
            System.out.println("intranet qps:" + result.getIntranetQps());
            System.out.println("extranet qps:" + result.getExtranetDownloadBw());
            
            //Delete bucket qos info
            ossClient.deleteBucketQosInfo(bucketName);
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
}
