package com.qcloud.cos.demo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.region.Region;

public class ProxyDemo {

    // 通过代理上传数据
    public static void ProxyUploadDemo(String proxyIp, int proxyPort, String username, String password) {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDXXXXXXXX", "1A2Z3YYYYYYYYYY");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-shanghai"));
        clientConfig.setHttpProxyIp(proxyIp);
        clientConfig.setHttpProxyPort(proxyPort);
        clientConfig.setProxyUsername(username); // 如果代理不需要basic认证无需指定userName和password
        clientConfig.setProxyPassword(password);
        clientConfig.setUseBasicAuth(true);
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名需包含appid
        String bucketName = "mybucket-1251668577";
        String key = "aaa/bbb.txt";
        try {
            PutObjectResult putObjectResult = cosclient.putObject(bucketName, key, "data");
            // putobjectResult会返回文件的etag
            String etag = putObjectResult.getETag();
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }

        // 关闭客户端
        cosclient.shutdown();
    }

    public static void main(String[] args) {
        if(args.length < 4) {
            System.out.println("proxy ip, port, username and password required!");
            return;
        }
        ProxyUploadDemo(args[0], Integer.valueOf(args[1]), args[2], args[3]);
    }
}

