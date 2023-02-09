import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;

import java.io.IOException;


public class StorageTypeSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";


    public static void main(String[] args) {
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Convert the storage class of an object
            // The following code provides an example of how to convert the storage class of an object from Standard or IA to Archive
            changeStorageTypeObject(ossClient);

            // Convert the storage class of an object
            // The following code provides an example of how to convert the storage class of an object from Archive to IA
            changeStorageTypeObject2(ossClient);

        } catch (OSSException oe) {
            System.out.println("Error Message: " + oe.getErrorMessage());
            System.out.println("Error Code:       " + oe.getErrorCode());
            System.out.println("Request ID:      " + oe.getRequestId());
            System.out.println("Host ID:           " + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Error Message: " + ce.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            /*
             * Do not forget to shut down the client finally to release all allocated resources.
             */
            ossClient.shutdown();
        }
    }

    private static void changeStorageTypeObject(OSS ossClient) {
        // This example requires a bucket containing an object of the Standard or IA storage class.
        String objectName = "*** Provide object name ***";
        // Create CopyObjectRequest.
        CopyObjectRequest request = new CopyObjectRequest(bucketName, objectName, bucketName, objectName) ;

        // Create ObjectMetadata.
        ObjectMetadata objectMetadata = new ObjectMetadata();

        // Encapsulate the header. Set the storage class to Archive.
        objectMetadata.setHeader("x-oss-storage-class", StorageClass.Archive);
        request.setNewObjectMetadata(objectMetadata);

        // Modify the storage class of the object.
        CopyObjectResult result = ossClient.copyObject(request);
    }

    private static void changeStorageTypeObject2(OSS ossClient) throws InterruptedException, IOException {
        // This example requires a bucket containing an object of the Archive storage class.
        String objectName = "*** Provide object name ***";
        // Obtain the object metadata.
        ObjectMetadata objectMetadata = ossClient.getObjectMetadata(bucketName, objectName);

        // Check whether the storage class of the source object is Archive. If the storage class of the source object is Archive, you must restore the object before you can modify the storage class.
        StorageClass storageClass = objectMetadata.getObjectStorageClass();
        System.out.println("storage type:" + storageClass);
        if (storageClass == StorageClass.Archive) {
            // Restore the object.
            ossClient.restoreObject(bucketName, objectName);

            // Wait until the object is restored.
            do {
                Thread.sleep(1000);
                objectMetadata = ossClient.getObjectMetadata(bucketName, objectName);
            } while (! objectMetadata.isRestoreCompleted());
        }

        // Create CopyObjectRequest.
        CopyObjectRequest request = new CopyObjectRequest(bucketName, objectName, bucketName, objectName) ;

        // Create ObjectMetadata.
        objectMetadata = new ObjectMetadata();

        // Encapsulate the header. Set the storage class to IA.
        objectMetadata.setHeader("x-oss-storage-class", StorageClass.IA);
        request.setNewObjectMetadata(objectMetadata);

        // Modify the storage class of the object.
        CopyObjectResult result = ossClient.copyObject(request);
    }
}
