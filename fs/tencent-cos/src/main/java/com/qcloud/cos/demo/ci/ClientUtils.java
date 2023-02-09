package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;

/**
 * Generate request client tool class
 */
public class ClientUtils {

    public static COSClient getTestClient() {
        // 1 初始化用户身份信息（secretId, secretKey）。
        String secretId = "secretId";
        String secretKey = "secretKey";
        return getCosClient(secretId,secretKey,"ap-guangzhou");
    }

    public static COSClient getCosClient(String secretId, String secretKey, String _region) {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的区域, CI 地域的简称请参照 https://cloud.tencent.com/document/product/436/6224
        // clientConfig 中包含了设置 region, https(默认 https), 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
        Region region = new Region(_region);
        ClientConfig clientConfig = new ClientConfig(region);
        clientConfig.setHttpProtocol(HttpProtocol.http);
        // 3 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }
}
