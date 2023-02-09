package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.queue.MediaListQueueResponse;
import com.qcloud.cos.model.ciModel.queue.MediaQueueRequest;
import com.qcloud.cos.model.ciModel.queue.MediaQueueResponse;

/**
 * 媒体处理 queue接口相关demo 详情见https://cloud.tencent.com/document/product/460/38913
 */
public class QueueDemo {

    public static void main(String[] args) {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        updateMediaQueue(client);
    }

    /**
     * DescribeMediaQueues 接口用于搜索队列。
     * @param client
     */
    public static void describeMediaQueues(COSClient client){
        //1.创建队列请求对象
        MediaQueueRequest request = new MediaQueueRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        //3.调用接口,获取队列响应对象
        MediaListQueueResponse response = client.describeMediaQueues(request);
        System.out.println(response);
    }

    /**
     * UpdateMediaQueue 接口用于更新队列
     * Request中 Name,QueueID,State,NotifyConfig 为必填字段
     * @param client
     */
    public static void updateMediaQueue(COSClient client){
        //1.创建队列请求对象
        MediaQueueRequest request = new MediaQueueRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setQueueId("p9900025e4ec44b5e8225e70a521*****");
        request.getNotifyConfig().setUrl("cloud.tencent.com");
        request.setState("Active");
        request.setName("queue-2");
        //3.调用接口,获取队列响应对象
        MediaQueueResponse response = client.updateMediaQueue(request);
        System.out.println(response);
    }
}
