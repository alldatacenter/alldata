package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.job.MediaAudioObject;
import com.qcloud.cos.model.ciModel.job.MediaTimeIntervalObject;
import com.qcloud.cos.model.ciModel.job.MediaTransConfigObject;
import com.qcloud.cos.model.ciModel.job.MediaVideoObject;
import com.qcloud.cos.model.ciModel.template.MediaListTemplateResponse;
import com.qcloud.cos.model.ciModel.template.MediaTemplateObject;
import com.qcloud.cos.model.ciModel.template.MediaTemplateRequest;
import com.qcloud.cos.model.ciModel.template.MediaTemplateResponse;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 转码模板接口相关demo 请求详情参见：https://cloud.tencent.com/document/product/460/46999
 */
public class TranscodeTemplateDemo {
    public static void main(String[] args) throws Exception {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        describeMediaTemplates(client);
    }

    /**
     * CreateMediaTemplate 用于新增转码模板。
     *
     * @param client
     */
    public static void createMediaTemplate(COSClient client) throws UnsupportedEncodingException {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTag("Transcode");
        request.setName("mark-test-2");
        request.getContainer().setFormat("mp4");
        //2.1 添加video对象的值
        MediaVideoObject video = request.getVideo();
        video.setCodec("H.264");
        video.setProfile("high");
        video.setBitrate("1000");
        video.setWidth("1280");
        video.setFps("30");
        video.setPreset("medium");
        video.setBufSize("1000");
        video.setMaxrate("10");
        //2.2  添加audio对象的值
        MediaAudioObject audio = request.getAudio();
        audio.setCodec("aac");
        audio.setSamplerate("44100");
        audio.setBitrate("128");
        audio.setChannels("1");
        //2.3 添加TransConfig对象的值
        MediaTransConfigObject transConfig = request.getTransConfig();
        transConfig.setAdjDarMethod("scale");
        transConfig.setIsCheckReso("false");
        transConfig.setResoAdjMethod("1");
        //2.4 添加TimeInterval对象的值
        MediaTimeIntervalObject timeInterval = request.getTimeInterval();
        timeInterval.setStart("0");
        timeInterval.setDuration("60");
        //3. 调用接口 获取模板响应对象
        MediaTemplateResponse response = client.createMediaTemplate(request);
        System.out.println(response);
    }

    /**
     * DeleteMediaTemplate 用于删除转码模板。
     *
     * @param client
     */
    public static void deleteMediaTemplate(COSClient client) {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTemplateId("t1619b0381be1a46738796e97fff4*****");
        //3.调用接口,获取模板响应对象
        Boolean response = client.deleteMediaTemplate(request);
        System.out.println(response);
    }

    /**
     * DescribeMediaTemplates 用于查询转码模板。
     *
     * @param client
     */
    public static void describeMediaTemplates(COSClient client) {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTag("Transcode");
        //3.调用接口,获取模板响应对象
        MediaListTemplateResponse response = client.describeMediaTemplates(request);
        List<MediaTemplateObject> templateList = response.getTemplateList();
        for (MediaTemplateObject mediaTemplateObject : templateList) {
            System.out.println(mediaTemplateObject);
        }
    }

    /**
     * UpdateMediaTemplate 用于更新转码模板。
     *
     * @param client
     */
    public static void updateMediaTemplate(COSClient client) throws UnsupportedEncodingException {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTemplateId("t138b37dc82e59422d85f03bb7a8*****");
        request.setTag("Transcode");
        request.setName("mark-test-update-01");
        request.getContainer().setFormat("mp4");
        MediaVideoObject video = request.getVideo();
        video.setCodec("H.264");
        video.setProfile("high");
        video.setBitrate("1000");
        video.setWidth("1280");
        video.setFps("30");
        video.setPreset("medium");
        video.setBufSize("1000");
        video.setMaxrate("10");
        MediaAudioObject audio = request.getAudio();
        audio.setCodec("aac");
        audio.setSamplerate("44100");
        audio.setBitrate("128");
        audio.setChannels("1");
        MediaTransConfigObject transConfig = request.getTransConfig();
        transConfig.setAdjDarMethod("scale");
        transConfig.setIsCheckReso("false");
        transConfig.setResoAdjMethod("1");
        MediaTimeIntervalObject timeInterval = request.getTimeInterval();
        timeInterval.setStart("1");
        timeInterval.setDuration("30");
        //3.调用接口,获取模板响应对象
        Boolean aBoolean = client.updateMediaTemplate(request);
        System.out.println(aBoolean);
    }
}
