package com.qcloud.cos.demo;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.region.Region;


public class GetObjectMetadataDemo {
    public static void main(String[] args) {
        getObjectMetadataDemo();
    }

    // 将本地文件上传到COS
    public static void getObjectMetadataDemo() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-beijing-1"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";

        String key = "aaa/bbb.txt";
        ObjectMetadata objectMetadata = cosclient.getObjectMetadata(bucketName, key);
        System.out.println(objectMetadata.getCrc64Ecma());
        System.out.println(objectMetadata.getLastModified());
        // 关闭客户端
        cosclient.shutdown();
    }
}

