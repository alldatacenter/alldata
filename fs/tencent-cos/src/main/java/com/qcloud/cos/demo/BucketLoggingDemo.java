package com.qcloud.cos.demo;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;

public class BucketLoggingDemo {
    public static void SetGetBucketLoggingDemo() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";
        BucketLoggingConfiguration bucketLoggingConfiguration = new BucketLoggingConfiguration();
        bucketLoggingConfiguration.setDestinationBucketName(bucketName);
        bucketLoggingConfiguration.setLogFilePrefix("logs");
        SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest =
                new SetBucketLoggingConfigurationRequest(bucketName, bucketLoggingConfiguration);
        cosclient.setBucketLoggingConfiguration(setBucketLoggingConfigurationRequest);
        BucketLoggingConfiguration bucketLoggingConfiguration1 = cosclient.getBucketLoggingConfiguration(bucketName);
    }
    public static void main(String[] args) {
        SetGetBucketLoggingDemo();
    }
}
