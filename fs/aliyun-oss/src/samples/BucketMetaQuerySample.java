package sample;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import java.util.ArrayList;
import java.util.List;


public class BucketMetaQuerySample {

    private static String endpoint = "*** Provide OSS endpoint ***";
    private static String accessKeyId = "*** Provide your AccessKeyId ***";
    private static String accessKeySecret = "*** Provide your AccessKeySecret ***";
    private static String bucketName = "*** Provide bucket name ***";

    public static void main(String[] args) {

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            openMetaQuery(ossClient);

            getMetaQueryStatus(ossClient);

            doMetaQueryWithFile(ossClient);

            doMetaQueryWithAggregation(ossClient);

            closeMetaQuery(ossClient);
        } catch (OSSException oe) {
            System.out.println("Error Message: " + oe.getErrorMessage());
            System.out.println("Error Code:       " + oe.getErrorCode());
            System.out.println("Request ID:      " + oe.getRequestId());
            System.out.println("Host ID:           " + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Error Message: " + ce.getMessage());
        } finally {
            if(ossClient != null){
                ossClient.shutdown();
            }
        }
    }

    private static void openMetaQuery(OSS ossClient) {
        ossClient.openMetaQuery(bucketName);
    }

    private static void getMetaQueryStatus(OSS ossClient) {
        GetMetaQueryStatusResult getResult = ossClient.getMetaQueryStatus(bucketName);
        System.out.println(getResult.getPhase());
        System.out.println(getResult.getState());
        System.out.println(getResult.getCreateTime());
        System.out.println(getResult.getUpdateTime());
    }

    private static void doMetaQuery(OSS ossClient) {
        int maxResults = 20;
        String query = "{\"Field\": \"Size\",\"Value\": \"1048576\",\"Operation\": \"lt\"}";
        String sort = "Size";
        DoMetaQueryRequest doMetaQueryRequest = new DoMetaQueryRequest(bucketName, maxResults, query, sort);
        DoMetaQueryResult doMetaQueryResult = ossClient.doMetaQuery(doMetaQueryRequest);
        if(doMetaQueryResult.getFiles() != null){
            for(ObjectFile file : doMetaQueryResult.getFiles().getFile()){
                System.out.println("Filename: " + file.getFilename());
                System.out.println("ETag: " + file.getETag());
                System.out.println("ObjectACL: " + file.getObjectACL());
                System.out.println("OssObjectType: " + file.getOssObjectType());
                System.out.println("OssStorageClass: " + file.getOssStorageClass());
                System.out.println("TaggingCount: " + file.getOssTaggingCount());
                if(file.getOssTagging() != null){
                    for(Tagging tag : file.getOssTagging().getTagging()){
                        System.out.println("Key: " + tag.getKey());
                        System.out.println("Value: " + tag.getValue());
                    }
                }
                if(file.getOssUserMeta() != null){
                    for(UserMeta meta : file.getOssUserMeta().getUserMeta()){
                        System.out.println("Key: " + meta.getKey());
                        System.out.println("Value: " + meta.getValue());
                    }
                }
            }
        } else if(doMetaQueryResult.getAggregations() != null){
            for(Aggregation aggre : doMetaQueryResult.getAggregations().getAggregation()){
                System.out.println("Field: " + aggre.getField());
                System.out.println("Operation: " + aggre.getOperation());
            }
        } else {
            System.out.println("NextToken: " + doMetaQueryResult.getNextToken());
        }
    }

    private static void doMetaQueryWithFile(OSS ossClient) {
        int maxResults = 20;
        String query = "{\"Field\": \"Size\",\"Value\": \"1048576\",\"Operation\": \"lt\"}";
        String sort = "Size";
        DoMetaQueryRequest doMetaQueryRequest = new DoMetaQueryRequest(bucketName, maxResults, query, sort);
        DoMetaQueryResult doMetaQueryResult = ossClient.doMetaQuery(doMetaQueryRequest);
        if(doMetaQueryResult.getFiles() != null){
            for(ObjectFile file : doMetaQueryResult.getFiles().getFile()){
                System.out.println("Filename: " + file.getFilename());
                System.out.println("ETag: " + file.getETag());
                System.out.println("ObjectACL: " + file.getObjectACL());
                System.out.println("OssObjectType: " + file.getOssObjectType());
                System.out.println("OssStorageClass: " + file.getOssStorageClass());
                System.out.println("TaggingCount: " + file.getOssTaggingCount());
                if(file.getOssTagging() != null){
                    for(Tagging tag : file.getOssTagging().getTagging()){
                        System.out.println("Key: " + tag.getKey());
                        System.out.println("Value: " + tag.getValue());
                    }
                }
                if(file.getOssUserMeta() != null){
                    for(UserMeta meta : file.getOssUserMeta().getUserMeta()){
                        System.out.println("Key: " + meta.getKey());
                        System.out.println("Value: " + meta.getValue());
                    }
                }
            }
        }
    }

    private static void doMetaQueryWithAggregation(OSS ossClient) {
        int maxResults = 20;
        DoMetaQueryRequest doMetaQueryRequest = new DoMetaQueryRequest(bucketName, maxResults);
        Aggregations aggregations = new Aggregations();
        List<Aggregation> aggregationList = new ArrayList<Aggregation>();
        Aggregation aggregation = new Aggregation();
        aggregation.setField("Size");
        aggregation.setOperation("max");
        Aggregation aggregation2 = new Aggregation();
        aggregation2.setField("Size");
        aggregation2.setOperation("group");
        aggregationList.add(aggregation);
        aggregationList.add(aggregation2);
        aggregations.setAggregation(aggregationList);
        doMetaQueryRequest.setAggregations(aggregations);
        doMetaQueryRequest.setOrder(SortOrder.ASC);
        DoMetaQueryResult doMetaQueryResult = ossClient.doMetaQuery(doMetaQueryRequest);
        if(doMetaQueryResult.getAggregations() != null){
            for(Aggregation aggre : doMetaQueryResult.getAggregations().getAggregation()){
                System.out.println("Field: " + aggre.getField());
                System.out.println("Operation: " + aggre.getOperation());
                System.out.println("Value: " + aggre.getValue());
                if(aggre.getGroups() != null){
                    for(AggregationGroup group : aggre.getGroups().getGroup()){
                        System.out.println("Value: " + group.getValue());
                        System.out.println("Count: " + group.getCount());
                    }
                }
            }
        }
    }

    private static void closeMetaQuery(OSS ossClient) {
        ossClient.closeMetaQuery(bucketName);
    }
}
