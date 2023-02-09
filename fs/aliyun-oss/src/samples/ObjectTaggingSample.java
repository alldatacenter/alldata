import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.model.*;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ObjectTaggingSample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";
    private static String objectName = "*** Provide object name ***";

    public static void main(String[] args) throws ParseException {
        //  Create an OSSClient instance.
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // Add tags to an object when you upload the object
            setObjectTagging(ossClient);

            // Add tags to or modify the tags of an existing object
            addObjectTaggingeExisting(ossClient);

            // Add tags to or modify the tags of a specified version of an object
            setObjectTaggingByVersion(ossClient);

            // Add tags to a symbolic link
            setSymlinkTagging(ossClient);

            // Query the tags of an object
            getObjectTagging(ossClient);

            // Query the tags of a specified version of an object
            getObjectTaggingByVersionId(ossClient);

            // Delete the tags of an object
            deleteObjectTagging(ossClient);

            // Delete the tags of a specified version of an object
            deleteObjectTaggingByVersion(ossClient);

            // Set tags as the matching condition of a lifecycle rule
            setTagsOfBucketLifecycle(ossClient);

            // View the tags set as the matching condition of a lifecycle rule
            getTagsOfBucketLifecycle(ossClient);

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

    private static void setObjectTagging(OSS ossClient) {
        Map<String, String> tags = new HashMap<String, String>();
        // Specify the key and value of the object tag. For example, set the key to owner and the value to John.
        tags.put("owner", "John");
        tags.put("type", "document");

        // Configure the tags in the HTTP header.
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setObjectTagging(tags);

        // Upload the object and add tags to it.
        String content = "<yourtContent>";
        ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content.getBytes()), metadata);
    }

    private static void addObjectTaggingeExisting(OSS ossClient) {
        Map<String, String> tags = new HashMap<String, String>();
        // Specify the key and value of the object tag. For example, set the key to owner and the value to John.
        tags.put("owner", "John");
        tags.put("type", "document");

        // Add tags to the object.
        ossClient.setObjectTagging(bucketName, objectName, tags);
    }

    private static void setObjectTaggingByVersion(OSS ossClient) {
        // Specify the version ID of the object. Example: CAEQMxiBgICAof2D0BYiIDJhMGE3N2M1YTI1NDQzOGY5NTkyNTI3MGYyMzJm****.
        String versionId = "CAEQMxiBgICAof2D0BYiIDJhMGE3N2M1YTI1NDQzOGY5NTkyNTI3MGYyMzJm****";
        Map<String, String> tags = new HashMap<String, String>(1);
        // Specify the key and value of the object tag. For example, set the key to owner and the value to John.
        tags.put("owner", "John");
        tags.put("type", "document");

        SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(bucketName, objectName, tags);
        setObjectTaggingRequest.setVersionId(versionId);
        ossClient.setObjectTagging(setObjectTaggingRequest);
    }

    private static void setSymlinkTagging(OSS ossClient) {
        // Specify the full path of the symbolic link. Example: shortcut/myobject.txt.
        String symLink = "shortcut/myobject.txt";
        // Specify the full path of the object. Example: exampledir/exampleobject.txt. The full path of the object cannot contain the bucket name.
        String destinationObjectName = "exampledir/exampleobject.txt";
        // Configure the tags to be added to the symbolic link.
        Map<String, String> tags = new HashMap<String, String>();
        // Specify the key and value of the object tag. For example, set the key to owner and the value to John.
        tags.put("owner", "John");
        tags.put("type", "document");

        // Create metadata for the object to upload.
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setObjectTagging(tags);

        // Create a request to create the symbolic link.
        CreateSymlinkRequest createSymlinkRequest = new CreateSymlinkRequest(bucketName, symLink, destinationObjectName);

        // Set the object metadata.
        createSymlinkRequest.setMetadata(metadata);

        // Create the symbolic link.
        ossClient.createSymlink(createSymlinkRequest);

        //  View the tags added to the symbolic link.
        TagSet tagSet = ossClient.getObjectTagging(bucketName, symLink);
        Map<String, String> getTags = tagSet.getAllTags();
        System.out.println("symLink tagging: " + getTags.toString());
    }

    private static void getObjectTagging(OSS ossClient) {
        // Query the tags of the object.
        TagSet tagSet = ossClient.getObjectTagging(bucketName, objectName);
        Map<String, String> tags = tagSet.getAllTags();

        // View the tags of the object.
        System.out.println("object tagging: " + tags.toString());
    }

    private static void getObjectTaggingByVersionId(OSS ossClient) {
        // Specify the version ID of the object. Example: CAEQMxiBgICAof2D0BYiIDJhMGE3N2M1YTI1NDQzOGY5NTkyNTI3MGYyMzJm****.
        String versionId = "CAEQMxiBgICAof2D0BYiIDJhMGE3N2M1YTI1NDQzOGY5NTkyNTI3MGYyMzJm****";

        GenericRequest genericRequest = new GenericRequest(bucketName, objectName, versionId);
        TagSet tagSet = ossClient.getObjectTagging(genericRequest);

        // View the tags of the object.
        System.out.println("object tagging: " + tagSet.toString());
    }

    private static void deleteObjectTagging(OSS ossClient) {
        // Specify the full path of the object. Example: exampledir/exampleobject.txt. The full path of the object cannot contain bucket names.
        String objectName = "exampledir/exampleobject.txt";

        // Delete the tags of the object.
        ossClient.deleteObjectTagging(bucketName, objectName);
    }

    private static void deleteObjectTaggingByVersion(OSS ossClient) {
        // Specify the version ID of the object. Example: CAEQMxiBgICAof2D0BYiIDJhMGE3N2M1YTI1NDQzOGY5NTkyNTI3MGYyMzJm****.
        String versionId = "CAEQMxiBgICAof2D0BYiIDJhMGE3N2M1YTI1NDQzOGY5NTkyNTI3MGYyMzJm****";

        // Specify the full path of the object. Example: exampledir/exampleobject.txt. The full path of the object cannot contain bucket names.
        String objectName = "exampledir/exampleobject.txt";

        GenericRequest genericRequest = new GenericRequest(bucketName, objectName, versionId);
        ossClient.deleteObjectTagging(genericRequest);
    }

    private static void setTagsOfBucketLifecycle(OSS ossClient) throws ParseException {
        // Creates a SetBucketLifecycleRequest.
        SetBucketLifecycleRequest request = new SetBucketLifecycleRequest(bucketName);

        // Sets the rule ID and the prefixes and tags used as the matching condition.
        String ruleId0 = "rule0";
        String matchPrefix0 = "A0/";
        Map<String, String> matchTags0 = new HashMap<String, String>();
        matchTags0.put("key0", "value0");


        String ruleId1 = "rule1";
        String matchPrefix1 = "A1/";
        Map<String, String> matchTags1 = new HashMap<String, String>();
        matchTags1.put("key1", "value1");


        String ruleId2 = "rule2";
        String matchPrefix2 = "A2/";

        String ruleId3 = "rule3";
        String matchPrefix3 = "A3/";

        // Sets an expiration rule for objects so that objects are expired three days after they are modified for the last time.
        LifecycleRule rule = new LifecycleRule(ruleId0, matchPrefix0, LifecycleRule.RuleStatus.Enabled, 3);
        rule.setTags(matchTags0);
        request.AddLifecycleRule(rule);

        // Sets an expiration rule for objects so that objects created before the specified date are expired.
        rule = new LifecycleRule(ruleId1, matchPrefix1, LifecycleRule.RuleStatus.Enabled);
        rule.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
        rule.setTags(matchTags1);
        request.AddLifecycleRule(rule);

        // Sets an expiration rules for parts so that parts are expired 3 days after they are generated.
        rule = new LifecycleRule(ruleId2, matchPrefix2, LifecycleRule.RuleStatus.Enabled);
        LifecycleRule.AbortMultipartUpload abortMultipartUpload = new LifecycleRule.AbortMultipartUpload();
        abortMultipartUpload.setExpirationDays(3);
        rule.setAbortMultipartUpload(abortMultipartUpload);
        request.AddLifecycleRule(rule);

        // Sets an expiration rules for parts so that parts generated before the specified date are expired.
        rule = new LifecycleRule(ruleId3, matchPrefix3, LifecycleRule.RuleStatus.Enabled);
        abortMultipartUpload = new LifecycleRule.AbortMultipartUpload();
        abortMultipartUpload.setCreatedBeforeDate(DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z"));
        rule.setAbortMultipartUpload(abortMultipartUpload);
        request.AddLifecycleRule(rule);

        ossClient.setBucketLifecycle(request);

    }

    private static void getTagsOfBucketLifecycle(OSS ossClient) {
        // Obtains the lifecycle rule set for the object.
        List<LifecycleRule> rules = ossClient.getBucketLifecycle(bucketName);

        // Views the lifecycle rule set for the object.
        for (LifecycleRule rule1 : rules) {
            // Views the rule ID.
            System.out.println("rule id: " + rule1.getId());

            // Views the prefixes set as the matching condition.
            System.out.println("rule prefix: " + rule1.getPrefix());

            // Views the tags set as the matching condition.
            if (rule1.hasTags()) {
                System.out.println("rule tagging: "+ rule1.getTags().toString());
            }

            // Views the expiration rules in which objects are expired some days after they are modified for the last time.
            if (rule1.hasExpirationDays()) {
                System.out.println("rule expiration days: " + rule1.getExpirationDays());
            }

            // Views the expiration rules in which objects created before a specified date are expired.
            if (rule1.hasCreatedBeforeDate()) {
                System.out.println("rule expiration create before days: " + rule1.getCreatedBeforeDate());
            }

            // Views the expiration rule for parts.
            if(rule1.hasAbortMultipartUpload()) {
                if(rule1.getAbortMultipartUpload().hasExpirationDays()) {
                    System.out.println("rule abort uppart days: " + rule1.getAbortMultipartUpload().getExpirationDays());
                }

                if (rule1.getAbortMultipartUpload().hasCreatedBeforeDate()) {
                    System.out.println("rule abort uppart create before date: " + rule1.getAbortMultipartUpload().getCreatedBeforeDate());
                }
            }
        }
    }
}
