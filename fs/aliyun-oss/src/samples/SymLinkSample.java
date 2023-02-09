import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;


public class SymLinkSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";
    private static String objectName = "*** Provide object name ***";

    public static void main(String[] args) {
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Create a symbolic link
            createSymlink(ossClient);

            // Obtain the name of the object to which the symbolic link points
            getSymlink(ossClient);

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

    private static void getSymlink(OSS ossClient) {
        String symLink = "<yourSymLink>";
        // Obtain the symbolic link.
        OSSSymlink symbolicLink = ossClient.getSymlink(bucketName, symLink);
        // Obtain the content of the object to which the symbolic link points.
        System.out.println(symbolicLink.getSymlink());
        System.out.println(symbolicLink.getTarget());
        System.out.println(symbolicLink.getRequestId());
    }

    private static void createSymlink(OSS ossClient) {
        String symLink = "<yourSymLink>";
        String destinationObjectName = "<yourDestinationObjectName>";
        // Create metadata for the object to upload.
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        // Set the value of the property parameter for the user metadata to property-value.
        metadata.addUserMetadata("property", "property-value");

        // Create a request to create the symbolic link.
        CreateSymlinkRequest createSymlinkRequest = new CreateSymlinkRequest(bucketName, symLink, destinationObjectName);

        // Set the object metadata.
        createSymlinkRequest.setMetadata(metadata);

        // Create the symbolic link.
        ossClient.createSymlink(createSymlinkRequest);
    }
}
