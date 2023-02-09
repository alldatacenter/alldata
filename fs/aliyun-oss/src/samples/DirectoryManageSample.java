import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;


public class DirectoryManageSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";
    private static String objectName = "*** Provide object name ***";

    public static void main(String[] args) {
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Create a directory.
            createDirectory(ossClient);

            // Rename a directory
            renameDirectory(ossClient);

            // Delete a directory
            deleteDirectory(ossClient);

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

    private static void createDirectory(OSS ossClient) {
        // Specify the absolute path of the directory. The absolute path of the directory cannot contain bucket names.
        String directoryName = "exampledir";
        // Create a directory.
        CreateDirectoryRequest createDirectoryRequest = new CreateDirectoryRequest(bucketName, directoryName);
        ossClient.createDirectory(createDirectoryRequest);
    }

    private static void renameDirectory(OSS ossClient) {
        // Specify the absolute path of the source directory. The absolute path of the directory cannot contain bucket names.
        String sourceDir = "exampledir";
        // Specify the absolute path of the destination directory that is in the same bucket as the source directory. The absolute path of the directory cannot contain bucket names.
        String destnationDir = "newexampledir";

        // Change the absolute path of the source directory to that of the destination directory.
        RenameObjectRequest renameObjectRequest = new RenameObjectRequest(bucketName, sourceDir, destnationDir);
        ossClient.renameObject(renameObjectRequest);
    }

    private static void deleteDirectory(OSS ossClient) {
        // Specify the absolute path of the directory. The absolute path of the directory cannot contain bucket names.
        String directoryName = "exampledir";

        // Delete the directory. By default, the non-recursive delete method is used. Make sure that all objects and subdirectories are deleted from this directory.
        DeleteDirectoryRequest deleteDirectoryRequest = new DeleteDirectoryRequest(bucketName, directoryName);
        DeleteDirectoryResult deleteDirectoryResult = ossClient.deleteDirectory(deleteDirectoryRequest);

        // Display the absolute path of the deleted directory.
        System.out.println("delete dir name :" + deleteDirectoryResult.getDirectoryName());
        // Display the total number of deleted objects and directories.
        System.out.println("delete number:" + deleteDirectoryResult.getDeleteNumber());

    }
}
