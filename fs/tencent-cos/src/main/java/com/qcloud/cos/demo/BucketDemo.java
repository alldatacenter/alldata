package com.qcloud.cos.demo;

import java.util.LinkedList;
import java.util.List;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.model.BucketLoggingConfiguration;
import com.qcloud.cos.model.BucketTaggingConfiguration;
import com.qcloud.cos.model.BucketVersioningConfiguration;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.CreateBucketRequest;
import com.qcloud.cos.model.SetBucketLoggingConfigurationRequest;
import com.qcloud.cos.model.SetBucketTaggingConfigurationRequest;
import com.qcloud.cos.model.SetBucketVersioningConfigurationRequest;
import com.qcloud.cos.model.TagSet;
import com.qcloud.cos.region.Region;

/**
 * 展示了创建bucket, 删除bucket, 查询bucket是否存在的demo
 *
 */
public class BucketDemo {
    // 创建bucket
    public static void CreateBucketDemo() {
        // 1 初始化用户身份信息(appid, secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名称, 需包含appid
        String bucketName = "publicreadbucket-1251668577";
        
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        // 设置bucket的权限为PublicRead(公有读私有写), 其他可选有私有读写, 公有读私有写
        createBucketRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        Bucket bucket = cosclient.createBucket(createBucketRequest);
        
        // 关闭客户端
        cosclient.shutdown();
    }

    // 开启 bucket 版本控制
    public static void SetBucketVersioning() {
        // 1 初始化用户身份信息(appid, secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名称, 需包含appid
        String bucketName = "examplebucket-1251668577";

        // 开启版本控制
        BucketVersioningConfiguration bucketVersioningConfiguration = new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
        // 关闭版本控制
        //BucketVersioningConfiguration bucketVersioningConfiguration = new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED);
        SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest = new SetBucketVersioningConfigurationRequest(bucketName, bucketVersioningConfiguration);
        cosclient.setBucketVersioningConfiguration(setBucketVersioningConfigurationRequest);

        cosclient.shutdown();
    }

    // 开启日志存储
    public static void SetBucketLogging() {
        // 1 初始化用户身份信息(appid, secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名称, 需包含appid
        String bucketName = "examplebucket-1251668577";

        BucketLoggingConfiguration bucketLoggingConfiguration = new BucketLoggingConfiguration();
        // 设置日志存储的 bucket
        bucketLoggingConfiguration.setDestinationBucketName(bucketName);
        // 设置日志存储的前缀
        bucketLoggingConfiguration.setLogFilePrefix("logs/");
        SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest =
                new SetBucketLoggingConfigurationRequest(bucketName, bucketLoggingConfiguration);
        cosclient.setBucketLoggingConfiguration(setBucketLoggingConfigurationRequest);
    }

    // 使用 bucket tag
    public static void SetGetDeleteBucketTagging() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";
        List<TagSet> tagSetList = new LinkedList<TagSet>();
        TagSet tagSet = new TagSet();
        tagSet.setTag("age", "18");
        tagSet.setTag("name", "xiaoming");
        tagSetList.add(tagSet);
        BucketTaggingConfiguration bucketTaggingConfiguration = new BucketTaggingConfiguration();
        bucketTaggingConfiguration.setTagSets(tagSetList);
        SetBucketTaggingConfigurationRequest setBucketTaggingConfigurationRequest =
                new SetBucketTaggingConfigurationRequest(bucketName, bucketTaggingConfiguration);
        cosclient.setBucketTaggingConfiguration(setBucketTaggingConfigurationRequest);

        cosclient.getBucketTaggingConfiguration(bucketName);
        cosclient.deleteBucketTaggingConfiguration(bucketName);
    }
    
    // 删除bucket, 只用于空bucket, 含有数据的bucket需要在删除前清空删除。
    public static void DeleteBucketDemo() {
        // 1 初始化用户身份信息(appid, secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名称, 需包含appid        
        String bucketName = "publicreadbucket-1251668577";
        // 删除bucket
        cosclient.deleteBucket(bucketName);
        
        // 关闭客户端
        cosclient.shutdown();
    }
    
    // 查询bucket是否存在
    public static void JudgeBucketExistDemo() {
        // 1 初始化用户身份信息(appid, secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        
        String bucketName = "publicreadbucket-1251668577";
        // 判断bucket是否存在
        cosclient.doesBucketExist(bucketName);
        
        // 关闭客户端
        cosclient.shutdown();
    }    

    public static void ListBuckets() {
        // 1 初始化用户身份信息(appid, secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "****************************");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-shanghai"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);

        List<Bucket> buckets = cosclient.listBuckets();

        for (Bucket bucket : buckets) {
            System.out.println(bucket.getName());
            System.out.println(bucket.getLocation());
            System.out.println(bucket.getOwner());
            System.out.println(bucket.getType());
            System.out.println(bucket.getBucketType());
        }
    }

    public static void main(String[] args) {
        ListBuckets();
    }
}
