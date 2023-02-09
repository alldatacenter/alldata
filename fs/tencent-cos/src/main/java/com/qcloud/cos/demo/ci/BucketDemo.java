package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketRequest;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketResponse;

/**
 * 媒体处理 bucket接口相关demo 详情见https://cloud.tencent.com/document/product/460/38914
 */
public class BucketDemo {

    public static void main(String[] args) throws Exception {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        describeMediaBuckets(client);
    }

    /**
     * DescribeMediaBuckets 接口用于查询存储桶是否已开通媒体处理功能。
     *
     * @param client
     */
    public static void describeMediaBuckets(COSClient client) {
        //1.创建模板请求对象
        MediaBucketRequest request = new MediaBucketRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        //3.调用接口,获取桶响应对象
        MediaBucketResponse response = client.describeMediaBuckets(request);
        System.out.println(response);
    }
}
