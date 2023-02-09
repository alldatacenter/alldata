package com.aliyun.oss.integrationtests;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BucketMetaQueryTest extends TestBase {
    private static int maxResults = 20;
    private static String query = "{\"Field\": \"Size\",\"Value\": \"1048576\",\"Operation\": \"lt\"}";
    private static String query2 = "{\"SubQueries\":[{\"Field\":\"Filename\",\"Value\":\"<>&=?/1\\\\2\\\\-test1.txt\",\"Operation\":\"eq\"},{\"Field\":\"Size\",\"Value\":\"1000000\",\"Operation\":\"lt\"}],\"Operation\":\"and\"}";
    private static String sort = "Size";
    private static String nextToken = "";
    private static String fileName1 = "<>&=?/1\\2\\-test1.txt";
    private static String fileName2 = "<>&=?/1\\2\\-test2.txt";
    private static String fileName3 = "<>&=?/1\\2\\-test3.txt";
    @Before
    public void before(){
        try {
            ossClient.getMetaQueryStatus(bucketName);
        }  catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.META_QUERY_NOT_EXIST, e.getRawResponseError().substring(e.getRawResponseError().indexOf("<Code>")+6, e.getRawResponseError().indexOf("</Code>")));
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName1, new ByteArrayInputStream("Hello OSS".getBytes()));
        ObjectMetadata metadata = new ObjectMetadata();
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("type0", "document0");
        tags.put("type1", "document1");
        metadata.setObjectTagging(tags);


        Map<String, String> userMetadata = new HashMap<String, String>();
        userMetadata.put("location0", "hangzhou0");
        userMetadata.put("location1", "hangzhou1");
        metadata.setUserMetadata(userMetadata);
        putObjectRequest.setMetadata(metadata);
        ossClient.putObject(putObjectRequest);
        VoidResult result = ossClient.openMetaQuery(bucketName);
        Assert.assertEquals(200, result.getResponse().getStatusCode());
    }

    @After
    public void after(){
        VoidResult closeResult = ossClient.closeMetaQuery(bucketName);
        Assert.assertEquals(200, closeResult.getResponse().getStatusCode());
    }

    @Test
    public void testBucketMetaQueryWithFile() {

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName2, new ByteArrayInputStream("Hello OSS".getBytes()));
        ObjectMetadata metadata = new ObjectMetadata();
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("type", "document");
        tags.put("type3", "document3");
        metadata.setObjectTagging(tags);


        Map<String, String> userMetadata = new HashMap<String, String>();
        userMetadata.put("location", "hangzhou");
        userMetadata.put("location2", "hangzhou2");
        metadata.setUserMetadata(userMetadata);
        putObjectRequest.setMetadata(metadata);
        ossClient.putObject(putObjectRequest);

        try {

            try {
                ossClient.openMetaQuery(bucketName);
            }  catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.META_QUERY_ALREADY_EXIST, e.getRawResponseError().substring(e.getRawResponseError().indexOf("<Code>")+6, e.getRawResponseError().indexOf("</Code>")));
            }

            DoMetaQueryResult doMetaQueryResult = null;
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                GetMetaQueryStatusResult getResult = ossClient.getMetaQueryStatus(bucketName);
                Assert.assertEquals(200, getResult.getResponse().getStatusCode());
                DoMetaQueryRequest doMetaQueryRequest = new DoMetaQueryRequest(bucketName, maxResults, query, sort);
                doMetaQueryRequest.setOrder(SortOrder.DESC);
                if ("Running".equals(getResult.getState())) {
                    doMetaQueryResult = ossClient.doMetaQuery(doMetaQueryRequest);
                    if (doMetaQueryResult.getFiles() != null) {
                        break;
                    }
                }
            }
            validateFile(doMetaQueryResult);

            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                GetMetaQueryStatusResult getResult = ossClient.getMetaQueryStatus(bucketName);
                Assert.assertEquals(200, getResult.getResponse().getStatusCode());
                DoMetaQueryRequest doMetaQueryRequest = new DoMetaQueryRequest(bucketName, maxResults, query2, sort);
                if ("Running".equals(getResult.getState())) {
                    doMetaQueryRequest.setOrder(SortOrder.DESC);
                    doMetaQueryResult = ossClient.doMetaQuery(doMetaQueryRequest);
                    if (doMetaQueryResult.getFiles() != null) {
                        break;
                    }
                }
            }
            validateFile(doMetaQueryResult);
        } catch (OSSException e) {
            System.out.println("ErrorCode:" + e.getErrorCode() + "Message:" +e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testBucketMetaQueryWithAggregation() {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName3, new ByteArrayInputStream("Hello OSS".getBytes()));
        ObjectMetadata metadata = new ObjectMetadata();
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("type", "document");
        tags.put("type3", "document3");
        metadata.setObjectTagging(tags);

        Map<String, String> userMetadata = new HashMap<String, String>();
        userMetadata.put("x-oss-meta-location", "hangzhou");
        userMetadata.put("x-oss-meta-location2", "hangzhou2");
        metadata.setUserMetadata(userMetadata);
        putObjectRequest.setMetadata(metadata);
        ossClient.putObject(putObjectRequest);

        try {
            GetMetaQueryStatusResult getResult = ossClient.getMetaQueryStatus(bucketName);
            Assert.assertEquals(200, getResult.getResponse().getStatusCode());
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
            doMetaQueryRequest.setOrder(SortOrder.ASC);
            doMetaQueryRequest.setAggregations(aggregations);
            DoMetaQueryResult doMetaQueryResult = null;
            while (true){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                doMetaQueryResult = ossClient.doMetaQuery(doMetaQueryRequest);
                if(doMetaQueryResult.getAggregations() != null){
                    break;
                }
            }

            Assert.assertEquals("Size", doMetaQueryResult.getAggregations().getAggregation().get(0).getField());
            Assert.assertEquals("max", doMetaQueryResult.getAggregations().getAggregation().get(0).getOperation());
            Assert.assertEquals("Size", doMetaQueryResult.getAggregations().getAggregation().get(1).getField());
            Assert.assertEquals("group", doMetaQueryResult.getAggregations().getAggregation().get(1).getOperation());
        } catch (OSSException e) {
            System.out.println("ErrorCode:" + e.getErrorCode() + " Message:" +e.getMessage());
        }
    }


    private void validateFile(DoMetaQueryResult doMetaQueryResult) {
        if(fileName1.equals(doMetaQueryResult.getFiles().getFile().get(0).getFilename())){
            Assert.assertEquals(fileName1, doMetaQueryResult.getFiles().getFile().get(0).getFilename());
            Assert.assertEquals("Normal", doMetaQueryResult.getFiles().getFile().get(0).getOssObjectType());
            Assert.assertEquals("Standard", doMetaQueryResult.getFiles().getFile().get(0).getOssStorageClass());
            Assert.assertEquals("default", doMetaQueryResult.getFiles().getFile().get(0).getObjectACL());

            Assert.assertEquals(2, doMetaQueryResult.getFiles().getFile().get(0).getOssTaggingCount());
            Assert.assertEquals("type0", doMetaQueryResult.getFiles().getFile().get(0).getOssTagging().getTagging().get(0).getKey());
            Assert.assertEquals("document0", doMetaQueryResult.getFiles().getFile().get(0).getOssTagging().getTagging().get(0).getValue());
            Assert.assertEquals("type1", doMetaQueryResult.getFiles().getFile().get(0).getOssTagging().getTagging().get(1).getKey());
            Assert.assertEquals("document1", doMetaQueryResult.getFiles().getFile().get(0).getOssTagging().getTagging().get(1).getValue());
            Assert.assertEquals("hangzhou0", doMetaQueryResult.getFiles().getFile().get(0).getOssUserMeta().getUserMeta().get(0).getValue());
            Assert.assertEquals("hangzhou1", doMetaQueryResult.getFiles().getFile().get(0).getOssUserMeta().getUserMeta().get(1).getValue());
        } else if (fileName2.equals(doMetaQueryResult.getFiles().getFile().get(0).getFilename())){
            Assert.assertEquals(fileName2, doMetaQueryResult.getFiles().getFile().get(0).getFilename());
            Assert.assertEquals("Normal", doMetaQueryResult.getFiles().getFile().get(0).getOssObjectType());
            Assert.assertEquals("Standard", doMetaQueryResult.getFiles().getFile().get(0).getOssStorageClass());
            Assert.assertEquals("default", doMetaQueryResult.getFiles().getFile().get(0).getObjectACL());

            Assert.assertEquals(2, doMetaQueryResult.getFiles().getFile().get(0).getOssTaggingCount());
            Assert.assertEquals("type", doMetaQueryResult.getFiles().getFile().get(0).getOssTagging().getTagging().get(0).getKey());
            Assert.assertEquals("document", doMetaQueryResult.getFiles().getFile().get(0).getOssTagging().getTagging().get(0).getValue());
            Assert.assertEquals("type3", doMetaQueryResult.getFiles().getFile().get(0).getOssTagging().getTagging().get(1).getKey());
            Assert.assertEquals("document3", doMetaQueryResult.getFiles().getFile().get(0).getOssTagging().getTagging().get(1).getValue());
            Assert.assertEquals("hangzhou", doMetaQueryResult.getFiles().getFile().get(0).getOssUserMeta().getUserMeta().get(0).getValue());
            Assert.assertEquals("hangzhou2", doMetaQueryResult.getFiles().getFile().get(0).getOssUserMeta().getUserMeta().get(1).getValue());
        }
    }

}