package com.qcloud.cos.demo;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.AnonymousCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.endpoint.UserSpecifiedEndpointBuilder;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;

public class GetObjectURLDemo {
    public static void main(String[] args) {
        getObjectUrlWithEndpoint();
    }

    public static void getObjectUrl() {
        // getObjectUrl 不需要验证身份信息
        COSCredentials cred = new AnonymousCOSCredentials();
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 设置生成的 url 的协议名
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);

        String key = "test/my_test中文.json";
        String bucketName = "mybucket-1251668577";

        System.out.println(cosclient.getObjectUrl(bucketName, key));
    }

    public static void getObjectUrlWithVersionId() {
        // getObjectUrl 不需要验证身份信息
        COSCredentials cred = new AnonymousCOSCredentials();
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 设置生成的 url 的协议名
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);

        String key = "test/my_test中文.json";
        String bucketName = "mybucket-1251668577";
        String versionId = "xxxyyyzzz111222333";

        System.out.println(cosclient.getObjectUrl(bucketName, key, versionId));
    }

    public static void getObjectUrlWithEndpoint() {
        // getObjectUrl 不需要验证身份信息
        COSCredentials cred = new AnonymousCOSCredentials();
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 设置生成的 url 的协议名
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 设置自定义的域名
        UserSpecifiedEndpointBuilder endpointBuilder = new UserSpecifiedEndpointBuilder("test.endpoint.com", "service.cos.myqcloud.com");
        clientConfig.setEndpointBuilder(endpointBuilder);
        // 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);

        String key = "test/my_test中文.json";
        String bucketName = "mybucket-1251668577";
        String versionId = "xxxyyyzzz111222333";

        System.out.println(cosclient.getObjectUrl(bucketName, key, versionId));
    }
}
