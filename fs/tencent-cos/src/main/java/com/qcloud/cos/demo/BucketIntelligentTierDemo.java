package com.qcloud.cos.demo;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.BucketIntelligentTierConfiguration;
import com.qcloud.cos.model.SetBucketIntelligentTierConfigurationRequest;
import com.qcloud.cos.region.Region;

public class BucketIntelligentTierDemo {
    public static void main(String[] args) {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("SECRET_ID", "SECRET_KEY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";

        BucketIntelligentTierConfiguration bucketIntelligentTierConfiguration = new BucketIntelligentTierConfiguration();
        bucketIntelligentTierConfiguration.setStatus(BucketIntelligentTierConfiguration.ENABLED);
        bucketIntelligentTierConfiguration.setTransition(new BucketIntelligentTierConfiguration.Transition(30));
        SetBucketIntelligentTierConfigurationRequest setBucketIntelligentTierConfigurationRequest = new SetBucketIntelligentTierConfigurationRequest();
        setBucketIntelligentTierConfigurationRequest.setBucketName(bucketName);
        setBucketIntelligentTierConfigurationRequest.setIntelligentTierConfiguration(bucketIntelligentTierConfiguration);
        cosclient.setBucketIntelligentTieringConfiguration(setBucketIntelligentTierConfigurationRequest);
        BucketIntelligentTierConfiguration bucketIntelligentTierConfiguration1 = cosclient.getBucketIntelligentTierConfiguration(bucketName);
        System.out.println(bucketIntelligentTierConfiguration1.getStatus());
        System.out.println(bucketIntelligentTierConfiguration1.getTransition().getDays());
    }
}
