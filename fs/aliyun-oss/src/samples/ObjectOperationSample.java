import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;

import java.io.ByteArrayInputStream;


public class ObjectOperationSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";
    private static String objectName = "<yourObjectName>";
    private static String sourceBucketName = "<yourSourceBucketName>";
    private static String sourceObjectName = "<yourSourceObjectName>";
    private static String destinationBucketName = "<yourDestinationBucketName>";
    private static String destinationObjectName = "<yourDestinationObjectName>";
    private static OSS ossClient = null;

    public static void main(String[] args) {
        // Create an OSSClient instance.
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // Disable overwrite for objects with the same name - Simple upload
            putObject();

            // Disable overwrite for objects with the same name - Copy objects。
            copyObject();

            // Rename objects
            renameObject();
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

    public static void putObject() {
        String content = "Hello OSS!";
        // Create a PutObjectRequest object.
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new ByteArrayInputStream(content.getBytes()));

        // Specify whether to overwrite the object with the same name.
        // By default, if x-oss-forbid-overwrite is not specified, the object with the same name is overwritten.
        // If x-oss-forbid-overwrite is set to false, the object with the same name is overwritten.
        // If x-oss-forbid-overwrite is set to true, the object with the same name is not overwritten. If an object with the same name already exists, an error is reported.
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setHeader("x-oss-forbid-overwrite", "true");
        putObjectRequest.setMetadata(metadata);

        // Upload the object.
        ossClient.putObject(putObjectRequest);
    }

    public static void copyObject() {
        // Create an OSSClient instance.
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucketName, sourceObjectName, destinationBucketName, destinationObjectName);

        // Configure new object metadata.
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/html");
        // Specify whether to overwrite the object with the same name.
        // By default, if x-oss-forbid-overwrite is not specified, the object with the same name is overwritten.
        // If x-oss-forbid-overwrite is set to false, the object with the same name is overwritten.
        // If x-oss-forbid-overwrite is set to true, the object with the same name is not overwritten. If an object with the same name already exists, an error is reported.
        metadata.setHeader("x-oss-forbid-overwrite", "true");
        copyObjectRequest.setNewObjectMetadata(metadata);

        // Copy the object.
        CopyObjectResult result = ossClient.copyObject(copyObjectRequest);
        System.out.println("ETag: " + result.getETag() + " LastModified: " + result.getLastModified());
    }

    public static void renameObject() {
        // Set the absolute path of the source object. The absolute path of the directory cannot contain the bucket name.
        String sourceObject = "exampleobject.txt";
        // Set the absolute path of the destination object in the same bucket as that of the source object. The absolute path of the object cannot contain the bucket name.
        String destnationObject = "newexampleobject.txt";

        // Change the absolute path of the source object in the bucket to the absolute path of the destination object.
        RenameObjectRequest renameObjectRequest = new RenameObjectRequest(bucketName, sourceObject, destnationObject);
        ossClient.renameObject(renameObjectRequest);
    }
}

