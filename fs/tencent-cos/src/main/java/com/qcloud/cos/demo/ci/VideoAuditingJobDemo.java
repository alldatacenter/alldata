package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.auditing.AuditingInfo;
import com.qcloud.cos.model.ciModel.auditing.VideoAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.VideoAuditingResponse;

import java.util.List;

/**
 * 内容审核 视频审核接口相关demo 详情见https://cloud.tencent.com/document/product/460/46427
 */
public class VideoAuditingJobDemo {

    public static void main(String[] args) throws InterruptedException {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        describeAuditingJob(client);
    }

    /**
     * createVideoAuditingJob 接口用于创建视频审核任务。
     */
    public static void createVideoAuditingJob(COSClient client) {
        //1.创建任务请求对象
        VideoAuditingRequest request = new VideoAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("demo-123456789");
        //2.1.对象地址或直接传入审核资源url
        request.getInput().setObject("1.mp4");
//        request.getInput().setUrl("https://demo-123456789.cos.ap-beijing.myqcloud.com/1.mp4");
        request.getConf().setDetectType("all");
        request.getConf().getSnapshot().setCount("10");
        request.getConf().getSnapshot().setMode("Interval");
        request.getConf().getSnapshot().setTimeInterval("10");
        //3.调用接口,获取任务响应对象
        VideoAuditingResponse response = client.createVideoAuditingJob(request);
        System.out.println(response);
    }

    /**
     * DescribeAuditingJob 接口用于创建视频审核任务。
     *
     * @param client
     */
    public static void describeAuditingJob(COSClient client) throws InterruptedException {
        //1.创建任务请求对象
        VideoAuditingRequest request = new VideoAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("demo-123456789");
        request.setJobId("av2628fdd21c4d11ecb3fa5254009*****");
        //3.调用接口,获取任务响应对象
        while (true) {
            //3.调用接口,获取任务响应对象
            VideoAuditingResponse response = client.describeAuditingJob(request);
            String state = response.getJobsDetail().getState();
            if ("Success".equalsIgnoreCase(state) || "Failed".equalsIgnoreCase(state)) {
                System.out.println(response.getRequestId());
                System.out.println(state);
                System.out.println(response.getJobsDetail());
                //4.根据业务逻辑进行处理结果，此处工具类处理操作仅供参考。
                List<AuditingInfo> auditingInfoList = AuditingResultUtil.getAuditingInfoList(response.getJobsDetail());
                System.out.println(auditingInfoList);
                break;
            }
            Thread.sleep(100);
        }
    }
}
