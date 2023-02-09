package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.auditing.AudioAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.AudioAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.AuditingInfo;

import java.util.List;


/**
 * 内容审核 音频审核接口相关demo 详情见https://cloud.tencent.com/document/product/460/53395
 */
public class AudioAuditingJobDemo {

    public static void main(String[] args) throws InterruptedException {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        describeAudioAuditingJob(client);
    }

    /**
     * createImageAuditingJob 接口用于创建音频审核任务。
     *
     * @param client
     */
    public static void createAudioAuditingJobs(COSClient client) {
        //1.创建任务请求对象
        AudioAuditingRequest request = new AudioAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("demo-123456789");
//        request.getInput().setObject("pron.mp3");
        request.getInput().setObject("pron.mp3");
        request.getInput().setDataId("TestDataId");
        request.getConf().setDetectType("all");
        request.getConf().setCallback("http://cloud.tencent.com/");
        //3.调用接口,获取任务响应对象
        AudioAuditingResponse response = client.createAudioAuditingJobs(request);
        System.out.println(response);
    }

    /**
     * describeAudioAuditingJob 接口用于创建音频审核任务。
     *
     * @param client
     */
    public static void describeAudioAuditingJob(COSClient client) throws InterruptedException {
        //1.创建任务请求对象
        AudioAuditingRequest request = new AudioAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("demo-123456789");
        request.setJobId("sa9175bc451c4b11ecb3fa5254009*****");
        while (true) {
            //3.调用接口,获取任务响应对象
            AudioAuditingResponse response = client.describeAudioAuditingJob(request);
            String state = response.getJobsDetail().getState();
            if ("Success".equalsIgnoreCase(state) || "Failed".equalsIgnoreCase(state)) {
                System.out.println(response.getRequestId());
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
