package com.qcloud.cos.demo;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.Tag.Tag;
import com.qcloud.cos.region.Region;

import java.util.LinkedList;
import java.util.List;

public class ObjectTaggingDemo {
    public static void SetGetDelObjectTaggingDemo() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("COS_SECRET_ID", "COS_SECRET_KEY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";
        String key = "aaa/bbb.txt";
        cosclient.putObject(bucketName, key, "data");
        List<Tag> tags = new LinkedList<>();
        tags.add(new Tag("key", "value"));
        tags.add(new Tag("key-1", "value-1"));
        ObjectTagging objectTagging = new ObjectTagging(tags);
        SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(bucketName, key, objectTagging);
        cosclient.setObjectTagging(setObjectTaggingRequest);
        GetObjectTaggingResult getObjectTaggingResult = cosclient.getObjectTagging(new GetObjectTaggingRequest(bucketName, key));
        List<Tag> resultTagSet = getObjectTaggingResult.getTagSet();
        cosclient.deleteObjectTagging(new DeleteObjectTaggingRequest(bucketName, key));
        GetObjectTaggingResult getObjectTaggingResultSecond = cosclient.getObjectTagging(new GetObjectTaggingRequest(bucketName, key));
        List<Tag> resultTagSetSecond = getObjectTaggingResultSecond.getTagSet();
    }
    public static void main(String[] args) {
        SetGetDelObjectTaggingDemo();
    }
}
