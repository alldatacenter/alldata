package com.qcloud.cos.demo.ci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.template.MediaListTemplateResponse;
import com.qcloud.cos.model.ciModel.template.MediaTemplateObject;
import com.qcloud.cos.model.ciModel.template.MediaTemplateRequest;
import com.qcloud.cos.model.ciModel.template.MediaTemplateResponse;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 截图模板接口相关demo 请求详情参见：https://cloud.tencent.com/document/product/460/46994
 */
public class SnapshotTemplateDemo {
    public static void main(String[] args) throws Exception {
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSClient client = ClientUtils.getTestClient();
        // 2 调用要使用的方法。
        describeMediaTemplates(client);
    }

    /**
     * CreateMediaTemplate 用于新增截图模板。
     *
     * @param client
     */
    public static void createMediaTemplate(COSClient client) throws UnsupportedEncodingException {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTag("Snapshot");
        request.setName("mark-test-1");
        request.getSnapshot().setMode("Interval");
        request.getSnapshot().setWidth("1280");
        request.getSnapshot().setStart("0");
        request.getSnapshot().setCount("1");
        //3.调用接口,获取模板响应对象
        MediaTemplateResponse response = client.createMediaTemplate(request);
        System.out.println(response);
    }

    /**
     * DeleteMediaTemplate 用于删除截图模板。
     *
     * @param client
     */
    public static void deleteMediaTemplate(COSClient client) {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTemplateId("t19c4a60ae1a694621a01f0c7130c*****");
        //3.调用接口,获取模板响应对象
        Boolean response = client.deleteMediaTemplate(request);
        System.out.println(response);
    }

    /**
     * DescribeMediaTemplates 用于查询截图模板。
     *
     * @param client
     */
    public static void describeMediaTemplates(COSClient client) {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTag("Snapshot");
        //3.调用接口,获取模板响应对象
        MediaListTemplateResponse response = client.describeMediaTemplates(request);
        List<MediaTemplateObject> templateList = response.getTemplateList();
        for (MediaTemplateObject mediaTemplateObject : templateList) {
            System.out.println(mediaTemplateObject);
        }
        System.out.println(response);
    }

    /**
     * UpdateMediaTemplate 用于更新截图模板。
     *
     * @param client
     */
    public static void updateMediaTemplate(COSClient client) throws UnsupportedEncodingException {
        //1.创建模板请求对象
        MediaTemplateRequest request = new MediaTemplateRequest();
        //2.添加请求参数 参数详情请见api接口文档
        request.setBucketName("DemoBucket-123456789");
        request.setTemplateId("t1e9679e24887e401eb3f46282fb9*****");
        request.setTag("Snapshot");
        request.setName("mark-test-update");
        request.getSnapshot().setMode("Interval");
        request.getSnapshot().setWidth("1280");
        request.getSnapshot().setStart("0");
        request.getSnapshot().setCount("2");
        //3.调用接口,获取模板响应对象
        Boolean aBoolean = client.updateMediaTemplate(request);
        System.out.println(aBoolean);
    }
}
