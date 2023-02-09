package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.auditing.AuditingInfo;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingInputObject;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.BatchImageJobDetail;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import com.qcloud.cos.transfer.ImageAuditingImpl;
import com.qcloud.cos.transfer.MultipleImageAuditingImpl;
import com.qcloud.cos.transfer.TransferManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 内容审核 图片审核接口相关demo 详情见https://cloud.tencent.com/document/product/460/37318
 */
public class ImageAuditingDemo {

    public static void main(String[] args) throws InterruptedException {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        imageAuditing(client);
    }

    /**
     * createImageAuditingJob 接口用于创建图片审核任务。(发送单个任务 推荐)
     *
     * @param client
     */
    public static void imageAuditing(COSClient client) {
        //1.创建任务请求对象
        ImageAuditingRequest request = new ImageAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        //2.1设置请求bucket
        request.setBucketName("demo-123456789");
        //2.2设置审核类型
        request.setDetectType("porn");
        //2.3设置bucket中的图片位置
        request.setObjectKey("1.jpg");
        //3.调用接口,获取任务响应对象
        ImageAuditingResponse response = client.imageAuditing(request);
        //4调用工具类，获取各审核类型详情集合 (也可自行根据业务解析)
        List<AuditingInfo> imageInfoList = AuditingResultUtil.getImageInfoList(response);
        System.out.println(response);
    }

    /**
     * batchImageAuditing 接口批量创建图片审核任务。 (推荐)
     *
     * @param client
     */
    public static void batchImageAuditing(COSClient client) {
        //1.创建任务请求对象
        BatchImageAuditingRequest request = new BatchImageAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        //2.1设置请求bucket
        request.setBucketName("demo-123456789");
        //2.2添加请求内容
        List<BatchImageAuditingInputObject> inputList = request.getInputList();
        BatchImageAuditingInputObject input = new BatchImageAuditingInputObject();
        input.setObject("1.jpg");
        input.setDataId("DataId");
        inputList.add(input);

        input = new BatchImageAuditingInputObject();
        input.setUrl("https://demo-123456789.cos.ap-chongqing.myqcloud.com/1.png");
        input.setDataId("DataId");

        inputList.add(input);

        //2.2设置审核类型
        request.getConf().setDetectType("all");
        //3.调用接口,获取任务响应对象
        BatchImageAuditingResponse response = client.batchImageAuditing(request);
        List<BatchImageJobDetail> jobList = response.getJobList();
        for (BatchImageJobDetail batchImageJobDetail : jobList) {
            List<AuditingInfo> imageInfoList = AuditingResultUtil.getBatchImageInfoList(batchImageJobDetail);
            System.out.println(imageInfoList);
        }
    }


    /**
     * 批量发送图片审核任务 使用sdk并发发送
     */
    public static void batchPostImageAuditing(COSClient client) throws InterruptedException {
        List<ImageAuditingRequest> requestList = new ArrayList<>();
        ImageAuditingRequest request = new ImageAuditingRequest();
        request.setBucketName("demo-123456789");
        request.setObjectKey("1.png");
        request.setDetectType("all");
        requestList.add(request);

        request = new ImageAuditingRequest();
        request.setBucketName("demo-123456789");
        request.setObjectKey("1.jpg");
        request.setDetectType("all");
        requestList.add(request);

        // 传入一个threadpool, 若不传入线程池, 默认TransferManager中会生成一个单线程的线程池。
        ExecutorService threadPool = Executors.newFixedThreadPool(4);
        TransferManager transferManager = new TransferManager(client, threadPool);
        MultipleImageAuditingImpl multipleImageAuditing = transferManager.batchPostImageAuditing(requestList);
        multipleImageAuditing.waitForCompletion();
        List<ImageAuditingImpl> imageAuditingList = multipleImageAuditing.getImageAuditingList();
        for (ImageAuditingImpl imageAuditing : imageAuditingList) {
            System.out.println(imageAuditing.getState());
            System.out.println(imageAuditing.getResponse());
            System.out.println(imageAuditing.getErrMsg());
        }
        transferManager.shutdownNow();
        client.shutdown();
    }
}
