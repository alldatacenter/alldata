import com.aliyun.oss.*;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.model.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.aliyun.oss.internal.OSSHeaders.OSS_USER_METADATA_PREFIX;


public class AuthorizedAccessSample {

    public static void main(String[] args) {

        try {
            // Use STS to authorize temporary access
            createSTSAuthorization(ossClient);

            // Use a signed URL to authorize temporary access
            createSignedUrl(ossClient);

            // Generate a signed URL that allows other HTTP requests
            createSignedUrlWithParamer(ossClient);

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

    private static void createSTSAuthorization(OSS ossClient) {
        // Set yourEndpoint to the endpoint of the region in which the bucket is located. For example, if the bucket is located in the China (Hangzhou) region, set yourEndpoint to https://oss-cn-hangzhou.aliyuncs.com.
        String endpoint = "yourEndpoint";
        // Specify the temporary AccessKey pair obtained from STS.
        String accessKeyId = "yourAccessKeyId";
        String accessKeySecret = "yourAccessKeySecret";
        // Specify the security token obtained from STS.
        String securityToken = "yourSecurityToken";
        // Specify the name of the bucket in which the object you want to access is stored. Example: examplebucket.
        //String bucketName = "examplebucket";
        // Specify the full path of the object. Example: exampleobject.txt. The full path of the object cannot contain bucket names.
        //String objectName = "exampleobject.txt";

        // You can use the AccessKey pair and security token contained in the temporary access credential obtained from STS to create an OSSClient.
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, securityToken);

        // Perform operations on OSS resources, such as upload or download objects.
        // Upload an object. In this example, a local file is uploaded to OSS as an object.
        // Specify the full path of the local file to upload. If the path of the local file is not specified, the file is uploaded to the path of the project to which the sample program belongs.
        //PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new File("D:\\localpath\\examplefile.txt"));
        //ossClient.putObject(putObjectRequest);

        // Download an object to your local computer. If the specified local file already exists, the file is replaced by the downloaded object. If the specified local file does not exist, the local file is created.
        // If the path for the object is not specified, the downloaded object is saved to the path of the project to which the sample program belongs.
        //ossClient.getObject(new GetObjectRequest(bucketName, objectName), new File("D:\\localpath\\examplefile.txt"));

        // Shut down the OSSClient instance.
        ossClient.shutdown();
    }

    private static void createSignedUrl(OSS ossClient) {
        // Set yourEndpoint to the endpoint of the region in which the bucket is located. For example, if the bucket is located in the China (Hangzhou) region, set yourEndpoint to https://oss-cn-hangzhou.aliyuncs.com.
        String endpoint = "yourEndpoint";
        // Specify the temporary AccessKey pair obtained from STS.
        String accessKeyId = "yourAccessKeyId";
        String accessKeySecret = "yourAccessKeySecret";
        // Specify the security token obtained from STS.
        String securityToken = "yourSecurityToken";
        // Specify the name of the bucket in which the object you want to access is stored. Example: examplebucket.
        String bucketName = "examplebucket";
        // Specify the full path of the object. Example: exampleobject.txt. The full path of the object cannot contain bucket names.
        String objectName = "exampleobject.txt";

        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, securityToken);

        // Set the validity period of the signed URL to 3,600 seconds (1 hour).
        Date expiration = new Date(new Date().getTime() + 3600 * 1000);
        // Generate the signed URL that allows HTTP GET requests. Visitors can enter the URL in a browser to access specified OSS resources.
        URL url = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
        System.out.println(url);
        // Shut down the OSSClient instance.
        ossClient.shutdown();
    }

    private static void createSignedUrlWithParamer(OSS ossClient) {
        // Set yourEndpoint to the endpoint of the region in which the bucket is located. For example, if the bucket is located in the China (Hangzhou) region, set yourEndpoint to https://oss-cn-hangzhou.aliyuncs.com.
        String endpoint = "yourEndpoint";
        // Specify the temporary AccessKey pair obtained from STS.
        String accessKeyId = "yourAccessKeyId";
        String accessKeySecret = "yourAccessKeySecret";
        // Specify the security token obtained from STS.
        String securityToken = "yourSecurityToken";
        // Specify the name of the bucket in which the object you want to access is stored. Example: examplebucket.
        String bucketName = "examplebucket";
        // Specify the full path of the object. Example: exampleobject.txt. The full path of the object cannot contain bucket names.
        String objectName = "exampleobject.txt";

        // You can use the AccessKey pair and security token contained in the temporary access credential obtained from STS to create an OSSClient.
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, securityToken);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectName, HttpMethod.PUT);
        // Set the validity period of the signed URL to 3,600 seconds (1 hour).
        Date expiration = new Date(new Date().getTime() + 3600 * 1000);
        request.setExpiration(expiration);
        // Set ContentType.
        request.setContentType("text/plain");
        // Set user metadata.
        request.addUserMetadata("author", "aliy");

        // Generate the signed URL.
        URL signedUrl = ossClient.generatePresignedUrl(request);
        System.out.println(signedUrl);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        // Set ContentType. Make sure that the ContentType value must be the same as the content type specified when you generate the signed URL.
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, "text/plain");
        // Set user metadata.
        requestHeaders.put(OSS_USER_METADATA_PREFIX + "author", "aliy");

        // Use the signed URL to upload an object.
        ossClient.putObject(signedUrl, new ByteArrayInputStream("Hello OSS".getBytes()), -1, requestHeaders, true);

        // Shut down the OSSClient instance.
        ossClient.shutdown();
    }
}
