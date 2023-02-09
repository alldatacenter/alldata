package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.job.*;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 媒体处理 Concat接口相关demo 详情见https://cloud.tencent.com/document/product/460/49168
 */
public class ConcatDemo {

    public static void main(String[] args) throws Exception {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        createMediaJobs(client);
    }

    /**
     * createMediaJobs 接口用于创建任务。
     * @param client
     */
    public static void createMediaJobs(COSClient client) throws UnsupportedEncodingException {
        //1.创建任务请求对象
        MediaJobsRequest request = new MediaJobsRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        request.setTag("Concat");
        request.getInput().setObject("demo.mp4");
        MediaConcatTemplateObject mediaConcatTemplate = request.getOperation().getMediaConcatTemplate();
        List<MediaConcatFragmentObject> concatFragmentList = mediaConcatTemplate.getConcatFragmentList();
        MediaConcatFragmentObject mediaConcatFragmentObject = new MediaConcatFragmentObject();
        mediaConcatFragmentObject.setMode("Start");
        mediaConcatFragmentObject.setUrl("http://examplebucket-1250000000.cos.ap-chongqing.myqcloud.com/demo1.mp4");
        concatFragmentList.add(mediaConcatFragmentObject);

        mediaConcatFragmentObject = new MediaConcatFragmentObject();
        mediaConcatFragmentObject.setMode("End");
        mediaConcatFragmentObject.setUrl("http://examplebucket-1250000000.cos.ap-chongqing.myqcloud.com/demo2.mp4");
        concatFragmentList.add(mediaConcatFragmentObject);

        MediaAudioObject audio = mediaConcatTemplate.getAudio();
        audio.setCodec("mp3");
        MediaVideoObject video = mediaConcatTemplate.getVideo();
        video.setCodec("H.264");
        video.setBitrate("1000");
        video.setWidth("1280");
        video.setFps("30");

        MediaContainerObject container = mediaConcatTemplate.getContainer();
        container.setFormat("mp4");

        mediaConcatTemplate.setIndex("1");

        request.getOperation().getOutput().setBucket("examplebucket-1250000000");
        request.getOperation().getOutput().setRegion("ap-chongqing");
        request.getOperation().getOutput().setObject("concat.mp4");
        request.setQueueId("p9900025e4ec44b5e8225e70a521*****");
        //3.调用接口,获取任务响应对象
        MediaJobResponse response = client.createMediaJobs(request);
        System.out.println(response);
    }

    /**
     * describeMediaJob 根据jobId查询任务信息
     * @param client
     */
    public static void describeMediaJob(COSClient client)  {
        //1.创建任务请求对象
        MediaJobsRequest request = new MediaJobsRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        request.setJobId("j6fc9306c8bd411eb8b416b8ff9172c91");
        //3.调用接口,获取任务响应对象
        MediaJobResponse response = client.describeMediaJob(request);
        System.out.println(response);
    }

    /**
     * describeMediaJobs 查询任务列表
     * @param client
     */
    public static void describeMediaJobs(COSClient client)  {
        //1.创建任务请求对象
        MediaJobsRequest request = new MediaJobsRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        request.setQueueId("p9900025e4ec44b5e8225e70a521*****");
        request.setTag("Concat");
        request.setSize(100);
        //3.调用接口,获取任务响应对象
        MediaListJobResponse response = client.describeMediaJobs(request);
        List<MediaJobObject> jobsDetail = response.getJobsDetailList();
        for (MediaJobObject mediaJobObject : jobsDetail) {
            System.out.println(mediaJobObject);
        }
    }

    /**
     * cancelMediaJob 取消任务
     * @param client
     */
    public static void cancelMediaJob(COSClient client) {
        //1.创建任务请求对象
        MediaJobsRequest request = new MediaJobsRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("examplebucket-1250000000");
        request.setJobId("jbfb0d02a092111ebb3167781d*****");
        //3.调用接口,获取任务响应对象
        Boolean response = client.cancelMediaJob(request);
        System.out.println(response);
    }
}
