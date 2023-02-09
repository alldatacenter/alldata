package com.qcloud.cos.demo;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.BucketRefererConfiguration;
import com.qcloud.cos.region.Region;

public class BucketRefererDemo {
    public static COSClient cosClient = null;
    // bucket名需包含appid
    public static String bucketName = "examplebucket-1251668577";

    public static void createCosClient() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKID********************************", "********************************");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-shanghai"));
        // 3 生成cos客户端
        cosClient = new COSClient(cred, clientConfig);
    }

    public static void shutdown() {
        if (cosClient != null) {
            cosClient.shutdown();
            cosClient = null;
        }
    }

    public static void setBucketReferer() {
        BucketRefererConfiguration configuration = new BucketRefererConfiguration();

        // 启用防盗链
        configuration.setStatus(BucketRefererConfiguration.DISABLED);
        // 设置防盗链类型为白名单
        //configuration.setRefererType(BucketRefererConfiguration.WHITELIST);
        // 设置防盗链类型为黑名单 (与白名单二选一)
        configuration.setRefererType(BucketRefererConfiguration.BLACKLIST);

        // 填写要设置的域名
        configuration.addDomain("test.com");
        configuration.addDomain("test.1.com");

        // （可选）设置是否允许空防盗链访问，缺省就是 DENY
        configuration.setEmptyReferConfiguration(BucketRefererConfiguration.DENY);
        // configuration.setEmptyReferConfiguration(BucketRefererConfiguration.ALLOW);

        cosClient.setBucketRefererConfiguration(bucketName, configuration);
    }

    public static void getBucketReferer() {
        BucketRefererConfiguration configuration = cosClient.getBucketRefererConfiguration(bucketName);

        if (configuration == null) {
            System.out.printf("bucket %s does not have referer configuration\n", bucketName);
            return;
        }

        System.out.printf("status: %s\n", configuration.getStatus());
        System.out.printf("referer type: %s\n", configuration.getRefererType());
        System.out.printf("empty referer config: %s\n", configuration.getEmptyReferConfiguration());

        for (String domain : configuration.getDomainList()) {
            System.out.printf("domain: %s\n", domain);
        }
    }

    public static void main(String[] args) {
        createCosClient();

        setBucketReferer();
        getBucketReferer();

        shutdown();
    }
}
