package com.qcloud.cos.demo;

import java.io.File;
import java.util.Date;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;

public class PutObjectDemo {

    static COSClient cosClient = createCli();;

    static COSClient createCli() {
        return createCli("ap-shanghai");
    }

    static COSClient createCli(String region) {
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX","1A2Z3YYYYYYYYYY");
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 生成cos客户端
        return new COSClient(cred, clientConfig);
    }

    static void putObjectDemo() {
        String bucketName = "examplebucket-1251668577";
        String key = "abc/abc.txt";

        String localPath = "abc.txt";

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setHeader("expires", new Date(1660000000000L));

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, new File(localPath));
        putObjectRequest.withMetadata(objectMetadata);

        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

        System.out.println(putObjectResult.getRequestId());

        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        COSObject cosObject = cosClient.getObject(getObjectRequest);
        System.out.println(cosObject.getObjectMetadata().getRequestId());

        cosClient.shutdown();
    }

    public static void main(String[] args) {
        putObjectDemo();
    }
}
