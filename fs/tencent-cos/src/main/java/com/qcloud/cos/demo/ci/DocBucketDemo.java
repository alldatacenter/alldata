package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.bucket.DocBucketRequest;
import com.qcloud.cos.model.ciModel.bucket.DocBucketResponse;

/**
 * 文档预览bucket相关demo https://cloud.tencent.com/document/product/460/46945
 */
public class DocBucketDemo {
    public static void main(String[] args) {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        describeDocProcessBuckets(client);
    }

    /**
     * createDocProcessBucket 开通bucket的文档预览功能。
     *
     * @param client 客户端对象
     */
    public static void createDocProcessBucket(COSClient client) {
        //1.创建任务请求对象
        DocBucketRequest request = new DocBucketRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        //3.调用接口
        Boolean result = client.createDocProcessBucket(request);
        System.out.println(result);
    }

    /**
     * describeDocProcessBuckets 查询账号下已开通文档预览功能的bucket。
     *
     * @param client 客户端对象
     */
    public static void describeDocProcessBuckets(COSClient client) {
        //1.创建任务请求对象
        DocBucketRequest request = new DocBucketRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        //3.调用接口,获取任务响应对象
        DocBucketResponse docBucketResponse = client.describeDocProcessBuckets(request);
        System.out.println(docBucketResponse);
    }

}
