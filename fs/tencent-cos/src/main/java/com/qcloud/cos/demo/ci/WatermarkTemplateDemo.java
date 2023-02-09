package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.template.*;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 视频水印模板接口相关demo 请求详情参见：https://cloud.tencent.com/document/product/460/46989
 */
public class WatermarkTemplateDemo {
    public static void main(String[] args) throws Exception {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        describeMediaTemplates(client);
    }

    /**
     * CreateMediaTemplate 用于新增视频水印模板。
     * for Text
     * @param client
     */
    public static void createMediaTemplate1(COSClient client) throws UnsupportedEncodingException {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTag("Watermark");
        request.setName("mark-watermark-01");
        MediaWatermark waterMark = request.getWatermark();
        waterMark.setType("Text");
        waterMark.setLocMode("Absolute");
        waterMark.setDx("128");
        waterMark.setDy("128");
        waterMark.setPos("TopRight");
        waterMark.setStartTime("0");
        waterMark.setEndTime("100.5");
        MediaWaterMarkText text = waterMark.getText();
        text.setText("水印内容");
        text.setFontSize("30");
        text.setFontType("simfang.ttf");
        text.setFontColor("0x112233");
        text.setTransparency("30");
        MediaTemplateResponse response = client.createMediaTemplate(request);
        System.out.println(response);
    }

    /**
     * CreateMediaTemplate 用于新增视频水印模板。
     * for Pic
     * @param client
     */
    public static void createMediaTemplate2(COSClient client) throws UnsupportedEncodingException {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTag("Watermark");
        request.setName("mark-test6");
        MediaWatermark waterMark = request.getWatermark();
        waterMark.setType("Image");
        waterMark.setLocMode("Absolute");
        waterMark.setDx("128");
        waterMark.setDy("128");
        waterMark.setPos("TopRight");
        waterMark.setStartTime("0");
        waterMark.setEndTime("100.5");
        MediaWaterMarkImage image = waterMark.getImage();
        image.setUrl("http://DemoBucket-123456789.cos.ap-chongqing.myqcloud.com/1.png");
        image.setMode("Proportion");
        image.setWidth("10");
        image.setHeight("10");
        image.setTransparency("30");

        MediaTemplateResponse response = client.createMediaTemplate(request);
        System.out.println(response);
    }

    /**
     * DeleteMediaTemplate 用于删除视频水印模板。
     *
     * @param client
     */
    public static void deleteMediaTemplate(COSClient client) {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTemplateId("t1c2da66ede73c423bae95d885f7a******");
        Boolean response = client.deleteMediaTemplate(request);
        System.out.println(response);
    }

    /**
     * DescribeMediaTemplates 用于查询视频水印模板。
     *
     * @param client
     */
    public static void describeMediaTemplates(COSClient client) {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        MediaListTemplateResponse response = client.describeMediaTemplates(request);
        List<MediaTemplateObject> templateList = response.getTemplateList();
        for (MediaTemplateObject mediaTemplateObject : templateList) {
            System.out.println(mediaTemplateObject);
        }
    }

    /**
     * UpdateMediaTemplate 用于更新视频水印模板。
     *
     * @param client
     */
    public static void updateMediaTemplate(COSClient client) throws UnsupportedEncodingException {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTemplateId("t1be67e35b0d3b46168cf125f56e*****");
        request.setTag("Watermark");
        request.setName("mark-test-watermark-01");
        MediaWatermark waterMark = request.getWatermark();
        waterMark.setType("Text");
        waterMark.setLocMode("Absolute");
        waterMark.setDx("128");
        waterMark.setDy("128");
        waterMark.setPos("TopRight");
        waterMark.setStartTime("0");
        waterMark.setEndTime("100.5");
        MediaWaterMarkText text = waterMark.getText();
        text.setText("修改水印内容");
        text.setFontSize("30");
        text.setFontType("simfang.ttf");
        text.setFontColor("0x112233");
        text.setTransparency("30");
        Boolean aBoolean = client.updateMediaTemplate(request);
        System.out.println(aBoolean);
    }
}
