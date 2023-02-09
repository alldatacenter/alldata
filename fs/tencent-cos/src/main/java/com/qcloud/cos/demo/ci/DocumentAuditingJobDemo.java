package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.auditing.DocumentAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.DocumentAuditingResponse;


/**
 * 内容审核 文档审核接口相关demo 详情见https://cloud.tencent.com/document/product/460/59380
 */
public class DocumentAuditingJobDemo {

    public static void main(String[] args) {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        describeAuditingDocumentJob(client);
    }

    /**
     * createAuditingTextJobs 接口用于创建文档审核任务。
     */
    public static void createAuditingDocumentJobs(COSClient client) {
        //1.创建任务请求对象
        DocumentAuditingRequest request = new DocumentAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("demo-123456789");
        //2.1.1设置对象地址
//        request.getInput().setObject("1.txt");
        //2.1.2或直接设置请求内容,文本内容的Base64编码
        request.getInput().setUrl("https://demo-123456789.cos.ap-chongqing.myqcloud.com/test.docx");
        //2.2设置审核类型参数
        request.getConf().setDetectType("all");
        //2.3设置审核模板（可选）
//        request.getConf().setBizType("aa3e9d84a6a079556b0109a935c*****");
        //3.调用接口,获取任务响应对象
        DocumentAuditingResponse response = client.createAuditingDocumentJobs(request);
    }

    /**
     * DescribeAuditingTextJob 接口用于查询文档审核任务。
     *
     * @param client
     */
    public static void describeAuditingDocumentJob(COSClient client) {
        //1.创建任务请求对象
        DocumentAuditingRequest request = new DocumentAuditingRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("demo-123456789");
        request.setJobId("sd0312aa91510711eca163525400863904");
        //3.调用接口,获取任务响应对象
        DocumentAuditingResponse response = client.describeAuditingDocumentJob(request);
    }
}
