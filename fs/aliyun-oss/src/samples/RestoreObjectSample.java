import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;

import java.io.IOException;


public class RestoreObjectSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";
    private static String objectName = "*** Provide object name ***";

    public static void main(String[] args) {
        // Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Restore archived objects
            restoreStorageObject(ossClient);

            // Restore cold archived objects
            resotreObject(ossClient);

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

    private static void resotreObject(OSS ossClient) {
        // Refer to the following code if you set the storage class of the object to upload to Cold Archive.
        // Create a PutObjectRequest object.
        // PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new ByteArrayInputStream("<yourContent>".getBytes()));
        // Set the storage class of the object to Cold Archive in the object metadata.
        // ObjectMetadata metadata = new ObjectMetadata();
        // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.ColdArchive.toString());
        // putObjectRequest.setMetadata(metadata);
        // Configure the storage class of the object to upload.
        // ossClient.putObject(putObjectRequest);

        // Configure the restore mode for the cold archived object.
        // RestoreTier.RESTORE_TIER_EXPEDITED: The object is restored within an hour.
        // RestoreTier.RESTORE_TIER_STANDARD: The object is restored within two to five hours.
        // RestoreTier.RESTORE_TIER_BULK: The object is restored within five to twelve hours.
        RestoreJobParameters jobParameters = new RestoreJobParameters(restoreTier);

        // Configure parameters. For example, set the restore mode for the object to Standard, and set the duration for which the object can remain in the restored state to two days.
        // The first parameter indicates the validity period of the restored state. The default value is one day. This parameter applies to archived and cold archived objects.
        // The jobParameters indicates the restore mode of the object and applies only to cold archived objects.
        RestoreConfiguration configuration = new RestoreConfiguration(2, jobParameters);

        // Initiate a restore request.
        ossClient.restoreObject(bucketName, objectName, configuration);

    }

    private static void restoreStorageObject(OSS ossClient) throws InterruptedException, IOException {
        ObjectMetadata objectMetadata = ossClient.getObjectMetadata(bucketName, objectName);

        // Verify whether the object is an archived object.
        StorageClass storageClass = objectMetadata.getObjectStorageClass();
        if (storageClass == StorageClass.Archive) {
            // Restore the object.
            ossClient.restoreObject(bucketName, objectName);

            // Wait until the object is restored.
            do {
                Thread.sleep(1000);
                objectMetadata = ossClient.getObjectMetadata(bucketName, objectName);
            } while (! objectMetadata.isRestoreCompleted());
        }

        // Obtain the restored object.
        OSSObject ossObject = ossClient.getObject(bucketName, objectName);
        ossObject.getObjectContent().close();
    }
}
