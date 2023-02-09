package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.auditing.WebpageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.WebpageAuditingResponse;
import com.qcloud.cos.utils.Jackson;



/**
 * 网页审核相关demo 详情见https://cloud.tencent.com/document/product/460/63968
 */
public class WebpageAuditingJobDemo {

    public static void main(String[] args) throws InterruptedException {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        describeWebpageAuditingJob(client);
    }

    /**
     * createWebpageAuditingJob 接口用于提交网页审核任务。
     *
     * @param client
     */
    public static void createWebpageAuditingJob(COSClient client) {
        //1.创建任务请求对象
        WebpageAuditingRequest request = new WebpageAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("demo-123456789");
//        request.getInput().setObject("pron.mp3");
        request.getInput().setUrl("https://console.cloud.tencent.com/");
        request.getConf().setDetectType("all");
        //3.调用接口,获取任务响应对象
        WebpageAuditingResponse response = client.createWebpageAuditingJob(request);
        System.out.println(response);
    }

    /**
     * describeWebpageAuditingJob 接口用于查询网页审核任务。
     *
     * @param client
     */
    public static void describeWebpageAuditingJob(COSClient client) throws InterruptedException {
        //1.创建任务请求对象
        WebpageAuditingRequest request = new WebpageAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("demo-123456789");
        request.setJobId("sh1acd0928572f11ecaf945254008*****");
        while (true) {
            //3.调用接口,获取任务响应对象
            WebpageAuditingResponse response = client.describeWebpageAuditingJob(request);
            String state = response.getJobsDetail().getState();
            if ("Success".equalsIgnoreCase(state) || "Failed".equalsIgnoreCase(state)) {
                System.out.println(Jackson.toJsonString(response));
                break;
            }
            Thread.sleep(100);
        }

    }
}
