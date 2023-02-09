package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoRequest;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoResponse;

/**
 * GenerateMediainfo 接口用于获取媒体文件的信息。
 * 请求详情参见：https://cloud.tencent.com/document/product/460/38935
 */
public class MediaInfoDemo {
    public static void main(String[] args) {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        generateMediainfo(client);
    }

    /**
     * GenerateMediainfo 接口用于获取媒体文件的信息。
     * @param client
     */
    public static void generateMediainfo(COSClient client)  {
        //1.创建媒体信息请求对象
        MediaInfoRequest request = new MediaInfoRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        request.getInput().setObject("1.mp3");
        //3.调用接口,获取媒体信息响应对象
        MediaInfoResponse response = client.generateMediainfo(request);
        System.out.println(response.getRequestId());
    }
}
