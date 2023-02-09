package samples;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.*;

public class AsyncFetchTaskSample {
    
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
            String callbackContent = "{\"callbackUrl\":\"www.abc.com/callback\",\"callbackBody\":\"${etag}\"}";
            String callback = BinaryUtil.toBase64String(callbackContent.getBytes());

            AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration()
                    .withUrl("<yourSourceObjectUrl>")
                    .withContentMd5("<yourSourceObjectContentMd5>")
                    .withCallback(callback)
                    .withIgnoreSameKey(false)
                    .withObjectName("<yourDestinationObjectName>");


            SetAsyncFetchTaskResult setTaskResult = ossClient.setAsyncFetchTask(bucketName, configuration);

            String taskId = setTaskResult.getTaskId();
            System.out.println("task id:" + taskId);

            GetAsyncFetchTaskResult getTaskResult = ossClient.getAsyncFetchTask(bucketName, taskId);

            System.out.println("=====Get async fetch task result======");
            System.out.println("taskId:" + getTaskResult.getTaskId());
            System.out.println("state:" + getTaskResult.getAsyncFetchTaskState());
            System.out.println("errorMsg:" + getTaskResult.getErrorMsg());
            System.out.println("=====task configuration======");
            AsyncFetchTaskConfiguration config = getTaskResult.getAsyncFetchTaskConfiguration();
            System.out.println("url:" + config.getUrl());
            System.out.println("object:" + config.getObjectName());
            System.out.println("host:" + config.getHost());
            System.out.println("contentMd5:" + config.getContentMd5());
            System.out.println("callback:" + config.getCallback());
            System.out.println("ignoreSameKey:" + config.getIgnoreSameKey());
        } catch (OSSException oe) {
                System.out.println("Caught an OSSException, which means your request made it to OSS, "
                        + "but was rejected with an error response for some reason.");
                System.out.println("Error Message: " + oe.getMessage());
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