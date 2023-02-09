package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.common.MediaOutputObject;
import com.qcloud.cos.model.ciModel.job.DocHtmlRequest;
import com.qcloud.cos.model.ciModel.job.DocJobDetail;
import com.qcloud.cos.model.ciModel.job.DocJobListRequest;
import com.qcloud.cos.model.ciModel.job.DocJobListResponse;
import com.qcloud.cos.model.ciModel.job.DocJobObject;
import com.qcloud.cos.model.ciModel.job.DocJobRequest;
import com.qcloud.cos.model.ciModel.job.DocJobResponse;
import com.qcloud.cos.model.ciModel.job.DocProcessObject;
import com.qcloud.cos.model.ciModel.queue.DocListQueueResponse;
import com.qcloud.cos.model.ciModel.queue.DocQueueRequest;
import com.qcloud.cos.model.ciModel.queue.MediaQueueObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 文档预览任务相关demo
 */
public class DocJobDemo {
    public static void main(String[] args) throws MalformedURLException, URISyntaxException, InterruptedException {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        createDocJobs(client);
    }

    /**
     * createDocJobs 接口用于创建异步文档预览任务。
     * 将文档转为指定类型（jpg、png、pdf）并保存至指定的cos路径下
     * 该接口为发送任务，如果需要获取转换结果 需要调用查询接口。
     *
     * @param client
     */
    public static void createDocJobs(COSClient client) {
        //1.创建任务请求对象
        DocJobRequest request = new DocJobRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        DocJobObject docJobObject = request.getDocJobObject();
        docJobObject.setTag("DocProcess");
        docJobObject.getInput().setObject("demo.docx");
        docJobObject.setQueueId("pc02270c617ae4b6d9b0a52cb1c*****");
        DocProcessObject docProcessObject = docJobObject.getOperation().getDocProcessObject();
        docProcessObject.setQuality("100");
        docProcessObject.setZoom("100");
        docProcessObject.setStartPage("1");
        docProcessObject.setEndPage("3");
        docProcessObject.setTgtType("png");
        docProcessObject.setDocPassword("123");
        MediaOutputObject output = docJobObject.getOperation().getOutput();
        output.setRegion("ap-chongqing");
        output.setBucket("examplebucket-1250000000");
        output.setObject("mark/pic-${Page}.jpg");
        //3.调用接口,获取任务响应对象
        DocJobResponse docProcessJobs = client.createDocProcessJobs(request);
        System.out.println(docProcessJobs);
    }

    /**
     * describeMediaJob 根据jobId查询任务信息
     *
     * @param client
     */
    public static void describeMediaJob(COSClient client) {
        //1.创建任务请求对象
        DocJobRequest request = new DocJobRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        request.setJobId("d75b6ea083df711eb8d09476dfb8*****");
        //3.调用接口,获取任务响应对象
        DocJobResponse docJobResponse = client.describeDocProcessJob(request);
        System.out.println(docJobResponse);
    }

    /**
     * describeMediaJobs 查询任务列表
     *
     * @param client
     */
    public static void describeMediaJobs(COSClient client) {
        //1.创建任务请求对象
        DocJobListRequest request = new DocJobListRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        request.setQueueId("pc02270c617ae4b6d9b0a52cb1c*****");
        request.setTag("DocProcess");
        request.setStartCreationTime("2020-12-10T16:20:07+0800");
        //3.调用接口,获取任务响应对象
        DocJobListResponse docJobResponse = client.describeDocProcessJobs(request);
        for (DocJobDetail jobDetail : docJobResponse.getDocJobDetailList()) {
            System.out.println(jobDetail);
        }
    }

    /**
     * GenerateDocPreviewUrl 生成文档预览同步请求预览地址
     * https://cloud.tencent.com/document/product/460/47074
     */
    public static void generateDocPreviewUrl(COSClient client) throws URISyntaxException {
        //1.创建任务请求对象
        DocHtmlRequest request = new DocHtmlRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("markjrzhang-1251704708");
        request.setType(DocHtmlRequest.DocType.html);
        request.setObjectKey("1.pptx");
        //3.调用接口,获取任务响应对象
        String previewUrl = client.GenerateDocPreviewUrl(request);
        System.out.println(previewUrl);
    }

    /**
     * 文档预览完整调用demo
     */
    public static void processDocJob(COSClient client) throws InterruptedException {
        String bucketName = "demo-123456789";
        //1.获取队列id,需要先开启文档预览功能。
        DocQueueRequest queueRequest = new DocQueueRequest();
        queueRequest.setBucketName(bucketName);
        DocListQueueResponse response = client.describeDocProcessQueues(queueRequest);
        List<MediaQueueObject> queueList = response.getQueueList();
        String queueId = "";
        if (queueList.size() != 0) {
            MediaQueueObject mediaQueueObject = queueList.get(0);
            queueId = mediaQueueObject.getQueueId();
        } else {
            System.out.println("获取队列失败");
            return;
        }
        //2.发送文档预览任务
        //2.1添加请求参数 参数详情请见api接口文档
        DocJobRequest request = new DocJobRequest();
        request.setBucketName(bucketName);
        DocJobObject docJobObject = request.getDocJobObject();
        docJobObject.setTag("DocProcess");
        docJobObject.getInput().setObject("1.pdf");
        docJobObject.setQueueId(queueId);
        DocProcessObject docProcessObject = docJobObject.getOperation().getDocProcessObject();
        docProcessObject.setQuality("100");
        docProcessObject.setZoom("100");
        docProcessObject.setEndPage("-1");
        docProcessObject.setTgtType("jpg");
        MediaOutputObject output = docJobObject.getOperation().getOutput();
        output.setRegion(client.getClientConfig().getRegion().getRegionName());
        output.setBucket(bucketName);
        output.setObject("demo/pic-${Number}.jpg");
        //2.2发送预览请求
        DocJobResponse docProcessJobs = client.createDocProcessJobs(request);

        //3.轮询查询任务结果（也可以配置队列的回调url,使用回调接口获取任务结果）
        DocJobRequest docJobRequest = new DocJobRequest();
        docJobRequest.setBucketName(bucketName);
        String jobId = docProcessJobs.getJobsDetail().getJobId();
        docJobRequest.setJobId(jobId);
        while (true) {
            DocJobResponse docJobResponse = client.describeDocProcessJob(docJobRequest);
            String state = docJobResponse.getJobsDetail().getState();
            //判断任务状态
            if ("Success".equalsIgnoreCase(state) || "Failed".equalsIgnoreCase(state)) {
                //处理业务逻辑
                System.out.println(docJobResponse);
                break;
            } else {
                Thread.sleep(500);
            }
        }
    }
}
